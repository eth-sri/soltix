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

public class ASTPragmaDirective extends ASTNode {
    private String type = null;
    private String args = null;

    public ASTPragmaDirective(long id, String type, String args) throws Exception {
        super(id);
        this.type = type;
        this.args = args;
    }

    @Override
    public String toSolidityCode() {
        String code = "pragma " + type;
        if (args != null) {
            code += " ";
            if (type.equals("experimental")) {
                code += "\"";
            }
            code += args;
            if (type.equals("experimental")) {
                code += "\"";
            }
        }
        return code;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() > 0) {
            throw new Exception("Unexpected pragma directive child count " + getChildCount());
        }
    }
}
