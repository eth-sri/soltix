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

import soltix.ASTEditor;
import soltix.Configuration;
import soltix.ast.AST;
import soltix.ast.ASTBinaryOperation;
import soltix.ast.ASTNode;
import soltix.ast.ASTUnaryOperation;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.expressions.ExpressionEvaluationErrorHandler;
import soltix.interpretation.expressions.ExpressionEvaluator;
import soltix.interpretation.values.BoolValue;
import soltix.interpretation.values.IntegerValue;
import soltix.interpretation.values.Value;
import soltix.interpretation.values.ValueContainer;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.interpretation.variables.VariableValues;
import soltix.util.RandomNumbers;

/**
 * Class to generate randomized predicates with a specified truth value.
 *
 * This follows the outline given in "Finding Compiler Bugs via Live Code Mutation" (Sun et al., 2016)
 */
public class PredicateGenerator {
    private RandomNumbers prng;
    private AST ast; // for things like user-defined type lookups
    private ASTEditor astEditor;
    private ExpressionEvaluator expressionEvaluator;

    public PredicateGenerator(AST ast, ASTEditor astEditor, RandomNumbers prng) {
        this.prng = prng;
        this.ast = ast;
        this.astEditor = astEditor;
        expressionEvaluator = new ExpressionEvaluator(new ExpressionEvaluationErrorHandler(prng), null); //ast);
    }

    protected Expression debugVerifyResult(VariableEnvironment environment, Expression result, boolean expectedValue) throws Exception {
        if (Configuration.checkCompileTimeTautologyCorrectness) {
            // Check whether the expected predicate result is fulfilled for all variable values that were encountered
            // TODO turn this into evaluateForAll
            for (int i = 0; i < environment.getValueCount(); ++i) {
                Value synthesizedValue = expressionEvaluator.evaluateForOne(environment, i, result);

                if (!(synthesizedValue instanceof BoolValue)
                        || ((BoolValue) synthesizedValue).getValue() != expectedValue) {
                    environment.printValueSetToLogger(i);
                    throw new Exception("Synthesized boolean expression with unexpected value: " + result.toASTNode().toSolidityCode()
                            + " != expectedValue " + expectedValue + " for statement " + environment.getStatementID());
                }
            }
        }
        return result;
    }

    public Expression synthesizePredicate(VariableEnvironment environment, boolean expectedValue, int depth) throws Exception {
        if (depth == 0) {
            return synthesizeAtom(environment, expectedValue);
        }

        Expression result;

        switch ((int)prng.generateLongInteger(0, 3)) {
            case 0:
                result = synthesizeNegation(environment, expectedValue, depth);
                break;
            case 1:
                result = synthesizeConjunction(environment, expectedValue, depth);
                break;
            case 2:
                result = synthesizeDisjunction(environment, expectedValue, depth);
                break;
            case 3:
                result = synthesizeAtom(environment, expectedValue);
                break;
            default:
                throw new Exception("synthesizePredicate: Unknown switch case");
        }

        if (Configuration.checkRuntimeTautologyCorrectness) {
            astEditor.prependExpressionValueCheck(result, expectedValue);
        }
        return result;
    }

    protected Expression synthesizeNegation(VariableEnvironment environment, boolean expectedValue, int depth) throws Exception {
        return new Expression(ASTUnaryOperation.Operator.OP_LNEG, synthesizePredicate(environment, !expectedValue, depth - 1));
    }

    protected Expression synthesizeConjunction(VariableEnvironment environment, boolean expectedValue, int depth) throws Exception {
        boolean leftExpectedValue;
        boolean rightExpectedValue;

        if (expectedValue) {
            leftExpectedValue = true;
            rightExpectedValue = true;
        } else if (prng.flipCoin()) {
            leftExpectedValue = true;
            rightExpectedValue = false;
        } else {
            leftExpectedValue = false;
            rightExpectedValue = prng.flipCoin();
        }

        Expression leftPredicate = synthesizePredicate(environment, leftExpectedValue, depth - 1);
        Expression rightPredicate = synthesizePredicate(environment, rightExpectedValue, depth - 1);
        return debugVerifyResult(environment, new Expression(leftPredicate, ASTBinaryOperation.Operator.OP_LAND, rightPredicate), expectedValue);
    }

    protected Expression synthesizeDisjunction(VariableEnvironment environment, boolean expectedValue, int depth) throws Exception {
        boolean leftExpectedValue;
        boolean rightExpectedValue;

        if (!expectedValue) {
            leftExpectedValue = false;
            rightExpectedValue = false;
        } else if (prng.flipCoin()) {
            leftExpectedValue = false;
            rightExpectedValue = true;
        } else {
            leftExpectedValue = true;
            rightExpectedValue = prng.flipCoin();
        }

        Expression leftPredicate = synthesizePredicate(environment, leftExpectedValue, depth - 1);
        Expression rightPredicate = synthesizePredicate(environment, rightExpectedValue, depth - 1);
        return debugVerifyResult(environment, new Expression(leftPredicate, ASTBinaryOperation.Operator.OP_LOR, rightPredicate), expectedValue);
    }

    protected Expression synthesizeNonIntegerExpression(VariableEnvironment environment, boolean expectedValue) throws Exception {
        // TODO Generate more complex expression using non-integer variables if possible
        // TODO Test case 00ebfdb326ed8bc7f73bd50228e63614ceb69af586f1764dcda0afc12a9c5520 is an example with strings
        return new Expression(ValueContainer.getBoolValue(expectedValue));
    }

    protected Expression synthesizeAtom(VariableEnvironment environment, boolean expectedValue) throws Exception {
        Expression result;
        if (true) { // TODO:   prng.flipCoin()) {
            // Rule 1:
            // Choose one variable randomly and...
            int variableCount = environment.getIntegerVariableCount();
            if (variableCount == 0) {
                // Fallback - generate anything that fulfills the value
                return synthesizeNonIntegerExpression(environment, expectedValue);
            }
            int chosenVariableIndex = (int)prng.generateLongInteger(1, variableCount) - 1;
            VariableValues variableValues = environment.getIntegerVariableValues(chosenVariableIndex);
            // TODO Support integer arrays and structs with integer fields as well

            // Workaround for integer values that couldn't properly be read from JSON text:
            if (variableValues.getValueCount() == 0 || !(variableValues.getValue(0) instanceof IntegerValue)) {
                return new Expression(ValueContainer.getBoolValue(expectedValue));
            }

            result = synthesizeRelationalPredicateOneVariable(variableValues, expectedValue);
        } else {
        }
        return debugVerifyResult(environment, result, expectedValue);
    }


    protected Expression synthesizeRelationalPredicateOneVariable(VariableValues variableValues, boolean expectedValue) throws Exception {
        // Randomly select an operator to relate the variable to some constant number to yield the desired expectedValue
        // result. The profiled variable state gives us an interval [min,max] containing the encountered variables.
        // To relate some constant c to such a variable x, we can pick a number outside of the interval - which may
        // be a problem for unsigned variables set to 0. To address this, we could pick a number in a hole between
        // two sub intervals instead by generating a more complex expression with the && operator
        int desiredOperatorIndex = (int)prng.generateLongInteger(0, 3);
        Value generatedValue = null;
        ASTBinaryOperation.Operator operator = null;

        int val = (int)prng.generateLongInteger(0, 3);
        switch (val) {
            case 0: // <
                if (expectedValue == true) {
                    // Generated value is always smaller
                    generatedValue = ValueGenerator.generateSmallerIntegerValue(variableValues.getSmallestValue());
                } else {
                    // Generated value is never smaller
                    generatedValue = variableValues.getLargestValue();
                }
                operator = ASTBinaryOperation.Operator.OP_LT;
                break;
            case 1: // >
                if (expectedValue == true) {
                    // Generated value is always larger
                    generatedValue = ValueGenerator.generateBiggerIntegerValue(variableValues.getLargestValue());
                } else {
                    // Generated value is never larger
                    generatedValue = variableValues.getSmallestValue();
                }
                operator = ASTBinaryOperation.Operator.OP_GT;
                break;
            case 2: // <=
                if (expectedValue == true) {
                    // Generated value is always smaller or equal
                    generatedValue = variableValues.getSmallestValue();
                } else {
                    // Generated value is never smaller or equal
                    generatedValue = ValueGenerator.generateBiggerIntegerValue(variableValues.getLargestValue());
                }
                operator = ASTBinaryOperation.Operator.OP_LTE;
                break;
            case 3: // >=
                if (expectedValue) {
                    // Generated value is always larger or equal
                    generatedValue = variableValues.getLargestValue();
                } else {
                    // Generated value is never larger or equal
                    generatedValue = ValueGenerator.generateSmallerIntegerValue(variableValues.getSmallestValue());
                }
                operator = ASTBinaryOperation.Operator.OP_GTE;
                break;
                // TODO do (in-)equality too?
            default:
                throw new Exception("Cannot generate relational expression");
        }

        if (generatedValue == null) {
            // The desired truth value cannot be fulfilled for the selected operator. For now we
            // insert "var != var" or "var == var" for false or true instead.
            // TODO Consider sub-intervals to find unused values and build more complex expressions instead
            Expression variableExpression = new Expression(variableValues.getVariable());
            if (expectedValue == true) {
                return new Expression(variableExpression, ASTBinaryOperation.Operator.OP_EQ, variableExpression);
            } else {
                return new Expression(variableExpression, ASTBinaryOperation.Operator.OP_NEQ, variableExpression);
            }
        } else {
            Expression firstOperand;
            Expression secondOperand;

            // Reverse operands half the time to ensure that the variable can also be the first operand
            // in this expression, not only the constant
            if (prng.flipCoin()) {
                // Normal order for selected operator - constant first
                firstOperand = new Expression(generatedValue);
                secondOperand = new Expression(variableValues.getVariable());
            } else {
                // Flipped order - variable first
                firstOperand = new Expression(variableValues.getVariable());
                secondOperand = new Expression(generatedValue);
                // Reverse operator
                if      (operator == ASTBinaryOperation.Operator.OP_LT)  operator = ASTBinaryOperation.Operator.OP_GT;  // <    =>    >
                else if (operator == ASTBinaryOperation.Operator.OP_GT)  operator = ASTBinaryOperation.Operator.OP_LT;  // >    =>    <
                else if (operator == ASTBinaryOperation.Operator.OP_LTE) operator = ASTBinaryOperation.Operator.OP_GTE; // <=   =>    >=
                else if (operator == ASTBinaryOperation.Operator.OP_GTE) operator = ASTBinaryOperation.Operator.OP_LTE; // >=   =>    <=
                else throw new Exception("Cannot flip unexpected predicate operator " + operator);
            }
            return new Expression(firstOperand, operator, secondOperand);
        }
    }

    protected Expression synthesizeRelationalPredicateTwoVariables() {
        return null;
    }

    // Synthesize a predicate with a known truth value based on a given expression (this isn't directly used
    // by pure predicate generation but in an expression conversion context for now)
    public Expression synthesizeRelationalPredicateOneExpression(VariableEnvironment environment,
                                                                 Expression expression,
                                                                 boolean expectedValue) throws Exception {
        // We take the very naive approach of:
        //   1. Computing the expression value for each encountered variable set
        //   2. Choosing a result value that wasn't encountered - if possible (or generating some fallback value instead)
        //   3. Relating it to the expression such that the desired predicate value holds if possible
        throw new Exception("PredicateGenerator.synthesizeRelationalPredicateOneExpression is unimplemented");
    }
}
