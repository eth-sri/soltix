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

public class ASTForStatement extends ASTNode implements IControlStructure {

    public ASTForStatement(long id, boolean needInitPart, boolean needCondPart, boolean needLoopPart) throws Exception {
        super(id);
        this.needInitPart = needInitPart;
        this.needCondPart = needCondPart;
        this.needLoopPart = needLoopPart;
    }

    private boolean needInitPart = false;
    private boolean needCondPart = false;
    private boolean needLoopPart = false;

    private ASTNode initPart = null;
    private ASTNode condPart = null;
    private ASTNode loopPart = null;
    private ASTNode body = null;

    public ASTNode getInitPart() { return initPart; }
    public ASTNode getBody() { return body; }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "for (";
        if (initPart != null) {
            code += initPart.toSolidityCode();
        }
        code += ";";
        if (condPart != null) {
            code += " " + condPart.toSolidityCode();
        }
        code += ";";
        if (loopPart != null) {
            code += " " + loopPart.toSolidityCode();
        }
        code += ")";
        return code;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }


    @Override
    public void finalize() throws Exception {
        if (getChildCount() < 1 || getChildCount() > 4) {
            throw new Exception("Unexpected for loop child count " + getChildCount() + " = " + childrenToText());
        }

        // There are four components in      for (x; y; z) s
        //    - the loop body (s) must be the last child item
        //    - the need* variables specify which of the other components are needed
        if ( (needInitPart? 1: 0) + (needCondPart? 1: 0) + (needLoopPart? 1: 0) > getChildCount() - 1) {
            throw new Exception("For loop part count does not match");
        }

        // Process the three top loop components, but keep the body as a child node that can
        // be emitted separately for proper nesting etc.
        if (needInitPart) {
            initPart = getChild(0);
            removeChildNode(0);
        }
        if (needCondPart) {
            condPart = getChild(0);
            removeChildNode(0);
        }
        if (needLoopPart) {
            loopPart = getChild(0);
            removeChildNode(0);
        }
        body = getChild(0); // only set for debugging for now
        //setChildren(null);
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
