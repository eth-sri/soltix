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

import soltix.ast.ASTEnumDefinition;
import soltix.ast.ASTEnumValue;
import soltix.ast.ASTNode;
import soltix.ast.ASTVerbatimText;

/**
 * Class to represent an enum value
 */
public class EnumValue extends Value {
    private ASTEnumDefinition type;
    private ASTEnumValue value;

    public EnumValue(ASTEnumDefinition type, ASTEnumValue value) {
        //super(type);
        this.type = type;
        this.value = value;
    }

    @Override
    public ASTNode getType() { return type; }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        if (!forJavaScript) {
            return value; // Original value node can just be kept
        } else {
            // Need ordinal number
            return new ASTVerbatimText(0, String.valueOf(value.getOrdinalNumber()));
        }
    }

    @Override
    public Object toJSONRepresentation() throws Exception {
        throw new Exception("Enum.toJSONRepresentation is unimplemented");
    }
}
