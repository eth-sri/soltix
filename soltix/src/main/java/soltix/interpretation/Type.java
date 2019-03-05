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

/**
 * Class to perform various mostly comparative operations on types.
 *
 * For now we stick with AST nodes to represent types, and move higher-level semantic evaluations of them to
 * this class. The AST representation is fairly clunky, but this avoids conversions between different representations
 * in both directions
*/
public class Type {
    public static boolean isSignedIntegerType(ASTNode type) {
        return type instanceof ASTElementaryTypeName
            && ((ASTElementaryTypeName)type).isSignedIntegerType();
    }

    public static boolean isUnsignedIntegerType(ASTNode type) {
        return type instanceof ASTElementaryTypeName
                && ((ASTElementaryTypeName)type).isUnsignedIntegerType();
    }

    public static boolean isIntegerType(ASTNode type) {
        return type instanceof ASTElementaryTypeName
            && ((ASTElementaryTypeName)type).isIntegerType();
    }

    public static boolean isBoolType(ASTNode type) {
        return type instanceof ASTElementaryTypeName
            && ((ASTElementaryTypeName)type).getElementaryType() == ASTElementaryTypeName.ElementaryType.ELEMENTARY_TYPE_BOOL;
    }

    public static boolean isVariableByteType(ASTNode type) throws Exception {
        return type instanceof ASTElementaryTypeName
                && ((ASTElementaryTypeName)type).getElementaryType() == ASTElementaryTypeName.ElementaryType.ELEMENTARY_TYPE_BYTE
                && ((ASTElementaryTypeName)type).getBytes() == 0;
    }

    public static boolean isByteType(ASTNode type) {
        return type instanceof ASTElementaryTypeName
            && ((ASTElementaryTypeName)type).getElementaryType() == ASTElementaryTypeName.ElementaryType.ELEMENTARY_TYPE_BYTE;
    }

    public static boolean isStructType(AST ast, ASTNode type) {
        return type instanceof ASTUserDefinedTypeName
            && ast.getStructDefinition(type.getName()) != null;
    }

    public static boolean isContractType(AST ast, ASTNode type) {
        return type instanceof ASTUserDefinedTypeName
            && ast.getContract(type.getName()) != null;
    }

    public static boolean isStringType(ASTNode type) {
        return type instanceof ASTElementaryTypeName
            && ((ASTElementaryTypeName)type).getElementaryType() == ASTElementaryTypeName.ElementaryType.ELEMENTARY_TYPE_STRING;
    }

    public static boolean isAddressType(ASTNode type) {
        return type instanceof ASTElementaryTypeName
            && ((ASTElementaryTypeName)type).getElementaryType() == ASTElementaryTypeName.ElementaryType.ELEMENTARY_TYPE_ADDRESS;
    }

    public static boolean isArrayType(ASTNode type) {
        return type instanceof ASTArrayTypeName;
    }

    public static boolean isMappingType(ASTNode type) {
        return type instanceof ASTMapping;
    }

    public static boolean isSameType(AST ast, ASTNode firstType, ASTNode secondType) throws Exception {
        //if (isIntegerType(firstType) && isIntegerType(secondType)) {
        if (firstType instanceof ASTElementaryTypeName && secondType instanceof ASTElementaryTypeName) {
            ASTElementaryTypeName firstElementaryType = (ASTElementaryTypeName) firstType;
            ASTElementaryTypeName secondElementaryType = (ASTElementaryTypeName) secondType;
            if (isIntegerType(firstType) && isIntegerType(secondType)) {
                return firstElementaryType.isSigned() == secondElementaryType.isSigned()
                        && firstElementaryType.getBits() == secondElementaryType.getBits();
            } else if (isByteType(firstType) && isByteType(secondType)) {
                return firstElementaryType.getBytes() == secondElementaryType.getBytes();
            } else if (firstElementaryType.getElementaryType().equals(secondElementaryType.getElementaryType())) {
                return true;
            } else {
                return false;
            }
        /*} else if (firstType instanceof ASTArrayTypeName && secondType instanceof ASTArrayTypeName) {
            ASTArrayTypeName firstArrayType = (ASTArrayTypeName)firstType;
            ASTArrayTypeName secondArrayType = (ASTArrayTypeName)secondType;
            return isSameType(firstArrayType.getBaseType(), secondArrayType.getBaseType())
                    && firstArrayType.getElementCount() == secondArrayType.getElementCount(); // TODO: undefined sizes?
        }*/
        } else if (firstType instanceof ASTArrayTypeName || secondType instanceof ASTArrayTypeName) {
            // TODO The code above addresses this, but would need an integer- as opposed to ASTNode-based
            // element count. The Live-EMI context where this is used cannot use array types easily anyway
            throw new Exception("Type.isSameType is not defined for arrays");
        } else if (firstType instanceof ASTMapping) {
            throw new Exception("Type.isSameType is not defined for mappings");
        } else {
            if (firstType instanceof ASTUserDefinedTypeName && secondType instanceof ASTUserDefinedTypeName) {
                ASTStructDefinition firstStruct = ast.getStructDefinition(firstType.getName());
                if (firstStruct != null) {
                    return firstType.getName().equals(secondType.getName());
                }
                // Try enum instead
                ASTEnumDefinition firstEnum = ast.getEnumDefinition(firstType.getName());
                if (firstEnum != null) {
                    return firstType.getName().equals(secondType.getName());
                }
                // TODO Try contract instead
                throw new Exception("Type.isSameType: Unsupported UDT " + firstType.getName());
            } else {
                // Some other constellation such as elementary type vs struct
                return false;
            }
        }
    }

    public static ASTNode getCompatibleIntegerType(ASTNode firstType, ASTNode secondType) throws Exception {
        if (!(firstType instanceof ASTElementaryTypeName) || !(secondType instanceof ASTElementaryTypeName)) {
            throw new Exception("Type.getCompatibleIntegerType applied to non-ASTElementaryTypeName argument in "
                + firstType.getClass().getName() + " vs " + secondType.getClass().getName());
        }

        ASTElementaryTypeName firstElementaryType = (ASTElementaryTypeName)firstType;
        ASTElementaryTypeName secondElementaryType = (ASTElementaryTypeName)secondType;

        if (!firstElementaryType.isIntegerType() || !secondElementaryType.isIntegerType()) {
            throw new Exception("Type.getCompatibleIntegerType applied to non-integer type in "
                + firstElementaryType.toSolidityCode() + " vs " + secondElementaryType.toSolidityCode());
        }

        // TODO use bit and sign info to determine correct type
        return firstType;
    }

    protected static boolean doIsRecursiveMappingValueType(AST ast, ASTStructDefinition structDefinition) {
        for (ASTVariableDeclaration member : structDefinition.getMembers()) {
            if (Type.isMappingType(member.getTypeName())) {
                return true;
            } else if (Type.isStructType(ast, member.getTypeName())) {
                ASTStructDefinition memberStructType = ast.getStructDefinition(member.getTypeName().getName());
                if (doIsRecursiveMappingValueType(ast, memberStructType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isRecursiveMappingValueType(AST ast, ASTNode type) {
        if (!isStructType(ast, type)) {
            return false;
        }
        ASTStructDefinition structDefinition = (ASTStructDefinition)ast.getStructDefinition(type.getName());
        return doIsRecursiveMappingValueType(ast, structDefinition);
    }
}
