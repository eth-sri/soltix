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

import static soltix.ast.ASTLiteral.LiteralType.LITERAL_TYPE_INTEGER_HEXADECIMAL;

/**
 * Class to represent address values
 */
public class AddressValue extends Value implements IAddressOperations {
    private String value;
    static private ASTNode addressType = null;

    public AddressValue(String value) throws Exception { // TODO input 160-bits BigInteger address value instead?
        this.value = value;
        addressType = TypeContainer.getAddressType();
    }

    @Override
    public ASTNode getType() { return addressType; }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        return new ASTLiteral(0, addressType.getName(), value, LITERAL_TYPE_INTEGER_HEXADECIMAL, null);
    }

    @Override
    public Object toJSONRepresentation() {
        return value;
    }

    public BoolValue isEqualTo(AddressValue otherValue) throws Exception {
       boolean isEqual = value.equals(otherValue.value);
       return ValueContainer.getBoolValue(isEqual);
    }

    public BoolValue isNotEqualTo(AddressValue otherValue) throws Exception {
        return ValueContainer.getBoolValue(! isEqualTo(otherValue).getValue());
    }
}
