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
package soltix.profiling;

import soltix.ast.*;
import soltix.interpretation.*;
import soltix.interpretation.variables.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to perform profiling instrumentations
 */
public class ProfilingLogInstrumentation {
    private AST ast;
    private ASTContractDefinition contract;
    private ProfilingEvent currentEvent = null;
    // List of all instrumentation events in the contract
    private ArrayList<ProfilingEvent> totalResultList = null;

    public ProfilingLogInstrumentation(AST ast, ASTContractDefinition contract) {
        this.ast = ast;
        this.contract = contract;
    }

    protected boolean includeVariable(Scope scope, Variable variable) throws Exception {
        // TODO This doesn't deal gracefully with tuples
        ASTVariableDeclaration declaration = variable.getDeclaration();

        /*
        // Exclude inner declarations that shadow outer ones. It may be desirable to
        // keep them and treat them normally in case the declaration precedes the current
        // context, but we have no indicator to distinguish between this case and the
        // case where the declaration follows later further down the function, and we
        // want to exclude at least that one due to unclear semantics
        if (variable.getShadowNode() != null
                || (!scope.getVariablesByName().containsKey(variable.getName())   // not encountered yet
                    && scope.currentFunctionContainsVariable(variable.getName()))) {  // ... but will be later
            return false;
        }*/

        // Above still not working, another attempt:
        //
        // We do not wish to reference variables "from the future" (declared below the current context),
        // even though the Solidity scoping rules allow this to some extent, because that leads to unclear
        // semantics and - more severely - compiler errors for event emissions referencing them.
        //
        // This is indicated as follows:
        //
        //    - The variable under consideration here is only based on the statements encountered in the current context
        //    - Case 1: The function's total local variable declaration list (collected during AST construction)
        //              contains no variable of this name -> No conflict, we keep the declaration
        //    - Case 2: A local variable declaration of the same name exists
        //            - Case 1: It has the same variable ID -> No conflict, the variable we already encountered is a local one
        //            - Case 2: It has a different ID -> There will later be a shadow declarattion, so we want to drop this variable
        ASTNode localDeclaration = scope.currentFunctionVariableLookup(variable.getName());
        if (localDeclaration != null && localDeclaration.getID() != declaration.getID()) {
            return false;
        }


        ASTNode type = declaration.getTypeName();

        // Perform struct exclusions, if necessary
        if (type instanceof ASTUserDefinedTypeName) {
            // Exclude non-"memory" storage structs, because they lead to internal
            // compiler errors:
            //
            // Unimplemented feature:
            // solidity/libsolidity/codegen/CompilerUtils.cpp(148): Throw in function void dev::solidity::CompilerUtils::storeInMemoryDynamic(const dev::solidity::Type&, bool)
            // Dynamic exception type: boost::exception_detail::clone_impl<dev::solidity::UnimplementedFeatureError>
            //        std::exception::what: Only in-memory reference type can be stored.
            //        [dev::tag_comment*] = Only in-memory reference type can be stored.
            if (declaration.getStorageLocation() != Definitions.StorageLocation.STORAGE_LOCATION_MEMORY) {
                return false;
            }

            // Exclude structs containing mappings
            ASTStructDefinition definition = contract.getStructDefinition(type.getName());
            if (definition == null) {
                throw new Exception("Reference to unknown struct type " + type.getName());
            }

            if (!definition.getHaveDeepContainsMapping()) {
                // Resolve "contains mapping" info recursively
                definition.resolveStructContainsMappingInfo(ast, scope.getContract());
            }

            if (definition.getContainsMapping()) {
                return false;
            }
        }

        // Exclude unnamed arguments
        if (variable.getName().equals("")) {
            return false;
        }

        // Exclude mappings, arrays, function types and type-less variables ("var" keyword) - keep the rest
        // TODO: Excluding arrays does not appear to be accurate, events do allow those
        // TODO: Structs with mappings (and probably function types) must also be excluded
        return type != null && !(type instanceof ASTMapping) && !(type instanceof ASTArrayTypeName) && !(type instanceof ASTFunctionTypeName);
    }

    public ArrayList<ProfilingEvent> constructEventList(ASTNode statement, Scope currentScope) throws Exception {
        HashMap<Long, Variable> variablesMap = currentScope.getVariablesById();
        ArrayList<ProfilingEvent> currentStatementResultList = new ArrayList<ProfilingEvent>();

        // We definitely want an event for coverage at this point, even if no variables of interest are defined
        currentEvent = new ProfilingEvent(contract.getName(), statement.getID(), 0);
        currentStatementResultList.add(currentEvent);
        if (totalResultList == null) {
            totalResultList = new ArrayList<ProfilingEvent>();
        }
        totalResultList.add(currentEvent);

        // Define all events that are needed to record the current scope's state
        for (Variable variable : variablesMap.values()) {
            if (!includeVariable(currentScope, variable)) {
                continue;
            }

            // Start new event if the last event is full
            //if (currentEvent.isFull()) {
            if (StackCalculator.isProfilingEventFull(currentScope.getCurrentFunction(), currentEvent)) {
                if (currentEvent.getArguments() == null) { //currentEvent.getArguments().size() == 0) {
                    // Including any argument at all would yield a stack overflow - just keep the existing plain event
                    currentEvent.setStackOverflow(true);
                    break;
                }
                currentEvent = new ProfilingEvent(contract.getName(), statement.getID(), currentEvent.getPartNumber()+1);
                currentStatementResultList.add(currentEvent);
                totalResultList.add(currentEvent);
            }
            currentEvent.addArgument(variable);
        }
        return currentStatementResultList;
    }

}
