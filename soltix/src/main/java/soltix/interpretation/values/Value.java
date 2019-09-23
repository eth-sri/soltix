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

import soltix.ast.*;
import soltix.interpretation.TypeContainer;

import java.math.BigDecimal;
import java.math.BigInteger;

import static soltix.ast.ASTLiteral.LiteralType.*;

/**
 * Super class for all value representations
 */
abstract public class Value {
    // Type (if known - otherwise null)
    //protected ASTNode type;

    //public Value(ASTNode type) {
    //    this.type = type;
    //}

    abstract public ASTNode getType() throws Exception; // { return type; }
    abstract public ASTNode toASTNode(boolean forJavaScript) throws Exception;
    abstract public Object toJSONRepresentation() throws Exception; // "String", "Boolean", "JSONObject", "JSONArray", ...

    public static Value fromASTNode(ASTLiteral literal) throws Exception {
        Value value;

        if (literal.getSubdenomination() != null) {
            throw new Exception("Value.fromASTNode for unimplemented concept of subdenominations");
        }

        // Create 256-bit-sized integers (signed) for now. Allowing IntegerValue to work out types by itself causes
        // issues with type size limitations once operations are performed.
        // Working on 256-bit items until a restriction is needed appears to be more in line with the documentation,
        // but context-dependent type inference is missing.
        switch (literal.getType()) {
            case LITERAL_TYPE_INTEGER_DECIMAL: // decimal integral constant - e.g. 123
                value = new IntegerValue(TypeContainer.getIntegerType(true, 256),
                                         new BigInteger(literal.getValueString()));
                ((IntegerValue)value).setIsIndeterminateType(true);
                break;
            case LITERAL_TYPE_INTEGER_HEXADECIMAL: // hexadecimal integral constant - 0x123
                value = new IntegerValue(TypeContainer.getIntegerType(true, 256),
                                         new BigInteger(literal.getValueString().replace("0x", ""), 16));
                ((IntegerValue)value).setIsIndeterminateType(true);
                break;
            case LITERAL_TYPE_HEXADECIMAL:     // hexadecimal string constant -  hex"123" (TODO distinction from 0x123 unclear)
                throw new Exception("Value.fromASTNode for unimplemented literal type " + literal.getType());
            case LITERAL_TYPE_BOOL:            // boolean constant - true/false
                value = ValueContainer.getBoolValue(literal.getValueString().equals("true"));
                break;
            case LITERAL_TYPE_STRING:           // string constant - "foo"
                value = new StringValue(literal.getValueString());
                break;
            default:
                throw new Exception("Value.fromASTNode for unimplemented literal type " + literal.getType());
        }
        return value;
    }
}
