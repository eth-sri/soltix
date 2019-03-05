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

import soltix.interpretation.values.Value;

import java.util.ArrayList;

/**
 * Class to represent an exception caused by evaluating an invalid expression
 */
public class ExpressionEvaluationException extends Exception {
    private Expression faultyExpression;
    private ExceptionType type;
    private String text;
    private ExpressionEvaluator.ComputedValues values;

    // There are currently two general categories of evaluation exceptions (that could also be split into separate
    // exception classes if need be):
    //
    //    - Type mismatch issues, such as mixing signed and unsigned integers in arithmetic, that could be fixed
    // with a cast
    //    - Invalid operand values, such as a zero division or negative shift count
    //
    // Type errors must notably also be detected for empty value sets: If a statement was never reached, it may be
    // associated with a VariableEnvironment holding 0 values for each variable. It is probably still desirable to
    // synthesize live code mutation expressions (which will never be executed) in this case, but the type errors
    // must be caught independently rather than as a byproduct of evaluating values
    public enum ExceptionType {
        // Type errors
        ExceptionType_TypeError_SignednessMismatch,   // mix of signed vs unsigned
        ExceptionType_TypeError_WrongSign,            // for **: args are signed, should be unsigned
        // Value errors
        ExceptionType_ValueError_ArrayBounds,
        ExceptionType_ValueError_ZeroDivision,
        ExceptionType_ValueError_NegativeSecondOperand
    }

    public ExpressionEvaluationException(Expression faultyExpression,
                                         ExceptionType type,
                                         String text,
                                         ExpressionEvaluator.ComputedValues values) {
        this.faultyExpression = faultyExpression;
        this.type = type;
        this.text = text;
        this.values = values;
    }

    public ExceptionType getType() { return type; }
    public Expression getFaultyExpression() { return faultyExpression; }
    public ExpressionEvaluator.ComputedValues getValues() { return values; }


    public void sanityCheckCorrectionContext(Expression context) throws Exception {
        if (faultyExpression != context) {
            throw new Exception("Attempt to fix up faulty expression in wrong context. Faulty: "
                    + faultyExpression.toASTNode().toSolidityCode()
                    + " but trying to fix " + context.toASTNode().toSolidityCode());
        }
    }

    @Override
    public String toString() {
        return new String("ExpressionEvaluationException: " + text);
    }
}
