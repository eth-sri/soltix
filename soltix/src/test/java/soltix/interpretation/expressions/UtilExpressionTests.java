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

import soltix.ast.ASTNode;
import soltix.ast.ASTVariableDeclaration;
import soltix.interpretation.values.Value;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;

/*
 * Convenience functions to abbreviate expression evaluation tests
 */
public class UtilExpressionTests {
    protected Variable createVariable(ASTNode type, String name) throws Exception {
        ASTVariableDeclaration declaration = new ASTVariableDeclaration(0, name, type.toSolidityCode(),
                "default", "internal", false, false);
        declaration.addChildNode(type);
        declaration.finalize();
        return new Variable(declaration);
    }


    // e(expr) abbreviates the construct expressionEvaluator.evaluate(environment, expr, true).values.get(0).toASTNode(false).toSolidityCode()
    protected VariableEnvironment e_environment;
    protected  ExpressionEvaluator e_expressionEvaluator;
    protected String e(Expression expr) throws Exception {
        return e_expressionEvaluator.evaluate(e_environment, VariableEnvironment.NO_VALUE_SET_SELECTED, expr, false).values.get(0).toASTNode(false).toSolidityCode();
    }

}
