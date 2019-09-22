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

package soltix.interpretation;

import soltix.ast.*;
import soltix.synthesis.StructTypeGenerator;
import soltix.util.RandomNumbers;
import soltix.util.WeightedRandomChoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static soltix.ast.ASTElementaryTypeName.VARIABLE_LENGTH_BYTES_TYPE_NAME;

/**
 * Class to produce and cache types.
 */
public class TypeContainer {
    static private HashMap<Integer, ASTElementaryTypeName> typesSignedInteger;
    static private HashMap<Integer, ASTElementaryTypeName> typesUnsignedInteger;
    static private ASTElementaryTypeName typeBool;
    static private HashMap<Integer, ASTElementaryTypeName> typesByte;
    static private ASTElementaryTypeName typeString;
    static private ASTElementaryTypeName typeAddress;
    static private HashMap<String, ASTUserDefinedTypeName> userDefinedTypes;
    // For random type choice:
    static private WeightedRandomChoice weightedRandomChoice;


    static public ASTElementaryTypeName getIntegerType(boolean signed, int bits) throws Exception {
        if (signed) {
            if (typesSignedInteger == null) {
                typesSignedInteger = new HashMap<Integer, ASTElementaryTypeName>();
            }
            ASTElementaryTypeName type;
            type = typesSignedInteger.get(bits);
            if (type == null) {
                type = new ASTElementaryTypeName(0, "int" + String.valueOf(bits));
                typesSignedInteger.put(bits, type);
            }
            return type;
        } else {
            if (typesUnsignedInteger == null) {
                typesUnsignedInteger = new HashMap<Integer, ASTElementaryTypeName>();
            }
            ASTElementaryTypeName type;
            type = typesUnsignedInteger.get(bits);
            if (type == null) {
                type = new ASTElementaryTypeName(0, "uint" + String.valueOf(bits));
                typesUnsignedInteger.put(bits, type);
            }
            return type;
        }
    }
    static public ASTElementaryTypeName getBoolType() throws Exception {
        if (typeBool == null) {
            typeBool = new ASTElementaryTypeName(0, "bool");
        }
        return typeBool;
    }
    static public ASTElementaryTypeName getByteType(int bytes) throws Exception {
        if (typesByte == null) {
            typesByte = new HashMap<Integer, ASTElementaryTypeName>();
        }
        ASTElementaryTypeName type = typesByte.get(bytes);
        if (type == null) {
            type = new ASTElementaryTypeName(0, bytes == 0? VARIABLE_LENGTH_BYTES_TYPE_NAME: "bytes" + String.valueOf(bytes));
            typesByte.put(bytes, type);
        }
        return type;
    }
    static public ASTElementaryTypeName getStringType() throws Exception {
        if (typeString == null) {
            typeString = new ASTElementaryTypeName(0, "string");
        }
        return typeString;
    }
    static public ASTElementaryTypeName getAddressType() throws Exception {
        if (typeAddress == null) {
            typeAddress = new ASTElementaryTypeName(0, "address");
        }
        return typeAddress;
    }

    static public ASTUserDefinedTypeName getUserDefinedType(AST ast, String name) throws Exception {
        ASTUserDefinedTypeName udt = userDefinedTypes != null? userDefinedTypes.get(name): null;
        if (udt == null) {
            if (userDefinedTypes == null) {
                userDefinedTypes = new HashMap<String, ASTUserDefinedTypeName>();
            }
            if (ast.getContract(name) == null
                    && ast.getStructDefinition(name) == null
                    && ast.getEnumDefinition(name) == null) {
                throw new Exception("Unknown user-defined type " + name);
            }
            udt = new ASTUserDefinedTypeName(0, name);
            userDefinedTypes.put(name, udt);
        }
        return udt;
    }

    public static ASTElementaryTypeName getRandomIntegerType(RandomNumbers prng) throws Exception {
        boolean signed = prng.flipCoin();

        // Available types: (u)int8 to (u)int256 in steps of 8
        // TODO This should perhaps be biased to smaller types to provoke more constellations in which
        // items of small types are packed into a larger block
        int bitCount = 8 * (int)prng.generateLongInteger(1, 32);

        return getIntegerType(signed, bitCount);
    }

    public static ASTElementaryTypeName getRandomByteType(RandomNumbers prng) throws Exception {
        // Available types: bytes1 to bytes32
        int byteCount = (int)prng.generateLongInteger(1, 32);
        return getByteType(byteCount);
    }



    protected static ASTStructDefinition tryGetRandomStructDefinition(AST ast, RandomNumbers prng) {
        Collection<ASTStructDefinition> structDefinitions = ast.getStructDefinitions();
        int randomId = (int)prng.generateLongInteger(1, structDefinitions.size());
        for (ASTStructDefinition definition : structDefinitions) {
            if (--randomId == 0) {
                return definition;
            }
        }
        return null;
    }

    protected static ASTUserDefinedTypeName tryGetRandomStructType(AST ast, RandomNumbers prng) {
        ASTStructDefinition structDefinition = tryGetRandomStructDefinition(ast, prng);
        if (structDefinition == null) {
            return null;
        }
        return structDefinition.getUserDefinedType();
    }


    public static ASTNode chooseRandomType(AST ast,
                                           ASTContractDefinition contract,
                                           boolean allowStructs,
                                           StructTypeGenerator structTypeGenerator,
                                           RandomNumbers prng) throws Exception {
        ASTNode type;
        int choiceValue;

        if (weightedRandomChoice == null) {
            weightedRandomChoice = new WeightedRandomChoice();
            // Prepare random weighted type choice
            // TODO weights should be configurable - separately for storage variables and parameters
            weightedRandomChoice.addItem(35); // 0: Integers - overrepresented due to richer set of operations
            weightedRandomChoice.addItem(30); // 1: Bytes
            weightedRandomChoice.addItem(20); // 2: Struct
            weightedRandomChoice.addItem(10); // 3: Bool
           // TODO strings still require expression generator changes to
           // - convert to bytes where needed
           // - subscript the result to obtain a value to work with
           // Disabled for now to avoid breaking all code generation

           weightedRandomChoice.addItem(5); //  4: String: not much we can do with them, but they test the ABI
            // TODO Enums, arrays, and eventually other contracts (eating into integer weight) - accessing "ast"
          //  weightedRandomChoice.addItem(5); //  3: Enum
        }


        type = null;
        while (type == null) {
            switch (choiceValue = weightedRandomChoice.choice(prng)) {
                case 0: // Integers
                    type = TypeContainer.getRandomIntegerType(prng);
                    break;
                case 1: // Bytes
                    type = TypeContainer.getRandomByteType(prng);
                    break;
                case 2: // Struct
                    if (allowStructs) { // if not allowed, just continue
                        if (structTypeGenerator != null) {
                            // Generate new type
                            type = structTypeGenerator.generate(ast, contract); // returns UDT for consistency, not struct definition
                        } else {
                            // Use existing type - if possible. Otherwise we just keep looping due to type = null
                            type = tryGetRandomStructType(ast, prng);
                        }
                    }
                    break;
                case 3: // Bool
                    type = TypeContainer.getBoolType();
                    break;
                case 4: // String
                    type = TypeContainer.getStringType();
                    break;
                case 5: // Enum
                default:
                    throw new Exception("VariableGenerator.chooseRandomType: Unexpected type index " + choiceValue);
            }
        }
        return type;
    }
}
