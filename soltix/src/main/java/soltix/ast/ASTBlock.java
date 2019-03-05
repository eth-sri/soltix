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

public class ASTBlock extends ASTNode {
    public ASTBlock(long id) throws Exception { super(id); }

    @Override
    public String toSolidityCode() { return "{"; }
    @Override
    public String toSolidityCodePostfix() { return "}"; }

    public int getChildNodeIndex(ASTNode node) throws Exception {
        for (int i = 0; i < getChildCount(); ++i) {
            if (getChild(i) == node) {
                return i;
            }
        }
        throw new Exception("Could not locate node " + node.getID() + " in block " + getID());
    }

    public void replaceChildNode(ASTNode oldNode, ASTNode newNode) throws Exception {
        for (int i = 0; i < getChildCount(); ++i) {
            if (getChild(i) == oldNode) {
                setChild(i, newNode);
                oldNode.parent = null;
                newNode.parent = this;
                return;
            }
        }
        throw new Exception("ASTBlock.replaceChildNode: Cannot locate statement requested for replacement");
    }
}
