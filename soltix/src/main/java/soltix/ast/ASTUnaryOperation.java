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

public class ASTUnaryOperation extends ASTNode {
    private Operator operator;
    private ASTNode operand;
    private boolean isPrefix;

    public ASTUnaryOperation(long id, String name, boolean isPrefix) throws Exception {
        super(id);
        if ((operator = Operator.fromString(name, isPrefix)) == null) {
            throw new Exception("Unknown unary operator " + name);
        }
        this.isPrefix = isPrefix;
    }

    public Operator getOperator() { return operator; }

    public enum Operator {
        OP_LNEG("!", true),
        OP_BNEG("~", true),
        OP_INC_PRE("++", true),
        OP_INC_POST("++", false),
        OP_DEC_PRE("--", true),
        OP_DEC_POST("--", false),
        OP_PLUS("+", true),
        OP_MINUS("-", true),
        OP_DELETE("delete", true);

        private String name;
        private boolean isPrefix;

        Operator(String name, boolean isPrefix) {
            this.name = name;
            this.isPrefix = isPrefix;
        }

        public boolean isPrefix() {
            return isPrefix;
        }

        @Override
        public String toString() {
            return name;
        }

        static public Operator fromString(String name, boolean isPrefix) {
            for (Operator o : Operator.values()) {
                if (o.toString().equals(name) && o.isPrefix() == isPrefix) {
                    return o;
                }
            }
            return null;
        }
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "";
        if (isPrefix) {
            code = operator.toString() + " "; // whitespace needed at least for "delete"
        }
        code += operand.toSolidityCode();
        if (!isPrefix) {
            code += operator.toString();
        }
        return code;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 1) {
            throw new Exception("Unexpected unary operation child count " + getChildCount() + " = " + childrenToText());
        }
        operand = getChild(0);
        removeChildNode(0);
    }
}
