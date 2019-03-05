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
import soltix.interpretation.values.BoolValue;
import soltix.interpretation.values.BytesValue;
import soltix.interpretation.values.IntegerValue;
import soltix.interpretation.values.ValueContainer;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.interpretation.variables.VariableValues;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;

import static soltix.ast.ASTBinaryOperation.Operator.*;

/*
 * Expression evaluator tests (non-exhaustive, primarily addressing bug cases)
 */
public class ExpressionEvaluatorTest extends UtilExpressionTests {
    @Test
    public void testEnvironmentUpdates() throws Exception {
        // This test ensures the correct propagation of increment/decrement operator side effets through
        // conditional and short-circuiting operators in a value-recoding VariableEnvironment

        // Environment: Create two integer variables x, y with values 5, 10
        VariableEnvironment environment = new VariableEnvironment(null, true);

        ASTNode integerType = TypeContainer.getIntegerType(true, 32);
        Variable varX = createVariable(integerType, "x");
        Variable varY = createVariable(integerType, "y");
        VariableValues varXValues = new VariableValues(varX, 0);
        VariableValues varYValues = new VariableValues(varY, 0);

        IntegerValue initialXValue = new IntegerValue(varX.getType(), 5);
        IntegerValue initialYValue = new IntegerValue(varY.getType(), 10);
        varXValues.addValue(initialXValue);
        varYValues.addValue(initialYValue);

        environment.addVariableValues(varX, varXValues);
        environment.addVariableValues(varY, varYValues);
        environment.finishAddingValues();

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);


        // Build expression "true? ++x: --y"
        Expression xExpression = new Expression(varX);
        Expression yExpression = new Expression(varY);

        Expression trueFirstOperand = new Expression(new BoolValue(true));
        Expression falseFirstOperand = new Expression(new BoolValue(false));

        Expression secondCondOpOperand = new Expression(ASTUnaryOperation.Operator.OP_INC_POST, xExpression);
        Expression thirdCondOpOperand = new Expression(ASTUnaryOperation.Operator.OP_DEC_PRE, yExpression);

        Expression conditionalOperatorFirstBranchTaken = new Expression(trueFirstOperand, secondCondOpOperand, thirdCondOpOperand);
        Expression conditionalOperatorSecondBranchTaken = new Expression(falseFirstOperand, secondCondOpOperand, thirdCondOpOperand);


        //////////////// Process   true? x++: --y    , expecting x to increase by 1 //////////////////////////
        ArrayList<Expression> undoOperations = new ArrayList<Expression>();
        System.out.println("expr " + conditionalOperatorFirstBranchTaken.toASTNode(undoOperations).toSolidityCode());

        ExpressionEvaluator.ComputedValues result;
        result = expressionEvaluator.evaluateForAll(environment, conditionalOperatorFirstBranchTaken);
        // Although the side effect of x++ increments it, the original value must be returned by the expression:
        Assert.assertTrue(      ((IntegerValue)result.values.get(0))  . isEqualTo(initialXValue).getValue());

        // Should have executed ++x, i.e. x=6, y=10
        System.out.println("x value = " + varXValues.getValue(0).toString());
        Assert.assertTrue(varXValues.getValue(0).toString().equals("6"));
        System.out.println("y value = " + varYValues.getValue(0).toString());
        Assert.assertTrue(varYValues.getValue(0).toString().equals("10"));


        //////////////// Process   false? x++: --y    , expecting y to decrease by 1 //////////////////////////

        // Reset to original state, x = 5, y = 10
        undoOperations.clear();
        environment.updateVariableValue(varX, initialXValue);
        environment.updateVariableValue(varY, initialYValue);

        System.out.println("expr " + conditionalOperatorSecondBranchTaken.toASTNode(undoOperations).toSolidityCode());

        result = expressionEvaluator.evaluateForAll(environment, conditionalOperatorSecondBranchTaken);
        // Since a prefix --y is applied, the expression's result must also be 9:
        Assert.assertTrue(      ((IntegerValue)result.values.get(0))  . isEqualTo( ValueContainer.getSmallIntegerValue(varY.getType(), 9)).getValue());

        // Should have executed --y, i.e. x=5, y=9
        System.out.println("x value = " + varXValues.getValue(0).toString());
        Assert.assertTrue(varXValues.getValue(0).toString().equals("5"));
        System.out.println("y value = " + varYValues.getValue(0).toString());
        Assert.assertTrue(varYValues.getValue(0).toString().equals("9"));



        // Build (++x > 0), used as second operand for all subsquent tests
        Expression xGreaterThanZero = new Expression(new Expression(ASTUnaryOperation.Operator.OP_INC_PRE, new Expression(varX)),
                                                    ASTBinaryOperation.Operator.OP_GT,
                                                    new Expression(ValueContainer.getSmallIntegerValue(varX.getType(), 0)));


        //////////////// Process   false && (++x > 0), expecting x not to change //////////////////////////
        undoOperations.clear();
        environment.updateVariableValue(varX, initialXValue); // Start over at 5 again

        Expression logicalAndSecondBranchNotTaken = new Expression(falseFirstOperand,
                                                                    ASTBinaryOperation.Operator.OP_LAND,
                                                                    xGreaterThanZero);

        System.out.println("expr " + logicalAndSecondBranchNotTaken.toASTNode(undoOperations).toSolidityCode());

        result = expressionEvaluator.evaluateForAll(environment, logicalAndSecondBranchNotTaken);
        Assert.assertFalse(((BoolValue)result.values.get(0)).getValue());

        // Should NOT have executed ++x - x = 5 holds
        System.out.println("x value = " + varXValues.getValue(0).toString());
        Assert.assertTrue(varXValues.getValue(0).toString().equals("5"));


        //////////////// Process   true && (++x > 0), expecting x to increase by 1 //////////////////////////
        undoOperations.clear();
        environment.updateVariableValue(varX, initialXValue); // Start over at 5 again

        Expression logicalAndSecondBranchTaken = new Expression(trueFirstOperand,
                ASTBinaryOperation.Operator.OP_LAND,
                xGreaterThanZero);

        result = expressionEvaluator.evaluateForAll(environment, logicalAndSecondBranchTaken);
        Assert.assertTrue(((BoolValue)result.values.get(0)).getValue());

        // Should have executed ++x - x = 6 now
        System.out.println("x value = " + varXValues.getValue(0).toString());
        Assert.assertTrue(varXValues.getValue(0).toString().equals("6"));

        //////////////// Process   true || (++x > 0), expecting x not to change  //////////////////////////
        undoOperations.clear();
        environment.updateVariableValue(varX, initialXValue); // Start over at 5 again

        Expression logicalOrSecondBranchNotTaken = new Expression(trueFirstOperand,
                ASTBinaryOperation.Operator.OP_LOR,
                xGreaterThanZero);

        result = expressionEvaluator.evaluateForAll(environment, logicalOrSecondBranchNotTaken);
        Assert.assertTrue(((BoolValue)result.values.get(0)).getValue());

        // Should NOT have executed ++x - x = 5 still holds
        System.out.println("x value = " + varXValues.getValue(0).toString());
        Assert.assertTrue(varXValues.getValue(0).toString().equals("5"));


        //////////////// Process   false || (++x > 0), expecting x to decrease by 1  //////////////////////////
        undoOperations.clear();
        environment.updateVariableValue(varX, initialXValue); // Start over at 5 again

        Expression logicalOrSecondBranchTaken = new Expression(falseFirstOperand,
                ASTBinaryOperation.Operator.OP_LOR,
                xGreaterThanZero);

        result = expressionEvaluator.evaluateForAll(environment, logicalOrSecondBranchTaken);
        Assert.assertTrue(((BoolValue)result.values.get(0)).getValue());

        // Should have executed ++x - x = 6 now
        System.out.println("x value = " + varXValues.getValue(0).toString());
        Assert.assertTrue(varXValues.getValue(0).toString().equals("6"));
    }


    // Bug case:
    // Given:
    //  4 uint16 v2 = 11;
    //  5 uint216 v3 = 11;
    //  7 int32 v5 = 7;
    // The interpretation incorrectly expected a zero value for the following expression:
    //  16 v3 = (((+(-(uint216((+(uint16(v5) ^ v2))) - v3))) >= 11)? 1: 0); if (v3 != 0) emit EXPR_ERROR(1);
    //
    // This apparently can't be reproduced here.
    //
    // Likely explanation: The contract code generation created an intermediate expression involving an increment
    // on v3. This was dropped, but the side effect is currently incorrectly recorded, so v3 in the above expression
    // became 12 and messed up the result. As in other cases, expression side effects must be handled correctly
    @Test
    public void testBugCase0() throws Exception {
        // This test ensures the correct propagation of increment/decrement operator side effets through
        // conditional and short-circuiting operators in a value-recoding VariableEnvironment

        ExpressionEvaluator.freeIntermediateResults = false; // for debug output below

        // Environment: Create variables v2, v3, v5
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        ASTNode v2Type = TypeContainer.getIntegerType(false, 16);
        ASTNode v3Type = TypeContainer.getIntegerType(false, 216);
        ASTNode v5Type = TypeContainer.getIntegerType(true, 32);
        Variable varV2 = createVariable(v2Type, "v2");
        Variable varV3 = createVariable(v3Type, "v3");
        Variable varV5 = createVariable(v5Type, "v5");
        VariableValues varV2Values = new VariableValues(varV2, 0);
        VariableValues varV3Values = new VariableValues(varV3, 0);
        VariableValues varV5Values = new VariableValues(varV5, 0);


        varV2Values.addValue(new IntegerValue(v2Type, 11));
        varV3Values.addValue(new IntegerValue(v3Type, 11));
        varV5Values.addValue(new IntegerValue(v5Type, 7));

        environment.addVariableValues(varV2, varV2Values);
        environment.addVariableValues(varV3, varV3Values);
        environment.addVariableValues(varV5, varV5Values);
        environment.finishAddingValues();

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;


        // Build the expression inside-out:
        //  16 v3 = (((+(-(uint216((+(uint16(v5) ^ v2))) - v3))) >= 11)? 1: 0); if (v3 != 0) emit EXPR_ERROR(1);
        Expression v2Expr = new Expression(varV2);
        Expression v3Expr = new Expression(varV3);
        Expression v5Expr = new Expression(varV5);

        Expression castV5Expr = new Expression(v5Expr, TypeContainer.getIntegerType(false, 16));
        System.out.println("got " + castV5Expr.toASTNode().toSolidityCode()
                            + " = " + e(castV5Expr));

        Expression xorExpr = new Expression(castV5Expr, ASTBinaryOperation.Operator.OP_BXOR, v2Expr);
        System.out.println("got " + xorExpr.toASTNode().toSolidityCode()
                        + " " + e(castV5Expr)
                        + " ^ " + e(v2Expr)
                        + " = " + e(xorExpr));

        Expression castUint216 = new Expression(xorExpr, TypeContainer.getIntegerType(false, 216));

        System.out.println("got " + castUint216.toASTNode().toSolidityCode()
                        + " = " + e(castUint216));

        Expression minusExpr = new Expression(castUint216, ASTBinaryOperation.Operator.OP_MINUS, v3Expr);

        System.out.println("got " + minusExpr.toASTNode().toSolidityCode()
                        + " = " + e(minusExpr));

        Expression negatedExpr = new Expression(ASTUnaryOperation.Operator.OP_MINUS, minusExpr);

        System.out.println("got " + negatedExpr.toASTNode().toSolidityCode()
                        + " = " + e(negatedExpr));

        negatedExpr = new Expression(ASTUnaryOperation.Operator.OP_PLUS, negatedExpr);

        System.out.println("got " + negatedExpr.toASTNode().toSolidityCode()
                        + " = " + e(negatedExpr));
        Assert.assertTrue(e(negatedExpr).equals("uint216(105312291668557186697918027683670432318895095400549111254310977535)"));
        Expression gteRHSOperand = new Expression(ValueContainer.getSmallIntegerValue(TypeContainer.getIntegerType(false, 216), 11));

        Expression gteExpr = new Expression(negatedExpr, ASTBinaryOperation.Operator.OP_GTE, gteRHSOperand);

        System.out.println("got " + gteExpr.toASTNode().toSolidityCode()
                        + " = " + e(gteExpr));
        Assert.assertTrue(e(gteExpr).equals("true"));
    }

    // Bug case (internal evaluation):
    // Given:
    //  int248 v1 = int248(7);
    //  int192 v2 = int192(7);
    // The interpretation yields a type error for this expression:
    //     (~(~(   int248(v2++) % (v1 + int248(7))    )))
    // indicating that an unexpected type change to boolean occurs for an operand to the "~" operator
    @Test
    public void testBugCase1() throws Exception {
        // Environment: Create variables v41, v63
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        ASTNode v1Type = TypeContainer.getIntegerType(true, 248);
        ASTNode v2Type = TypeContainer.getIntegerType(true, 192);
        Variable varV1 = createVariable(v1Type, "v1");
        Variable varV2 = createVariable(v2Type, "v2");
        VariableValues varV1Values = new VariableValues(varV1, 0);
        VariableValues varV2Values = new VariableValues(varV2, 0);

        varV1Values.addValue(new IntegerValue(v1Type, 7));
        varV2Values.addValue(new IntegerValue(v2Type, 7));

        environment.addVariableValues(varV1, varV1Values);
        environment.addVariableValues(varV2, varV2Values);
        environment.finishAddingValues();

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;


        // Build expression (~(~(   int248(v2++) % (v1 + int248(7))    )))
        Expression firstBinaryOperand
                = new Expression(   // int248(...)
                    new Expression(   // v2++
                            ASTUnaryOperation.Operator.OP_INC_POST,
                            new Expression(   // v2
                                    varV2
                            )
                    ),
                v1Type // cast to this
        );
        Expression secondBinaryOperand
                = new Expression( // (v1 + int248(7))
                    new Expression( // v1
                            varV1
                    ),
                    ASTBinaryOperation.Operator.OP_PLUS,
                    new Expression( // int248(7)
                            new Expression( // 7
                                    ValueContainer.getSmallIntegerValue(v1Type, 7)
                            ),
                            v1Type
                    )
        );

        Expression modExpression = new Expression( // (int248(v2++) % (v1 + int248(7)))
                firstBinaryOperand,
                ASTBinaryOperation.Operator.OP_MOD,
                secondBinaryOperand
        );

        Expression firstNegationExpression = new Expression( // ~(...)
                ASTUnaryOperation.Operator.OP_BNEG,
                modExpression
        );

        Expression secondNegationExpression = new Expression( // ~(~(...))
                ASTUnaryOperation.Operator.OP_BNEG,
                firstNegationExpression
        );

        System.out.println("got " + secondNegationExpression.toASTNode().toSolidityCode()
                                + " = " + e(secondNegationExpression));
    }

    // Test the expression ~uint200(1) ** 11
    @Test
    public void testBugCase2() throws Exception {
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;

        ASTNode integerType = TypeContainer.getIntegerType(false, 200);
        Expression expression = new Expression(
                new Expression(ASTUnaryOperation.Operator.OP_BNEG, new Expression(new IntegerValue(integerType, 1))),
                        OP_EXP,
                        new Expression(new IntegerValue(integerType, 11))
        );


        String resultString = e(expression);
        System.out.println("got " + expression.toASTNode().toSolidityCode() + " = " + resultString);
        Assert.assertTrue(resultString.equals("uint200(1606938044258990275541962092341162602522202993782792835299328)"));
    }

    // Test shift-right of negative values (but per definition positive shift bit count) for compliance
    // with the description:
    //
    //     The results produced by shift right of negative values of signed integer types is different from those
    // produced by other programming languages. In Solidity, shift right maps to division so the shifted negative
    // values are going to be rounded towards zero (truncated). In other programming languages the shift right of
    // negative values works like division with rounding down (towards negative infinity).
    //
    // Update: As of 0.5 it does round towards negative infinity.
    @Test
    public void testBugCase3() throws Exception {
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;

        ASTNode integerType = TypeContainer.getIntegerType(true, 224);

        Expression expression = new Expression(
                new Expression(new IntegerValue(integerType, -1)), OP_BSHR, new Expression(new IntegerValue(integerType,1))
        );

        // This should be properly rounded to 0 rather than stay at -1 in an earlier buggy BigInteger shift operation
        String resultText = e(expression);
        System.out.println("got " + expression.toASTNode().toSolidityCode() + " = " + resultText);
        Assert.assertTrue(resultText.equals("int224(-1)"));   //0.4 behavior: "int224(0)"));

        expression = new Expression(
                new Expression(new IntegerValue(integerType, -8)), OP_BSHR, new Expression(new IntegerValue(integerType,1))
        );

        resultText = e(expression);
        System.out.println("got " + expression.toASTNode().toSolidityCode() + " = " + resultText);
        Assert.assertTrue(resultText.equals("int224(-4)"));
    }

    @Test
    public void testBugCase4() throws Exception {
        // ((uint112(((~ int184(7)) << (int184(1)))) >> uint112(4720269871395297844118633026563722))
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;

        ASTNode typeInt184 = TypeContainer.getIntegerType(true, 184);
        ASTNode typeUint112 = TypeContainer.getIntegerType(false, 112);

        Expression negatedValue = new Expression(
                ASTUnaryOperation.Operator.OP_BNEG,
                new Expression(new IntegerValue(typeInt184, 7))
        );

        System.out.println("got " + negatedValue.toASTNode().toSolidityCode() + " = " + e(negatedValue));

        Expression firstShift = new Expression(
                negatedValue,
                OP_BSHL,
                new Expression(new IntegerValue(typeInt184, 1))
        );

        System.out.println("got " + firstShift.toASTNode().toSolidityCode() + " = " + e(firstShift));

        Expression castExpression = new Expression(firstShift, typeUint112);

        String resultString = e(castExpression);
        System.out.println("got " + castExpression.toASTNode().toSolidityCode() + " = " + resultString);
        Assert.assertTrue(resultString.equals("uint112(5192296858534827628530496329220080)"));

        Expression secondShift = new Expression(
                castExpression,
                OP_BSHR,
                new Expression(new IntegerValue(typeUint112, new BigInteger("4720269871395297844118633026563722")))
        );

        resultString = e(secondShift);
        System.out.println("got " + secondShift.toASTNode().toSolidityCode() + " = " + e(secondShift));
        Assert.assertTrue(resultString.equals("uint112(0)"));
    }

    @Test
    public void testBugCase5() throws Exception {

        // uint184(24519928653854221733733552434404946937899825954937634815) ** uint184(11)

        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;

        ASTNode typeUint184 = TypeContainer.getIntegerType(false, 184);

        Expression expression = new Expression(
                new Expression(new IntegerValue(typeUint184, new BigInteger("24519928653854221733733552434404946937899825954937634815"))),
                OP_EXP,
                new Expression(new IntegerValue(typeUint184, 11))
        );

        String resultString = e(expression);
        System.out.println("got " + expression.toASTNode().toSolidityCode() + " = " + resultString);

        Expression negatedExpression = new Expression(
          ASTUnaryOperation.Operator.OP_BNEG, expression
        );

        resultString = e(negatedExpression);
        System.out.println("got " + negatedExpression.toASTNode().toSolidityCode() + " = " + resultString);
    }

    @Test
    public void testBugCase6() throws Exception {
        // int32(1843829075) * int32(7)
        //    Is:          8611836229 = 1000000001010011100011000101000101   (too large - int32 invariant violation)
        //    Solidity:      21901637 =          1010011100011000101000101
        //    int32 limit: 2147483647 =    1111111111111111111111111111111
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;

        ASTNode typeInt32 = TypeContainer.getIntegerType(true, 32);

        // Test for the positive case
        Expression expression = new Expression(
                new Expression(new IntegerValue(typeInt32, 1843829075)),
                OP_MUL,
                new Expression(new IntegerValue(typeInt32, 7))
        );
        String resultString = e(expression);
        System.out.println("got " + expression.toASTNode().toSolidityCode() + " = " + resultString);
        Assert.assertTrue(resultString.equals("int32(21901637)"));

        // Test for the negative case
        expression = new Expression(
                new Expression(new IntegerValue(typeInt32, -1843829075)),
                OP_MUL,
                new Expression(new IntegerValue(typeInt32, 7))
        );
        resultString = e(expression);
        System.out.println("got " + expression.toASTNode().toSolidityCode() + " = " + resultString);
        Assert.assertTrue(resultString.equals("int32(-21901637)"));
    }


    @Test
    public void testBugCase7() throws Exception {
        VariableEnvironment environment = new VariableEnvironment(null, true);
        e_environment = environment;

        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;

        ASTNode bytes7Type = TypeContainer.getByteType(7);
        ASTNode bytes9Type = TypeContainer.getByteType(9);
        ASTNode integer72Type = TypeContainer.getIntegerType(false, 72);
        ASTNode integer112Type = TypeContainer.getIntegerType(false, 112);
        ASTNode integer216Type = TypeContainer.getIntegerType(false, 216);


        // 0x56 57 0d e2 87 d7 3c
        byte[] v5Value = new byte[]{(byte)0x56, (byte)0x57, (byte)0x0d, (byte)0xe2, (byte)0x87, (byte)0xd7, (byte)0x3c};
        byte[] v29Value = new byte[]{
                (byte)0x56, (byte)0x57, (byte)0x0d, (byte)0xe2, (byte)0x87, (byte)0xd7, (byte)0x3c, (byte)0xd1, (byte)0xcb
        };

        //145 emit BYTES9(bytes9(  v5 >> (~ v22)));
        //146 emit BYTES9(v29 ^ bytes9(0x56570DE287D73CD1CB));

        Expression e1 = new Expression( // bytes9(...)
                new Expression( // v5 >> ~v22
                        new Expression(new BytesValue(bytes7Type, v5Value)),
                        OP_BSHR,
                        new Expression(ASTUnaryOperation.Operator.OP_BNEG, new Expression(new IntegerValue(integer112Type, 0)))
                ),
                bytes9Type
        );

        String result = e(e1);
        System.out.println("got " + e1.toASTNode().toSolidityCode() + " = " + result);


        Expression e2 = new Expression(
                new Expression(new BytesValue(bytes9Type, v29Value)),
                OP_BXOR,
                new Expression(new BytesValue(bytes9Type, v29Value))
        );

        result = e(e2);
        System.out.println("got " + e2.toASTNode().toSolidityCode() + " = " + result);


        Expression e3 = new Expression(
                e1,
                OP_NEQ,
                e2
        );

        result = e(e3);
        System.out.println("got " + result);

        Expression e4 = new Expression(
                new Expression(new IntegerValue(integer216Type, 0)),
                OP_GTE,
                new Expression(new IntegerValue(integer216Type, 11))
        );
        e4 = new Expression(ASTUnaryOperation.Operator.OP_LNEG, e4);

        result = e(e4);
        System.out.println("got " + e4.toASTNode().toSolidityCode() + " = " + result);


        Expression condOp = new Expression(
            new Expression(e3, OP_GTE, e4),
            new Expression(new IntegerValue(integer72Type, 1)),
                new Expression(new IntegerValue(integer72Type, 0))
        );

        result = e(condOp);
        System.out.println("got " + condOp.toASTNode().toSolidityCode() + " = " + result);


        byte[] bytes9ZeroValue = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        Expression e5 = new Expression(
                ASTUnaryOperation.Operator.OP_BNEG,
                new Expression(ASTUnaryOperation.Operator.OP_BNEG,
                        new Expression(new BytesValue(bytes9Type, bytes9ZeroValue)))
        );

        result = e(e5);
        System.out.println("got " + e5.toASTNode().toSolidityCode() + " = " + result);
    }

}
