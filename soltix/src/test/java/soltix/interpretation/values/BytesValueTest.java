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
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for class BytesValue
 */
public class BytesValueTest extends ValueTestTypes {

    @Test
    public void isEqualTo() throws Exception {
        // Entry interpretation: Pairs of from (idx i) and to (idx i+1), boolValue of to is the result.
        // The second entry is converted to the type of the first one if needed
        //
        // Test observations on bytes equality for different sizes:
        //
        //   - In terms of index and other operations, values are ordered in "big endian" format:
        //                                  idx 0  idx1
        //       bytes1 b1 = 0xff;   ->      [0xff]
        //       bytes2 b2 = 0xff;   ->      [0x00, 0xff]
        //
        //     But a comparison "b1 == b2"   or even  "bytes2(b1) == b2" yields false, so the smaller
        //     type's bytes are treated as most significant part in the larger type.
        //     The following values yield true for equality:
        //
        //       bytes1 b1 = 0xff;   ->      [0xff]
        //       bytes2 b2 = 0xff00; ->      [0xff, 0x00]
        //
        // Examples:
        //       (bytes1(0xff) == bytes2(0xff))     = false
        //       (bytes1(0x0) == bytes2(0x0))     = true
        //       (bytes1(0xff) == bytes2(0xffff)) = false
        //       (bytes1(0xff) == bytes2(0xff00)) = true
        //       (bytes1(0xff) == bytes2(0x00ff)) = false
        //
        BytesTestValues[] a = new BytesTestValues[] {
                // Same types
                new BytesTestValues("bytes1", new byte[]{b(0xaa)}, null, 1, null),
                new BytesTestValues("bytes1", new byte[]{b(0xaa)}, null, 1, null, true),

                new BytesTestValues("bytes1", new byte[]{b(0xaa)}, null, 1, null),
                new BytesTestValues("bytes1", new byte[]{b(0xbb)}, null, 1, null, false),

                new BytesTestValues("bytes2", new byte[]{b(0xaa), b(0xbb)}, null, 2, null),
                new BytesTestValues("bytes2", new byte[]{b(0xaa), b(0xbb)}, null, 2, null, true),

                new BytesTestValues("bytes2", new byte[]{b(0xaa), b(0xbb)}, null, 2, null),
                new BytesTestValues("bytes2", new byte[]{b(0xaa), b(0xbc)}, null, 2, null, false),

                new BytesTestValues("bytes2", new byte[]{b(0xaa), b(0xbb)}, null, 2, null),
                new BytesTestValues("bytes2", new byte[]{b(0xab), b(0xbb)}, null, 2, null, false),

                // Mixed types
                // Examples in comment above:
                //
                // (bytes1(0xff) == bytes2(0x00ff))  = false
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null),
                new BytesTestValues("bytes2", new byte[]{b(0x00), b(0xff)}, null, 2, null, false),

                // (bytes1(0x0) == bytes2(0x0))     = true
                new BytesTestValues("bytes1", new byte[]{b(0x00)}, null, 1, null),
                new BytesTestValues("bytes2", new byte[]{b(0x00), b(0x00)}, null, 2, null, true),

                // (bytes1(0xff) == bytes2(0xffff)) = false
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null),
                new BytesTestValues("bytes2", new byte[]{b(0xff), b(0xff)}, null, 2, null, false),

                // (bytes1(0xff) == bytes2(0xff00)) = true
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null),
                new BytesTestValues("bytes2", new byte[]{b(0xff), b(0x00)}, null, 2, null, true),

                // Reverse example conversion direction: Convert bytes2 intermediate values to bytes1 prior to the comparison.
                // (bytes1(0xff) == bytes1(bytes2(0x00ff)))  = false
                new BytesTestValues("bytes2", new byte[]{b(0x00), b(0xff)}, null, 2, null),
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null, false),

                // (bytes1(0x0) == bytes1(bytes2(0x0)))     = true
                new BytesTestValues("bytes2", new byte[]{b(0x00), b(0x00)}, null, 2, null),
                new BytesTestValues("bytes1", new byte[]{b(0x00)}, null, 1, null, true),


                // (bytes1(0xff) == bytes1(bytes2(0xffff))) = true
                new BytesTestValues("bytes2", new byte[]{b(0xff), b(0xff)}, null, 2, null),
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null, true),


                // (bytes1(0xff) == bytes1(bytes2(0xff00))) = true
                new BytesTestValues("bytes2", new byte[]{b(0xff), b(0x00)}, null, 2, null),
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null, true),

                //   new BytesTestValues("bytes1", new byte[]{b(0xaa)}, null, 1, null),
            //    new BytesTestValues("bytes2", new byte[]{b(0xaa),}, null, 1, null, false),
        };

        for (int i = 0; i < a.length; i += 2) {
            BytesTestValues sourceData = a[i];
            BytesTestValues targetData = a[i+1];
            ASTElementaryTypeName sourceType = TypeContainer.getByteType(sourceData.typeBytes);
            ASTElementaryTypeName targetType = TypeContainer.getByteType(targetData.typeBytes);

            BytesValue source = new BytesValue(sourceType, sourceData.valueArray);
            BytesValue target = new BytesValue(targetType, targetData.valueArray);

            if (source.getBytesCount() != target.getBytesCount()) {
                // Mixing bytes types is allowed, but is implemented using a preceding type conversion to simplify
                // the operations
                source = source.convertToBytesType(targetType);
            }

            BoolValue resultValue = source.isEqualTo(target);

            System.out.println("got " + resultValue.getValue() + " wanted " + targetData.boolValue);

            assert resultValue.getValue() == targetData.boolValue;
        }
    }

    @Test
    public void isNotEqualTo() throws Exception {
        // (trivial isEqualTo complement)
    }

    @Test
    public void isSmallerThan() throws Exception {
        // See equality test case above on byte significance, which analogously affects relational comparisons
        BytesTestValues[] a = new BytesTestValues[]{
                // Mixed types, first converted to second prior to operation
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null),
                new BytesTestValues("bytes2", new byte[]{b(0xff), b(0x00)}, null, 2, null, false),

                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null),
                new BytesTestValues("bytes2", new byte[]{b(0xff), b(0x01)}, null, 2, null, true)

        };

        for (int i = 0; i < a.length; i += 2) {
            BytesTestValues sourceData = a[i];
            BytesTestValues targetData = a[i+1];
            ASTElementaryTypeName sourceType = TypeContainer.getByteType(sourceData.typeBytes);
            ASTElementaryTypeName targetType = TypeContainer.getByteType(targetData.typeBytes);

            BytesValue source = new BytesValue(sourceType, sourceData.valueArray);
            BytesValue target = new BytesValue(targetType, targetData.valueArray);

            if (source.getBytesCount() != target.getBytesCount()) {
                // Mixing bytes types is allowed, but is implemented using a preceding type conversion to simplify
                // the operations
                source = source.convertToBytesType(targetType);
            }

            BoolValue resultValue = source.isSmallerThan(target);

            System.out.println("got " + resultValue.getValue() + " wanted " + targetData.boolValue);

            assert resultValue.getValue() == targetData.boolValue;
        }
    }

    @Test
    public void isSmallerThanOrEqualTo() throws Exception {
    }

    @Test
    public void isGreaterThan() throws Exception {
    }

    @Test
    public void isGreaterThanOrEqualTo() throws Exception {
    }

    @Test
    public void negateBitwise() throws Exception {
        BytesTestValues[] a = new BytesTestValues[]{
                // Mixed types, first converted to second prior to operation
                new BytesTestValues("bytes2", new byte[]{b(0xff), b(0x00)}, null, 2,
                                                       new byte[]{b(0x00), b(0xff)})

        };

        for (BytesTestValues testData :a) {
            ASTElementaryTypeName sourceType = TypeContainer.getByteType(testData.typeBytes);
            BytesValue source = new BytesValue(sourceType, testData.valueArray);
            BytesValue expectedResult = new BytesValue(sourceType, testData.operationResult);

            BytesValue resultValue = source.negateBitwise();

            String resultString = resultValue.toASTNode(false).toSolidityCode();
            String expectedResultString = expectedResult.toASTNode(false).toSolidityCode();

            System.out.println("got " + resultString + " wanted " + expectedResultString);

            assert resultString.equals(expectedResultString);
        }
    }

    @Test
    public void bitwiseOr() throws Exception {
    }

    @Test
    public void bitwiseAnd() throws Exception {
    }

    @Test
    public void bitwiseXor() throws Exception {
    }

    @Test
    public void bitwiseShiftLeft() throws Exception {
        BytesTestValues[] a = new BytesTestValues[]{
                // 0x03 = 00000011   << 2    ---->    0x0C = 00001100
                new BytesTestValues("bytes3", new byte[]{b(0x00), b(0x00), b(0x03)}, new byte[]{2}, 3,
                                                        new byte[]{b(0x00), b(0x00), b(0x0C)}),
                // 0x03 = 00000011   << 4    ---->    0x30 = 00110000
                new BytesTestValues("bytes3", new byte[]{b(0x00), b(0x00), b(0x03)}, new byte[]{4}, 3,
                        new byte[]{b(0x00), b(0x00), b(0x30)}),
                // 0x03 = 00000011   << 20    ---->    0x300000 = 00110000 00000000 00000000
                new BytesTestValues("bytes3", new byte[]{b(0x00), b(0x00), b(0x03)}, new byte[]{20}, 3,
                        new byte[]{b(0x30), b(0x00), b(0x00)}),

                // 0x03 = 00000011   << 100    ---->    0
                new BytesTestValues("bytes3", new byte[]{b(0x00), b(0x00), b(0x03)}, new byte[]{100}, 3,
                        new byte[]{b(0x00), b(0x00), b(0x00)}),

        };

        for (BytesTestValues testData :a) {
            ASTElementaryTypeName sourceType = TypeContainer.getByteType(testData.typeBytes);
            BytesValue source = new BytesValue(sourceType, testData.valueArray);
            BytesValue expectedResult = new BytesValue(sourceType, testData.operationResult);


            BytesValue resultValue = source.bitwiseShiftLeft(new IntegerValue("int", testData.secondValueArray[0]));

            String resultString = resultValue.toASTNode(false).toSolidityCode();
            String expectedResultString = expectedResult.toASTNode(false).toSolidityCode();

            System.out.println("got " + resultString + " wanted " + expectedResultString);

            assert resultString.equals(expectedResultString);
        }
    }

    @Test
    public void bitwiseShiftRight() throws Exception {
        BytesTestValues[] a = new BytesTestValues[]{
                // 0x300000    >> 20    ---->    0x000003
                new BytesTestValues("bytes3", new byte[]{b(0x30), b(0x00), b(0x00)}, new byte[]{20}, 3,
                        new byte[]{b(0x00), b(0x00), b(0x03)}),
                // 0x300000    >> 100    ---->    0
                new BytesTestValues("bytes3", new byte[]{b(0x30), b(0x00), b(0x00)}, new byte[]{100}, 3,
                        new byte[]{b(0x00), b(0x00), b(0x00)}),
        };

        for (BytesTestValues testData : a) {
            ASTElementaryTypeName sourceType = TypeContainer.getByteType(testData.typeBytes);
            BytesValue source = new BytesValue(sourceType, testData.valueArray);
            BytesValue expectedResult = new BytesValue(sourceType, testData.operationResult);


            BytesValue resultValue = source.bitwiseShiftRight(new IntegerValue("int", testData.secondValueArray[0]));

            String resultString = resultValue.toASTNode(false).toSolidityCode();
            String expectedResultString = expectedResult.toASTNode(false).toSolidityCode();

            System.out.println("got " + resultString + " wanted " + expectedResultString);

            assert resultString.equals(expectedResultString);
        }
    }

    protected byte b(int i) { return (byte)i; }

    @Test
    public void indexAccess() throws Exception {
        BytesTestValues[] a = new BytesTestValues[] {
                new BytesTestValues("bytes4", new byte[]{b(0xaa), b(0xbb), b(0xcc), b(0xdd)}, new byte[]{0}, 4, new byte[]{b(0xaa)}),
                new BytesTestValues("bytes4", new byte[]{b(0xaa), b(0xbb), b(0xcc), b(0xdd)}, new byte[]{1}, 4, new byte[]{b(0xbb)}),
                new BytesTestValues("bytes4", new byte[]{b(0xaa), b(0xbb), b(0xcc), b(0xdd)}, new byte[]{2}, 4, new byte[]{b(0xcc)}),
                new BytesTestValues("bytes4", new byte[]{b(0xaa), b(0xbb), b(0xcc), b(0xdd)}, new byte[]{3}, 4, new byte[]{b(0xdd)})
        };

        for (BytesTestValues testData : a) {
            ASTNode type = TypeContainer.getByteType(testData.typeBytes);
            BytesValue value = new BytesValue(type, testData.valueArray);

            BytesValue resultValue = value.indexAccess(new IntegerValue(TypeContainer.getIntegerType(false, 8), testData.secondValueArray[0]));

            String resultString = resultValue.toASTNode(false).toSolidityCode();
            String expectedResultString = String.format("bytes1(0x%02X)", testData.operationResult[0]);
            System.out.println("got " + resultString + " wanted " + expectedResultString);

            assert resultString.equals(expectedResultString);
        }
    }

    @Test
    public void length() throws Exception {
    }


    @Test
    public void castToIntegerType() throws Exception {
        BytesTestValues[] ab = new BytesTestValues[]{
                new BytesTestValues("bytes1", new byte[]{b(0xff)}, null, 1, null),
                new BytesTestValues("bytes2", new byte[]{b(0x1), b(0x0)}, null, 2, null)
        };
        IntegerTestValues[] ai = new IntegerTestValues[]{
                new IntegerTestValues("int8", null, true, 8, "-1"),
                new IntegerTestValues("int32", null, true, 32, "256")
        };
        for (int i = 0; i < ab.length; ++i) {
            BytesTestValues sourceData = ab[i];
            IntegerTestValues destData = ai[i];

            ASTElementaryTypeName sourceType = TypeContainer.getByteType(sourceData.typeBytes);
            BytesValue value = new BytesValue(sourceType, sourceData.valueArray);

            ASTElementaryTypeName destType = TypeContainer.getIntegerType(destData.isSigned, destData.typeBits);
            IntegerValue resultValue = value.convertToIntegerType(destType);

            String got = s(resultValue);
            String wanted = decstring(destData.typeText, destData.operationResult);
            System.out.println("got " + got + " wanted " + wanted);

            Assert.assertTrue(got.equals(wanted));
        }

    }
}
