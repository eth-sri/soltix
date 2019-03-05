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

package soltix.interpretation.expressions;

import soltix.ast.*;
import soltix.interpretation.values.Value;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;

import java.util.ArrayList;

/**
 * Class to build an internal Expression representation from ASTNode expression input.
 *
 * TODO complete this
 */
public class ExpressionBuilder {
    public static Expression fromASTNode(VariableEnvironment environment, ASTNode astNode) throws Exception {
        Expression result = null;

        if (astNode instanceof ASTLiteral) {
            Value literalValue = Value.fromASTNode((ASTLiteral)astNode);
            result = new Expression(literalValue);
        } else if (astNode instanceof ASTIdentifier) {
            ASTIdentifier identifier = (ASTIdentifier) astNode;
            Variable variable = environment.getVariable(identifier.getName());
            result = new Expression(variable);
        } else if (astNode instanceof ASTFunctionCall) {
            ASTFunctionCall functionCall = (ASTFunctionCall)astNode;
            if (functionCall.getCalled() instanceof ASTElementaryTypeName) {
                // A conversion operation - int8(...)
                ArrayList<ASTNode> arguments = functionCall.getArguments();
                if (arguments.size() != 1) {
                    throw new Exception("Unexpected elementary type name conversion argument count (not 1): " + arguments.size());
                }
                Expression toConvert = fromASTNode(environment, arguments.get(0));
                result = new Expression(toConvert, functionCall.getCalled()); // type conversion expression
            }
        }
        /*else if (astNode instanceof ASTUnaryOperation) {
            ASTUnaryOperation unaryOperation = (ASTUnaryOperation)astNode;
            unaryOperation.get
        }*/ else {
            throw new Exception("ExpressionBuilder.fromASTNode for unimplemented node type " + astNode.getClass().getName());
        }

        return result;
    }
}
