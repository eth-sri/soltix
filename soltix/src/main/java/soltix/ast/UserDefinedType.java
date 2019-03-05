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

abstract public class UserDefinedType extends ASTNode {
    private ASTUserDefinedTypeName userDefinedType;

    // "Canonical" name, <contract>.<type-name>, e.g.
    //     library L { struct S {...} }
    //     contract C { L.S s; }
    private String canonicalName;

    public UserDefinedType(long id, String name, String canonicalName) throws Exception {
        super(id, name);
        this.canonicalName = canonicalName;
        userDefinedType = new ASTUserDefinedTypeName(0, name);
    }

    public String getCanonicalName() { return canonicalName; }

    public ASTUserDefinedTypeName getUserDefinedType() { return userDefinedType; }
}
