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

import soltix.Configuration;

public class ASTMemberAccess extends ASTNode {
    private ASTNode appliedTo = null;
    private ASTContractDefinition contract = null;
    private boolean isRenamedTimestamp = false;

    // Pass contract for renaming bookkeeping.
    // TODO Probably finalize() should receive the AST for such state info instead. UPDATE: Probably not, since the finalize() environment is too restricted by a lack of resolved references etc.
    public void setContract(ASTContractDefinition contract) { this.contract = contract; }

    public ASTMemberAccess(long id, String name, long referencedDeclarationId) throws Exception {
        super(id, name);
        this.referencedDeclarationId = referencedDeclarationId;
    }

    public ASTNode getAppliedTo() { return appliedTo; }

    @Override
    public String toSolidityCode() throws Exception {
        if (isRenamedTimestamp) {
            return "_mynow";
        } else {
            return appliedTo.toSolidityCode() + "." + name;
        }
    }
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 1) {
            throw new Exception("Member access with unexpected operand count " + getChildCount() + " = " + childrenToText());
        }
        appliedTo = getChild(0);
        removeChildNode(0);

        if (Configuration.renameNowTimestamps != 0
                && getName().equals("timestamp")
                && appliedTo instanceof ASTIdentifier
                && appliedTo.getName().equals("block")) {
            isRenamedTimestamp = true;
            contract.setContainsRenamedNowReferences(true);
        }
    }
}
