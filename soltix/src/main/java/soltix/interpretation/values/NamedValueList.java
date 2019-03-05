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

import soltix.ast.ASTNode;
import soltix.ast.ASTVariableDeclaration;
import soltix.ast.ASTVerbatimText;
import soltix.interpretation.Type;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to represent an ordered list of fields and values for structs and contracts
 */
public class NamedValueList {
    // Values and corresponding struct member declarations at same positions.
    // These are ArrayLists to preserve the order in which values are added, such that StructValues can be constructed
    // in the order in which members appear.
    // The variable declarations may not be needed. They would be required to map values to members if there are holes
    // in the initializer list
    // TODO clear this up
    private ArrayList<ASTVariableDeclaration> declarations;
    private HashMap<String, ASTVariableDeclaration> declarationsMap;
    private ArrayList<Value> values;
    private HashMap<String, Value> valuesMap;
    private HashMap<String, Integer> indexMap; // to know where to update the list as well. TODO this is getting ugly fast

    public void addMember(ASTVariableDeclaration variable, Value value) {
        if (declarations == null) {
            declarations = new ArrayList<ASTVariableDeclaration>();
            declarationsMap = new HashMap<String, ASTVariableDeclaration>();
            values = new ArrayList<Value>();
            valuesMap = new HashMap<String, Value>();
            indexMap = new HashMap<String, Integer>();
        }
        indexMap.put(variable.getName(), values.size());
        declarations.add(variable);
        declarationsMap.put(variable.getName(), variable);
        values.add(value);
        valuesMap.put(variable.getName(), value);
    }

    public void updateValue(String name, Value value) throws Exception {
        ASTVariableDeclaration declaration = declarationsMap.get(name);
        if (!Type.isSameType(null, declaration.getTypeName(), value.getType())) {
            throw new Exception("NamedValueList: Update type mismatch " + declaration.getName() + " " +
                    declaration.getTypeName().toSolidityCode() + " vs " + value.getType().toSolidityCode());
        }

        valuesMap.put(name, value);

        int index = indexMap.get(name);
        values.set(index, value);
    }

    public ArrayList<ASTVariableDeclaration> getDeclarations() { return declarations; }
    public ArrayList<Value> getValues() { return values; }

    public Value getMemberValue(String name) {
        return valuesMap.get(name);
    }

    public ASTNode genericToASTNode(ASTNode type, boolean forJavaScript) throws Exception {
        // "Struct T" or "Contract C" value syntax in:
        //     JavaScript: [m1, m2, ...]
        //     Solidity:   T(m1, m2, ...)
        String result = forJavaScript? "[": (type.getName() + "(");

        int i = 0;
        if (getValues() != null) {
            for (Value value : getValues()) {
                if (i > 0) {
                    result += ", ";
                }
                ++i;
                result += value.toASTNode(forJavaScript).toSolidityCode();
            }
        }
        if (forJavaScript) {
            result += "]";
        } else {
            result += ")";
        }
        return new ASTVerbatimText(0, result);
    }
}
