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

package soltix.interpretation.variables;

import soltix.ast.*;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.expressions.ExpressionEvaluator;
import soltix.interpretation.expressions.UtilExpressionTests;
import soltix.interpretation.values.IntegerValue;
import soltix.interpretation.values.StructValue;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for class VariableEnvironment
 */
public class VariableEnvironmentTest extends UtilExpressionTests {
    // Test correct struct field updates for assignments and increment/decrement side effects.
    // This primarily tests accurate resolution of struct member fields in update operations:
    //        x.y.z = 123;
    //        x.y.z++;
    @Test
    public void testBugCase1() throws Exception {
        // Environment: Create variable v1
        AST ast = new AST(); // needed for struct type lookups
        VariableEnvironment environment = new VariableEnvironment(ast, true);
        e_environment = environment;

        // Create struct member variables m0, m1 (256 bits)
        ASTNode v1Type = TypeContainer.getIntegerType(true, 256);
        ASTNode v2Type = TypeContainer.getIntegerType(true, 256);
        Variable memberV1 = createVariable(v1Type, "m0");
        Variable memberV2 = createVariable(v2Type, "m1");
        //VariableValues varV1Values = new VariableValues(varV1, 0);
        //VariableValues varV2Values = new VariableValues(varV2, 0);

        ASTUserDefinedTypeName structUDT = new ASTUserDefinedTypeName(0, "s0");
        ASTStructDefinition structDefinition = new ASTStructDefinition(0, "s0", "s0");
        structDefinition.addChildNode(memberV1.getDeclaration());
        structDefinition.addChildNode(memberV2.getDeclaration());
        structDefinition.finalize();
        ast.addStructDefinition(structDefinition);

        Variable varStruct = createVariable(structUDT, "v1");

        StructValue structValue = new StructValue(structUDT, structDefinition);
        structValue.addMember(memberV1.getDeclaration(), new IntegerValue(v1Type, 7));
        structValue.addMember(memberV2.getDeclaration(), new IntegerValue(v2Type, 8));

        VariableValues varV1Values = new VariableValues(varStruct, 0);
        varV1Values.addValue(structValue);
        environment.addVariableValues(varStruct, varV1Values);
        environment.finishAddingValues();

        String currentValueString = environment.resolveVariableValue(0, "v1").toASTNode(false).toSolidityCode();
        System.out.println("pre-eval " + currentValueString);
        Assert.assertTrue(currentValueString.equals("s0(int256(7), int256(8))"));


        environment.updateVariableValue(varStruct,
                new Expression(new Expression(varStruct), memberV1.getDeclaration()), //     v1.m0
                new IntegerValue(v1Type, 123));                                 // ... = 123

        currentValueString = environment.resolveVariableValue(0, "v1").toASTNode(false).toSolidityCode();
        System.out.println("post-update " + currentValueString);
        Assert.assertTrue(currentValueString.equals("s0(int256(123), int256(8))"));


        // Prepare expression evaluation
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(null); //null);
        e_expressionEvaluator = expressionEvaluator;

        Expression incrementExpression = new Expression(ASTUnaryOperation.Operator.OP_INC_PRE,
                new Expression(new Expression(varStruct), memberV2.getDeclaration()) //     v1.m1
        );
        String expressionValue = e(incrementExpression);
        System.out.println("got " + incrementExpression.toASTNode().toSolidityCode() + " = " + expressionValue);
        Assert.assertTrue(expressionValue.equals("int256(9)"));

        currentValueString = environment.resolveVariableValue(0, "v1").toASTNode(false).toSolidityCode();
        Assert.assertTrue(currentValueString.equals("s0(int256(123), int256(9))"));

        Expression decrementExpression = new Expression(ASTUnaryOperation.Operator.OP_DEC_POST,
                new Expression(new Expression(varStruct), memberV2.getDeclaration()) //     v1.m1
        );
        expressionValue = e(decrementExpression);
        System.out.println("got " + decrementExpression.toASTNode().toSolidityCode() + " = " + expressionValue);
        Assert.assertTrue(expressionValue.equals("int256(9)"));
        currentValueString = environment.resolveVariableValue(0, "v1").toASTNode(false).toSolidityCode();
        Assert.assertTrue(currentValueString.equals("s0(int256(123), int256(8))"));

        expressionValue = e(decrementExpression);
        System.out.println("got " + decrementExpression.toASTNode().toSolidityCode() + " = " + expressionValue);
        Assert.assertTrue(expressionValue.equals("int256(8)"));
        currentValueString = environment.resolveVariableValue(0, "v1").toASTNode(false).toSolidityCode();
        Assert.assertTrue(currentValueString.equals("s0(int256(123), int256(7))"));
    }
}
