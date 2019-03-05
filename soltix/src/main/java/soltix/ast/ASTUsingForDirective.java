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

public class ASTUsingForDirective extends ASTNode {
    private ASTNode used = null;
    private ASTNode user = null;
    private boolean emptyTypeName;

    public ASTUsingForDirective(long id, boolean emptyTypeName) throws Exception {
        super(id);
        this.emptyTypeName = emptyTypeName;
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "using " + used.toSolidityCode() + " for ";
        if (emptyTypeName) {
            code += "*";
        } else {
            code += user.toSolidityCode();
        }
        return code;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if ((!emptyTypeName && getChildCount() != 2)
                || (emptyTypeName && getChildCount() != 1)) {
            throw new Exception("Unexpected using for statement child count " + getChildCount());
        }
        // TODO type-checking?
        used = getChild(0);
        removeChildNode(0);
        if (!emptyTypeName) {
            user = getChild(0);
            removeChildNode(0);
        }
    }
}
