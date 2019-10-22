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

package soltix.interpretation.values;

import soltix.ast.ASTContractDefinition;
import soltix.ast.ASTNode;
import soltix.ast.ASTVariableDeclaration;
import soltix.ast.ASTVerbatimText;

import java.util.ArrayList;

/**
 * Class to represent contract values
 */
public class ContractValue extends Value {
    private ASTContractDefinition contractType;
    private NamedValueList members;
    private NamedValueList constructorArguments;

    public ContractValue(ASTContractDefinition contractType,
                         ArrayList<Value> arguments) {

        this.contractType = contractType;
        members = new NamedValueList();
        constructorArguments = new NamedValueList();
        // TODO for attributes with existing initializers, evaluate those and assign the values (addMember())


        /*        for (int i = 0; i < structType.getMembers().size(); ++i) {
            ASTVariableDeclaration member = structType.getMembers().get(i);
            Value value = fieldValues.get(i);
            // TODO type-checking
            addMember(member, value);
        }*/
    }

    public void addMember(ASTVariableDeclaration variable, Value value) { members.addMember(variable, value); }
    public Value getMemberValue(String name) {
        return members.getMemberValue(name);
    }

    public void addConstructorArgument(ASTVariableDeclaration variable, Value value) { constructorArguments.addMember(variable, value); }
    public Value getConstructorArgumentValue(String name) {
        return constructorArguments.getMemberValue(name);
    }

    public NamedValueList getConstructorValueList() { return constructorArguments; }

    @Override
    public ASTNode getType() { return contractType; }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        return members.genericToASTNode(contractType, forJavaScript);
    }

    @Override
    public Object toJSONRepresentation() throws Exception {
        throw new Exception("ContractValue.toJSONRepresentation is unimplemented");
    }
}
