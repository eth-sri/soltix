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

public class ASTModifierDefinition extends FunctionScope {
    //private ASTNode parameters = null;

    public ASTModifierDefinition(long id, String name) throws Exception {
        super(id, name);
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "modifier " + name;
        code += "(" + parameters.toSolidityCode() + ")";
        return code;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() < 1) {
            throw new Exception("Unexpected modifier child count " + getChildCount());
        }
        if (!(getChild(0) instanceof ASTParameterList)) {
            throw new Exception("Unexpected modifier definition child node of type " + getChild(0).getClass().getName());
        }
        parameters = (ASTParameterList)getChild(0);
        removeChildNode(0);
        if (getChildCount() > 0) {
            body = (ASTBlock)getChild(0);
        }
    }
}
