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

public class ASTElementaryTypeNameExpression extends ASTNode {
    private ASTNode elementaryTypeName;
    private String typeString;

    public ASTElementaryTypeNameExpression(long id, String name, String typeString) throws Exception {
        super(id, name);
        this.typeString = typeString;
    }

    @Override
    public String toSolidityCode() { return name; }
    @Override
    public String toSolidityCodePostfix() { return null; }

    public ASTNode getElementaryTypeName() { return elementaryTypeName; }

    @Override
    public void finalize() throws Exception {
        String typePrefix = "type(";
        if (!typeString.startsWith(typePrefix) || !typeString.endsWith(")")) {
            throw new Exception("Invalid ASTElementaryTypeNameExpression type string " + typeString);
        }

        String typeName = typeString.substring(typePrefix.length(), typeString.length() - 1);
        elementaryTypeName = new ASTElementaryTypeName(0, typeName);
    }
}
