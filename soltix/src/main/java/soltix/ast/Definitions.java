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

// This class contains various enumerations for Solidity concepts like visibility, storage location, and so on.
//
// (Some of these can be directly associated with a single ASTNode and could be placed there (ContractKind)
// but keeping this in one place also has advantages)
public class Definitions {
    public enum StorageLocation {
        STORAGE_LOCATION_DEFAULT("default"),
        STORAGE_LOCATION_STORAGE("storage"),
        STORAGE_LOCATION_MEMORY("memory");

        private String name;
        StorageLocation(String name) {
            this.name = name;
        }

        static public StorageLocation fromString(String name) {
            for (StorageLocation s: StorageLocation.values()) {
                if (s.toString().equals(name)) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Visibility {
        VISIBILITY_INTERNAL("internal"),
        VISIBILITY_EXTERNAL("external"),
        VISIBILITY_PRIVATE("private"),
        VISIBILITY_PUBLIC("public");

        private String name;
        Visibility(String name) { this.name = name; }

        static public Visibility fromString(String name) {
            for (Visibility v: Visibility.values()) {
                if (v.toString().equals(name)) {
                    return v;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ContractKind {
        CONTRACT_KIND_CONTRACT("contract"),
        CONTRACT_KIND_LIBRARY("library"),
        CONTRACT_KIND_INTERFACE("interface");

        private String name;
        ContractKind(String name) { this.name = name; }

        static public ContractKind fromString(String name) {
            for (ContractKind ck : ContractKind.values()) {
                if (ck.toString().equals(name)) {
                    return ck;
                }
            }
            return null;
        }

        @Override
        public String toString() { return name; }
    }
}
