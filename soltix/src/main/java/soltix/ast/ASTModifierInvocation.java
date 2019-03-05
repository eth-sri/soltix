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

public class ASTModifierInvocation extends ASTNode {
    private ArrayList<ASTNode> arguments = null;
    private boolean argumentsSet = false;

    // If this is actually a *modifier* invocation: reference to its code (once available)
    private ASTModifierDefinition definition = null;
    // TODO: ... if this is really a base contract constructor call: reference to the contract

    public ASTModifierInvocation(long id, boolean argumentsSet) throws Exception {
        super(id);
        this.argumentsSet = argumentsSet;
    }

    public ASTModifierDefinition getDefinition() { return definition; }
    public void setDefinition(ASTModifierDefinition definition) { this.definition = definition; }

    @Override
    public String toSolidityCode() throws Exception {
        String code = name;
        if (arguments != null) {
            code += "(";
            for (int i = 0; i < arguments.size(); ++i) {
                if (i > 0) {
                    code += ", ";
                }
                code += arguments.get(i).toSolidityCode();
            }
            code += ")";
        } else if (argumentsSet) {
            // The "arguments" attribute is an array containing only null - indicating that this
            // modifier is invoked using an empty argument list rather than without any argument list.
            // This must be honored to preserve AST-JSON form
            code += "()";
        }
        return code;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() < 1) {
            throw new Exception("Unexpected modifier invocation child count " + getChildCount());
        }
        if (!(getChild(0) instanceof ASTIdentifier)) {
            throw new Exception("Unexpected modifier invocation child type " + getChild(0).getClass().getName());
        }
        setName(getChild(0).getName());
        removeChildNode(0);

        // The remaining children are arguments
        if (getChildCount() > 0) {
            arguments = getChildren();
        }
        setChildren(null);
    }
}
