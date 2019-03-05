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

import soltix.ast.ASTBinaryOperation;
import soltix.ast.ASTElementaryTypeName;
import soltix.ast.ASTUnaryOperation;
import soltix.interpretation.values.IntegerValue;
import soltix.interpretation.values.Value;
import soltix.interpretation.values.ValueContainer;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.synthesis.ExpressionGenerator;
import soltix.synthesis.ValueGenerator;
import soltix.util.RandomNumbers;

import static soltix.ast.ASTBinaryOperation.Operator.OP_DIV;
import static soltix.ast.ASTBinaryOperation.Operator.OP_LT;
import static soltix.ast.ASTBinaryOperation.Operator.OP_PLUS;

/**
 * Class to perform corrective measures on invalid expressions.
 */
public class ExpressionEvaluationErrorHandler implements IExpressionEvaluatorErrorCallback {
    private RandomNumbers prng;
    private ValueGenerator valueGenerator;

    public ExpressionEvaluationErrorHandler(RandomNumbers prng) {
        this.prng = prng;
        valueGenerator = new ValueGenerator(prng);
    }

    // TODO write set of generic fix-up routines, randomly (weighted) use those before falling back to error-specific
    // solutions
    // TODO: E.g. function calls mapping bad to good value, conditional operator

    public Expression fixZeroDivision(ExpressionEvaluator expressionEvaluator,
                                      VariableEnvironment environment,
                                      Expression originalExpression,
                                      ExpressionEvaluator.ComputedValues values) throws Exception {

        IntegerValue zeroValue = ValueContainer.getSmallIntegerValue(originalExpression.getType(), 0);

        for (int i = 0; i < 10; ++i) {
            // Try to pick some small random number that can be added to or subtracted from the existing expression without
            // ever resulting in a zero value
            IntegerValue candidateOffset;
            try {
                candidateOffset = (IntegerValue)valueGenerator.generateRandomValue(null, originalExpression.getType(),
                                                            ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_EXCLUSIVELY_SMALL);
            } catch (Exception e) {
                return null;
            }

            boolean subtractionOK = true;
            boolean additionOK = true;
            for (int j = 0; j < values.values.size(); ++j) {
                IntegerValue currentValue = (IntegerValue) values.values.get(j);
                if (additionOK && currentValue.add(candidateOffset).isEqualTo(zeroValue).getValue()) {
                    additionOK = false;
                }
                if (subtractionOK && currentValue.sub(candidateOffset).isEqualTo(zeroValue).getValue()) {
                    subtractionOK = false;
                }
                if (!additionOK && !subtractionOK) {
                    break;
                }
            }

            // TODO See comment in ExpressionEvaluator.evaluateBinaryOperatorForAll() on why evaluateForAll() is used.
            // We don't do this in fixNegativeOperand() and probably can't do it there so easily (argument isn't just
            // a constant but may contain side effects?!), so the workaround is probably incomplete anyway
            if (additionOK) {
                Expression candidateOffsetExpression = new Expression(candidateOffset);
                expressionEvaluator.evaluateForAll(environment, candidateOffsetExpression);
                //return new Expression(originalExpression, ASTBinaryOperation.Operator.OP_PLUS, candidateOffsetExpression);
                return new Expression(candidateOffsetExpression, ASTBinaryOperation.Operator.OP_PLUS, originalExpression);
             //
            }
            if (subtractionOK) {
                Expression candidateOffsetExpression = new Expression(candidateOffset);
                expressionEvaluator.evaluateForAll(environment, candidateOffsetExpression);
                //return new Expression(originalExpression, ASTBinaryOperation.Operator.OP_MINUS, candidateOffsetExpression);
                return new Expression(candidateOffsetExpression, ASTBinaryOperation.Operator.OP_MINUS, originalExpression);
            }
        }

        // Use less sophisticated default fix-up routine instead
        return ExpressionGenerator.makeOperandSafeForOperation(prng, OP_DIV, originalExpression);
    }


    public Expression fixNegativeOperand(Expression originalExpression,
                                        ExpressionEvaluator.ComputedValues values) throws Exception {
        // TODO 1. Move this to a separate method? 2. Support more transformatons?
        // Address a negative second operand by adding a unary negation where needed.
        // This may need to be wrapped up in a conditional operator, since positive and negative
        // values may be encountered for the same expression at different times
        Expression newExpression;

        /*System.out.println("fixnegative - values");
        for (int i = 0; i < values.values.size(); ++i) {
            System.out.println("   i: " + values.values.get(i).toASTNode(false).toSolidityCode());
        }*/

        if (values.values.size() > 1) {
            // Wrap up in conditional operator
            Expression negatedOriginalExpression = new Expression(ASTUnaryOperation.Operator.OP_MINUS, originalExpression);
            Expression conditionExpression = new Expression(originalExpression,
                    OP_LT,
                    new Expression(ValueContainer.getSmallIntegerValue(originalExpression.getType(), 0)));
            newExpression = new Expression(conditionExpression, negatedOriginalExpression, originalExpression);
        } else {
            // A negation for the single value is sufficient for most values
            // Special case: Signed types with their minimum value yield that same value when negated. So
            // for those we add 1 to e.g. turn -128 into -127 before negating to 127
            IntegerValue value = (IntegerValue)values.values.get(0);
            if (((ASTElementaryTypeName)value.getType()).isSigned() && value.isEqualTo(value.getMinValue()).getValue()) {
                // Subtract 1, negate below
                newExpression = new Expression(originalExpression, OP_PLUS, new Expression(new IntegerValue(value.getType(), 1)));
            } else {
                newExpression = originalExpression;
            }
            // Negate
            newExpression = new Expression(ASTUnaryOperation.Operator.OP_MINUS, newExpression);
        }

        return newExpression;
    }
}
