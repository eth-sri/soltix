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

public class ASTIfStatement extends ASTNode implements IControlStructure {
    private ASTNode condition = null;
    private ASTNode ifBranch = null;
    private ASTNode elseBranch = null;

    public ASTIfStatement(long id) throws Exception { super(id); }

    @Override
    public String toSolidityCode() throws Exception { return "if (" + condition.toSolidityCodeNonNull() + ")"; }
    @Override
    public String toSolidityCodePostfix() { return null; }

    public ASTNode getCondition() { return condition; }
    public void setCondition(ASTNode condition) { this.condition = condition; }

    public ASTNode getIfBranch() { return ifBranch; }
    public void setIfBranch(ASTNode ifBranch) { this.ifBranch = ifBranch; }

    public ASTNode getElseBranch() { return elseBranch; }
    public void setElseBranch(ASTNode elseBranch) { this.elseBranch = elseBranch; }

    public void finalize() throws Exception {
        // This statement should have at least a condition and if body, and potentially an else body
        if (getChildCount() < 2 || getChildCount() > 3) {
            throw new Exception("If statement has unexpected child count - " + getChildCount());
        }
        // Set and remove condition
        condition = getChild(0);
        removeChildNode(0);

        // Set but keep branch nodes in tree
        ifBranch = getChild(0);
        if (getChildCount() > 1) {
            elseBranch = getChild(1);
        }
    }


    public void replaceChildStatement(ASTNode oldNode, ASTNode newNode) throws Exception {
        if (ifBranch != oldNode && elseBranch != oldNode) {
            throw new Exception("Cannot find old child node " + oldNode.getID());
        }
        oldNode.parent = null;
        newNode.parent = this;

        if (ifBranch == oldNode) {
            ifBranch = newNode;
            setChild(0, newNode);
        } else {
            elseBranch = newNode;
            setChild(1, newNode);
        }
    }
}
