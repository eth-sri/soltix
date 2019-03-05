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
import soltix.interpretation.TypeContainer;

import static soltix.ast.ASTLiteral.LiteralType.LITERAL_TYPE_BOOL;

/**
 * Class to represent bool values
 */
public class BoolValue extends Value implements IBoolOperations {
    private boolean value;
    static private ASTNode boolType = null;

    public BoolValue(boolean value) throws Exception {
        this.boolType = TypeContainer.getBoolType();
        this.value = value;
    }

    public boolean getValue() { return value; }

    @Override
    public ASTNode getType() { return boolType; }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        return new ASTLiteral(0, /*type.getName()*/"", String.valueOf(value), LITERAL_TYPE_BOOL, null);
    }

    @Override
    public Object toJSONRepresentation() {
        return Boolean.valueOf(value);
    }

    public BoolValue isEqualTo(BoolValue value) throws Exception {
        return ValueContainer.getBoolValue(getValue() == value.getValue());
    }

    public BoolValue isNotEqualTo(BoolValue value) throws Exception {
        return ValueContainer.getBoolValue(!isEqualTo(value).getValue());
    }

    public BoolValue isGreaterThan(BoolValue value) throws Exception {
        // solc seems to allow this - guessed and untested behavior: (TODO verify this)
        int firstValue = getValue() == true? 1: 0;
        int secondValue = value.getValue() == true? 1: 0;
        return ValueContainer.getBoolValue(firstValue > secondValue);
    }

    public BoolValue isSmallerThan(BoolValue value) throws Exception {
        // solc seems to allow this - guessed and untested behavior: (TODO verify this)
        int firstValue = getValue() == true? 1: 0;
        int secondValue = value.getValue() == true? 1: 0;
        return ValueContainer.getBoolValue(firstValue < secondValue);
    }

    public BoolValue isGreaterThanOrEqualTo(BoolValue value) throws Exception {
        return ValueContainer.getBoolValue(isGreaterThan(value).getValue() || isEqualTo(value).getValue());
    }

    public BoolValue isSmallerThanOrEqualTo(BoolValue value) throws Exception{
        return ValueContainer.getBoolValue(isSmallerThan(value).getValue() || isEqualTo(value).getValue());
    }

    public BoolValue logicalOr(BoolValue value) throws Exception {
        return ValueContainer.getBoolValue(getValue() || value.getValue());
    }

    public BoolValue logicalAnd(BoolValue value) throws Exception {
        return ValueContainer.getBoolValue(getValue() && value.getValue());
    }
}
