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

package soltix.interpretation;

import soltix.ast.*;
import soltix.interpretation.values.Value;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.util.JSONValueConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Class to represent a transaction and load/store it in JSON
 */
public class Transaction {
    private boolean isConstruction;
    private ASTContractDefinition contract;
    private Variable contractVariable;
    private String contractObjectName;
    private ASTFunctionDefinition function;
    private ArrayList<Value> arguments = new ArrayList<Value>();


    // Constructor to build - and later write as JSON - a transaction
    public Transaction(boolean isConstruction, ASTContractDefinition contract, String contractObjectName, ASTFunctionDefinition function) {
        this.isConstruction = isConstruction;
        this.contract = contract;
        this.contractObjectName = contractObjectName;
        this.function = function;
    }
    // Constructor to load a transaction from a JSON object
    public Transaction(AST ast, JSONObject jsonObject, VariableEnvironment environment) throws Exception {
        fromJSONObject(ast, jsonObject, environment);
    }

    //public ASTContractDefinition getContract() { return contract; }
    public ASTContractDefinition getContract() { return contract; }
    public Variable getContractVariable() { return contractVariable; }
    public String getContractObjectName() { return contractObjectName; }
    public ASTFunctionDefinition getFunction() { return function; }
    public ArrayList<Value> getArguments() { return arguments; }

    public void addArgumentValue(Value value) {
        arguments.add(value);
    }

    public JSONObject toJSONObject() throws Exception {
        JSONObject result = new JSONObject();

        if (isConstruction) {
            result.put("construction", contractObjectName);
            result.put("type", contract.getName());
        } else {
            result.put("call", contractObjectName);
            result.put("function", function.getName());
        }

        JSONArray jsonArgumentArray = new JSONArray();
        for (Value argumentValue : arguments) {
            jsonArgumentArray.add(JSONValueConverter.objectFromValue(argumentValue));
        }
        result.put("args", jsonArgumentArray);
        return result;
    }

    public void fromJSONObject(AST ast, JSONObject jsonObject, VariableEnvironment environment) throws Exception {
        if (jsonObject.get("construction") != null) {
            // Contract object construction / deployment case
            contractObjectName = (String)jsonObject.get("construction");

            String contractName = (String)jsonObject.get("type");
            if (contractName == null) {
                throw new Exception("Transaction.fromJSONObject: cannot find 'type' field to go wih 'construction' field");
            }
            contract = ast.getContract(contractName);
            if (contract == null) {
                throw new Exception("Transaction.fromJSONObject: reference to undefined contract " + contractName);
            }
            function = contract.getConstructor(); // may be null
        } else {
            // Transaction / function call case
            contractObjectName = (String)jsonObject.get("call");
            String functionName = (String)jsonObject.get("function");
            contractVariable = environment.getVariableIncludingParentEnvironments(contractObjectName);
            if (contractVariable == null) {
                throw new Exception("Transaction.fromJSONObject: reference to undefined object "
                        + contractObjectName + " - need preceding construction entry");
            }
            if (!(contractVariable.getType() instanceof ASTUserDefinedTypeName)) {
                throw new Exception("Transaction.fromJSONObject: call to non-contract type object " + contractVariable.getName()
                    + " - is of type " + contractVariable.getType().toSolidityCode());
            } else {
                ASTUserDefinedTypeName userDefinedTypeName = (ASTUserDefinedTypeName)contractVariable.getType();
                contract = ast.getContract(userDefinedTypeName.getName());
                if (contract == null) {
                    throw new Exception("Transaction.fromJSONObject: reference to undefined contract "
                            + userDefinedTypeName.getName());
                }
            }
            function = contract.getFunction(functionName);
            if (function == null) {
                throw new Exception("Transaction.fromJSONObject: reference to undefined function " + functionName);
            }
        }


        JSONArray jsonArgumentArray = (JSONArray)jsonObject.get("args");

        if (isConstruction && function == null) {
            if (jsonArgumentArray.size() != 0) {
                throw new Exception("Transaction.fromJSONObject: construction with arguments, but no constructor exists");
            }
        } else {
            // Load argument values in accordance with declared function parameters

            ArrayList<ASTVariableDeclaration> declaredParameters = function.getParameterList().toArrayList();
            if (declaredParameters.size() != jsonArgumentArray.size()) {
                throw new Exception("Transaction.fromJSONObject: function '" + function.getName() + "' has "
                    + declaredParameters.size() + " declared parameters, but received " + jsonArgumentArray.size()
                    + " arguments");
            }
            for (int i = 0; i < jsonArgumentArray.size(); ++i) {
                Object item = (Object) jsonArgumentArray.get(i);
                ASTVariableDeclaration parameterDeclaration = (ASTVariableDeclaration) declaredParameters.get(i);
                Value value = JSONValueConverter.valueFromObject(parameterDeclaration.getTypeName(), item);

                arguments.add(value);
            }
        }
    }
}
