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
import soltix.ast.ASTNode;
import soltix.interpretation.TypeContainer;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;


import java.math.BigInteger;

/**
 * Test class for class IntegerValue
 */
public class IntegerValueTest extends ValueTestTypes {
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void getMinValue() {
    }

    @Test
    public void getMaxValue() {
    }

    @Test
    public void negate() throws Exception {
        // As per "emit e(-u);" with uint8 u=60 and the e result being 196, the "-" operator performs a 2's complement
        // negation for unsigned type.
        // We check some sample value and the type's min and max values.
        // The min value - 0 - must remain 0 after negation, demonstrating proper wraparound
        // The max value - e.g. 0xff for 8 bits - has to wrap around to 1
        //
        // TODO the type text isn't used yet
        IntegerTestValues[] a = new IntegerTestValues[] {
                new IntegerTestValues("uint8", "60", false, 8, "196"),
                new IntegerTestValues("uint8", String.valueOf(0xff), false, 8, "1"),
                new IntegerTestValues("uint8", "0", false, 8, "0"),

                new IntegerTestValues("uint16", "60", false, 16, "65476"),
                new IntegerTestValues("uint16", String.valueOf(0xffff), false, 16, "1"),
                new IntegerTestValues("uint16", "0", false, 16, "0"),

                new IntegerTestValues("uint24", "60", false, 24, "16777156"),
                new IntegerTestValues("uint24", String.valueOf(0xffffff), false, 24, "1"),
                new IntegerTestValues("uint24", "0", false, 24, "0"),


                new IntegerTestValues("uint32", "60", false, 32, "4294967236"),
                new IntegerTestValues("uint32", new BigInteger("ffffffff", 16).toString(), false, 32, "1"),
                new IntegerTestValues("uint32", "0", false, 32, "0"),

                // Shortcut to representatives uint64, uint128, uint256 without looking at intermediate types:
                new IntegerTestValues("uint64", "60", false, 64, "18446744073709551556"),
                new IntegerTestValues("uint64", new BigInteger("ffffffffffffffff", 16).toString(), false, 64, "1"),
                new IntegerTestValues("uint64", "0", false, 64, "0"),

                new IntegerTestValues("uint128", "60", false, 128, "340282366920938463463374607431768211396"),
                new IntegerTestValues("uint128", new BigInteger("ffffffffffffffffffffffffffffffff", 16).toString(), false, 128, "1"),
                new IntegerTestValues("uint128", "0", false, 128, "0"),

                new IntegerTestValues("uint256", "60", false, 256, "115792089237316195423570985008687907853269984665640564039457584007913129639876"),
                new IntegerTestValues("uint256", new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16).toString(), false, 256, "1"),
                new IntegerTestValues("uint256", "0", false, 256, "0"),


                //////////////////////////// Signed types //////////////////////////////////////////
                new IntegerTestValues("int8", "60", true, 8, "-60"),
                new IntegerTestValues("int8", "-128", true, 8, "-128"),
                new IntegerTestValues("int8", "-127", true, 8, "127"),
                new IntegerTestValues("int8", "0", true, 8, "0"),

                // TODO...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue value = new IntegerValue(type, testData.valueText);

            assert value.toString().equals(testData.valueText);

            IntegerValue resultValue = value.negate();

            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void negateBitwise() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("int8", "60", true, 8, "-61"),
                new IntegerTestValues("int8", "100", true, 8, "-101"),
                new IntegerTestValues("int8", "-101", true, 8, "100"),
                new IntegerTestValues("int8", "-128", true, 8, "127"),
                new IntegerTestValues("int8", "0", true, 8, "-1"),
                // TODO further signed types
                new IntegerTestValues("uint8", "60", false, 8, "195"),
                new IntegerTestValues("uint8", "0", false, 8, "255"),
                new IntegerTestValues("uint8", "255", false, 8, "0")
                // TODO further unsigned types
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue value = new IntegerValue(type, testData.valueText);

            assert value.toString().equals(testData.valueText);

            IntegerValue resultValue = value.negateBitwise();

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void add() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("uint8", "255", "1", false, 8, "0"),
                new IntegerTestValues("uint8", "255", "2", false, 8, "1"),

                // Overflows
                new IntegerTestValues("int8", "127", "1", true, 8, "-128"),
                new IntegerTestValues("int8", "127", "127", true, 8, "-2"),
                new IntegerTestValues("int8", "127", "-128", true, 8, "-1"),
                // Underflows
                new IntegerTestValues("int8", "-128", "-1", true, 8, "127"),
                new IntegerTestValues("int8", "-128", "-128", true, 8, "0")
                // TODO more...
        };
        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.add(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void sub() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("uint8", "3", "4", false, 8, "255"),
                new IntegerTestValues("uint8", "0", "128", false, 8, "128")
                // TODO ...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.sub(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void div() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("int8", "10", "-2", true, 8, "-5"),
                new IntegerTestValues("int8", "-10", "2", true, 8, "-5"),
                new IntegerTestValues("int8", "-10", "-2", true, 8, "5")
                // TODO ...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.div(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void mul() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("uint8", "128", "2", false, 8, "0"),
                new IntegerTestValues("int8", "127", "2", true, 8, "-2")
                // TODO ...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.mul(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void bitwiseOr() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("int8", "1", "-128", true, 8, "-127"),

                // TODO ...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.bitwiseOr(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void bitwiseAnd() {
    }

    @Test
    public void bitwiseXor() {
    }

    @Test
    public void bitwiseShiftLeft() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("int8", "1", "7", true, 8, "-128"),
                new IntegerTestValues("int8", "1", "8", true, 8, "0"),
                new IntegerTestValues("int8", "1", "3", true, 8, "8")
                // TODO ...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.bitwiseShiftLeft(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void bitwiseShiftRight() throws Exception {
        // Initial shift right tests to test our correction for the breaking change from 0.4.x semantics:
        //
        //   The result of a shift operation is the type of the left operand.
        //   The expression x << y is equivalent to x * 2**y, and x >> y is equivalent to x / 2**y.
        //   This means that shifting negative numbers sign extends. Shifting by a negative amount throws a runtime exception.
        //   Warning
        //   The results produced by shift right of negative values of signed integer types is different from those produced
        //   by other programming languages. In Solidity, shift right maps to division so the shifted negative values are going
        //   to be rounded towards zero (truncated). In other programming languages the shift right of negative values works
        //   like division with rounding down (towards negative infinity).
        //
        // To 0.5.x semantics:
        //
        //   Signed right shift now uses proper arithmetic shift, i.e. rounding towards negative infinity, instead of
        //   rounding towards zero. Signed and unsigned shift will have dedicated opcodes in Constantinople, and are
        //   emulated by Solidity for the moment.
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("int8", "16", "2", true, 8, "4"),
                new IntegerTestValues("int8", "16", "4", true, 8, "1"),
                new IntegerTestValues("int8", "16", "5", true, 8, "0"),
                new IntegerTestValues("int8", "16", "6", true, 8, "0"),
                new IntegerTestValues("int8", "-16", "1", true, 8, "-8"),
                new IntegerTestValues("int8", "-16", "3", true, 8, "-2"),
                new IntegerTestValues("int8", "-16", "4", true, 8, "-1"),
                new IntegerTestValues("int8", "-16", "5", true, 8, "-1"),
                new IntegerTestValues("int32", "-16", "5", true, 8, "-1"),

                new IntegerTestValues("int32", new BigInteger("3c6b9cf7", 16).negate().toString(),
                                               new BigInteger("6453e737", 16).toString(), true, 32, "-1")

                // TODO ...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.bitwiseShiftRight(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult + " for " + testData.valueText + " >> " + testData.secondValueText);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void mod() throws Exception {
        IntegerTestValues[] a = new IntegerTestValues[]{
                new IntegerTestValues("int8", "-10", "3", true, 8, "-1"),
                new IntegerTestValues("int8", "-10", "-3", true, 8, "-1"),
                new IntegerTestValues("int8", "10", "-3", true, 8, "1"),
                new IntegerTestValues("int8", "10", "3", true, 8, "1")
                // TODO ...
        };

        for (IntegerTestValues testData : a) {
            ASTNode type = TypeContainer.getIntegerType(testData.isSigned, testData.typeBits);
            IntegerValue firstOperand = new IntegerValue(type, testData.valueText);
            IntegerValue secondOperand = new IntegerValue(type, testData.secondValueText);

            assert firstOperand.toString().equals(testData.valueText);
            assert secondOperand.toString().equals(testData.secondValueText);

            IntegerValue resultValue = firstOperand.mod(secondOperand);

            System.out.println("got " + resultValue + " wanted " + testData.operationResult);
            assert resultValue.toString().equals(testData.operationResult);
        }
    }

    @Test
    public void exp() throws Exception {
        // TODO
    }

    @Test
    public void isSmallerThan() {
    }

    @Test
    public void isSmallerThanOrEqualTo() {
    }

    @Test
    public void isGreaterThan() {
    }

    @Test
    public void isGreaterThanOrEqualTo() {
    }

    @Test
    public void isEqualTo() {
    }

    @Test
    public void isNotEqualTo() {
    }

    @Test
    public void castToIntegerType() throws Exception {
        // Entry interpretation: Pairs of from (idx i) and to (idx i+1), operationResult of to counts
        IntegerTestValues[] a = new IntegerTestValues[] {
                // Unsigned
                new IntegerTestValues("uint16", String.valueOf(0x01ff), false, 16, ""),
                new IntegerTestValues("uint8", "", false, 8, String.valueOf(0xff)),

                new IntegerTestValues("uint8", String.valueOf(0xff), false, 8, ""),
                new IntegerTestValues("uint16", "", false, 16, String.valueOf(0xff)),

                // Signed
                new IntegerTestValues("int16", String.valueOf(0x8000), true, 16, ""),
                new IntegerTestValues("int8", "", true, 8, "0"),

                new IntegerTestValues("int16", String.valueOf(0x8080), true, 16, ""),
                new IntegerTestValues("int8", "", true, 8, "-128"),

                // Mixed unsigned/signed
                new IntegerTestValues("uint8", String.valueOf(0xff), false, 8, ""),
                new IntegerTestValues("int8", "", true, 8, "-1"),

                new IntegerTestValues("int8", "-1", true, 8, ""),
                new IntegerTestValues("uint16", "", false, 16, String.valueOf(0xffff)),

                new IntegerTestValues("int24", "-1", true, 24, ""),
                new IntegerTestValues("uint32", "", false, 32, new BigInteger("ffffffff", 16).toString())
        };

        for (int i = 0; i < a.length; i += 2) {
            IntegerTestValues sourceData = a[i];
            IntegerTestValues targetData = a[i+1];
            ASTElementaryTypeName sourceType = TypeContainer.getIntegerType(sourceData.isSigned, sourceData.typeBits);
            ASTElementaryTypeName targetType = TypeContainer.getIntegerType(targetData.isSigned, targetData.typeBits);

            IntegerValue value = new IntegerValue(sourceType, sourceData.valueText);

            assert value.toString().equals(sourceData.valueText);

            IntegerValue resultValue = value.convertToIntegerType(targetType);

            System.out.println("got " + resultValue + " wanted " + targetData.operationResult);
            assert resultValue.toString().equals(targetData.operationResult);
        }
    }

    protected byte b(int i) { return (byte)i; }

    // TODO Find behavior justification in documentation or point out the problem that behavior must be reverse-engineered (as we did here).
    // This strictly speaking already depends on potentially incorrect compiler behavior.
    //
    // It's not in https://solidity.readthedocs.io/en/v0.4.24/types.html#explicit-conversions
    @Test
    public void castToBytesType() throws Exception {
        IntegerTestValues[] ai = new IntegerTestValues[]{
                // Unsigned
                new IntegerTestValues("uint16", String.valueOf(0x01ff), false, 16, ""),
                new IntegerTestValues("int24", String.valueOf(-1), true, 24, ""),
                new IntegerTestValues("uint16", String.valueOf(0x0030), false, 16, ""),
                new IntegerTestValues("uint32", String.valueOf(0x40000001), false, 32, ""),
                new IntegerTestValues("uint32", String.valueOf(0x40000001), false, 32, "")
        };
        BytesTestValues[] ab = new BytesTestValues[] {
                new BytesTestValues("bytes1", null, null, 1, new byte[]{b(0xff)}),
                new BytesTestValues("bytes4", null, null, 4, new byte[]{b(0xff),b(0xff),b(0xff),b(0xff)}),
                new BytesTestValues("bytes7", null, null, 7, new byte[]{b(0x0),b(0x0),b(0x0),b(0x0),b(0x0),b(0x0),b(0x30)}),
                new BytesTestValues("bytes7", null, null, 7, new byte[]{b(0x0),b(0x0),b(0x0),b(0x40),b(0x0),b(0x0),b(0x01)}),
                new BytesTestValues("bytes2", null, null, 2, new byte[]{b(0x0),b(0x01)})

        };

        for (int i = 0; i < ai.length; ++i) {
            IntegerTestValues sourceData = ai[i];
            BytesTestValues destData = ab[i];
            ASTElementaryTypeName sourceType = TypeContainer.getIntegerType(sourceData.isSigned, sourceData.typeBits);
            ASTElementaryTypeName destType = TypeContainer.getByteType(destData.typeBytes);

            IntegerValue value = new IntegerValue(sourceType, sourceData.valueText);

            assert value.toString().equals(sourceData.valueText);

            BytesValue resultValue = value.convertToBytesType(destType);
            String got = s(resultValue);
            String wanted = hexstring(destData.typeBytes, destData.operationResult);
            System.out.println("got " + s(resultValue) + " wanted " + wanted);

            Assert.assertTrue(got.equalsIgnoreCase(wanted));
        }
    }

    @Test
    public void castToAddressType() throws Exception {
        IntegerTestValues[] ai = new IntegerTestValues[] {
            new IntegerTestValues("uint8", String.valueOf(0xff), false, 8, ""),
            new IntegerTestValues("uint256", new BigInteger("0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20", 16).toString(), false, 256, ""),
            new IntegerTestValues("int8", String.valueOf("-1"), true, 8, "")
        };

        AddressTestValues[] aa = new AddressTestValues[] {
                new AddressTestValues(null, "0xff"),
                new AddressTestValues(null, "0x0d0e0f101112131415161718191a1b1c1d1e1f20"),
                new AddressTestValues(null, "0xffffffffffffffffffffffffffffffffffffffff"),
        };

        for (int i = 0; i < ai.length; ++i) {
            IntegerTestValues sourceData = ai[i];
            AddressTestValues destData = aa[i];
            ASTElementaryTypeName sourceType = TypeContainer.getIntegerType(sourceData.isSigned, sourceData.typeBits);

            IntegerValue value = new IntegerValue(sourceType, sourceData.valueText);

            AddressValue resultValue = value.convertToAddressType();
            String got = s(resultValue);
            String wanted = destData.operationResult;

            System.out.println("got " + got + " wanted " + wanted);

            Assert.assertTrue(got.equals(wanted));
        }
    }

    @Test
    public void bugCase() throws Exception {
        ASTElementaryTypeName fromType = TypeContainer.getIntegerType(false, 224);
        ASTElementaryTypeName toType = TypeContainer.getIntegerType(true, 64);
        IntegerValue value = new IntegerValue(fromType, new BigInteger("11988971846691410581385115388184131611967272372387111788269705427221"));
        IntegerValue converted = value.convertToIntegerType(toType);

        System.out.println("from " + value.toASTNode(false).toSolidityCode() + " to " + converted.toASTNode(false).toSolidityCode());
    }
}
