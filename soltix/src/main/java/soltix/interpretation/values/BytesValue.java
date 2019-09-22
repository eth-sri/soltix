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

import soltix.Configuration;
import soltix.ast.ASTElementaryTypeName;
import soltix.ast.ASTNode;
import soltix.ast.ASTVerbatimText;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.util.Hash;

import java.math.BigInteger;

/**
 * Class to reprsent bytes* values
 */
public class BytesValue extends Value implements IByteOperations {
    //private String value;
    private byte[] value;
    private BigInteger integerValue; // for relational operators
    private ASTElementaryTypeName type;
    private boolean isBytesType = false;

    // Constructor for "bytes<count>" types
    public BytesValue(ASTNode type, /*String*/ byte[] value) throws Exception {
        //super(type);
        this.type = (ASTElementaryTypeName)type;
        if (this.type.getElementaryType() != ASTElementaryTypeName.ElementaryType.ELEMENTARY_TYPE_BYTE) {
            throw new Exception("BytesValue constructed with incompatible type");
        }
        this.value = value;
        integerValue = new BigInteger(1, value); // for relational operators
        isBytesType = this.type.getBytes() == 0;
        if (!isBytesType && this.type.getBytes() != value.length) {
            throw new Exception("Inconsistent BytesValue type bytes " + this.type.getBytes() + " vs value bytes " + value.length);
        }
    }


    public int getBytesCount() throws Exception {
        return type.getBytes();
    }

    public byte[] getValue() { return value; }

    @Override
    public ASTNode getType() { return type; }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        String valueString;

        // The byte[0-9]* types can be output as hexadecimal numbers. However, the "bytes" type seems to require
        // a hex string in Solidity (which also may not be cast to the result type bytes) - but not in JavaScript,
        // so it can apparently be treated like the other byte* types there!
        if (isBytesType && !forJavaScript) {
            // bytes type
            valueString = "hex\"";
            for (byte b : value) {
                valueString += String.format("%02X", b);
            }
            valueString += "\"";
        } else {
            /*valueString = "0x";
            for (byte b : value) {
                valueString += String.format("%02X", b);
            }*/
            valueString = toHexConstantString();

            if (value.length == 20 && Configuration.languageVersionMinor >= 5) {
                // Ensure that an address-like bytes constant also passes the checksum test, or else
                // a 0.5+ compiler will error out
                valueString = Hash.toChecksumAddress(valueString);
            }

            if (forJavaScript) {
                // Enclose value between quotes to prevent ABI errors caused by JavaScript hex constant limits
                valueString = "\"" + valueString + "\"";
            } else {
                valueString = type.toSolidityCode() + "(" + valueString + ")";
            }
        }
        return new ASTVerbatimText(0, valueString);
    }

    public String toHexConstantString() {
        String result = "0x";
        for (byte b : value) {
            result += String.format("%02X", b);
        }
        return result;
    }

    @Override
    public Object toJSONRepresentation() throws Exception {
        // Result: hex string with 0x prefix and 2 characters per digit
        StringBuilder result = new StringBuilder();
        result.append("0x");
        for (int i = 0; i < getBytesCount(); ++i) {
            result.append(String.format("%02x", value[i]));
        }
        return result.toString();
    }

    public BoolValue isEqualTo(BytesValue other) throws Exception {
        for (int i = 0; i < getBytesCount(); ++i) {
            if (i == other.getBytesCount()) {
                // End of other value reached - the remainder of this value must be 0 bytes
                for (int j = i; j < getBytesCount(); ++j) {
                    if (value[j] != 0) {
                        return ValueContainer.getBoolValue(false);
                    }
                }
                break;
            } else {
                // Both sides have comparable values
                if (value[i] != other.value[i]) {
                    return ValueContainer.getBoolValue(false);
                }
            }
        }
        // If the other value contains more bytes, those must be 0
        for (int i = getBytesCount(); i < other.getBytesCount(); ++i) {
            if (other.value[i] != 0) {
                return ValueContainer.getBoolValue(false);
            }
        }
        return ValueContainer.getBoolValue(true);
    }

    public BoolValue isNotEqualTo(BytesValue other) throws Exception {
        return ValueContainer.getBoolValue(!isEqualTo(other).getValue());
    }



    public BoolValue isSmallerThan(BytesValue value) throws Exception {
        return ValueContainer.getBoolValue(integerValue.compareTo(value.integerValue) < 0);
    }

    public BoolValue isSmallerThanOrEqualTo(BytesValue value) throws Exception {
        return ValueContainer.getBoolValue(integerValue.compareTo(value.integerValue) <= 0);
    }

    public BoolValue isGreaterThan(BytesValue value) throws Exception {
        return ValueContainer.getBoolValue(integerValue.compareTo(value.integerValue) > 0);
    }

    public BoolValue isGreaterThanOrEqualTo(BytesValue value) throws Exception {
        return ValueContainer.getBoolValue(integerValue.compareTo(value.integerValue) >= 0);
    }

    public BytesValue negateBitwise() throws Exception {
        byte[] newValue = new byte[type.getBytes()];
        for (int i = 0; i < newValue.length; ++i) {
            newValue[i] = (byte)~value[i];
        }
        return new BytesValue(type, newValue);
    }

    public BytesValue bitwiseOr(Value otherValue) throws Exception {
        BytesValue otherBytesValue = (BytesValue)otherValue;
        assert getBytesCount() == otherBytesValue.getBytesCount();

        byte[] newValue = new byte[type.getBytes()];
        for (int i = 0; i < getBytesCount(); ++i) {
            newValue[i] = (byte)(value[i] | otherBytesValue.value[i]);
        }
        return new BytesValue(type, newValue);
    }

    public BytesValue bitwiseAnd(Value otherValue) throws Exception {
        BytesValue otherBytesValue = (BytesValue)otherValue;
        assert getBytesCount() == otherBytesValue.getBytesCount();

        byte[] newValue = new byte[type.getBytes()];
        for (int i = 0; i < getBytesCount(); ++i) {
            newValue[i] = (byte)(value[i] & otherBytesValue.value[i]);
        }
        return new BytesValue(type, newValue);
    }

    public BytesValue bitwiseXor(Value otherValue) throws Exception {
        BytesValue otherBytesValue = (BytesValue)otherValue;
        assert getBytesCount() == otherBytesValue.getBytesCount();

        byte[] newValue = new byte[type.getBytes()];
        for (int i = 0; i < getBytesCount(); ++i) {
            newValue[i] = (byte)(value[i] ^ otherBytesValue.value[i]);
        }
        return new BytesValue(type, newValue);
    }

    private BigInteger ffMask = new BigInteger("ff", 16);
    protected void bigIntegerValueToBytesArray(byte[] array, BigInteger value) {
        for (int i = array.length - 1; i >= 0; --i) {
            array[i] = (byte)value.and(ffMask).intValue();
            value = value.shiftRight(8);
        }
    }

    public BytesValue bitwiseShiftLeft(Value value) throws Exception {
        IntegerValue shiftBits = (IntegerValue)value;
        byte[] result = new byte[type.getBytes()];
        if (shiftBits.isGreaterThanOrEqualTo(ValueContainer.getBigIntegerValue(shiftBits.getType(), BigInteger.valueOf(256))).getValue()) {
            // All bits are shifted out of range
            for (int i = 0; i < result.length; ++i) {
                result[i] = 0;
            }
        } else if (shiftBits.isSmallerThan(ValueContainer.getBigIntegerValue(shiftBits.getType(), BigInteger.ZERO)).getValue()) {
            throw new Exception("BytesValue.bitwiseShiftLeft with invalid negative count operand");
        } else {
            // For simplicity use BigInteger for now
            BigInteger shiftedValue = integerValue.shiftLeft(shiftBits.toInt());
            bigIntegerValueToBytesArray(result, shiftedValue);
        }

        return new BytesValue(type, result);
    }

    public BytesValue bitwiseShiftRight(Value value) throws Exception {
        IntegerValue shiftBits = (IntegerValue)value;
        byte[] result = new byte[type.getBytes()];
        if (shiftBits.isGreaterThanOrEqualTo(ValueContainer.getBigIntegerValue(shiftBits.getType(), BigInteger.valueOf(256))).getValue()) {
            // All bits are shifted out of range
            for (int i = 0; i < result.length; ++i) {
                result[i] = 0;
            }
        } else if (shiftBits.isSmallerThan(ValueContainer.getBigIntegerValue(shiftBits.getType(), BigInteger.ZERO)).getValue()) {
            throw new Exception("BytesValue.bitwiseShiftLeft with invalid negative count operand");
        } else {
            // For simplicity use BigInteger for now
            BigInteger shiftedValue = integerValue.shiftRight(shiftBits.toInt());
            bigIntegerValueToBytesArray(result, shiftedValue);
        }

        return new BytesValue(type, result);
    }

    public BytesValue indexAccess(Value index) throws Exception {
        IntegerValue integerIndex = (IntegerValue)index;
        IntegerValue valueLength = length();
        if (integerIndex.isGreaterThanOrEqualTo(valueLength).getValue()
                || integerIndex.isSmallerThan(ValueContainer.getBigIntegerValue(index.getType(), BigInteger.ZERO)).getValue()) {
            throw new Exception("Invalid bytes index access out of bounds for type " + type.toSolidityCode() + ": " + index.toString());
        }

        byte selectedValue = value[integerIndex.toInt()];
        return new BytesValue(TypeContainer.getByteType(1), new byte[]{selectedValue});
    }

    public IntegerValue length() throws Exception {
        // Experiments indicate that the length is of type uint8 for fixed-size arrays, and uint256 for the
        // variable-length "bytes" type
        int resultBits;
        if (type.getBytes() == 0) {
            resultBits = 256;
        } else {
            resultBits = 8;
        }
        BigInteger valueLength = BigInteger.valueOf(value.length);
        return ValueContainer.getBigIntegerValue(TypeContainer.getIntegerType(false, resultBits), valueLength);
    }

    public BytesValue convertToBytesType(ASTElementaryTypeName newType) throws Exception {
        byte[] result = new byte[newType.getBytes()];

        if (newType.getBytes() == 0) {
            // Converting to variable-length "bytes" does not appear to be allowed
            throw new Exception("BytesValue.convertToBytesType called for variable-length bytes type");
        }

        if (newType.getBytes() == type.getBytes()) {
            // No-op
            return this;
        } else if (newType.getBytes() < type.getBytes()) {
            // Converting to a smaller type takes the most significant bytes (= at lowest indices):
            //
            // bytes4 v = 0xaabbccdd;
            // ==> bytes2(v) = 0xaabb = v[0],v[1]
            for (int i = 0; i < newType.getBytes(); ++i) {
                result[i] = value[i];
            }
        } else if (newType.getBytes() > type.getBytes()) {
            // Converting to a larger type takes the old value as most significant bytes (= at lowest indices) and
            // pads with least significant zero bytes up to the desired size:
            //
            // bytes2 v = 0xaabb;
            // ==> bytes4(v) = 0xaabb0000;
            for (int i = 0; i < type.getBytes(); ++i) {
                result[i] = value[i];
            }
            for (int i = type.getBytes(); i < newType.getBytes(); ++i) {
                result[i] = 0;
            }
        }
        return new BytesValue(newType, result);
    }

    public IntegerValue convertToIntegerType(ASTElementaryTypeName newType) throws Exception {
        // First construct unsigned integer type of same size and with the value
        String s = "";
        for (int i = 0; i < value.length; ++i) {
            s += String.format("%02x", value[i]);
        }
        BigInteger unsgnedInteger = new BigInteger(s, 16);

        ASTElementaryTypeName unsignedType = TypeContainer.getIntegerType(false, type.getBytes() * 8);
        IntegerValue unsignedIntegerValue = ValueContainer.getBigIntegerValue(unsignedType, unsgnedInteger);

        if (Type.isSameType(null, unsignedType, newType)) {
            return unsignedIntegerValue;
        } else {
            return unsignedIntegerValue.convertToIntegerType(newType);
        }
    }
}
