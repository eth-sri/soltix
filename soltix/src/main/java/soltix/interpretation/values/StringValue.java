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

package soltix.interpretation.values;

import soltix.ast.ASTElementaryTypeName;
import soltix.ast.ASTLiteral;
import soltix.ast.ASTNode;
import soltix.ast.ASTVerbatimText;
import soltix.interpretation.TypeContainer;

import static soltix.ast.ASTLiteral.LiteralType.LITERAL_TYPE_STRING;

/**
 * Class to represent string values
 */
public class StringValue extends Value {
    private String value;
    static private ASTNode stringType = null;

    public StringValue(String value) throws Exception {
        stringType = TypeContainer.getStringType();
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public ASTNode getType() { return stringType; }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        return new ASTLiteral(0, "", value, LITERAL_TYPE_STRING, null); // TODO or use ASTVerbatimText?
    }

    @Override
    public Object toJSONRepresentation() {
        return value;
    }
}
