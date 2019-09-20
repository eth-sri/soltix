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

public class ASTWhileStatement extends ASTNode implements IControlStructure {
    private ASTNode condition = null;
    private ASTNode body = null;

    public ASTWhileStatement(long id) throws Exception { super(id); }

    public ASTNode getBody() { return body; }
    public ASTNode getCondition() { return condition; }

    @Override
    public String toSolidityCode() throws Exception {
        return "while (" + condition.toSolidityCode() + ")";
    }
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 2) {
            throw new Exception("Unexpected while child count " + getChildCount() + " = " + childrenToText());
        }
        condition = getChild(0);
        removeChildNode(0);
        body = getChild(0);
        // Keep body as child node for output later
    }

    public void replaceChildStatement(ASTNode oldNode, ASTNode newNode) throws Exception {
        if (body != oldNode) {
            throw new Exception("Cannot find old child node " + oldNode.getID());
        }
        oldNode.parent = null;
        newNode.parent = this;

        body = newNode;
        setChild(0, newNode);
    }
}
