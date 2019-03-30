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

package soltix.synthesis;

import soltix.ast.*;
import soltix.util.RandomNumbers;

/**
 * Class to generate randomized struct types
 */
public class StructTypeGenerator {
    private RandomNumbers prng;
    private VariableGenerator variableGenerator;
    private int currentStructNumber = 0;

    public StructTypeGenerator(RandomNumbers prng) {
        this.prng = prng;
    }

    public void setVariableGenerator(VariableGenerator variableGenerator) {
        this.variableGenerator = variableGenerator;
    }

    public ASTUserDefinedTypeName generate(AST ast, ASTContractDefinition contract) throws Exception {
        String structName = "s" + String.valueOf(currentStructNumber++);
        String canonicalStructName = contract.getName() + "." + structName;
        ASTStructDefinition structDefinition = new ASTStructDefinition(0, structName, canonicalStructName);
        int memberCount = (int)prng.generateLongInteger(1, 5);

        // Generate struct member fields.
        // Note that this may in turn recurse into generating further struct definitions, and we'll technically
        // only terminate with high probability
        for (int i = 0; i < memberCount; ++i) {
            String memberName = "m" + String.valueOf(i);

            ASTVariableDeclaration memberVariable = variableGenerator.generateRandomVariable(contract, memberName, false,true, false);
            structDefinition.addChildNode(memberVariable);
        }
        structDefinition.finalize();

        // Add struct definition directly first for immediate type-lookups (TODO probably unneeded? The AST class is used for lookups),
        // but also as a child node for subsequent code emission
        contract.addStructDefinition(structDefinition);
        contract.addChildNode(structDefinition);
        ast.addStructDefinition(structDefinition);

        // Return an ASTUserDefinedType for consistency with existing type representations
        ASTUserDefinedTypeName udt = new ASTUserDefinedTypeName(0, structDefinition.getName());

        return udt;
    }
}
