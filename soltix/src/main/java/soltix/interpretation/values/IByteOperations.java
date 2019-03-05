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

/**
 * Interface with operations defined for BytesValue items
 */
public interface IByteOperations {
    public BoolValue isSmallerThan(BytesValue value) throws Exception;
    public BoolValue isSmallerThanOrEqualTo(BytesValue value) throws Exception;
    public BoolValue isGreaterThan(BytesValue value) throws Exception;
    public BoolValue isGreaterThanOrEqualTo(BytesValue value) throws Exception;
    public BoolValue isEqualTo(BytesValue value) throws Exception;
    public BoolValue isNotEqualTo(BytesValue value) throws Exception;

    public BytesValue negateBitwise() throws Exception;
    public BytesValue bitwiseOr(Value value) throws Exception;
    public BytesValue bitwiseAnd(Value value) throws Exception;
    public BytesValue bitwiseXor(Value value) throws Exception;
    public BytesValue bitwiseShiftLeft(Value value) throws Exception;
    public BytesValue bitwiseShiftRight(Value value) throws Exception;

    public BytesValue indexAccess(Value index) throws Exception;
    public IntegerValue length() throws Exception;

    public BytesValue convertToBytesType(ASTElementaryTypeName otherType) throws Exception;
}
