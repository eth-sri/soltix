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

public class ASTFunctionTypeName extends ASTNode {
    private String visibility = null;
    private ASTNode parameterList = null;
    private ASTNode returnList = null;
    //ASTNode modifierInvocation = null;
    private ASTNode returnType = null;

    public ASTFunctionTypeName(long id, String visibility) throws Exception {
        super(id);
        this.visibility = visibility;
    }

    public ASTNode getReturnType() { return returnType; }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "function(" + parameterList.toSolidityCode() + ") " + visibility;
        if (returnList != null) {
            code += " returns(" + returnList.toSolidityCode() + ")";
        }
        return code;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() < 2) {
            throw new Exception("Unexpected function type name child count " + getChildCount());
        }
        if (!(getChild(0) instanceof ASTParameterList)
                || !(getChild(1) instanceof ASTParameterList)) {
            throw new Exception("Unexpected function definition child sequence: " +
                    getChild(0).getClass().getName() + " " + getChild(1).getClass().getName());
        }

        // Remove parameter lists, keep block child node
        parameterList = getChild(0);
        returnList = getChild(1);
        ASTNode savedReturnList = returnList; // TODO consistency
        if (returnList.getChildCount() == 0) {
            // Empty return list - remove it
            returnList = null;
        }
        removeChildNode(0);
        removeChildNode(0);


        if (savedReturnList.getChildCount() > 0) {
            returnType = savedReturnList.getChild(0);
            // TODO tuple types - returnList.getChildCount() > 1
        }
    }
}
