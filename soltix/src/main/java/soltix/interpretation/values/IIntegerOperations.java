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

/**
 * Interface with operations on IntegerValue items
 */
public interface IIntegerOperations {
    public Value getMinValue() throws Exception;
    public Value getMaxValue() throws Exception;

    public IntegerValue negate() throws Exception;
    public IntegerValue negateBitwise() throws Exception;

    // TODO in-place variants of arithmetic operations?!
    public IntegerValue add(Value value) throws Exception;
    public IntegerValue sub(Value value) throws Exception;
    public IntegerValue div(Value value) throws Exception;
    public IntegerValue mul(Value value) throws Exception;
    public IntegerValue mod(Value value) throws Exception;
    public IntegerValue exp(Value value) throws Exception;

    public IntegerValue bitwiseOr(Value value) throws Exception;
    public IntegerValue bitwiseAnd(Value value) throws Exception;
    public IntegerValue bitwiseXor(Value value) throws Exception;
    public IntegerValue bitwiseShiftLeft(Value value) throws Exception;
    public IntegerValue bitwiseShiftRight(Value value) throws Exception;

    public BoolValue isSmallerThan(Value value) throws Exception;
    public BoolValue isSmallerThanOrEqualTo(Value value) throws Exception;
    public BoolValue isGreaterThan(Value value) throws Exception;
    public BoolValue isGreaterThanOrEqualTo(Value value) throws Exception;
    public BoolValue isEqualTo(Value value) throws Exception;
    public BoolValue isNotEqualTo(Value value) throws Exception;

    public IntegerValue convertToIntegerType(ASTElementaryTypeName otherType) throws Exception;
}
