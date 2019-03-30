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

import soltix.ast.AST;
import soltix.ast.ASTContractDefinition;
import soltix.ast.ASTNode;
import soltix.ast.ASTVariableDeclaration;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.values.Value;
import soltix.interpretation.variables.Variable;
import soltix.util.RandomNumbers;

/**
 * Class to generate variables of randomized type (with optional initializer)
 */
public class VariableGenerator {
    private AST ast;
    private RandomNumbers prng;
    private ValueGenerator valueGenerator;
    private StructTypeGenerator structTypeGenerator;

    public VariableGenerator(AST ast, RandomNumbers prng, StructTypeGenerator structTypeGenerator) throws Exception {
        this.ast = ast;
        this.prng = prng;
        valueGenerator = new ValueGenerator(prng);
        this.structTypeGenerator = structTypeGenerator;
    }


    public ASTVariableDeclaration generateRandomVariable(ASTContractDefinition contractDefinition,
                                                         String name,
                                                         boolean withInitializer,
                                                         boolean allowStructs,
                                                         boolean isFunctionParameter) throws Exception {
        ASTNode type = TypeContainer.chooseRandomType(ast, contractDefinition, allowStructs, structTypeGenerator, prng);
        ASTVariableDeclaration variableDeclaration;
        String storageLocation;

        if (isFunctionParameter && Type.isStringType(type)) {
            storageLocation = "memory";
        } else {
            storageLocation = "default";
        }

        variableDeclaration = new ASTVariableDeclaration(0, name, type.toSolidityCode(),
                storageLocation, "internal", false, false);
        // Specify type
        variableDeclaration.addChildNode(type);
        if (withInitializer) {
            // Generate initializer value
            Value initializerValue = valueGenerator.generateRandomValue(ast, type, ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL);
            variableDeclaration.setInitializerValue(initializerValue);                       // store for interpretation
            variableDeclaration.addChildNode(initializerValue.toASTNode(false));  // store for code output
        }
        variableDeclaration.finalize();

        if (true) System.out.println("" + name + ": " + variableDeclaration.toSolidityCode());
        return variableDeclaration;
    }
}
