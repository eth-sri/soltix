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
import soltix.ast.ASTNode;
import soltix.ast.ASTUnaryOperation;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.values.IntegerValue;
import soltix.interpretation.values.ValueContainer;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.interpretation.variables.VariableValues;
import soltix.util.RandomNumbers;
import org.junit.Assert;
import org.junit.Test;

import static soltix.ast.ASTBinaryOperation.Operator.OP_BSHL;

/*
 * Tests for corrections of faulty expressions using ExpressionEvaluationErrorHandler (non-exhaustive)
 */
public class ExpressionEvaluationErrorHandlerTest extends UtilExpressionTests {

    @Test
    public void testFixNegativeOperand() throws Exception {
        // Environment: Create variables v41, v63
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        ASTNode v1Type = TypeContainer.getIntegerType(true, 32);
        Variable varV1 = createVariable(v1Type, "v1");
        VariableValues varV1Values = new VariableValues(varV1, 0);

        varV1Values.addValue(new IntegerValue(v1Type, -3));

        environment.addVariableValues(varV1, varV1Values);
        environment.finishAddingValues();

        // Prepare expression evaluation
        RandomNumbers prng = new RandomNumbers(1);
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(new ExpressionEvaluationErrorHandler(prng)); //null);
        e_expressionEvaluator = expressionEvaluator;


        // The expression given here -
        //      1 << ++v1    with    v1 = -3
        // should fault due to a negative shift operator, resulting in a fixed expression
        //      1 << -(++v1)
        // which should increment v1 from -3 to -2, then negate it to 2, giving
        //      1 << 2     =    4 as total result
        //
        // This test addresses the bug where "++v1" was evaluated twice, giving a result "v1 += 2" - once for the
        // original and once for the fixed expression

        Expression faultyExpression = new Expression( // 1 << ++v1
                new Expression(ValueContainer.getSmallIntegerValue(v1Type, 1)),
                ASTBinaryOperation.Operator.OP_BSHL,
                new Expression(ASTUnaryOperation.Operator.OP_INC_PRE, new Expression(varV1)));

        String originalExpressionCode = faultyExpression.toASTNode().toSolidityCode();

        String result = e(faultyExpression);
        System.out.println("got " + originalExpressionCode + " fixed to " + faultyExpression.toASTNode().toSolidityCode() + " = " + result);
        Assert.assertTrue(result.equals("int32(4)") || result.equals("4"));
    }

    @Test
    public void testBugCase1() throws Exception {
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        // Prepare expression evaluation
        RandomNumbers prng = new RandomNumbers(1);
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(new ExpressionEvaluationErrorHandler(prng)); //null);
        e_expressionEvaluator = expressionEvaluator;

        ASTNode int8Type = TypeContainer.getIntegerType(true, 8);
        Expression e1 = new Expression(
                new Expression(new IntegerValue(int8Type, 1)),
                OP_BSHL,
                new Expression(new IntegerValue(int8Type, 7))
        );

        String resultString = e(e1);
        Expression faultyExpression = new Expression(
                new Expression(new IntegerValue(int8Type, 1)),
                OP_BSHL,
                e1);

        String originalExpressionCode = faultyExpression.toString();
        resultString = e(faultyExpression);

        System.out.println("got " + originalExpressionCode + " fixed to " + faultyExpression.toASTNode().toSolidityCode() + " = " + resultString);
    }
}
