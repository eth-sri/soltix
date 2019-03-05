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

import soltix.interpretation.expressions.Expression;

/**
 * AST node type that may include arbitrarily complex Solidity code to simplify
 * AST editing operations
 */
public class ASTVerbatimText extends ASTNode {
    private String text;

    private String firstText;
    private Expression expression;
    private String secondText;

    public ASTVerbatimText(long id, String text) throws Exception {
        super(id);
        this.text = text;
    }

    public ASTVerbatimText(long id, String firstText, Expression expression, String secondText) throws Exception {
        super(id);
        this.firstText = firstText;
        this.expression = expression;
        this.secondText = secondText;
    }

    @Override
    public String toSolidityCode() throws Exception {
        if (text != null) {
            return text;
        } else {
            return firstText + expression.toASTNode().toSolidityCode() + secondText;
        }
    }

    @Override
    public String toSolidityCodePostfix() {
        return null;
    }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() > 0) {
            throw new Exception("Unexpected verbatim text statement child count " + getChildCount());
        }
    }
}
