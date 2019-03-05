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

public class ASTParameterList extends ASTNode {
    private ArrayList<ASTVariableDeclaration> arrayList = null;

    public ASTParameterList(long id) throws Exception { super(id); }

    public ArrayList<ASTVariableDeclaration> toArrayList() {
        if (arrayList == null) {
            arrayList = new ArrayList<ASTVariableDeclaration>();
            for (int i = 0; i < getChildCount(); ++i) {
                arrayList.add((ASTVariableDeclaration)getChild(i));
            }
        }
        return arrayList;
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = "";
        for (int i = 0; i < getChildCount(); ++i) {
            if (i > 0) {
                code += ", ";
            }
            code += getChild(i).toSolidityCode();
        }
        return code;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }
}
