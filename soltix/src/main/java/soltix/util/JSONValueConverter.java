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

package soltix.util;

import soltix.ast.ASTElementaryTypeName;
import soltix.ast.ASTNode;
import soltix.interpretation.Type;
import soltix.interpretation.values.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Class to convert between internal Value sub-classes and JSON objects
 */
public class JSONValueConverter {
    static public Value valueFromObject(/*Variable variable,*/ ASTNode variableType, Object valueObject) throws Exception {
        // The value Object is expected to have been obtained from reading a JSONObject field.
        // To interpret its contents correctly, we need to reference the variable type information, since
        // must - but not all - JSON fields are represented as String objects
        //
        // TODO Actually do this - only store strings and integers for now.
        //     ASTNode variableType = variable.getType();

        if (Type.isIntegerType(variableType)) {
            // TODO: Use ValueContainer, decision of Big/Small dichotomy unclear here. Maybe we should be using BigInteger
            // for everything but that will hurt performance
            BigInteger bigValue;

            try {
                // In new truffle versions, integers are passed in hexadecimal format - try that first
                bigValue = new BigInteger((String)valueObject, 16);
            } catch (Exception e) {
                // Try to fall back to decimal
                BigDecimal decimal = new BigDecimal((String) valueObject); // to parse JSON fields in exponential notation
                bigValue = decimal.toBigInteger();
            }
            return new IntegerValue(variableType, bigValue);
        } else if (Type.isBoolType(variableType)) {
            return ValueContainer.getBoolValue((Boolean)valueObject);
        } else if (Type.isStringType(variableType)) {
            return new StringValue((String)valueObject);
        } else if (Type.isAddressType(variableType)) {
            return new AddressValue((String)valueObject);
        } else if (Type.isByteType(variableType)) {
            String value = (String)valueObject;
            if (!value.startsWith("0x")) {
                throw new Exception("Malformed byte value in profiling log: " + value);
            }

            // There must be 2 characters for each byte
            ASTElementaryTypeName elementaryTypeName = (ASTElementaryTypeName)variableType;
            int expectedSize = elementaryTypeName.getBytes() * 2;
            if (value.length() != expectedSize + 2) {  // +2 for 0x prefix
                throw new Exception("Byte (" + elementaryTypeName.getBytes()
                        + ") value of unexpected length in profiling log: " + value);
            }

            byte[] result = new byte[elementaryTypeName.getBytes()];
            for (int i = 0, index = 2; i < elementaryTypeName.getBytes(); ++i, index += 2) {
                result[i] = (byte)Integer.parseInt(value.substring(index, index+2) /*index+elementaryTypeName.getBytes()) ??? */, 16);
            }
            return new BytesValue(elementaryTypeName, result);
        } else {
            // TODO implement remaining types
            throw new Exception("Unimplemented profiling log type for " + /*variable.getName() +*/ ": " + variableType.toSolidityCode());
        }
    }

    static public Object objsoltixromValue(Value value) throws Exception {
        return value.toJSONRepresentation();
    }

}
