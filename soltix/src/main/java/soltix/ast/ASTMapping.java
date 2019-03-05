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

public class ASTMapping extends ASTNode {
    private ASTNode sourceType = null;
    private ASTNode destType = null;

    public ASTMapping(long id) throws Exception { super(id); }

    public ASTNode getSourceType() { return sourceType; }
    public ASTNode getDestinationType() { return destType; }

    @Override
    public String toSolidityCode() throws Exception { return "mapping(" + sourceType.toSolidityCode() + " => " + destType.toSolidityCode() + ")"; }
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 2) {
            throw new Exception("Unexpected child count for mapping: " + getChildCount());
        }
        if (!getChild(0).isTypeName() || !getChild(0).isTypeName()) {
            throw new Exception("Unexpected child type(s) for mapping: "
                    + getChild(0).getClass().getName() + "  " + getChild(1).getClass().getName());
        }
        sourceType = getChild(0);
        destType = getChild(1);
        setChildren(null);
    }
}
