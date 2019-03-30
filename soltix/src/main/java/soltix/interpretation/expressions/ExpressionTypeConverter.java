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

import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.synthesis.TypeConverter;
import soltix.util.RandomNumbers;

import static soltix.ast.ASTBinaryOperation.Operator.OP_EXP;

/**
 * Class to bring two expressions to a compatible type for binary operations by converting as needed
 */
public class ExpressionTypeConverter {
    private Expression firstOperand;
    private Expression secondOperand;
    private AST ast;
    private ExpressionEvaluator expressionEvaluator;
    private TypeConverter typeConverter;

    public ExpressionTypeConverter(AST ast,
                                   ExpressionEvaluator expressionEvaluator,
                                   TypeConverter typeConverter) {
        this.ast = ast;
        this.expressionEvaluator = expressionEvaluator;
        this.typeConverter = typeConverter;
    }

    public void setFirstOperand(Expression firstOperand) { this.firstOperand = firstOperand; }
    public void setSecondOperand(Expression secondOperand) { this.secondOperand = secondOperand; }
    public Expression getFirstOperand() { return firstOperand; }
    public Expression getSecondOperand() { return secondOperand; }

    public void run(VariableEnvironment environment, RandomNumbers prng) throws Exception {
        // Most distinct types aren't compatible for any operations, so we convert one operand to the
        // other type (typically not preserving any semblance to the original value)
        if (Configuration.verboseDebugOutput) System.out.println("    FIRST OPERAND " + firstOperand.toASTNode().toSolidityCode() + "   SECOND   " + secondOperand.toASTNode().toSolidityCode());

        // TODO Move all conversions below to TypeConverter?
        // TODO The type conversions here are pretty ad-hoc, e.g. in byte+integer being allowed for shifts.
        // It may be more appropriate to choose an operator to use first, then ensure the correct type of
        // both operands. But this approach would differ from what's described in the paper.
        // TODO Unification with expression evaluation exceptions (+ callbacks) to adapt things on demand
        // TODO There should also be more randomized conversion. Eg. in byte+integer a shift operator will
        // probably be selected but it may also be interesting to convert the integer to byte and apply
        // some other operator some of the time
        if (Type.isIntegerType(firstOperand.getType()) && Type.isIntegerType(secondOperand.getType())) {
            // Integers can be implicitly converted to a common type if they have the same sign
            ASTElementaryTypeName firstType = (ASTElementaryTypeName) firstOperand.getType();
            ASTElementaryTypeName secondType = (ASTElementaryTypeName) secondOperand.getType();
            if (firstType.isSigned() != secondType.isSigned()) {
                // Cast first operand to other type
                Expression castExpression = new Expression(firstOperand, secondOperand.getType());
                firstOperand = castExpression;
            } else if (firstType.getBits() != secondType.getBits()) {
                // TODO This could randomly choose between an explicit and implicit conversion (the latter always
                // converts to the larger type) at the level Solidity, although internally we need to apply both
                // the same in Expression
                Expression castExpression = new Expression(firstOperand, secondOperand.getType());
                firstOperand = castExpression;
            }
        } else if (Type.isByteType(firstOperand.getType())
                && Type.isIntegerType(secondOperand.getType())
                && !Configuration.ganacheOptimizationWorkarounds
                && !Configuration.avoidShiftOperators
                && prng.flipCoin()) {
            // This enables bitwise shift operators. The flipCoin() above is mixed in to permit this only
            // half the time and convert the second operand otherwise. This reduces the bias on shift
            // operations for the type combination
            ;
        } else if (Type.isByteType(firstOperand.getType()) && Type.isByteType(secondOperand.getType())) {
            // All bytes operations on two bytes types expect them to be of the same size - convert to second
            // operand type for now.
            // TODO As in the integers case, this could distinguish between explicit/implicit conversions and
            // randomize the target type
            Expression castExpression = new Expression(firstOperand, secondOperand.getType());
            firstOperand = castExpression;
        } else {
            // Adapt second operand
            secondOperand = typeConverter.convertToCompatibleType(environment, secondOperand, firstOperand.getType());

            // First and second operand are of the same type now, that may however still be unusable for most
            // operations. This is the case for structs and arrays, where we now first convert both expressions
            // to random non-struct, non-array member/element values
            ASTNode currentSharedType = firstOperand.getType();
            if (currentSharedType instanceof ASTUserDefinedTypeName) {
                ASTStructDefinition structDefinition = ast.getStructDefinition(currentSharedType.getName());
                if (structDefinition != null) {
                    Expression firstRandomMember = typeConverter.accessRandomStructMember(environment, firstOperand, structDefinition);
                    Expression secondRandomMember = typeConverter.accessRandomStructMember(environment, secondOperand, structDefinition);
                    secondRandomMember = typeConverter.convertToCompatibleType(environment, secondRandomMember, firstRandomMember.getType());
                    firstOperand = firstRandomMember;
                    secondOperand = secondRandomMember;
                    currentSharedType = firstOperand.getType();
                } else {
                    throw new Exception("ExpressionTypeConverter: Cannot locate struct type " + currentSharedType.getName());
                }
            } else if (currentSharedType instanceof ASTArrayTypeName) {
                Expression firstRandomElement = typeConverter.accessRandomArrayElement(environment, firstOperand);
                Expression secondRandomElement = typeConverter.accessRandomArrayElement(environment, secondOperand);

                // Both expressions cannot be assumed to have the same type, since struct members are also
                // traversed randomly and may result in different terminal members
                secondRandomElement = typeConverter.convertToCompatibleType(environment, secondRandomElement, firstRandomElement.getType());
                firstOperand = firstRandomElement;
                secondOperand = secondRandomElement;
                currentSharedType = firstOperand.getType();
            }
            // If this was a string from the beginning or since the conversions above, it must also be
            // converted to some other type, since there are no defined operators on strings
            if (Type.isStringType(currentSharedType)) {
                // Convert to keccak256 hash for now. This enables meaningful == and != operations too
                firstOperand = typeConverter.hashString(environment, firstOperand);
                secondOperand = typeConverter.hashString(environment, secondOperand);
                // For now: Take a string element s[x] and convert it to a randomly chosen type
                //    ASTElementaryTypeName newType;
                //    switch ((int)prng.generateLongInteger(0, 2)) {
                //        case 0:
                //            newType = TypeContainer.getRandomIntegerType(prng);
                //            break;
                //        case 1:
                //            newType = TypeContainer.getBoolType();
                //            break;
                //        case 2:
                //            newType = TypeContainer.getRandomByteType(prng);
                //             break;
                //        default:
                //            newType = null;
                //    }
                //    firstOperand = convertToCompatibleType(firstOperand, newType);
                //    secondOperand = convertToCompatibleType(secondOperand, newType);
            }
        }
    }
}
