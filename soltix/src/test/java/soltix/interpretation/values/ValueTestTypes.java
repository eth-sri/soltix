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

/**
 * Container of helper classes and methods for Value sub-class tests
 */
public class ValueTestTypes {
    protected String s(Value value) throws Exception {
        return value.toASTNode(false).toSolidityCode();
    }
    protected String hexstring(int bytes, byte[] result) {
        String s = "bytes" + bytes + "(0x";
        for (int i = 0; i < result.length; ++i) {
            s += String.format("%02x", result[i]);
        }
        return s + ")";
    }
    protected String decstring(String type, String value) {
        return type + "(" + value + ")";
    }
    protected class BytesTestValues {
        String typeText; // TODO: Not used yet
        byte[] valueArray;
        byte[] secondValueArray; // TODO second byte op operand be integer type in some contexts here and elsewhere in the soltix
        int typeBytes;
        byte[] operationResult;
        boolean boolValue;

        public BytesTestValues(String typeText, byte[] valueArray, int typeBytes, byte[] operationResult) {
            this.typeText = typeText;
            this.valueArray = valueArray;
            this.typeBytes = typeBytes;
            this.operationResult = operationResult;
        }

        public BytesTestValues(String typeText, byte[] valueArray,  byte[] secondValueArray, int typeBytes, byte[] operationResult) {
            this(typeText, valueArray, typeBytes, operationResult);
            this.secondValueArray = secondValueArray;
        }

        public BytesTestValues(String typeText, byte[] valueArray,  byte[] secondValueArray, int typeBytes, byte[] operationResult, boolean boolValue) {
            this(typeText, valueArray, secondValueArray, typeBytes, operationResult);
            this.boolValue = boolValue;
        }
    }


    protected class IntegerTestValues {
        String typeText;
        String valueText;
        String secondValueText;
        boolean isSigned;
        int typeBits;
        String operationResult;

        public IntegerTestValues(String typeText, String valueText, boolean isSigned, int typeBits, String operationResult) {
            this.typeText = typeText;
            this.valueText = valueText;
            this.isSigned = isSigned;
            this.typeBits = typeBits;
            this.operationResult = operationResult;
        }

        public IntegerTestValues(String typeText, String valueText, String secondValueText, boolean isSigned, int typeBits, String operationResult) {
            this(typeText, valueText, isSigned, typeBits, operationResult);
            this.secondValueText = secondValueText;
        }
    }


    protected class AddressTestValues {
        String valueText;
        String operationResult;

        public AddressTestValues(String valueText, String operationResult) {
            this.valueText = valueText;
            this.operationResult = operationResult;
        }
    }
}
