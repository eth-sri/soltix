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

import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.Type;

/**
 * Class to generate the events and outro function used to make contract variable state visible.
 */
public class StateEventOutputGenerator {
    static protected void generateStructOutro(AST ast, ASTContractDefinition contract,
                                              ASTBlock functionBody,
                                              ASTVariableDeclaration structVariable,
                                              String currentName,
                                              String currentExpression) throws Exception {
        ASTUserDefinedTypeName userDefinedType = (ASTUserDefinedTypeName) structVariable.getTypeName();
        ASTStructDefinition structType = ast.getStructDefinition(userDefinedType.getName());

        for (ASTVariableDeclaration memberDeclaration : structType.getMembers()) {
            String currentMemberName = currentName + "_" + memberDeclaration.getName();
            String currentMemberExpression = currentExpression + "." + memberDeclaration.getName();

            if (Type.isMappingType(memberDeclaration.getTypeName())) {
                // As with ordinary variables, mapping members cannot be processed here
                generateMappingVariableOutro(ast, contract, functionBody, currentMemberName, (ASTMapping)memberDeclaration.getTypeName());
            } else if (Type.isStructType(ast, memberDeclaration.getTypeName())) {
                // Descend to nested struct type
                generateStructOutro(ast, contract, functionBody, memberDeclaration,
                        currentMemberName,
                        currentMemberExpression);
            } else {
                // OK - output this member
                generateNonMappingVariableOutro(contract, functionBody, currentMemberName, currentMemberExpression, memberDeclaration.getTypeName());
            }
        }
    }

    static private final String outroLoopVariableName = "__loopCounter";

    static protected void generateMappingVariableOutro(AST ast,
                                                       ASTContractDefinition contract,
                                                       ASTBlock functionBody,
                                                       String variableName,
                                                       ASTMapping mappingType) throws Exception {

        // Generate event with one argment per index (more than one only for nested mappings)
        // and one for the assigned value
        String eventCode = "event finalResult_Mapping_" + variableName + "("
                + mappingType.getSourceType().toSolidityCode() + " arg1, ";

        int argIndex = 2;
        while (mappingType.getDestinationType() instanceof ASTMapping) {
            // Nested mapping
            mappingType = (ASTMapping)mappingType.getDestinationType();
            eventCode += mappingType.getSourceType().toSolidityCode() + " arg" + argIndex++ + ", ";
        }

        if (Type.isRecursiveMappingValueType(ast, mappingType.getDestinationType())) {
            // Mapping value is a struct type containing another mapping - unsupported for now (needs field-wise output)
            // TODO support it
            return;
        }

        if (!Configuration.useABIEncoderV2 && Type.isStructType(ast, mappingType.getDestinationType())) {
            // Can't do structs without V2 encoder - drop it
            return;
        }

        eventCode += mappingType.getDestinationType().toSolidityCode() + " arg" + argIndex++ + ");";
        ASTNode eventNode = new ASTVerbatimText(0, eventCode);
        contract.addChildNode(eventNode);
    }

    static protected void generateNonMappingVariableOutro(ASTContractDefinition contract,
                                                          ASTBlock functionBody,
                                                          String variableNameString,
                                                          String argumentExpressionString, ASTNode argumentType) throws Exception {
        if (argumentType instanceof ASTArrayTypeName) {
            ASTArrayTypeName arrayType = (ASTArrayTypeName)argumentType;
            if (Type.isStringType(arrayType.getBaseType())) {
                String loopCode = "for (" + outroLoopVariableName + " = 0; "
                        + outroLoopVariableName + " < " + argumentExpressionString + ".length; "
                        + " ++" + outroLoopVariableName + ")";
                ASTVerbatimText loopNode = new ASTVerbatimText(0, loopCode);
                functionBody.addChildNode(loopNode);

                argumentExpressionString += "[" + outroLoopVariableName + "]";
                argumentType = arrayType.getBaseType();
            }
        }

        String emitCode = "emit finalResult_" + variableNameString + "(" + argumentExpressionString + ");";
        ASTNode emitNode = new ASTVerbatimText(0, emitCode);
        functionBody.addChildNode(emitNode);

        // We can already add the event definition
        String eventCode = "event finalResult_" + variableNameString + "(" + argumentType.toSolidityCode() + " " + variableNameString + ");";
        ASTNode eventNode = new ASTVerbatimText(0, eventCode);
        contract.addChildNode(eventNode);
    }



    static public void generateOutroFunction(AST ast, ASTContractDefinition contract) throws Exception {
        ASTFunctionDefinition outroFunction = new ASTFunctionDefinition(0, "__outro", "public", "payable",
                false, false);

        ASTParameterList emptyParameterList = new ASTParameterList(0);
        ASTParameterList emptyReturnList = new ASTParameterList(0);
        outroFunction.addChildNode(emptyParameterList);
        outroFunction.addChildNode(emptyReturnList);

        ASTBlock functionBody = new ASTBlock(0);
        outroFunction.addChildNode(functionBody);

        //String loopVariable = "__loopCounter";
        String loopVariableDeclarationCode = "uint " + outroLoopVariableName + ";";
        functionBody.addChildNode(new ASTVerbatimText(0, loopVariableDeclarationCode));

        // Process all storage variables
        for (ASTNode variableNode : contract.getVariables()) {
            if (variableNode instanceof ASTVariableDeclarationStatement) {
                // Not clear whether/where this can occur - use an exception to find out
                throw new Exception("Unexpected ASTVariableDeclarationStatement for " + variableNode.getNonNullName());
            }
            ASTVariableDeclaration variable = (ASTVariableDeclaration)variableNode;

            if (Type.isMappingType(variable.getTypeName())) {
                // Mappings are supported by tracking individual assignments - we can skip the emit (which is generated
                // in ASTAssignment.toSolidityCode()) but we still need to create the associated event
                ASTMapping mappingType = (ASTMapping)variable.getTypeName();
                generateMappingVariableOutro(ast, contract, functionBody, variable.getName(), mappingType);
                continue;
            } else if (Type.isStructType(ast, variable.getTypeName())) {
                generateStructOutro(ast, contract, functionBody, variable, variable.getName(), variable.getName());
                continue;
            }

            // ... all remaining types (including arrays) are supported.
            //
            // Structure:    emit  finalResult_<name>(<expr>);
            //               event finalResult_<name>(<type> name);
            // where for most types <expr> is the name of the affected  variable, and <type> is the type of that
            // variable.
            // For arrays of strings, we need to loop over the array, subscript the array, and use the element type
            // (string) for the event definition
            String argumentExpressionString = variable.getName();
            ASTNode argumentType = variable.getTypeName();

            generateNonMappingVariableOutro(contract, functionBody, variable.getName(), argumentExpressionString, argumentType);
        }

        // Add function to contract
        outroFunction.finalize();
        contract.addChildNode(outroFunction);
        contract.setOutroFunction(outroFunction);
    }
}
