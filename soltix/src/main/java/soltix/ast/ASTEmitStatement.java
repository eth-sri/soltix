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

public class ASTEmitStatement extends ASTNode {
    private ASTNode argument = null;
    private ASTFunctionCall functionCall = null;

    public ASTEmitStatement(long id) throws Exception { super(id); }

    public ASTFunctionCall getFunctionCall() { return functionCall; }

    @Override
    public String toSolidityCode() throws Exception { return "emit " + argument.toSolidityCode() + ";"; }
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 1) {
            throw new Exception("Unexpected emit statement child count " + getChildCount());
        }
        argument = getChild(0);
        if (!(argument instanceof ASTFunctionCall)) {
            throw new Exception("Unexpected non-function-call argument to emit statement: " + argument.toSolidityCode());
        }
        functionCall = (ASTFunctionCall)argument;
        if (!(functionCall.getCalled() instanceof ASTIdentifier)) {
            throw new Exception("Unexpected call to non-identifier function in emit statement: " + functionCall.toSolidityCode());
        }
        ASTIdentifier identifier = (ASTIdentifier)functionCall.getCalled();
        setName(identifier.getName());
        setChildren(null);
    }
}
