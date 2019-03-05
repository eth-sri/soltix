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

public class ASTElementaryTypeName extends ASTNode {

    public enum ElementaryType {
        ELEMENTARY_TYPE_INT,
        ELEMENTARY_TYPE_UINT,
        ELEMENTARY_TYPE_BOOL,
        ELEMENTARY_TYPE_BYTE,
        ELEMENTARY_TYPE_STRING,
        ELEMENTARY_TYPE_ADDRESS
    }

    // Elementary type
    private ElementaryType elementaryType;

    // Type-specific additional type data
    // Integer-specific:
    private boolean signed = false;
    private int bits = 0;
    // Bytes:
    private int bytes = 0;

    // Integer-specific:
    public boolean isSigned() throws Exception {
        if (!isIntegerType()) throw new Exception("ASTElementaryTypeName.isSigned applied to non-integer type");
        return signed;
    }
    public int getBits() throws Exception {
        if (!isIntegerType()) throw new Exception("ASTElementaryTypeName.getBits applied to non-integer type");
        return bits;
    }
    // Byte-specific:
    public int getBytes() throws Exception {
        if (elementaryType != ElementaryType.ELEMENTARY_TYPE_BYTE) throw new Exception("ASTElementaryTypeName.getBytes appplied to non-byte type");
        return bytes;
    }

    //
    // Constructor to extract type from string - can fail (exception)
    public ASTElementaryTypeName(long id, String name) throws Exception {
        super(id, name);

        if (name.startsWith("int")) {
            elementaryType = ElementaryType.ELEMENTARY_TYPE_INT;
        } else if (name.startsWith("uint")) {
            elementaryType = ElementaryType.ELEMENTARY_TYPE_UINT;
        } else if (name.startsWith("bool")) {
            elementaryType = ElementaryType.ELEMENTARY_TYPE_BOOL;
        } else if (name.startsWith("byte")) {
            elementaryType = ElementaryType.ELEMENTARY_TYPE_BYTE;
        } else if (name.equals("string")) {
            elementaryType = ElementaryType.ELEMENTARY_TYPE_STRING;
        } else if (name.equals("address")) {
            elementaryType = ElementaryType.ELEMENTARY_TYPE_ADDRESS;
        } else {
            throw new Exception("Unknown elementary type " + name);
        }

        if (isIntegerType()) {
            this.bits = extractBitsFromType(name);
            this.signed = extractIsSignedFromType(name);
        } else if (elementaryType == ElementaryType.ELEMENTARY_TYPE_BYTE) {
            this.bytes = extractBytesCountFromType(name);
        }
    }


    public ElementaryType getElementaryType() {
        return elementaryType;
    }

    public boolean isIntegerType() {
        return     elementaryType == ElementaryType.ELEMENTARY_TYPE_INT
                || elementaryType == ElementaryType.ELEMENTARY_TYPE_UINT;
    }

    public boolean isSignedIntegerType() {
        return elementaryType == ElementaryType.ELEMENTARY_TYPE_INT;
    }
    public boolean isUnsignedIntegerType() {
        return elementaryType == ElementaryType.ELEMENTARY_TYPE_UINT;
    }

    // Assume uint* or int* type name
    protected boolean extractIsSignedFromType(String name) { //ASTNode type) {
        return /*type.getName()*/name.charAt(0) != 'u';
    }
    // Assume uint[0-9]* or int[0-9]* type name
    protected int extractBitsFromType(String name) throws Exception {
        //String name = type.getName();
        int startIndex;

        if (name.charAt(0) == 'u') { // uint[0-9]...
            startIndex = 4;
        } else { // signed - int[0-9]...
            startIndex = 3;
        }
        if (startIndex > name.length()) {
            throw new Exception("IntegerValue.extractBitsFromType: Unexpected type name " + name);
        } else if (startIndex == name.length()) {
            // No bit count is given, default to 256
            return 256;
        }
        return Integer.parseInt(name.substring(startIndex));
    }
    // byte or bytes[0-9]*
    protected int extractBytesCountFromType(String name) {
        if (name.equals("byte")) {
            return 1;
        } else if (name.equals("bytes")) {
            return 0; // unknown
        }
        return Integer.parseInt(name.substring("bytes".length()));
    }

    @Override
    public String toSolidityCode() { return name; }
    @Override
    public String toSolidityCodePostfix() { return null; }
}
