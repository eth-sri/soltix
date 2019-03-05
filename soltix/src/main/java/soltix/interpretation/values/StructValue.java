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

import soltix.ast.*;
import soltix.interpretation.Type;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to represent structure values
 */
public class StructValue extends Value {
    // Struct type as UDT (first class citizen for consistency with other AST representations) and struct type
    private ASTUserDefinedTypeName type;
    private ASTStructDefinition structType;

    private NamedValueList members;

    public StructValue(ASTUserDefinedTypeName type, ASTStructDefinition structType) {
        //super(type);
        this.type = type;
        this.structType = structType;
        members = new NamedValueList();
    }

    // TODO This should probably check consistency with structType
    public void addMember(ASTVariableDeclaration variable, Value value) { members.addMember(variable, value); }
    public Value getMemberValue(String name) {
        return members.getMemberValue(name);
    }
    public void updateMemberValue(String name, Value value) throws Exception { members.updateValue(name, value); }
    public NamedValueList getMemberValueList() { return members; }

    @Override
    public ASTNode getType() { return type; }
    public ASTStructDefinition getStructType() { return structType; }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        return members.genericToASTNode(structType, forJavaScript);
    }

    @Override
    public Object toJSONRepresentation() {
        JSONObject result = new JSONObject();
        for (int i = 0; i < members.getDeclarations().size(); ++i) {
            String memberName = members.getDeclarations().get(i).getName();
            Value memberValue = members.getValues().get(i);
            result.put(memberName, memberValue);
        }
        return result;
    }

    // Create a copy of all member value structures to avoid subsequent modifications. This assumes that:
    //     1. Declarations/types are not subject to change
    //     2. Member values are updated by *replacing* them, so old instances can be referenced with no change
    public StructValue createDeepValueCopy(AST ast) throws Exception {
        StructValue copiedValue = new StructValue(this.type, this.structType);

        ArrayList<ASTVariableDeclaration> declarations = members.getDeclarations();
        ArrayList<Value> values = members.getValues();

        for (int i = 0; i < declarations.size(); ++i) {
            Value memberValue = values.get(i);
            if (Type.isStructType(ast, values.get(i).getType())) {
                StructValue value = (StructValue)values.get(i);
                memberValue = value.createDeepValueCopy(ast);
            } else if (Type.isArrayType(values.get(i).getType())) {
                throw new Exception("createDeepValueCopy: Unsupported array type");
            }
            copiedValue.addMember(declarations.get(i), memberValue);
        }
        return copiedValue;
    }

}
