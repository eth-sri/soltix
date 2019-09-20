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
import soltix.output.Console;
import soltix.util.Util;

import java.util.ArrayList;

/**
 * Class to build an internal Expression representation from ASTNode expression input.
 *
 * TODO complete this
 */
public class ExpressionBuilder {
    public static Expression fromASTNode(ASTContractDefinition contractDefinition,
                                         VariableEnvironment environment,
                                         ASTNode astNode) throws Exception {
        Expression result = null;

        if (astNode instanceof ASTLiteral) {
            Value literalValue = Value.fromASTNode((ASTLiteral)astNode);
            result = new Expression(literalValue);
        } else if (astNode instanceof ASTIdentifier) {
            ASTIdentifier identifier = (ASTIdentifier) astNode;
            Variable variable = environment.getVariable(identifier.getName());

            if (variable == null) {
                Console.error(astNode, "Reference to unknown variable '"
                        + identifier.getName() + "' on scope " + environment.toString());
                throw new Exception("fromASTNode failed"); // TODO proper console errors vs. exceptions
            }
            result = new Expression(variable);
        } else if (astNode instanceof ASTFunctionCall) {
            ASTFunctionCall functionCall = (ASTFunctionCall) astNode;

            if (functionCall.getCalled() instanceof ASTElementaryTypeNameExpression) {
                ASTElementaryTypeNameExpression elementaryTypeNameExpression = (ASTElementaryTypeNameExpression)functionCall.getCalled();

                // A conversion operation - int8(...)
                ArrayList<ASTNode> arguments = functionCall.getArguments();
                if (arguments.size() != 1) {
                    throw new Exception("Unexpected elementary type name conversion argument count (not 1): " + arguments.size());
                }
                Expression toConvert = fromASTNode(contractDefinition, environment, arguments.get(0));
                result = new Expression(toConvert, elementaryTypeNameExpression.getElementaryTypeName()); // type conversion expression
            } else {
                // TODO
                //   - create ordinary function call expression
                //   - adapt expression evaluator to call back to interpreter for stack frame creation etc
                //   - embed return value in expression evaluation

                // Look up called function (note: function declarations without body are also ASTFunctionDefinitions)
                ASTFunctionDefinition functionDefinition = contractDefinition.getFunction(functionCall.getCalled().getName());

                if (functionDefinition == null) {
                    Console.error(functionCall, "Cannot locate function '" + functionCall.getCalled().getName()
                            + "', in call " + functionCall.toSolidityCode());
                    throw new Exception("ExpressionBuilder.fromASTNode failed");
                }
                ArrayList<Expression> arguments = functionCall.getExpressionArguments(contractDefinition, environment);

                ASTNode returnType = functionDefinition.getReturnType();

                // Build Expression objects for all function arguments
                functionCall.setInterpretationFunctionDefinition(functionDefinition);
                Expression callExpression = new Expression(functionCall, arguments, returnType);
                result = callExpression;
            }
        } else if (astNode instanceof  ASTBinaryOperation) {
            ASTBinaryOperation binaryOperation = (ASTBinaryOperation)astNode;
            Expression binaryExpression = new Expression(
                fromASTNode(contractDefinition, environment, binaryOperation.getLeftOperand()),
                binaryOperation.getOperator(),
                fromASTNode(contractDefinition, environment, binaryOperation.getRightOperand())
            );
            result = binaryExpression;
        } else if (astNode instanceof ASTConditional) {
            ASTConditional conditional = (ASTConditional) astNode;

            Expression conditionalExpression = new Expression(
                    fromASTNode(contractDefinition, environment, conditional.getCondition()),
                    fromASTNode(contractDefinition, environment, conditional.getTrueBranch()),
                    fromASTNode(contractDefinition, environment, conditional.getFalseBranch())
            );
            result = conditionalExpression;
        } else if (astNode instanceof ASTTupleExpression) {
            ASTTupleExpression tupleExpression = (ASTTupleExpression)astNode;
            ArrayList<Expression> tupleComponents = new ArrayList<Expression>();

            if (tupleExpression.getCount() == 1) {
                // The type of a single-item tuple troublingly decays to the type of its only component, e.g. in
                //     0     ==   (true? 0: 1)
                //     ^ int      ^^^^^^^^^^^^ tuple expression giving int
                // So we avoid modeling the tuple entirely and substitute it with the only element.
                result = fromASTNode(contractDefinition, environment, tupleExpression.getComponent(0));
            } else {
                for (int i = 0; i < tupleExpression.getCount(); ++i) {
                    ASTNode component = tupleExpression.getComponent(i);
                    tupleComponents.add(fromASTNode(contractDefinition, environment, component));
                }
                result = new Expression(tupleComponents, tupleExpression);
            }

        /*else if (astNode instanceof ASTUnaryOperation) {
            ASTUnaryOperation unaryOperation = (ASTUnaryOperation)astNode;
            unaryOperation.get
        }*/
        } else {
            throw new Exception("ExpressionBuilder.fromASTNode for unimplemented node type " + astNode.getClass().getName());
        }

        return result;
    }
}
