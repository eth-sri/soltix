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
import java.util.HashMap;

public class ASTStructDefinition extends UserDefinedType {
    // Store members once in HashMap for lookups
    //private HashMap<String, ASTNode> membersMap;
    // ... and once in ArrayList to guarantee preservation of field order (storage organization may
    // otherwise make a difference)
    private ArrayList<ASTVariableDeclaration> membersList;
    private HashMap<String, ASTVariableDeclaration> membersMap;
    private boolean containsMapping = false;
    private boolean haveDeepContainsMapping = false;

    public ASTStructDefinition(long id, String name, String canonicalName) throws Exception {
        super(id, name, canonicalName);
    }

    public void resolveStructContainsMappingInfo(AST ast, ASTContractDefinition contract) throws Exception {
        // Deep resolution of "containsMapping" info
        if (membersList != null) {
            for (ASTVariableDeclaration member : membersList) {
                ASTNode type = member.getTypeName();
                if (type instanceof ASTMapping) {
                    containsMapping = true;
                } else if (type instanceof ASTUserDefinedTypeName) {
                    ASTStructDefinition definition = contract.getStructDefinition(type.getName());
                    if (definition == null) {
                        if (contract.getEnumDefinition(type.getName()) != null
                                || ast.getContract(type.getName()) != null) {
                            // OK this is an enum or contract (which may interestingly be part of a struct, not a struct
                            continue;
                        }
                        throw new Exception("Reference to unknown user defined type " + type.getName());
                    }
                    definition.resolveStructContainsMappingInfo(ast, contract);
                    if (definition.getContainsMapping()) {
                        containsMapping = true;
                    }
                }
            }
        }
        haveDeepContainsMapping = true;
    }

    public boolean getContainsMapping() {
        return containsMapping;
    }
    public boolean getHaveDeepContainsMapping() {
        return haveDeepContainsMapping;
    }

    public ArrayList<ASTVariableDeclaration> getMembers() { return membersList; }

    public ASTVariableDeclaration lookupMember(String name) {
        return membersMap.get(name);
    }


    @Override
    public String toSolidityCode() {
        return "struct " + name + " {";
    }
    @Override
    public String toSolidityCodePostfix() {
        return "}";
    }

    @Override
    public void finalize() throws Exception {
        //membersMap = new HashMap<String, ASTNode>();
        membersList = new ArrayList<ASTVariableDeclaration>();
        membersMap = new HashMap<String, ASTVariableDeclaration>();
        for (int i = 0; i < getChildCount(); ++i) {
            if (!(getChild(i) instanceof ASTVariableDeclaration)) {
                throw new Exception("Struct member item with unexpected type " + getChild(i));
            }
            //membersMap.put(getChild(i).getName(), getChild(i));
            ASTVariableDeclaration declaration = (ASTVariableDeclaration)getChild(i);
            if (declaration.getTypeName() instanceof ASTMapping) {
                // Note: It would be nice to propagate mapping info from member structs as well, but
                // we may not be able to look up all struct declarations at this point (during AST
                // construction) yet
                containsMapping = true;
            }
            membersList.add(declaration);
            membersMap.put(declaration.getName(), declaration);
        }

        /*
       Keep the child nodes for now, then Solidity emission will be more seamless for those
       nodes due to automatic correct nesting, etc.

        setChildren(null);
        */
    }
}
