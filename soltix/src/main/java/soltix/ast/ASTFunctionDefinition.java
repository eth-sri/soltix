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

import soltix.Configuration;
import soltix.synthesis.FunctionGenerator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class ASTFunctionDefinition extends FunctionScope {
    private boolean isConstructor = false;
    private boolean isConstant = false;
    Definitions.Visibility visibility = null;
    private String stateMutability = null;
    //ASTParameterList parameterList = null;
    private ArrayList<ASTModifierInvocation> modifierInvocations = null;
    private SpecialFunction specialFunction = null;

    public enum SpecialFunction {
        SPECIAL_FUNCTION_ANNOTATE("__annotate");

        private String name;

        SpecialFunction(String name) {
            this.name = name;
        }
        public String getName() { return name; }

        static public SpecialFunction fromString(String name) {
            for (SpecialFunction value : SpecialFunction.values()) {
                if (value.getName().equals(name)) {
                    return value;
                }
            }
            return null;
        }
    }

    public ASTFunctionDefinition(long id, String name, String visibility, String stateMutability, boolean isConstructor, boolean isConstant) throws Exception {
        super(id, name);
        this.visibility = Definitions.Visibility.fromString(visibility);
        if (this.visibility == null) {
            throw new Exception("Unknown visibility " + visibility);
        }

	
        if (name.startsWith(FunctionGenerator.returnFunctionNamePrefix)) {
            // Avoid name clashes with subsequently generated return functions
            int returnFunctionID = Integer.parseInt(name.substring(FunctionGenerator.returnFunctionNamePrefix.length()));
            if (returnFunctionID >= FunctionGenerator.returnFunctionCounter) {
                FunctionGenerator.returnFunctionCounter = returnFunctionID+1;
            }
        }
        // TODO Mutability enum
        this.stateMutability = stateMutability;
        this.isConstructor = isConstructor;
        this.isConstant = isConstant;
        this.specialFunction = SpecialFunction.fromString(name);
    }

    public SpecialFunction isSpecialFunction() { return specialFunction; }

    public Definitions.Visibility getVisibility() { return visibility; }

    public boolean isConstructor() { return isConstructor; }
    public ArrayList<ASTModifierInvocation> getModifiers() { return modifierInvocations; }
    public ASTContractDefinition getContract() { return (ASTContractDefinition)getParent(); }

    @Override
    public String toSolidityCode() throws Exception {
        String code;

        // We remain faithful to the legacy ("function <Contract-Name>()") and new ("constructor()")
        // constructor definitions to avoid false positives when checking our AST parsing correctness.
        // The legacy way to write it is identified by a non-empty name field.
        if (isConstructor && name.equals("")) {
            code = "constructor(";
        } else {
            if (isConstructor) {
                // Retrieve name from parent contract node to support contract renaming
                assert parent instanceof ASTContractDefinition;
                name = parent.getName();
            }
            code = "function " + name + "(";
        }
        code += parameters.toSolidityCode() + ") " + visibility;

        // Note: If stateMutability = pure/view, then the "constant" flag is overridden/ignored.
        if (stateMutability == null) {
            ;
        } else if (stateMutability.equals("pure")) {
            if (Configuration.profiling) {
                // We ignore "pure" function declarations during profiling (thereby defaulting to the "nonpayable"
                // default mutability), because events we need for profiling may not be emitted from pure
                // functions
                ;
            } else if (Configuration.renameExceptionalEvents /*getContract().getContainsRenamedAssertReferences()
                    || getContract().getContainsRenamedRequireReferences()
                    || getContract().getContainsRenamedRevertReferences()*/) {
                // The parent contract contains renamed exceptional statements. To avoid mutability
                // errors caused by calling our rerouted exception handlers that may emit statements,
                // we ignore the mutability.
                //
                // Update: Pure functions are now globally disabled if renaming is used - same as with
                // profiling - since naive selective mutability changes introduce incomopatibilities
                // in the inheritance hierarchy.
                // TODO: Fix this - optimizations are probably also affected by purity, so keeping some is desirable
                ;
            } else {
                code += " pure";
            }
        } else if (stateMutability.equals("view")) {
            // "view" functions pose similar problems as "pure" functions. Event emissions are apparently permitted
            // here (with a solc warning), but the truffle framework only seems to return the return value for a
            // view function in our current (as of August 16) truffle event collection approach, e.g.
            //      "0"
            // rather than an object that also contains the event information, e.g.
            //      {"tx":"..","receipt":{...},"logs":[...]}
            // The old approach with allEvents().get() does not have this problem if it works, but cannot be made to
            // produce any data in the test VM, so it must be version-dependent.
            //
            // For now, we drop the view keyword.
            //
            // TODO view would still be interesting to test, since it could affect optimizer decisions - maybe do it manually.
            // TODO curiously, as per
            //    https://ethereum.stackexchange.com/questions/25200/solidity-what-is-the-difference-between-view-and-constant
            // it's acknowledged that "constant" isn't fully enforced, but tests seem to indicate that for "view" even plain
            // assignments as in
            //     uint x; function f() public view { x = 123; }
            // are accepted by solc, and generate an executable SSTORE instruction for x.
            // TODO Check whether the optimizer relies on any view/constant properties at all
            if (Configuration.dropViewMutability) {
                ;
            } else {
                code += " view";
            }
        } else if (isConstant) {
            code += " constant";
        }
        // The state mutability attribute appears to be a pre-defined modifier invocation which is not specified
        // as explicit JSON sub-item
        if (stateMutability != null && stateMutability.equals("payable")) { // "nonpayable" yields compiler errors
            code += " " + stateMutability;
        }
        // Explicit modifier invocations
        if (modifierInvocations != null) {
            for (int i = 0; i < modifierInvocations.size(); ++i) {
                code += " " + modifierInvocations.get(i).toSolidityCode();
            }
        }
        if (returnList != null) {
            code += " returns(" + returnList.toSolidityCode() + ")";
        }
        return code;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }

    public void finalize() throws Exception {
        if (getChildCount() < 2) {
            throw new Exception("Unexpected function child count " + getChildCount());
        }
        if (!(getChild(0) instanceof ASTParameterList)
                || !(getChild(1) instanceof ASTParameterList)) {
            throw new Exception("Unexpected function definition child sequence: " +
                getChild(0).getClass().getName() + " " + getChild(1).getClass().getName());
        }

        // Remove parameter lists, keep block child node
        parameters = (ASTParameterList)getChild(0);
        returnList = (ASTParameterList)getChild(1);
        if (returnList.getChildCount() == 0) {
            // Empty return list - remove it
            returnList = null;
        }
        removeChildNode(0);
        removeChildNode(0);
        while (getChildCount() > 0 && getChild(0) instanceof ASTModifierInvocation) {
            if (modifierInvocations == null) {
                modifierInvocations = new ArrayList<ASTModifierInvocation>();
            }
            modifierInvocations.add((ASTModifierInvocation)getChild(0));
            removeChildNode(0);
        }

        if (getChildCount() > 0) {
            if (getChildCount() > 1) {
                throw new Exception("Unexpected function definition body");
            }
            if (!(getChild(0) instanceof ASTBlock)) {
                throw new Exception("Unexpected function definition without block body");
            }
            // Note: The child node is still kept for output
            body = (ASTBlock)getChild(0);
        }
    }
}
