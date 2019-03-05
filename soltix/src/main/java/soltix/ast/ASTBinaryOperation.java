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
package soltix.ast;

import java.util.HashMap;

public class ASTBinaryOperation extends ASTNode {
    private Operator operator;
    private ASTNode leftOperand;
    private ASTNode rightOperand;

    public ASTBinaryOperation(long id, String operatorName) throws Exception {
        super(id);
        if ((operator = operators.get(operatorName)) == null) {
            throw new Exception("Unknown binary operator " + operatorName);
        }
    }

    public enum Operator {
        // Arithmetic
        OP_PLUS("+", false),
        OP_MINUS("-", false),
        OP_MUL("*", false),
        OP_DIV("/", false),
        OP_MOD("%", false),
        OP_EXP("**", false),

        // Equality / relational / logical
        OP_EQ("==", true),
        OP_NEQ("!=", true),
        OP_GT(">", true),
        OP_LT("<", true),
        OP_GTE(">=", true),
        OP_LTE("<=", true),
        OP_LOR("||", true),
        OP_LAND("&&", true),

        // Bitwise
        OP_BOR("|", false),
        OP_BAND("&", false),
        OP_BXOR("^", false),
        OP_BSHL("<<", false),
        OP_BSHR(">>", false);

        private String string;
        private boolean yieldsBoolResult; // Relational, equality, logical

        Operator(String string, boolean yieldsBoolResult) {
            this.string = string;
            this.yieldsBoolResult = yieldsBoolResult;
        }

        @Override
        public String toString() {
            return string;
        }

        public boolean yieldsBoolResult() { return yieldsBoolResult; }
    }


    private static final HashMap<String, Operator> operators;
    static {
        operators = new HashMap<String, Operator>();
        for (Operator op : Operator.values()) {
            operators.put(op.toString(), op);
        }
    }

    @Override
    public void finalize() throws Exception {
        // This node should have two children that constitute the left and right hand side arguments
        if (getChildCount() != 2) {
            throw new Exception("Binary operation has unexpected non-2 child count: " + getChildCount());
        }
        leftOperand = getChild(0);
        rightOperand = getChild(1);
        removeChildNode(0);
        removeChildNode(0);
    }



    @Override
    public String toSolidityCode() throws Exception { return leftOperand.toSolidityCode() + " " + operator.toString() + " " + rightOperand.toSolidityCode(); }
    @Override
    public String toSolidityCodePostfix() { return null; }
}
