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

public class ASTConditional extends ASTNode {
    private ASTNode condition = null;
    private ASTNode lhs = null;
    private ASTNode rhs = null;

    public ASTConditional(long id) throws Exception { super(id); }

    @Override
    public String toSolidityCode() throws Exception {
        return /*"("+*/ condition.toSolidityCode() + "? " + lhs.toSolidityCode() + ": " + rhs.toSolidityCode() ; //+ ")";
    }
    @Override
    public String toSolidityCodePostfix() { return null; }


    public ASTNode getCondition() { return condition; }
    public ASTNode getTrueBranch() { return lhs; }
    public ASTNode getFalseBranch() { return rhs; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 3) {
            throw new Exception("Unexpected conditional operator child count (not 3) = " + getChildCount());
        }
        condition = getChild(0);
        lhs = getChild(1);
        rhs = getChild(2);
    }
}
