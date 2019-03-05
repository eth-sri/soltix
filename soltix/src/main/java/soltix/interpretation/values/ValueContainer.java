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
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;

import javax.swing.text.AbstractDocument;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * Class to create and store, for potential subsequent reuse, integer value instances
 */
public class ValueContainer {
    static private HashMap<Integer, HashMap<Long, IntegerValue>> smallSignedIntegerValues
            = new HashMap<Integer, HashMap<Long, IntegerValue>>();
    static private HashMap<Integer, HashMap<Long, IntegerValue>> smallUnsignedIntegerValues
            = new HashMap<Integer, HashMap<Long, IntegerValue>>();

    static private BoolValue trueBoolValue;
    static private BoolValue falseBoolValue;

    static protected IntegerValue doGetIntegerValue(ASTNode type, long smallInitializer, BigInteger bigInitializer) throws Exception {
        if (!Type.isIntegerType(type)) {
            throw new Exception("ValueContainer.getSmallInteger value applied to non-integer type " + type.toSolidityCode());
        }
        if (bigInitializer != null) {
            // Not caching BigIntegers for now
            return new IntegerValue(type, bigInitializer);
        }

        ASTElementaryTypeName elementaryTypeName = (ASTElementaryTypeName)type;
        boolean signed = elementaryTypeName.isSigned();
        int bits = elementaryTypeName.getBits();

        if (signed) {
            HashMap<Long, IntegerValue> valueMap = smallSignedIntegerValues.get(bits);
            if (valueMap == null) {
                valueMap = new HashMap<Long, IntegerValue>();
                smallSignedIntegerValues.put(bits, valueMap);
            }
            IntegerValue value = valueMap.get(smallInitializer);
            if (value == null) {
                value = new IntegerValue(type, smallInitializer);
                valueMap.put(smallInitializer, value);
            }
            return value;
        } else {
            HashMap<Long, IntegerValue> valueMap = smallUnsignedIntegerValues.get(bits);
            if (valueMap == null) {
                valueMap = new HashMap<Long, IntegerValue>();
                smallUnsignedIntegerValues.put(bits, valueMap);
            }
            IntegerValue value = valueMap.get(smallInitializer);
            if (value == null) {
                value = new IntegerValue(type, smallInitializer);
                valueMap.put(smallInitializer, value);
            }
            return value;
        }
    }

    static public IntegerValue getSmallIntegerValue(ASTNode type, long initializer) throws Exception {
        return doGetIntegerValue(type, initializer, null);
    }

    static public IntegerValue getBigIntegerValue(ASTNode type, BigInteger initializer) throws Exception {
        return doGetIntegerValue(type, 0, initializer);
    }

    static public BoolValue getBoolValue(boolean value) throws Exception {
        if (value) {
            if (trueBoolValue == null) trueBoolValue = new BoolValue(true);
            return trueBoolValue;
        } else {
            if (falseBoolValue == null) falseBoolValue = new BoolValue(false);
            return falseBoolValue;
        }
    }
}
