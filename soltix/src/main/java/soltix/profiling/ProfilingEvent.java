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

import java.util.ArrayList;

/**
 * Class to represent a profiling event 
 */
public class ProfilingEvent {
    static final public String profilingEventPrefix = "Profiling_";

    private ASTContractDefinition contract = null;
    private String name;

    private boolean isOrdinaryUserEvent = false;

    // ID of statement to which this event belongs
    private long statementID;
    // Statement reference (if already available)
    private ASTNode statement = null;
    // Part number (to distinguish between multiple events for the same statement)
    private int partNumber;
    // Event arguments
    private ArrayList<Variable> arguments = null;
    private int stringArgumentCount = 0;

    // Indicates whether event arguments are incomplete due to a full stack
    private boolean stackOverflow = false;
    private int lastComputedStackSize = 0;

    public void setStackOverflow(boolean stackOverflow) { this.stackOverflow = stackOverflow; }
    public boolean getStackOverflow() { return stackOverflow; }
    public void setLastComputedStackSize(int lastComputedStackSize) { this.lastComputedStackSize = lastComputedStackSize; }

    public boolean getIsOrdinaryUserEvent() { return isOrdinaryUserEvent; }
    public ASTContractDefinition getContract() { return contract; }

    public ASTNode getStatement() { return statement; }

    // For the event-generating part:
    public ProfilingEvent(String contractName, long statementID, int partNumber) {
        this.statementID = statementID;
        this.partNumber = partNumber;
        /*this.name = "Profiling_" + contractName + "_" + statementID + "_" + partNumber;
        if (false) { // to debug stack size computation
            this.name += "_stacksize_" + lastComputedStackSize;
        }*/
        this.name = constructName(contractName, this.statementID, this.partNumber);
    }

    protected ProfilingEvent() {}

    protected String constructName(String contractName, long statementID, int partNumber) {
        String result = profilingEventPrefix + contractName + "_" + statementID + "_" + partNumber;
        if (false) { // to debug stack size computation
            result += "_stacksize_" + lastComputedStackSize;
        }
        return result;
    }

    static public ProfilingEvent fromJSON(AST ast, JSONObject jsonObject) throws Exception {
        String eventName = (String)jsonObject.get("event");
        ProfilingEvent result = new ProfilingEvent();

        if (eventName == null) { // These events sometimes occur for unknown reasons
            return null;
        }

        result.name = eventName;

        // Naming convention:
        // Profiling_<ContractName>_<StatementID>_<PartNumber>
        // Note that the contract name may also contain underscores
        if (eventName.startsWith(profilingEventPrefix)) {
            String[] parts = eventName.split("_");

            int expectedPartCount = 4;
            if (parts.length < expectedPartCount) { // not == due to possible underscores in contract names
                throw new Exception("Malformed profiling event with less than 3 components");
            }
            result.partNumber = Integer.parseInt(parts[parts.length - 1]);
            result.statementID = Integer.parseInt(parts[parts.length - 2]);
            result.statement = ast.getNodeById(result.statementID);

            String contractName = "";
            int contractParts = parts.length - expectedPartCount + 1;
            for (int i = 0; i < contractParts; ++i) {
                if (i > 0) {
                    contractName += "_";
                }
                contractName += parts[1+i];
            }

            ASTContractDefinition contract = ast.getContract(contractName);
            if (contract == null) {
                throw new Exception("Event log references non-existing contract " + contractName + ", extracted from event name: " + eventName);
            }
            result.contract = contract;

            JSONObject args = (JSONObject)jsonObject.get("args");
            if (args != null) {
                for (Object key : args.keySet()) {
                    Object value = (Object)args.get((String)key);

                    // Locate variable declaration to obtain type for value interpretation
                    VariableEnvironment environment = result.statement.getVariableEnvironment();
                    environment.storeVariableValue((String)key, value, result.statementID);
                }
            }
        } else {
            result.isOrdinaryUserEvent = true;
        }

        return result;
    }

    // For the event-reading part:
    public ProfilingEvent(String eventName) {
        this.name = eventName;
        //...
    }

    public String getName() { return name; }
    public long getStatementID() { return statementID; }
    public int getPartNumber() { return partNumber; }

    public void addArgument(Variable variable) {
        if (arguments == null) {
            arguments = new ArrayList<Variable>();
        }

        /*
        // TODO: We're still dealing with a Declaration vs DeclarationStatement dichotomy here - that should
        // probably be changed, but for now we distinguish here to avoid an exception
        ASTVariableDeclaration declaration;
        if (variable.getNode() instanceof ASTVariableDeclarationStatement) {
            // TODO tuples...
            declaration = ((ASTVariableDeclarationStatement)variable.getNode()).getDeclaration();
        } else {
            declaration = (ASTVariableDeclaration)variable.getNode();
        }*/
        ASTVariableDeclaration declaration = variable.getDeclaration();

        if (declaration.getTypeName() instanceof ASTElementaryTypeName && declaration.getTypeName().getName().equals("string")) {
            ++stringArgumentCount;
        }
        arguments.add(variable);
    }

    public ArrayList<Variable> getArguments() { return arguments; }
    public int getStringArgumentCount() { return stringArgumentCount; }
}
