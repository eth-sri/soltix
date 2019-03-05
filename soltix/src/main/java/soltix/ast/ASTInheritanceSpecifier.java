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

import java.util.ArrayList;

public class ASTInheritanceSpecifier extends ASTNode {
    private ASTNode typeName = null;
    private ArrayList<ASTNode> arguments = null;
    private boolean hasArgumentList = false;
    private ASTContractDefinition contract = null;

    public ASTInheritanceSpecifier(long id, boolean hasArgumentList) throws Exception {
        super(id);
        this.hasArgumentList = hasArgumentList;
    }

    public ASTNode getType() { return typeName; }

    public ASTContractDefinition getContract() { return contract; }
    public void setContract(ASTContractDefinition contract) { this.contract = contract; }

    @Override
    public String toSolidityCode() throws Exception {
        String code = typeName.toSolidityCode();
        if (arguments != null || hasArgumentList) {
            code += "(";
            if (arguments != null) {
                for (int i = 0; i < arguments.size(); ++i) {
                    if (i > 0) {
                        code += ", ";
                    }
                    code += arguments.get(i).toSolidityCode();
                }
            }
            code += ")";
        }
        return code;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() < 1) {
            throw new Exception("Unexpected inheritance specifier child count " + getChildCount() + " = " + childrenToText());
        }
        typeName = getChild(0);
        setName(typeName.getName());
        removeChildNode(0);
        if (getChildCount() > 0) {
            // These are arguments
            arguments = getChildren();
            setChildren(null);
        }
    }
}
