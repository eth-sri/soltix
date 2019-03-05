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

public class ASTVariableDeclarationStatement extends ASTNode {
    private ArrayList<ASTNode> declarations = null;
    private ASTNode initializer = null;
    private ArrayList<Integer> assignmentIds = null;

    public ASTVariableDeclarationStatement(long id, ArrayList<Integer> assignmentIds) throws Exception {
        super(id);
        this.assignmentIds = assignmentIds;
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code;


        if (assignmentIds.size() == 1) {
            // Single item
            code = declarations.get(0).toSolidityCode();
        } else {
            // Tuple
            code = "var (";
            for (int i = 0, decidx = 0; i < assignmentIds.size(); ++i) {
                if (i > 0) {
                    code += ", ";
                }
                if (assignmentIds.get(i) < 0) {
                    ;
                } else {
                    code += declarations.get(decidx++).getName();
                }
            }
            code += ")";
        }
        if (initializer != null) {
            code += " = " + initializer.toSolidityCode();
        }
        return code;
    }

    @Override
    public String getName() { return declarations.get(0).getName(); }
    public ASTVariableDeclaration getDeclaration() {
        return (ASTVariableDeclaration)declarations.get(0); }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() < 1) {
            throw new Exception("Unexpected VariableDeclarationStatement child items: " + getChildCount());
        }
        if (!(getChild(0) instanceof ASTVariableDeclaration)) {
            throw new Exception("Unexpected VariableDeclarationStatement part: " + getChild(0).getClass().getName());
        }

        ASTNode lastChild = getChild(getChildCount() - 1);
        if (!(lastChild instanceof ASTVariableDeclaration)) {
            initializer = lastChild;
            removeChildNode(getChildCount() - 1);
        }
        declarations = getChildren();
        setChildren(null);
    }
}
