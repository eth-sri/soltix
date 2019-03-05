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
package soltix.ast;

import soltix.Configuration;
import soltix.synthesis.TransactionGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


public class AST {
    // Tree root node
    private ASTNode root = null;
    // Current context node
    private ASTNode currentNode = null;
    // Nesting depth in code (-1 = SourceUnit, 0 = ContractDefinition, etc.)
    private int currentDepth = -1;
    private int currentNodeID = 0;
    // Current contract or null
    private ASTContractDefinition currentContract = null;

    // Current containing function or modifier definition, if any, so that we can
    // pre-record all variables in each function to resolve scope issues that will
    // subsequently arise as we start processing AST nodes that are affected by
    // variables defined in code that is encountered later:
    //    function f() { use(x);  { var x = val; } }
    // or (in the modifier case) compute the stack size when generating profiling
    // events
    private /*ASTFunctionDefinition*/FunctionScope currentFunction = null;

    private ArrayList<ASTContractDefinition> contracts = new ArrayList<ASTContractDefinition>();
    private HashMap<String, ASTEnumDefinition> enumDefinitions = new HashMap<String, ASTEnumDefinition>();
    private HashMap<String, ASTStructDefinition> structDefinitions  = new HashMap<String, ASTStructDefinition>();
    private ArrayList<ASTVariableDeclaration> topLevelVariables = new ArrayList<ASTVariableDeclaration>();
    private ArrayList<FunctionScope> functions = new ArrayList<FunctionScope>();
    private HashMap<Long, ASTVariableDeclaration> allVariablesById = new HashMap<Long, ASTVariableDeclaration>();
    private boolean functionsSortedBySize = false;

    // All nodes stored in this AST, indexed by their ID
    private HashMap<Long, ASTNode> nodesById = new HashMap<Long, ASTNode>();

    public ArrayList<ASTContractDefinition> getContracts() { return contracts; }
    public ASTContractDefinition getContract(String name) {
        if (contracts != null) {
            for (ASTContractDefinition contract : contracts) {
                if (contract.getName().equals(name)) {
                    return contract;
                }
            }
        }
        return null;
    }

    public ASTEnumDefinition getEnumDefinition(String name) { return enumDefinitions.get(name); }
    public ASTStructDefinition getStructDefinition(String name) { return structDefinitions.get(name); }
    public Collection<ASTStructDefinition> getStructDefinitions() { return structDefinitions.values(); }

    public ASTNode getRoot() { return root; }
    public ASTNode getCurrentNode() { return currentNode; }
    public void setCurrentNode(ASTNode currentNode) { this.currentNode = currentNode; }

    public ASTNode getNodeById(long id) throws Exception {
        if (!nodesById.containsKey(id)) {
            throw new Exception("AST request for unknown node ID " + id);
        }
        return nodesById.get(id);
    }

    public ArrayList<FunctionScope> getFunctions() { return functions; }

    public ASTFunctionDefinition getFunctionByStatementId(long statementId) throws Exception {
        ASTNode node = getNodeById(statementId);
        while (node.getParent() != null) {
            node = node.getParent();
            if (node instanceof ASTFunctionDefinition) {
                return (ASTFunctionDefinition)node;
            }
        }
        throw new Exception("Cannot locate containing function for statement " + statementId);
    }

    public ASTVariableDeclaration getVariableById(long id) {
        return allVariablesById.get(id);
    }

    // Can be used to add a struct definition while generating contracts - bypassing child node + finalize() structures
    public void addStructDefinition(ASTStructDefinition definition) {
        structDefinitions.put(definition.getName(), definition);
    }

    // Add new inner node that will be followed by one or more further child nodes,
    // e.g. a while loop that will later receive a body
    public void addInnerNode(ASTNode node) throws Exception {
        if (root == null) {
            root = node;
        }
        if (currentNode != null) {
            currentNode.addChildNode(node);
        }

        node.setDepth(currentDepth);
        //node.setID(currentNodeID); // TODO: We should probably propagate solc's node ID instead of rolling our own

        nodesById.put(node.getID(), node);
        node.setAST(this);

        //++currentNodeID;
        ++currentDepth;
        // The next sub node will extend this one
        currentNode = node;

        if (node instanceof ASTContractDefinition) {
            currentContract = (ASTContractDefinition)node;
            contracts.add(currentContract);
        } else if (node instanceof ASTVariableDeclaration) {
            ASTVariableDeclaration declaration = (ASTVariableDeclaration)node;
            // ASTVariableDeclaration and ASTVariableDeclarationStatement are both relevant in principle,
            // but since the latter contains the former as a child node (encountered later) as well, we
            // can just process that one and ignore the statement
            if (!declaration.isInternalVariable()) { // Exclude helper variables
                if (currentFunction != null) {
                    // Note: We cannot properly distinguish between a Declaration and DeclarationStatement yet,
                    // because the latter doesn't have the Declaration part in this context yet
                    currentFunction.addVariable(declaration);
                } else {
                    topLevelVariables.add(declaration);
                }
                allVariablesById.put(node.getID(), (ASTVariableDeclaration) node);
            }
            //} else if (node instanceof ASTFunctionCall) {
        } else if (node instanceof ASTFunctionCall) {
            ASTFunctionCall call = (ASTFunctionCall)node;
            call.setContract(currentContract);
        } else if (node instanceof ASTIdentifier) {
            ASTIdentifier identifier = (ASTIdentifier)node;
            identifier.setContract(currentContract);
        } else if (node instanceof ASTMemberAccess) {
            ASTMemberAccess memberAccess = (ASTMemberAccess)node;
            memberAccess.setContract(currentContract);
        } else if (node instanceof ASTThrow) {
            ASTThrow throwStatement = (ASTThrow)node;
            throwStatement.setContract(currentContract);
        } else if (node instanceof ASTEnumDefinition) {
            enumDefinitions.put(node.getName(), (ASTEnumDefinition) node);
        } else if (node instanceof ASTStructDefinition) {
            structDefinitions.put(node.getName(), (ASTStructDefinition)node);
        } else if (node instanceof ASTIndexAccess) {
            currentContract.addIndexAccess((ASTIndexAccess)node);
        }

        if (node instanceof ASTFunctionDefinition || node instanceof ASTModifierDefinition) {
            currentFunction = (FunctionScope)node;
            functions.add(currentFunction);
        } else if (currentFunction != null) {
            currentFunction.countNode(node);
        }
    }

    // Terminate current inner node, e.g. a while loop whose body is completed
    public void completeInnerNode() throws Exception {
        currentNode.finalize();
        if (currentNode instanceof ASTFunctionDefinition || currentNode instanceof ASTModifierDefinition) {
            currentFunction = null;
        } else if (currentNode instanceof ASTContractDefinition) {
            currentContract = null;
        }
        currentNode = currentNode.getParent();
        --currentDepth;
        assert currentDepth >= 0;
    }

    public void sortFunctionsBySize() {
        Collections.sort(functions, Collections.reverseOrder());
        if (Configuration.verboseDebugOutput) {
            System.out.println(" === functions by size ===");
            for (FunctionScope function : functions) {
                System.out.println(function.getName() + ": " + function.getBodyNodeCount());
            }
        }
        functionsSortedBySize = true;
    }

    public int getTotalFunctionBodyNodeCount() {
        int total = 0;
        for (FunctionScope function : functions) {
            total += function.getBodyNodeCount();
        }
        return total;
    }

    public void sortContractsBySize() {
        Collections.sort(contracts, Collections.reverseOrder());
        if (Configuration.verboseDebugOutput) {
            System.out.println(" === contracts by size ===");
            for (ASTContractDefinition contract : contracts) {
                System.out.println(contract.getName() + ": " + contract.getFunctionBodyNodeCount());
            }
        }
    }

    public ArrayList<ASTFunctionDefinition> getTransactableFunctions(ASTContractDefinition contract, boolean avoidAddressArguments) throws Exception {
        ArrayList<FunctionScope> allFunctions = getFunctions();
        ArrayList<ASTFunctionDefinition> result = new ArrayList<ASTFunctionDefinition>();

        if (!functionsSortedBySize) {
            throw new Exception("AST.getLargestFunction() may only be called after preceding AST.sortFunctionsBySize() call");
        }

        ASTFunctionDefinition bestAlternativeMatch = null;

        for (FunctionScope currentFunction : allFunctions) {
            if (currentFunction instanceof ASTFunctionDefinition) {
                ASTFunctionDefinition functionDefinition = (ASTFunctionDefinition) currentFunction;

                if (functionDefinition.getContract() != contract) {
                    continue;
                }
                if (!TransactionGenerator.isTransactableFunction(this, functionDefinition)) {
                    continue;
                }

                if (avoidAddressArguments) {
                    // Skip this function if it has an address argument (but use it later if
                    // there is no non-address-argument alternative)
                    ASTParameterList parameters = currentFunction.getParameterList();
                    boolean dropIt = false;
                    for (ASTNode p : parameters.getChildren()) {
                        ASTVariableDeclaration declaration = (ASTVariableDeclaration)p;
                        if (declaration.getTypeName() instanceof ASTElementaryTypeName
                               && declaration.getTypeName().getName().equals("address")
                               && !declaration.getName().equals("")) {  // Unnamed arguments can be kept
                            if (bestAlternativeMatch == null) {
                               bestAlternativeMatch = functionDefinition;
                            }
                            dropIt = true;
                            break;
                        }
                    }
                    if (dropIt) {
                        continue;
                    }
                }
                result.add(functionDefinition);
            }
        }
        if (result.size() == 0 && bestAlternativeMatch == null) {
            return null;
        } else if (avoidAddressArguments && bestAlternativeMatch != null) {
            result.add(bestAlternativeMatch);
        }
        return result;
    }
}
