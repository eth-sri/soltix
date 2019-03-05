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

import soltix.ast.AST;
import soltix.ast.ASTContractDefinition;
import soltix.ast.ASTFunctionDefinition;
import soltix.ast.ASTVariableDeclaration;
import soltix.interpretation.values.Value;
import soltix.util.JSONValueConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Class to represent a transaction and load/store it in JSON
 */
public class Transaction {
    private ASTContractDefinition contract;
    private ASTFunctionDefinition function;
    private ArrayList<Value> arguments;

    // Constructor to build - and later write as JSON - a transaction
    public Transaction(ASTContractDefinition contract, ASTFunctionDefinition function) {
        this.contract = contract;
        this.function = function;
        arguments = new ArrayList<Value>();
    }

    public ASTContractDefinition getContract() { return contract; }
    public ASTFunctionDefinition getFunction() { return function; }
    public ArrayList<Value> getArguments() { return arguments; }

    // Constructor to load a transaction from a JSON object
    public Transaction(AST ast, JSONObject jsonObject) throws Exception {
        fromJSONObject(ast, jsonObject);
    }

    public void addArgumentValue(Value value) {
        arguments.add(value);
    }

    public JSONObject toJSONObject() throws Exception {
        JSONObject result = new JSONObject();

        result.put("contract", contract.getName());
        result.put("function", function.getName());

        JSONArray jsonArgumentArray = new JSONArray();
        for (Value argumentValue : arguments) {
            jsonArgumentArray.add(JSONValueConverter.objsoltixromValue(argumentValue));
        }
        result.put("args", jsonArgumentArray);
        return result;
    }

    public void fromJSONObject(AST ast, JSONObject jsonObject) throws Exception {

        String contractName = (String)jsonObject.get("contract");
        String functionName = (String)jsonObject.get("function");

        contract = ast.getContract(contractName);
        function = contract.getFunction(functionName);

        JSONArray jsonArgumentArray = (JSONArray)jsonObject.get("args");

        // Load argument values in accordance with declared function parameters
        ArrayList<ASTVariableDeclaration> declaredParameters = function.getParameterList().toArrayList();
        for (int i = 0; i < jsonArgumentArray.size(); ++i) {
            Object item = (Object)jsonArgumentArray.get(i);
            Value value = JSONValueConverter.valueFromObject(declaredParameters.get(i), item);
            arguments.add(value);
        }
    }
}
