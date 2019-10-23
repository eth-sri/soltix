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

import soltix.interpretation.TypeContainer;

import java.util.ArrayList;
import java.util.HashMap;

public class ASTContractDefinition extends ASTNode implements Comparable {
    private ASTUserDefinedTypeName type;
    private Definitions.ContractKind contractKind = null;

    private ArrayList<ASTInheritanceSpecifier> inherits = null;
    //private ArrayList<ASTNode> inheritedBy = null;
    private ASTFunctionDefinition constructor = null;
    private ArrayList<ASTFunctionDefinition> functions = new ArrayList<ASTFunctionDefinition>();
    private HashMap<String, ASTFunctionDefinition> functionsByName = new HashMap<String, ASTFunctionDefinition>();
    private HashMap<String, ASTModifierDefinition> modifiersByName = new HashMap<String, ASTModifierDefinition>();
    //private ArrayList<ASTModifierDefinition> modifierDefinitions = new ArrayList<ASTModifierDefinition>();
    private HashMap<String, ASTModifierDefinition> modifierDefinitions = new HashMap<String, ASTModifierDefinition>();
    private ArrayList<ASTNode> variables = new ArrayList<ASTNode>();
    private HashMap<String, ASTNode> variablesByName = new HashMap<String, ASTNode>();

    private HashMap<String, ASTStructDefinition> structDefinitionsByName = new HashMap<String, ASTStructDefinition>();
    // Canonical struct references are shared across all contracts since they can be referenced from anywhere
    static private HashMap<String, ASTStructDefinition> structDefinitionsByCanoncialName = new HashMap<String, ASTStructDefinition>();

    private HashMap<String, ASTEnumDefinition> enumDefinitionsByName = new HashMap<String, ASTEnumDefinition>();
    private HashMap<String, ASTEnumDefinition> enumDefinitionsByCanonicalName = new HashMap<String, ASTEnumDefinition>();

    private HashMap<String, ASTEventDefinition> eventDefinitionsByName = new HashMap<String, ASTEventDefinition>();

    private ArrayList<ASTContractDefinition> inheritedBy = null;

    // Structure references that require helper variable generation
   // private HashMap<ASTStructDefinition, Boolean> structAccessMap = new HashMap<ASTStructDefinition, Boolean>();
    private ArrayList<ASTIndexAccess> indexAccessList = new ArrayList<ASTIndexAccess>();

    private ASTFunctionDefinition outroFunction = null;

    private boolean containsRenamedRevertReferences = false;
    private boolean containsRenamedAssertReferences = false;
    private boolean containsRenamedRequireReferences = false;
    private boolean containsRenamedNowReferences = false;
    private boolean containsRenamedThrows = false;

    public void setContainsRenamedRevertReferences(boolean value) { containsRenamedRevertReferences = value; }
    public void setContainsRenamedAssertReferences(boolean value) { containsRenamedAssertReferences = value; }
    public void setContainsRenamedRequireReferences(boolean value) { containsRenamedRequireReferences = value; }
    public void setContainsRenamedNowReferences(boolean value) { containsRenamedNowReferences = value; }
    public void setContainsRenamedThrows(boolean value) { containsRenamedThrows = value; }


    public boolean getContainsRenamedRevertReferences() { return containsRenamedRevertReferences; }
    public boolean getContainsRenamedAssertReferences() { return containsRenamedAssertReferences; }
    public boolean getContainsRenamedRequireReferences() { return containsRenamedRequireReferences; }
    public boolean getContainsRenamedNowReferences() { return containsRenamedNowReferences; }
    public boolean getContainsRenamedThrows() { return containsRenamedThrows; }


    public void setOutroFunction(ASTFunctionDefinition outroFunction) { this.outroFunction = outroFunction; }
    public ASTFunctionDefinition getOutroFunction() { return outroFunction; }


    public ASTContractDefinition(long id, String name, String contractKindName) throws Exception {
        super(id, name);
        contractKind = Definitions.ContractKind.fromString(contractKindName);
        if (contractKind == null) {
            throw new Exception("Unknown contract kind " + contractKindName);
        }
    }

    public Definitions.ContractKind getContractKind() { return contractKind; }

    // Obtain AST node count size metric for all contained functions, except for inherited-from contracts
    // and modifiers (TODO include them?)
    public int getFunctionBodyNodeCount() {
        int total = 0;
        for (ASTFunctionDefinition function : functions) {
            total += function.getBodyNodeCount();
        }
        return total;
    }

    public int compareTo(Object other) {
        ASTContractDefinition otherContract = (ASTContractDefinition)other;
        int ownBodyNodeCount = getFunctionBodyNodeCount();   // could be cached but realistcally shouldn't matter
        int otherBodyNodeCount = otherContract.getFunctionBodyNodeCount();
        if (ownBodyNodeCount > otherBodyNodeCount) {
            return 1;
        } else if (ownBodyNodeCount == otherBodyNodeCount) {
            return 0;
        } else {
            return -1;
        }
    }

    public ArrayList<ASTInheritanceSpecifier> getInherits() { return inherits; }
    public ASTFunctionDefinition getConstructor() { return constructor; }
    public ArrayList<ASTFunctionDefinition> getFunctions() { return functions; }
    public ASTFunctionDefinition getFunction(String name) { return functionsByName.get(name); }
    public ASTModifierDefinition getModifier(String name) { return modifiersByName.get(name); }

    //public ArrayList<ASTModifierDefinition> getModifierDefinitions() { return modifierDefinitions; }

    // TODO Refactoring: generic getEnumDefinition/getStructDefinition replacement on UserDefinedType
    public ASTEnumDefinition getEnumDefinition(String name) {
        ASTEnumDefinition result = enumDefinitionsByName.get(name);

        if (result == null) {
            result = enumDefinitionsByCanonicalName.get(name);
        }

        if (result == null && inherits != null) {
            // Try base contract(s)
            for (ASTInheritanceSpecifier baseContract : inherits) {
                result = baseContract.getContract().getEnumDefinition(name);
                if (result != null) {
                    // Found
                    break;
                }
            }
        }
        return result;
    }

    public ASTStructDefinition getStructDefinition(String name) {
        ASTStructDefinition result = structDefinitionsByName.get(name);

        if (result == null) {
            result = structDefinitionsByCanoncialName.get(name);
        }

        if (result == null && inherits != null) {
            // Try base contract(s)
            for (ASTInheritanceSpecifier baseContract : inherits) {
                result = baseContract.getContract().getStructDefinition(name);
                if (result != null) {
                    // Found
                    break;
                }
            }
        }
        return result;
    }

    public ASTModifierDefinition getModifierDefinition(String name) {
        ASTModifierDefinition result = modifierDefinitions.get(name);

        if (result == null && inherits != null) {
            // Try base contract(s)
            for (ASTInheritanceSpecifier baseContract : inherits) {
                result = baseContract.getContract().getModifierDefinition(name);
                if (result != null) {
                    // Found
                    break;
                }
            }
        }
        return result;
    }

    public ASTEventDefinition getEventDefinition(String name) {
        ASTEventDefinition result = eventDefinitionsByName.get(name);

        if (result == null && inherits != null) {
            // Try base contract(s)
            for (ASTInheritanceSpecifier baseContract : inherits) {
                result = baseContract.getContract().getEventDefinition(name);
                if (result != null) {
                    // Found
                    break;
                }
            }
        }
        return result;
    }

    public ArrayList<ASTNode> getVariables() { return variables; }

    public ASTVariableDeclaration lookupVariableDeclaration(String name) {
        ASTNode result = variablesByName.get(name);
        if (result == null) {
            return null;
        }
        return result instanceof ASTVariableDeclarationStatement?
                ((ASTVariableDeclarationStatement)result).getDeclaration():
                (ASTVariableDeclaration)result;
    }

    public ASTNode lookupVariableOrFunctionDeclaration(String name) {
        ASTNode result = lookupVariableDeclaration(name);
        if (result != null) {
            return result;
        }
        result = getFunction(name);
        return result;
    }

    public void addInheritedBy(ASTContractDefinition contract) {
        if (inheritedBy == null) {
            inheritedBy = new ArrayList<ASTContractDefinition>();
        }
        inheritedBy.add(contract);
    }
    public int getInheritedByCount() { return inheritedBy == null? 0: inheritedBy.size(); }
    public ASTContractDefinition getInheritedByContract(int index) { return inheritedBy.get(index); }

    public ArrayList<ASTIndexAccess> getIndexAccessList() {
        return indexAccessList;
    }

    public void addIndexAccess(ASTIndexAccess indexAccess) {
        indexAccessList.add(indexAccess);
    }

    public ASTUserDefinedTypeName getType() {
        return type;
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = contractKind.toString() + " " + name;
        if (inherits != null) {
            code += " is ";
            for (int i = 0; i < inherits.size(); ++i) {
                if (i > 0) {
                    code += ", ";
                }
                code += inherits.get(i).toSolidityCode();
            }
        }
        code += " {";
        return code;
    }

    // Can be used to add a struct definition while generating contracts - bypassing child node + finalize() structures
    // (which will however be called eventually and must avoid duplicated entries)
    public void addStructDefinition(ASTStructDefinition definition) {
        if (!structDefinitionsByName.containsKey(definition.getName())) {
            structDefinitionsByName.put(definition.getName(), definition);
            structDefinitionsByCanoncialName.put(definition.getCanonicalName(), definition);
        }
    }

    @Override
    public String toSolidityCodePostfix() { return "}"; }

    @Override
    public void finalize() throws Exception {
        for (int i = 0; i < getChildCount(); ++i) {
            if (getChild(i) instanceof ASTInheritanceSpecifier) {
                if (inherits == null) {
                    inherits = new ArrayList<ASTInheritanceSpecifier>();
                }
                inherits.add((ASTInheritanceSpecifier)getChild(i));


                removeChildNode(i);
                --i;
            } else if (getChild(i) instanceof ASTFunctionDefinition) {
                ASTFunctionDefinition functionDefinition = (ASTFunctionDefinition)getChild(i);
                functions.add(functionDefinition);
                functionsByName.put(functionDefinition.getName(), functionDefinition);
                if (functionDefinition.isConstructor()) {
                    if (constructor != null) {
                        throw new Exception("Multiple constructors defined for " + getName());
                    }
                    constructor = functionDefinition;
                }
            } else if (getChild(i) instanceof ASTVariableDeclaration
                    || getChild(i) instanceof ASTVariableDeclarationStatement) {
                variables.add(getChild(i));
                variablesByName.put(getChild(i).getName(), getChild(i));
            } else if (getChild(i) instanceof ASTModifierDefinition) {
                ASTModifierDefinition definition = (ASTModifierDefinition)getChild(i);
                modifierDefinitions.put(definition.getName(), definition);
                modifiersByName.put(definition.getName(), definition);
            } else if (getChild(i) instanceof ASTStructDefinition) {
                ASTStructDefinition definition = (ASTStructDefinition)getChild(i);
                addStructDefinition(definition);
            } else if (getChild(i) instanceof ASTEnumDefinition) {
                ASTEnumDefinition definition = (ASTEnumDefinition)getChild(i);
                enumDefinitionsByName.put(definition.getName(), definition);
                enumDefinitionsByCanonicalName.put(definition.getCanonicalName(), definition);
            } else if (getChild(i) instanceof  ASTEventDefinition) {
                ASTEventDefinition definition = (ASTEventDefinition)getChild(i);
                eventDefinitionsByName.put(definition.getName(), definition);
            }
        }

        type = TypeContainer.getUserDefinedType(ast, getName());
    }
}
