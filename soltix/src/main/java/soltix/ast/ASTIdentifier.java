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

import java.util.ArrayList;
import java.util.HashMap;

public class ASTIdentifier extends ASTNode {
    private ASTContractDefinition contract = null;

    private boolean isRenamedNow = false;

    // Pass contract for renaming bookkeeping.
    // TODO Probably finalize() should receive the AST for such state info instead. UPDATE: Probably not, since the finalize() environment is too restricted by a lack of resolved references etc.
   // public void setContract(ASTContractDefinition contract) { this.contract = contract; }
    public boolean getIsRenamedNow() { return isRenamedNow; }

    // Pass contract for renaming bookkeeping.
    // TODO Probably finalize() should receive the AST for such state info instead. UPDATE: Probably not, since the finalize() environment is too restricted by a lack of resolved references etc.
    public void setContract(ASTContractDefinition contract) { this.contract = contract; }

    public ASTIdentifier(long id, String name, long referencedDeclarationId) throws Exception {
        super(id, name);
        this.referencedDeclarationId = referencedDeclarationId;
    }

    @Override
    public String toSolidityCode() { return name; }
    @Override
    public String toSolidityCodePostfix() { return null; }


    @Override
    public void finalize() throws Exception {
        if (Configuration.renameNowTimestamps != 0 && name.equals("now")) {
            setName("_mynow");
            //contract.setContainsRenamedNowReferences(true);
            isRenamedNow = true;
            System.out.println(" IS RENAMED NOW FOR " + this);
            contract.setContainsRenamedNowReferences(true);
        }
    }
}
