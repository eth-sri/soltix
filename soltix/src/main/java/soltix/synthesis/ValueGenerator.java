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

package soltix.synthesis;

import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.values.*;
import soltix.util.RandomNumbers;
import soltix.util.Util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

/**
 * Class to generate randomized values of specified types
 */ 
public class ValueGenerator {
    private RandomNumbers prng;

    // Distinguish between various different approaches to generate integer values. Entirely random
    // values appear to be undesirable, since they will typically result in large values for most
    // types, and thus:
    //
    //    - Cannot meaningfully be used for array sizes or as loop iteration counts
    //    - Are useless as various operands, e.g. to shift operators that will only yield 0 for huge shift operations
    //
    // In these contexts small values are preferable. Externally supplied (rather than generated) contracts
    // are particularly prone to problems with large transaction arguments
    public enum IntegerGenerationPolicy {
        INTEGER_GENERATION_POLICY_EXCLUSIVELY_SMALL, // TODO yet another mode EXCLUSIVELY_SMALL_POSITIVE could be useful
        INTEGER_GENERATION_POLICY_FAVOR_SMALL,
        INTEGER_GENERATION_POLICY_RANDOM
    }

    public ValueGenerator(RandomNumbers prng) {
        this.prng = prng;
    }

    // Value creation primitives

    public static IntegerValue generateSmallerIntegerValue(IntegerValue value) throws Exception {
        if (value.isGreaterThan(value.getMinValue()).getValue()) {
            // We can generate a smaller value - just always take -1 for now
            return value.sub(new IntegerValue(value.getType(), 1));
        } else {
            // Not possible
            return null;
        }
    }

    public static IntegerValue generateBiggerIntegerValue(IntegerValue value) throws Exception {
        if (value.isSmallerThan(value.getMaxValue()).getValue()) {
            // We can generate a smaller value - just always take -1 for now
            return value.add(new IntegerValue(value.getType(), 1));
        } else {
            // Not possible
            return null;
        }
    }

    protected void generateValueList(AST ast,
                                     NamedValueList valueList,
                                     ArrayList<ASTVariableDeclaration> members,
                                     IntegerGenerationPolicy integerPolicy) throws Exception {
        for (ASTVariableDeclaration memberDeclaration : members) {
            if (memberDeclaration.getTypeName() instanceof ASTMapping) {
                // Mappings are ignored/skipped in struct initializers
                continue;
            }
            Value memberValue = generateRandomValue(ast, memberDeclaration.getTypeName(), integerPolicy);
            valueList.addMember(memberDeclaration, memberValue);
        }
    }

    protected byte generateRandomByte() {
        return (byte)prng.generateLongInteger(0, 255);
    }

    protected IntegerValue generateRandomUnsignedIntegerValue(ASTElementaryTypeName type, IntegerGenerationPolicy integerPolicy) throws Exception {
        // Magnitude representation (big endian) of the value
        byte[] valueArray = new byte[type.getBits()/8];

        switch (integerPolicy) {
            case INTEGER_GENERATION_POLICY_EXCLUSIVELY_SMALL:
                // Set lowest byte only
                valueArray[valueArray.length-1] = generateRandomByte();
                break;
            case INTEGER_GENERATION_POLICY_FAVOR_SMALL:
                if (prng.flipCoin()) {
                    // Set lowest byte only
                    valueArray[valueArray.length-1] = generateRandomByte();
                } else {
                    // Create random value
                    for (int i = 0; i < valueArray.length; ++i) {
                        valueArray[i] = generateRandomByte();
                    }
                }
                break;
            case INTEGER_GENERATION_POLICY_RANDOM:
                // Create random value
                for (int i = 0; i < valueArray.length; ++i) {
                    valueArray[i] = generateRandomByte();
                }
                break;
            default:
                throw new Exception("Unsupported integer value generation policy " + integerPolicy);
        }

        BigInteger bigInteger = new BigInteger(1, valueArray);
        return new IntegerValue(type, bigInteger);
    }

    protected Value generateRandomSignedIntegerValue(ASTElementaryTypeName type, IntegerGenerationPolicy integerPolicy) throws Exception {
        IntegerValue dummyValue = ValueContainer.getSmallIntegerValue(type, 0);
        boolean isPosiive = prng.flipCoin();
        IntegerValue result;

        // Generate unsigned integers of the same bit width until a sufficiently small value occurs
        ASTElementaryTypeName unsignedType = TypeContainer.getIntegerType(false, type.getBits());
        IntegerValue unsignedValue;

        // Loop until the signed maximum + 1 is not exceeded
        do {
            unsignedValue = generateRandomUnsignedIntegerValue(unsignedType, integerPolicy);
        } while (unsignedValue.getBigValue().compareTo(dummyValue.getMaxValue().getBigValue().add(BigInteger.ONE)) > 0); // Access representation to play it safe WRT type mismatches in isGreaterThan()

        // If positive and the special case of value = maximum + 1 (which is not positively represenable) is not
        // present here, keep the value. Otherwise negate it
        result = new IntegerValue(type, unsignedValue.getBigValue());
        if (isPosiive) {
            if (unsignedValue.getBigValue().compareTo(dummyValue.getMaxValue().getBigValue().add(BigInteger.ONE)) == 0) {
                result = dummyValue.getMinValue();
            }
        } else {
            result = result.negate();
        }

        return result;
    }

    protected Value generateRandomStringValue() throws Exception {
        // There's no specified size limit for strings - but we limit ourselves to 255 bytes
        int size = (int)prng.generateLongInteger(0, 255);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            byte character = generateRandomByte();
	        sb.append(StringValue.printableCharacterString((char)character));
        }
        return new StringValue(sb.toString());
    }

    protected Value generateRandomBytesValue(ASTElementaryTypeName type) throws Exception {
        byte [] value = new byte[type.getBytes()];
        for (int i = 0; i < value.length; ++i) {
            value[i] = generateRandomByte();
        }
        return new BytesValue(type, value);
    }

    protected Value generateRandomAddressValue(ASTElementaryTypeName type) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        for (int i = 0; i < 20; ++i) {
            sb.append(String.format("%02x", (int)(generateRandomByte() & 0xff)));
        }
        return new AddressValue(sb.toString());
    }


    public Value generateZeroValue(AST ast, ASTNode type) throws Exception {
        // Pretend that we're generating a random value, but make the PRNG yield zero values exclusively
        prng.setGenerateZerosOnly(true);
        Value result = generateRandomValue(ast, type, IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_RANDOM);
        prng.setGenerateZerosOnly(false);
        return result;
    }

    // General-purpose random value generation (for no specific context such as array indices)
    public Value generateRandomValue(AST ast, ASTNode type, IntegerGenerationPolicy integerPolicy) throws Exception {
        if (type instanceof ASTElementaryTypeName) {
            ASTElementaryTypeName elementaryTypeName = (ASTElementaryTypeName)type;
            if (Type.isUnsignedIntegerType(type)) {
                return generateRandomUnsignedIntegerValue(elementaryTypeName, integerPolicy);
            } else if (Type.isSignedIntegerType(type)) {
                return generateRandomSignedIntegerValue(elementaryTypeName, integerPolicy);
            } else if (Type.isStringType(type)) {
                return generateRandomStringValue();
            } else if (Type.isByteType(type)) {
                return generateRandomBytesValue(elementaryTypeName);
            } else if (Type.isAddressType(type)) {
                return generateRandomAddressValue(elementaryTypeName);
            } else if (Type.isBoolType(type)) {
                return ValueContainer.getBoolValue(prng.flipCoin());
            } else {
                throw new Exception("ValueGenerator.generateDefaultValue called for unknown elementary type name " + type.getName());
            }
        } else if (type instanceof ASTArrayTypeName) {
            ASTNode baseType = ((ASTArrayTypeName) type).getBaseType();
            int size = 5;
            ArrayValue arrayValue = new ArrayValue(baseType);
            for (int i = 0; i < size; ++i) {
                Value elementValue = generateRandomValue(ast, baseType, integerPolicy); // individual generation, currently redundant (fixed non-random default values)
                arrayValue.addElement(elementValue);
            }
            return arrayValue;
        } else if (type instanceof ASTUserDefinedTypeName) {
            return generateDefaultUDTValue(ast, (ASTUserDefinedTypeName)type, integerPolicy);
        } else {
            throw new Exception("ValueGenerator.generateRandomValue called for unsupported type " + type.toSolidityCode());
        }
    }

    protected Value generateDefaultUDTValue(AST ast, ASTUserDefinedTypeName type, IntegerGenerationPolicy integerPolicy) throws Exception {
        ASTEnumDefinition enumDefinition = ast.getEnumDefinition(type.getName());
        if (enumDefinition != null) {
            RandomNumbers generator = new RandomNumbers(Configuration.randomNumbersSeed);
            int selectedEnumIndex = (int)generator.generateLongInteger(1, enumDefinition.getValues().size()) - 1;
            EnumValue enumValue = new EnumValue(enumDefinition, enumDefinition.getValues().get(selectedEnumIndex));
            return enumValue;
        } else {
            ASTStructDefinition structDefinition = ast.getStructDefinition(type.getName());
            if (structDefinition != null) {
                StructValue structValue = new StructValue(type, structDefinition);
                NamedValueList valueList = structValue.getMemberValueList(); // to fill with value
                ArrayList<ASTVariableDeclaration> members = structDefinition.getMembers(); // variables to assign to
                generateValueList(ast, valueList, members, integerPolicy);
                return structValue;
            } else {
                //throw new Exception("Value.generateDefaultValue unsupported for user defined type " + type.getClass().getName());
                ASTContractDefinition contractDefinition = ast.getContract(type.getName());
                if (contractDefinition == null) {
                    throw new Exception("Unknown user-defined type " + type.getName());
                }

                ContractValue contractValue = new ContractValue(contractDefinition, null);
                NamedValueList valueList = contractValue.getConstructorValueList();
                ASTFunctionDefinition constructor  = contractDefinition.getConstructor();
                if (constructor != null) {
                    ArrayList<ASTVariableDeclaration> constructorParameters = constructor.getParameterList().toArrayList();
                    generateValueList(ast, valueList, constructorParameters, integerPolicy);
                }

                // TODO implement contract values. This should probably be a ContractValue that is initially
                // characterized by its (also generated) constructor argument value, which mirror struct values
                // as an ordered list of heterogeneous types.
                // Later, contract variables must also be considered to get state (which will involve contract
                // function evaluation to compute them)
                // See, for struct contract fields:
                // ./run-one-test.sh ~/securify-contracts/0174d4582cca163501d219def808fd4f9a126dd0bf0f7d224020c7d83d585dc5/contract.sol
                //  throw new Exception("Unsupported UDT value generation");


                Util.unimpl();
                return contractValue;
            }
        }

        // TODO Function types?
    }
}
