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

public class ASTArrayTypeName extends ASTNode {
    private ASTNode baseType = null;
    private ASTNode size = null; // TODO: do multi-dim arrays exist?

    public ASTArrayTypeName(long id) throws Exception {
        super(id);
    }

    public ASTNode getBaseType() { return baseType; }

    @Override
    public String toSolidityCode() throws Exception {
        String code = baseType.toSolidityCode() + "[";
        if (size != null) { // otherwise: undefined size
            code += size.toSolidityCode();
        }
        code += "]";
        return code;
    }

    @Override
    public String toSolidityCodePostfix() {
        return null;
    }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 1 && getChildCount() != 2) {
            throw new Exception("Unexpected array type name child count " + getChildCount() + " = " + childrenToText());
        }
        baseType = getChild(0);
        if (getChildCount() > 1) {
            size = getChild(1);
        }
        setChildren(null);
    }
}
