/*
 * SOLTIX - Scalable automated framework for testing Solidity compilers.
 *
 * Author: Nils Weller <nweller@uni-bremen.de>
 *
 * Copyright (C) 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package soltix.interpretation;

import soltix.ast.*;
import soltix.interpretation.values.Value;
import soltix.interpretation.variables.*;
import soltix.util.Util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to represent the current scope - all variables visible in the current AST
 * context, adapted as AST nodes are traversed
 */
public class Scope {
    // HashMap of all variables in the current scope. Lookup per variable ID (strings would
    // be ambiguous due to hidden outer declarations)
    private HashMap<Long, Variable> variablesById = new HashMap<Long, Variable>();
    private HashMap<String, Variable> variablesByName = new HashMap<String, Variable>();
    // During interpretation, a scope may be instantiated with a VariableEnvironment containing values for the variables
    private VariableEnvironment variableEnvironment = null;

    private ASTContractDefinition contract = null;
    private FunctionScope currentFunction = null; // function or modifier definition

    public ASTContractDefinition getContract() { return contract; }
    public FunctionScope getCurrentFunction() { return currentFunction; }

    // Variables by ID/name (only those that have been encountered so far)
    public HashMap<Long, Variable> getVariablesById() { return variablesById; }
    public HashMap<String, Variable> getVariablesByName() { return variablesByName; }

    // Variables in the current function (if any), regardless of whether they have
    // been encountered so far
    public boolean currentFunctionContainsVariable(String name) {
        return currentFunction != null && currentFunction.containsVariable(name);
    }
    public ASTNode currentFunctionVariableLookup(String name) {
        if (currentFunction == null) {
            return null;
        }
        return currentFunction.currentFunctionVariableLookup(name);
    }

    public Scope(ASTContractDefinition contract) throws Exception {
        // Initialize scope with all variables declared in the contract
        this.contract = contract;
        for (ASTNode variable : contract.getVariables()) { // TODO better solution? This meshes global + given scope
            enterNode(variable, getInitializer(variable));
        }
    }

    public void setVariableEnvironment(VariableEnvironment variableEnvironment) {
        this.variableEnvironment = variableEnvironment;
    }

    public VariableEnvironment getVariableEnvironment() {
        return variableEnvironment;
    }

    protected void addVariableToScope(ASTNode node, Value initializer) throws Exception {
        ASTVariableDeclaration declaration;

        if (node instanceof ASTVariableDeclaration) {
            declaration = (ASTVariableDeclaration)node;
        } else if (node instanceof ASTVariableDeclarationStatement) {
            declaration = ((ASTVariableDeclarationStatement)node).getDeclaration();
        } else {
            throw new Exception("addVariableToScope: Received unexpected node type " + node.getClass().getName());
        }

        if (declaration.isInternalVariable()) {
            // This variable for internal use may follow different rules and should not be included in any processing
            return;
        }

        Variable variable = new Variable(declaration);
        variablesById.put(declaration.getID(), variable);
        if (variablesByName.containsKey(declaration.getName())) {
            // This must be an inner declaration that shadows the outer one
            Variable outerVariable = variablesByName.get(declaration.getName());
            if (outerVariable.getShadowNode() != null) {
                throw new Exception("Variable " + outerVariable.getName() + " shadowed more than once");
            }

            outerVariable.setShadowNode(declaration);
        } else {
            variablesByName.put(declaration.getName(), variable);
        }

        if (variableEnvironment != null) {
            if (initializer == null) {
                // TODO use default initializer, unify this with other unimplemented initializer part
                System.out.println("cannot add " + declaration.getName());
                Util.unimpl();
            }
            VariableValues values = new VariableValues(variable, 0);
            values.addValue(initializer);
            variableEnvironment.addVariableValues(variable, values);
        }
    }

    private static Value getInitializer(ASTNode node) {
        if (node instanceof  ASTVariableDeclaration) {
            return ((ASTVariableDeclaration)node).getInitializerValue();
        } else if (node instanceof  ASTVariableDeclarationStatement) {
            return ((ASTVariableDeclarationStatement)node).getDeclaration().getInitializerValue();
        } else {
            return null;
        }
    }

    public void enterNode(ASTNode node, Value initializer) throws Exception {
        if (variablesById.containsKey(node.getID())) {
            throw new Exception("AST node " + node.getID() + " already contained in scope");
        }
        if (node instanceof ASTVariableDeclaration) {
            addVariableToScope(node, initializer);
        } else if (node instanceof ASTVariableDeclarationStatement) {
            addVariableToScope(node, initializer);
        } else if (node instanceof ASTFunctionDefinition || node instanceof ASTModifierDefinition) {
            currentFunction = (FunctionScope)node;

            ASTParameterList parameters = currentFunction.getParameterList();

            // Add parameters to scope
            for (int i = 0; i < parameters.getChildCount(); ++i) {
                ASTNode child = parameters.getChild(i);
                if (!child.getName().equals("")) { // No point in tracking unnamed arguments
                    addVariableToScope(child, getInitializer(child)); // TODO convert type if needed
                }
            }
        } else if (node instanceof ASTForStatement) {
            ASTForStatement forStatement = (ASTForStatement)node;
            ASTNode initPart = forStatement.getInitPart();
            if (initPart instanceof ASTVariableDeclaration || initPart instanceof ASTVariableDeclarationStatement) {
                addVariableToScope(initPart, getInitializer(initPart));
            }
        }
    }

    protected void removeVariableFromScope(ASTNode node) throws Exception {
        ASTVariableDeclaration declaration;

        if (node instanceof ASTVariableDeclaration) {
            declaration = (ASTVariableDeclaration)node;
        } else if (node instanceof ASTVariableDeclarationStatement) {
            declaration = ((ASTVariableDeclarationStatement)node).getDeclaration();
        } else {
            throw new Exception("removeVariableFromScope: Received unexpected node type " + node.getClass().getName());
        }

        if (declaration.isInternalVariable()) {
            // This variable for internal use may follow different rules and should not be included in any processing
            return;
        }

        variablesById.remove(declaration.getID());

        if (declaration.getName().equals("")) {
            return;
        }

        if (variablesByName.get(declaration.getName()).getDeclaration().getID() == declaration.getID()) {
            variablesByName.remove(declaration.getName());
        }
    }

    public void leaveNode(ASTNode node) throws Exception {
        if (node instanceof ASTFunctionDefinition || node instanceof ASTModifierDefinition) {
            assert currentFunction == node;

            ASTParameterList parameters = currentFunction.getParameterList();
            for (int i = 0; i < parameters.getChildCount(); ++i) {
                ASTNode child = parameters.getChild(i);
                removeVariableFromScope(child);
            }
            // Having left the current function, we can now remove all shadow declarations
            // (function-internal declarations hiding declarations in the containing contract)
            for (Variable v : variablesByName.values()) {
                if (v.getShadowNode() != null) {
                    v.setShadowNode(null);
                }
            }

            /*
            // We can now remove all items that have been marked for removal as well
            for (ASTNode nodeToRemove : markedForRemoval) {
                removeVariableFromScope(nodeToRemove);
            }
*/
            currentFunction = null;
        } else if (node instanceof ASTBlock) {
            for (int i = 0; i < node.getChildCount(); ++i) {
                ASTNode child = node.getChild(i);
                if (child instanceof ASTVariableDeclaration || child instanceof ASTVariableDeclarationStatement) {
                    removeVariableFromScope(child);
                }
            }
        } else if (node instanceof ASTForStatement) {
            ASTForStatement forStatement = (ASTForStatement)node;
            ASTNode initPart = forStatement.getInitPart();
            if (initPart instanceof ASTVariableDeclaration || initPart instanceof ASTVariableDeclarationStatement) {
                removeVariableFromScope(initPart);
            }

            // The body of a for statement may be a declaration without containing block as well (test case 1175)
            ASTNode body = forStatement.getBody();
            if (body instanceof ASTVariableDeclaration || body instanceof ASTVariableDeclarationStatement) {
                removeVariableFromScope(body);
            }
        } else if (node instanceof ASTIfStatement) {
            ASTIfStatement ifStatement = (ASTIfStatement)node;
            // The body of an if statement may be a declaration without containing block as well
            ASTNode body = ifStatement.getIfBranch();
            if (body instanceof ASTVariableDeclaration || body instanceof ASTVariableDeclarationStatement) {
                removeVariableFromScope(body);
            }
            body = ifStatement.getElseBranch();
            if (body != null && (body instanceof ASTVariableDeclaration || body instanceof ASTVariableDeclarationStatement)) {
                removeVariableFromScope(body);
            }
        } else if (node instanceof ASTWhileStatement) {
            ASTWhileStatement whileStatement = (ASTWhileStatement)node;
            ASTNode body = whileStatement.getBody();
            // The body of a while statement may be a declaration without containing block as well
            if (body instanceof ASTVariableDeclaration || body instanceof ASTVariableDeclarationStatement) {
                removeVariableFromScope(body);
            }
        } else if (node instanceof ASTDoWhileStatement) {
            ASTDoWhileStatement doWhileStatement = (ASTDoWhileStatement)node;
            ASTNode body = doWhileStatement.getBody();
            // The body of a do-while statement may be a declaration without containing block as well
            if (body instanceof ASTVariableDeclaration || body instanceof ASTVariableDeclarationStatement) {
                removeVariableFromScope(body);
            }
        }
    }


}
