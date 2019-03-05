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

public class ASTTupleExpression extends ASTNode {
    private ArrayList<ASTNode> tupleItems = null;
    private ArrayList<Boolean> tupleComponents = null;
    private String tupleTypeField = null;
    private boolean isInlineArray = false;

    public ASTTupleExpression(long id, boolean isInlineArray, String tupleTypeField, ArrayList<Boolean> tupleComponents) throws Exception {
        super(id);
        this.isInlineArray = isInlineArray;
        this.tupleComponents = tupleComponents;
        this.tupleTypeField = tupleTypeField;
    }

    private ArrayList<Boolean> getTupleFieldInfo(String typeName) throws Exception {
        int parensNesting = 0;
        int count = 1; // assume 1 by default, each comma adds another. are empty tuples possible?
        char lastChar = 0;
        ArrayList<Boolean> result = new ArrayList<Boolean>();

        if (typeName == null || !typeName.startsWith("tuple(")) {
            // Assume a single-field tuple such as "(true)" which is represented as TupleExpression,
            // but unlike multi-field tuples such as "(true,false)" will have type "bool", not "tuple(bool)"
            result.add(true);
            return result;
        }

        for (int i = 0; i < typeName.length(); ++i) {
            if (typeName.charAt(i) == '(') {
                ++parensNesting;
            } else if (typeName.charAt(i) == ')') {
                --parensNesting;
                if (parensNesting == 0) {
                    // The type apparently ends here
                    if (i+1 < typeName.length()) {
                        throw new Exception("Unexpected tuple field format");
                    }
                    if (lastChar != ',' && lastChar != '(' /*can't happen - empty tuple?!*/) {
                        // There was another field - already acounted for by "count", but not
                        // the empty/nonempty list
                        result.add(true); // was nonempty
                    } else {
                        result.add(false); // was empty
                    }
                }
            } else if (typeName.charAt(i) == ',') {
                if (parensNesting == 1) {
                    ++count;
                    if (lastChar == '(' || lastChar == ',') {
                        // Field is empty
                        result.add(false);
                    } else {
                        // Field is not empty
                        result.add(true);
                    }
                }
            }
            lastChar = typeName.charAt(i);
        }
       /* System.out.println("getTupleFieldInfo " + typeName + " = " + result.size());
        for (int i = 0; i < result.size(); ++i) {
            System.out.println(" " + i + " = " + result.get(i));
        }*/
        return result;
    }

    private int count = 0;

    @Override
    public String toSolidityCode() throws Exception {
        String code = isInlineArray? "[": "(";

       // System.out.println("items " + tupleItems.size() + " components " + tupleComponents.size() + " for " + this + " type "
       //     + (tupleTypeField != null? tupleTypeField: "unknown"));
        for (int i = 0, tupleIdx = 0; i < tupleComponents.size(); ++i) {
            //System.out.println("components " + i + " = " + tupleComponents.get(i));
            if (i > 0) {
                code += ", ";
            }
            if (tupleComponents.get(i)) {
                code += tupleItems.get(tupleIdx++).toSolidityCode();
            }
        }
        code += isInlineArray? "]": ")";
        //System.out.println("code = " + code);
        return code;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        tupleItems = getChildren();
        setChildren(null);

        // Tuple typing is super awkward due to irregularities if fields are omitted. For example, in
        //    (a,,) = function();
        // a tuple of 3 fields must be parsed, but the only discernible AST-JSON difference to
        //    (a,,,) = function();
        // with 4 fields appears to be the "type" attribute of "tuple(uint256,,)".
        // So we apparently have to parse the field in this case to determine the field count.
        //
        // But if instead of supplying the first field we omit it, e.g. in
        //    (,b,) = function();
        // then suddenly solc emits an additional attributes array called "components" that does appear
        // to give us the desired field count and mapping info by encoding it as:
        //    null, identifier, null
        //
        // The absence of the "components" field apparently tells us that all supplied tuple children
        // are at the beginning of the tuple. But we still have to parse the "type" attribute to
        // get the field count.
        //
        // Another special case in this madness: If the first tuple field is non-empty but there are
        // holes later, as in
        //    (a,,c) = function();
        // then we also don't get the "components" field we'd need. So we have to parse the type field
        // - which in this case is set to
        //    tuple(type_a,,type_c)
        // - not just to count the fields, but also for the empty field information.
        //
        // We need a final test - the "startWith("tuple("" one below to avoid these considerations
        // because tuple expressions which aren't marked with a "tuple" type do not have these
        // distinctions, e.g.
        //    return (true,false);
        // just produces two "bool"-typed child nodes. Maybe there's an lvalue vs. rvalue distinction
        //
        // This is pretty much guaranteed to break for pathological cases
        if (tupleComponents != null) {
            ;
        } else if (tupleTypeField != null && tupleTypeField.startsWith("tuple(")) {
                // As per above: Supplied items are at the beginning, but their count must be determined
                // from the type field
            /*int fieldCount = getTupleFieldCount(tupleTypeField);
            tupleComponents = new ArrayList<Boolean>();
            for (int i = 0; i < tupleItems.size(); ++i) {
                tupleComponents.add(true);
            }
            int remaining = fieldCount - tupleItems.size();
            for (int i = 0; i < remaining; ++i) {
                tupleComponents.add(false);
            }*/
            tupleComponents = getTupleFieldInfo(tupleTypeField);
        } else {
            tupleComponents = new ArrayList<Boolean>();
            for (int i = 0; i < tupleItems.size(); ++i) {
                tupleComponents.add(true);
            }
        }
    }
}
