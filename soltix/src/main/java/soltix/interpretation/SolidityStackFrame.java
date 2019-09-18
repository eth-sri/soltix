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
import soltix.interpretation.values.Value;
import soltix.interpretation.variables.VariableEnvironment;

import java.util.ArrayList;

public class SolidityStackFrame {
    private ASTContractDefinition contract;
    private ASTFunctionDefinition function;
    private ArrayList<Value> arguments;
    private Scope scope;
    private Value returnValue;

    public SolidityStackFrame(ASTContractDefinition contract,
                              ASTFunctionDefinition function,
                              ArrayList<Value> arguments,
                              AST ast/*,
                              VariableEnvironment localVariableEnvironment*/) throws Exception {
        this.contract = contract;
        this.function = function;
        this.arguments = arguments;
        scope = new Scope(contract);
        VariableEnvironment localVariableEnvironment = new VariableEnvironment(ast, true);
        scope.setVariableEnvironment(localVariableEnvironment);
    }

    public ASTContractDefinition getContract() { return contract; }

    public Scope getScope() { return scope; }

    public void setReturnValue(Value value) { returnValue = value; }
    public Value getReturnValue() { return returnValue; }
}
