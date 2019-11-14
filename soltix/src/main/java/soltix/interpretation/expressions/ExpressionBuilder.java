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
import soltix.interpretation.Type;
import soltix.interpretation.values.ContractValue;
import soltix.interpretation.values.FunctionValue;
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
    public static Expression fromASTNode(AST ast,
                                         ASTContractDefinition contractDefinition,
                                         // Contract instance, if any, within which we execute. This allows us to get a
                                         // handle on the top-level contract VariableEnvironment for the purpose of
                                         // function call interpretation. The VariableEnvironment passed below is not
                                         // sufficient for this particular purpose because it gives the environment
                                         // within the current call chain - potentially including current function
                                         // parameters that are not available in a subsequent function call
                                         ContractValue contractValueContext,
                                         // VariableEnvironment within which we execute
                                         VariableEnvironment environment,
                                         ASTNode astNode) throws Exception {
        Expression result = null;

        if (astNode instanceof ASTLiteral) {
            Value literalValue = Value.fromASTNode((ASTLiteral)astNode);
            result = new Expression(literalValue);
        } else if (astNode instanceof ASTIdentifier) {
            ASTIdentifier identifier = (ASTIdentifier) astNode;
            Variable variable = environment.getVariableIncludingParentEnvironments(identifier.getName());

            if (variable == null) {
                Console.error(astNode, "Reference to unknown variable '"
                        + identifier.getName() + "' on scope " + environment.toString());
                throw new Exception("fromASTNode failed"); // TODO proper console errors vs. exceptions
            }
            result = new Expression(variable);
        } else if (astNode instanceof ASTFunctionCall) {
            ASTFunctionCall functionCall = (ASTFunctionCall) astNode;

            if (functionCall.getCalled() instanceof ASTElementaryTypeNameExpression) {
                ASTElementaryTypeNameExpression elementaryTypeNameExpression = (ASTElementaryTypeNameExpression) functionCall.getCalled();

                // A conversion operation - int8(...)
                ArrayList<ASTNode> arguments = functionCall.getArguments();
                if (arguments.size() != 1) {
                    throw new Exception("Unexpected elementary type name conversion argument count (not 1): " + arguments.size());
                }
                Expression toConvert = fromASTNode(ast, contractDefinition, contractValueContext, environment, arguments.get(0));
                result = new Expression(toConvert, elementaryTypeNameExpression.getElementaryTypeName()); // type conversion expression
            } else if (functionCall.getCalled() instanceof ASTMemberAccess
                || functionCall.getCalled() instanceof ASTFunctionCall) { // TODO what about arrays?
                // This may be a structure initializer with canonical name including the contract:
                //     s0 s = c0.s0(1, 2, 3...);
                // Or it could be a function call to a contract method:
                //     bool b = c0.f();
                // TODO filter out the struct init case, handle like non-canonical structs below

                // This must be a call to an expression of type function
                Expression calledExpression = fromASTNode(ast, contractDefinition, contractValueContext, environment, functionCall.getCalled());
                if (!(calledExpression.getType() instanceof ASTFunctionTypeName)) {
                    throw new Exception("Call to something that is not a function in " + functionCall.toSolidityCode()
                        + ", type is " + calledExpression.getType().toSolidityCode());
                }
                ASTFunctionTypeName functionTypeName = (ASTFunctionTypeName) calledExpression.getType();
                ArrayList<Expression> arguments = functionCall.getExpressionArguments(contractDefinition, contractValueContext, environment);
                ASTNode returnType = functionTypeName.getReturnType();
                result = new Expression(calledExpression, arguments, returnType);
            } else if (functionCall.getCalled() instanceof ASTIdentifier) {
                // Look up called function (note: function declarations without body are also ASTFunctionDefinitions)
                ASTFunctionDefinition functionDefinition = contractDefinition.getFunction(functionCall.getCalled().getName());
                ASTNode returnType = null;

                if (functionDefinition == null) {
                    ASTStructDefinition structDefinition;
                    if ((structDefinition = contractDefinition.getStructDefinition(functionCall.getCalled().getName())) != null) {
                        // Structure value
                        returnType = structDefinition.getUserDefinedType();
                        functionCall.setInterpretationStructDefinition(structDefinition);
                    } else if (functionCall.getCalled().getName().equals("keccak256")) {
                        // Builtin
                        returnType = new ASTElementaryTypeName(0, ASTElementaryTypeName.VARIABLE_LENGTH_BYTES_TYPE_NAME);
                    } else {
                        // This may also be a variable holding a function value
                        Variable variable = environment.getVariableIncludingParentEnvironments(functionCall.getCalled().getName());

                        if (variable == null) {
                            Console.error(functionCall, "Cannot locate function '" + functionCall.getCalled().getName()
                                    + "', in call " + functionCall.toSolidityCode());
                            throw new Exception("ExpressionBuilder.fromASTNode failed");
                        }
                        if (!Type.isFunctionType(variable.getType())) {
                            throw new Exception("Unexpected call to non-function item " + variable.getName() +
                                    " of type " + variable.getType().toSolidityCode());
                        }



                        // OK - continue below ...
                        FunctionValue value = (FunctionValue)environment.resolveVariableValueIncludingParentEnvironments(0, variable.getName());

                        functionDefinition = value.getFunctionDefinition();
                        returnType = functionDefinition.getReturnType();
                        functionCall.setInterpretationFunctionValue(value); //functionDefinition);
                    }
                } else {
                    returnType = functionDefinition.getReturnType();
                    //functionCall.setInterpretationFunctionDefinition(functionDefinition);
                    functionCall.setInterpretationFunctionValue(
                            new FunctionValue(contractDefinition, functionDefinition, functionDefinition.getFunctionType(), contractValueContext)
                    );
                }
                // Build Expression objects for all function arguments
                ArrayList<Expression> arguments = functionCall.getExpressionArguments(contractDefinition, contractValueContext, environment);
                Expression callExpression = new Expression(functionCall, arguments, returnType);
                result = callExpression;
            } else if (functionCall.getCalled() instanceof ASTNewExpression) {
                ASTNewExpression newExpression = (ASTNewExpression)functionCall.getCalled();
                ASTNode typeName = newExpression.getTypeName();
                ASTContractDefinition contractTypeDefinition = null;

                if (!(typeName instanceof ASTUserDefinedTypeName)
                    || (contractTypeDefinition = ast.getContract(((ASTUserDefinedTypeName)typeName).getName())) == null) {
                    Console.error(functionCall, "Unexpected argument to 'new' - not a contract type: "
                            + ((ASTUserDefinedTypeName)typeName).getName());
                    Util.unimpl();
                }

                ArrayList<Expression> arguments = functionCall.getExpressionArguments(contractDefinition, contractValueContext, environment);
                result = new Expression(contractTypeDefinition, arguments);
            } else {
                throw new Exception("Function call to unsupported item " + functionCall.getCalled().getClass().getName());
            }
        } else if (astNode instanceof  ASTBinaryOperation) {
            ASTBinaryOperation binaryOperation = (ASTBinaryOperation)astNode;
            Expression binaryExpression = new Expression(
                fromASTNode(ast, contractDefinition, contractValueContext, environment, binaryOperation.getLeftOperand()),
                binaryOperation.getOperator(),
                fromASTNode(ast, contractDefinition, contractValueContext, environment, binaryOperation.getRightOperand())
            );
            result = binaryExpression;
        } else if (astNode instanceof ASTConditional) {
            ASTConditional conditional = (ASTConditional) astNode;

            Expression conditionalExpression = new Expression(
                    fromASTNode(ast, contractDefinition, contractValueContext, environment, conditional.getCondition()),
                    fromASTNode(ast, contractDefinition, contractValueContext, environment, conditional.getTrueBranch()),
                    fromASTNode(ast, contractDefinition, contractValueContext, environment, conditional.getFalseBranch())
            );
            result = conditionalExpression;
        } else if (astNode instanceof ASTTupleExpression) {
            ASTTupleExpression tupleExpression = (ASTTupleExpression) astNode;
            ArrayList<Expression> tupleComponents = new ArrayList<Expression>();

            if (tupleExpression.getCount() == 1) {
                // The type of a single-item tuple troublingly decays to the type of its only component, e.g. in
                //     0     ==   (true? 0: 1)
                //     ^ int      ^^^^^^^^^^^^ tuple expression giving int
                // So we avoid modeling the tuple entirely and substitute it with the only element.
                result = fromASTNode(ast, contractDefinition, contractValueContext, environment, tupleExpression.getComponent(0));
            } else {
                for (int i = 0; i < tupleExpression.getCount(); ++i) {
                    ASTNode component = tupleExpression.getComponent(i);
                    tupleComponents.add(fromASTNode(ast, contractDefinition, contractValueContext, environment, component));
                }
                result = new Expression(tupleComponents, tupleExpression);
            }
        } else if (astNode instanceof ASTAssignment) {
            ASTAssignment assignmentExpression = (ASTAssignment) astNode;
            Expression lhsExpression = fromASTNode(ast, contractDefinition, contractValueContext, environment, assignmentExpression.getLHS());
            Expression rhsExpression = fromASTNode(ast, contractDefinition, contractValueContext, environment, assignmentExpression.getRHS());

            // For now, we convert every compound assignment  x op y  to its non-compound form   x = x op y
            if (assignmentExpression.getOperator() != ASTAssignment.Operator.OP_ASSIGN) {
                ASTBinaryOperation.Operator binaryOperator = null;
                switch (assignmentExpression.getOperator()) {
                    case OP_ASSIGN_COMP_BAND:
                        binaryOperator = ASTBinaryOperation.Operator.OP_BAND;
                        break;
                    case OP_ASSIGN_COMP_BOR:
                        binaryOperator = ASTBinaryOperation.Operator.OP_BOR;
                        break;
                    case OP_ASSIGN_COMP_BXOR:
                        binaryOperator = ASTBinaryOperation.Operator.OP_BXOR;
                        break;
                    case OP_ASSIGN_COMP_DIV:
                        binaryOperator = ASTBinaryOperation.Operator.OP_DIV;
                        break;
                    case OP_ASSIGN_COMP_MINUS:
                        binaryOperator = ASTBinaryOperation.Operator.OP_MINUS;
                        break;
                    case OP_ASSIGN_COMP_MOD:
                        binaryOperator = ASTBinaryOperation.Operator.OP_MOD;
                        break;
                    case OP_ASSIGN_COMP_MUL:
                        binaryOperator = ASTBinaryOperation.Operator.OP_MUL;
                        break;
                    case OP_ASSIGN_COMP_PLUS:
                        binaryOperator = ASTBinaryOperation.Operator.OP_PLUS;
                        break;
                    case OP_ASSIGN_COMP_SHL:
                        binaryOperator = ASTBinaryOperation.Operator.OP_BSHL;
                        break;
                    case OP_ASSIGN_COMP_SHR:
                        binaryOperator = ASTBinaryOperation.Operator.OP_BSHR;
                        break;
                    default:
                        throw new Exception("ExpressionBuilder.fromASTNode: unsupported compound assignment operator "
                                + assignmentExpression.getOperator());
                }
                // Transform right side
                rhsExpression = new Expression(
                        lhsExpression,
                        binaryOperator,
                        rhsExpression
                );

            }
            result = new Expression(lhsExpression, ASTAssignment.Operator.OP_ASSIGN, rhsExpression);
        } else if (astNode instanceof ASTUnaryOperation) {
            ASTUnaryOperation unaryOperation = (ASTUnaryOperation) astNode;
            result = new Expression(unaryOperation.getOperator(),
                    fromASTNode(ast, contractDefinition, contractValueContext, environment, unaryOperation.getOperand()));
        } else if (astNode instanceof ASTMemberAccess) {
            ASTMemberAccess memberAccess = (ASTMemberAccess) astNode;
            Expression appliedToExpression = fromASTNode(ast, contractDefinition, contractValueContext, environment, memberAccess.getAppliedTo());

            ASTStructDefinition structDefinition = Type.getStructType(ast, appliedToExpression.getType());

            if (structDefinition != null) { // TODO same for contract?
                ASTVariableDeclaration accessedMember = structDefinition.lookupMember(memberAccess.getName());
                if (accessedMember == null) {
                    throw new Exception("ExpressionBuilder.fromASTNode access to nonexistent struct member "
                            + memberAccess.getName()
                            + " in " + structDefinition.getCanonicalName());
                }
                result = new Expression(appliedToExpression, accessedMember);
            } else {
                ASTContractDefinition accessedContractDefinition = Type.getContractType(ast, appliedToExpression.getType());
                if (accessedContractDefinition == null) {
                    throw new Exception("ExpressionBuilder.fromASTNode member access to something that is not a struct");
                }
                ASTNode accessedMember = accessedContractDefinition.lookupVariableOrFunctionDeclaration(memberAccess.getName());
                if (accessedMember == null) {
                    throw new Exception("ExpressionBuilder.fromASTNode access to nonexistent contract member "
                            + memberAccess.getName()
                            + " in " + accessedContractDefinition.getName());
                }

                // This may be a member variable declaration or a function declaration. We have to untangle this here
                // already to obtain the type of the expression
                ASTNode type;
                if (accessedMember instanceof ASTVariableDeclaration) {
                    type = ((ASTVariableDeclaration)accessedMember).getTypeName();
                } else if (accessedMember instanceof ASTFunctionDefinition) {
                    ASTFunctionDefinition functionDefinition = (ASTFunctionDefinition)accessedMember;
                    type = functionDefinition.getFunctionType();
                } else {
                    Util.unimpl();
                    throw new Exception("unimpl");
                }
                result = new Expression(appliedToExpression, accessedMember, type);
            }
        } else {
            throw new Exception("ExpressionBuilder.fromASTNode for unimplemented node type " + astNode.getClass().getName());
        }

        return result;
    }
}
