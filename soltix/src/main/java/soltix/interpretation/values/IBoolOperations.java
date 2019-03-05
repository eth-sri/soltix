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
 * Interface with operations on BoolValue items
 */
public interface IBoolOperations {
    public BoolValue isEqualTo(BoolValue value) throws Exception;
    public BoolValue isNotEqualTo(BoolValue value) throws Exception;
    public BoolValue isGreaterThan(BoolValue value) throws Exception;
    public BoolValue isSmallerThan(BoolValue value) throws Exception;
    public BoolValue isGreaterThanOrEqualTo(BoolValue value) throws Exception;
    public BoolValue isSmallerThanOrEqualTo(BoolValue value) throws Exception;
    public BoolValue logicalOr(BoolValue value) throws Exception;
    public BoolValue logicalAnd(BoolValue value) throws Exception;
}
