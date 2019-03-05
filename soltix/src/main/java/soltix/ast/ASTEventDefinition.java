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

import java.util.ArrayList;

public class ASTEventDefinition extends ASTNode {
    private ASTParameterList parameterList = null;
    private boolean isAnonymous = false;

    public ASTEventDefinition(long id, String name, boolean isAnonymous) throws Exception {
        super(id, name);
        this.isAnonymous = isAnonymous;
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "event " + name + "(" + parameterList.toSolidityCode() + ")";
        if (isAnonymous) {
            code += " anonymous";
        }
        return code;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 1) {
            throw new Exception("Unexpected event definition child count " + getChildCount() + " = " + childrenToText());
        }
        if (!(getChild(0) instanceof ASTParameterList)) {
            throw new Exception("Unexpected event definition child item type " + getChild(0).getClass().getName());
        }
        parameterList = (ASTParameterList)getChild(0);

        if (Configuration.nameUnnamedEventDefinitionParameters) {
            int renamedCounter = 0;
            for (ASTNode parameter : parameterList.getChildren()) {
                ASTVariableDeclaration declaration = (ASTVariableDeclaration)parameter;
                if (declaration.getName().equals("")) {
                    declaration.setName("_insertedParameterName" + renamedCounter);
                    ++renamedCounter;
                }
            }
        }
        setChildren(null);
    }
}
