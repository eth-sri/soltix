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
package soltix.ast;

import java.util.ArrayList;

public class ASTEnumDefinition extends UserDefinedType {
    private ArrayList<ASTEnumValue> values = null;

    public ASTEnumDefinition(long id, String name, String canonicalName) throws Exception { super(id, name, canonicalName); }

    public ArrayList<ASTEnumValue> getValues() { return values; }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "enum " + name + " { ";
        for (int i = 0; i < values.size(); ++i) {
            if (i > 0) {
                code += ", ";
            }
            code += values.get(i).toSolidityCode();
        }
        code += "}";
        return code;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        for (int i = 0; i < getChildCount(); ++i) {
            if (!(getChild(i) instanceof ASTEnumValue)) {
                throw new Exception("Unexpected enum child node of type " + getChild(i).getClass().getName());
            }
        }
        values = new ArrayList<ASTEnumValue>();

        int currentOrdinalNumber = 0;
        for (ASTNode childNode : getChildren()) {
            ASTEnumValue enumValue = (ASTEnumValue)childNode;
            enumValue.setOrdinalNumber(currentOrdinalNumber++);
            values.add(enumValue);
        }
        setChildren(null);
    }
}
