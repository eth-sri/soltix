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

import soltix.ast.ASTArrayTypeName;
import soltix.ast.ASTLiteral;
import soltix.ast.ASTNode;
import soltix.ast.ASTVerbatimText;
import soltix.interpretation.TypeContainer;
import org.json.simple.JSONArray;

import java.util.ArrayList;

/**
 * Class to represent array values
 */
public class ArrayValue extends Value implements IArrayOperations {
    private ASTNode baseType;
    private int lastTypeElementCount = -1;
    private ASTNode type = null;

    public ArrayValue(ASTNode baseType) {
        //super(baseType);
        this.baseType = baseType;
    }

    private ArrayList<Value> values;

    public void addElement(Value value) {
        if (values == null) {
            values = new ArrayList<Value>();
        }
        values.add(value);
    }

    // Exported array operations:
    public Value indexAccess(int index) throws Exception {
        return values.get(index);
    }
    public IntegerValue length() throws Exception {
        return ValueContainer.getSmallIntegerValue(TypeContainer.getIntegerType(false, 32), values.size());
    }

    @Override
    public ASTNode getType() throws Exception {
        if (type == null || lastTypeElementCount != values.size()) {
            ASTArrayTypeName arrayTypeName = new ASTArrayTypeName(0);
            arrayTypeName.addChildNode(baseType);
            lastTypeElementCount = values.size();
            ASTLiteral sizeLiteral = new ASTLiteral(0, "uint", String.valueOf(lastTypeElementCount), ASTLiteral.LiteralType.LITERAL_TYPE_INTEGER_DECIMAL, null);
            arrayTypeName.addChildNode(sizeLiteral);
            sizeLiteral.finalize();
            arrayTypeName.finalize();
        }
        return type;
    }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        // Build array value in JavaScript format for now, since we currently only use this in
        // truffle constructor/tx generation
        // TODO proper value representation and separation of JS/Solidity output
        String result = "["; //forJavaScript? "[": "";
        for (int i = 0; i < values.size(); ++i) {
            if (i > 0) {
                result += ", ";
            }
            Value value = values.get(i);
            result += value.toASTNode(forJavaScript).toSolidityCode();
        }
        //if (forJavaScript) {
            result += "]";
        //}
        return new ASTVerbatimText(0, result);
    }

    @Override
    public Object toJSONRepresentation() throws Exception {
        JSONArray result = new JSONArray();
        for (Value elementValue : values) {
            result.add(elementValue.toJSONRepresentation());
        }
        return result;
    }
}
