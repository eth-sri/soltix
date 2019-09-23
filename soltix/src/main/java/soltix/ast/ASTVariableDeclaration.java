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

import soltix.interpretation.values.Value;
import soltix.interpretation.variables.Variable;

public class ASTVariableDeclaration extends ASTNode {
    private String type; // Unneeded? uint256 (type) vs uint (type name)
    private ASTNode typeName = null;
    private ASTNode initializer = null;
    private Definitions.StorageLocation storageLocation = null;
    private Definitions.Visibility visibility = null;
    private boolean isConstant = false;
    private boolean knowIsFunctionArgument = false;
    private boolean isFunctionArgument = false;
    private boolean isIndexed = false;
    private boolean isInternalVariable = false;
    private Variable variable = null;
    private Value initializerValue = null;
    private Value lastTransactionValue = null;

    public ASTVariableDeclaration(long id, String name, String type, String storageLocationName, String visibilityName,
                                  boolean isConstant, boolean isIndexed) throws Exception {
        super(id, name);
        this.type = type;

        storageLocation = Definitions.StorageLocation.fromString(storageLocationName);
        if (storageLocation == null) {
            throw new Exception("Unknown variable storage location " + storageLocationName + " for " + name);
        }
        visibility = Definitions.Visibility.fromString(visibilityName);
        if (visibility == null) {
            throw new Exception("Unknown visibility " + visibilityName);
        }
        this.isConstant = isConstant;
        this.isIndexed = isIndexed;
    }

    public ASTNode getTypeName() { return typeName; }
    public Definitions.StorageLocation getStorageLocation() { return storageLocation; }
    public void setStorageLocation(Definitions.StorageLocation storageLocation) { this.storageLocation = storageLocation; }

    protected boolean isConstant() { return isConstant; } // not exported since callers will probably want isModifiable() instead
    public boolean isFunctionParameter() {
        if (knowIsFunctionArgument) {
            return isFunctionArgument;
        }

        knowIsFunctionArgument = true;
        if (getParent() instanceof ASTParameterList
         && getParent().getParent() instanceof ASTFunctionDefinition) {
            ASTFunctionDefinition functionDefinition = (ASTFunctionDefinition)getParent().getParent();
            isFunctionArgument = functionDefinition.getParameterList().containsChildNode(this);
        } else {
            isFunctionArgument = false;
        }
        return isFunctionArgument;
    }
    public boolean isModifiable() {
        // Unmodifiable are:
        //    - Variables declared as "constant"
        //    - Parameters in functions declared as "external"
        return !isConstant()
                && (!isFunctionParameter()
                   || ((ASTFunctionDefinition)getParent().getParent()).getVisibility() != Definitions.Visibility.VISIBILITY_EXTERNAL);
    }

    // TODO ASTNode vs Value initializers is asking for trouble
    public ASTNode getInitializer() { return initializer; }
    public void setInitializerValue(Value value) { 
        this.initializerValue = value; 
    }
    public Value getInitializerValue()  {
        return initializerValue;
    }

    public void setLastTransactionValue(Value value) { this.lastTransactionValue = value; }
    public Value getLastTransactionValue() { return lastTransactionValue; }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }
    public Variable getVariable() {
        return variable;
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = typeName != null? typeName.toSolidityCode(): "var";
        if (storageLocation != Definitions.StorageLocation.STORAGE_LOCATION_DEFAULT) {
            code += " " + storageLocation;
        }
        if (isIndexed) { // Without indexed event arguments, stack nesting errors arise in some contracts
            code += " indexed";
        }
        if (isConstant) {
            code += " constant";
        }
        // For now suppress internal visibility too. It would kind of be nicer to be explicit about it
        // but solc will fail to accept "internal" function parameter specifiers
        if (visibility != Definitions.Visibility.VISIBILITY_INTERNAL) {
            code += " " + visibility;
        }
        code += " " + name;
        if (initializer != null) {
            code += " = " + initializer.toSolidityCode();
        }
        return code;
    }

    public boolean isInternalVariable() { return isInternalVariable; }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (/*getChildCount() < 1 ||*/ getChildCount() > 2) {
            throw new Exception("Unexpected variable child count " + getChildCount());
        }

        if (getChildCount() > 0) { // 0 children = untyped variable "var foo" (deprecated)
            if (!(getChild(0) instanceof ASTElementaryTypeName)
                    && !(getChild(0) instanceof ASTMapping)
                    && !(getChild(0) instanceof ASTUserDefinedTypeName)
                    && !(getChild(0) instanceof ASTArrayTypeName)
                    && !(getChild(0) instanceof ASTFunctionTypeName)) {
                throw new Exception("Variable declaration with unexpected type name " + getChild(0).getClass().getName());
            }
            typeName = getChild(0);
            removeChildNode(0);

            // A declaration at the contract level can have an initializer child node as
            // well, e.g. in   contract foo { uint var = 0; }
            // If that same "var" declaration occurs in a function, the declaration is
            // instead embedded in a VariableDeclarationStatement, and the initializer
            // is a child to that object rather than the VariableDeclaration
            if (getChildCount() > 0) {
                initializer = getChild(0);
                removeChildNode(0);
            }
        }

        isInternalVariable = getName().startsWith("_internal"); // TODO collect constants/types for internal variables here?
    }
}
