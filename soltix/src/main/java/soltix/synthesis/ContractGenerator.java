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

package soltix.synthesis;

import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.interpretation.variables.VariableValues;
import soltix.util.RandomNumbers;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/*
 * Class to generate contracts (of the configured type)
 */
public class ContractGenerator {
    private AST ast;
    private AST transactionAST;
    private JSONObject transactionsJSONObject;
    private RandomNumbers prng;
    private int functionCount;
    private int variableCount;
    private int currentContractNumber = 0;
    private int currentVariableNumber = 0;
    private int currentFunctionNumber = 0;
    // Generation of contract components
    private StructTypeGenerator structTypeGenerator;
    private VariableGenerator variableGenerator;
    private ExpressionGenerator expressionGenerator;
    private FunctionGenerator functionGenerator;
    // Generation of transactions
    private TransactionGenerator transactionGenerator;
    // Continously single-value-set variable environment
    private VariableEnvironment environment;

    public TransactionGenerator getTransactionGenerator() { return transactionGenerator; }

    public ContractGenerator(AST ast, AST transactionAST, JSONObject transactionsJSONObject, RandomNumbers prng) throws Exception {
        this.ast = ast;
        this.transactionAST = transactionAST;
        this.transactionsJSONObject = transactionsJSONObject;
        this.prng = prng;

        // For assignment statements between 0 to 100 and 10 variables, this already breaks at 20
        functionCount = Configuration.contractGenerationFunctionCount;

        // For functions = 10, assignment statements between 0 to 100, 30 variables is close to the limit before
        // the framework fails
        variableCount = Configuration.contractGenerationVariableCount;

        structTypeGenerator = new StructTypeGenerator(prng); // TODO fix awkward circular StructTypeGenerator-VariableGenerator dependency
        variableGenerator = new VariableGenerator(ast, prng, structTypeGenerator);
        structTypeGenerator.setVariableGenerator(variableGenerator);
        functionGenerator = new FunctionGenerator(ast, prng, variableGenerator, structTypeGenerator);
        transactionGenerator = new TransactionGenerator(prng);
    }

    private String createContractName() {
        return "c" + currentContractNumber++;
    }
    private String createVariableName() {
        return "v" + currentVariableNumber++;
    }
    private String createFunctionName() { return "f" + currentFunctionNumber++; }

    public void run() throws Exception {
        ASTContractDefinition contractDefinition = new ASTContractDefinition(0, createContractName(), "contract");

        ast.addInnerNode(contractDefinition);

        // Prepare variable environment, which will hold a single, continuously updated value set while synthesizing
        // expressions
        environment = new VariableEnvironment(ast,true);

        // Generate storage variables
        for (int i = 0; i < variableCount; ++i) {
            String name = createVariableName();

            // Generate variable of random type - which is also generated and added to the contract on demand
            // for user-defined (struct) types
            ASTVariableDeclaration variableDeclaration = variableGenerator.generateRandomVariable(contractDefinition, name, true, true, false);

            contractDefinition.addChildNode(variableDeclaration);

            Variable variable  = new Variable(variableDeclaration);
            VariableValues variableValues = new VariableValues(variable, 0);

            // Start out with initializer value
            variableValues.addValue(variableDeclaration.getInitializerValue());
            environment.addVariableValues(variable, variableValues);
        }

        environment.printLatestToLogger();

        // Generate error event
        ASTVerbatimText eventNode = new ASTVerbatimText(0, "event EXPR_ERROR(uint arg);");
        eventNode.finalize();
        contractDefinition.addChildNode(eventNode);

        // Event for runtime tautology checking - probably obsolete now that we can evaluate constant
        // expressions at compile time
        // Disabled for now, since this is too wasteful to be enabled and also causes clashes with
        // the other inserted TAUTOLOGY_ERROR when processing generated contracts for Live EMI
        /*ASTVerbatimText tautologyErrorNode = new ASTVerbatimText(0, "event TAUTOLOGY_ERROR(uint statementId);");
        tautologyErrorNode.finalize();
        contractDefinition.addChildNode(tautologyErrorNode);*/

        // Generate functions
        for (int i = 0; i < functionCount; ++i) {
            String name = createFunctionName();
            // Generate new function definition (and possibly new struct types for arguments as a side effect)
            ASTFunctionDefinition functionDefinition = functionGenerator.generateTransactableFunctionSignature(contractDefinition, name);

            contractDefinition.addChildNode(functionDefinition);

            transactionGenerator.generateTruffleTransaction(ast /* to look up types */, transactionAST,
                transactionsJSONObject,
                contractDefinition, functionDefinition,
                    ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL);

            // Add all parameters to the variable environment. They start out with the known initial
            // value generated by TransactioGenerator and passed via getLastTransactionValue().
            // TODO For complete contracts that always computed valid values regardless of the input
            // (e.g. avoiding things like div-by-zero using the conditional operator), we could now
            // generate many additional transactions, probably discarding/ignoring getLastTransactionValue()
            // since these contracts are not interpreted anyway.
            ASTParameterList parameterList = functionDefinition.getParameterList();
            ArrayList<ASTVariableDeclaration> parameterVariablesList = parameterList.toArrayList();
            for (ASTVariableDeclaration parameter : parameterVariablesList) {
                Variable parameterVariable = new Variable(parameter);
                parameter.setVariable(parameterVariable);
                VariableValues variableValues = new VariableValues(parameterVariable, 0);

                variableValues.addValue(parameter.getLastTransactionValue());
                variableValues.finishAddingValues();

                environment.addVariableValues(parameterVariable, variableValues);
            }

            // Generate function body
            FunctionGenerator.FunctionType functionType = Configuration.generateFullContractFunctionType; //FunctionGenerator.FunctionType.FUNCTION_TYPE_RANDOM    FunctionGenerator.FunctionType.FUNCTION_TYPE_ASSIGNMENT_SEQUENCE;
            functionGenerator.generateFunctionBody(environment, contractDefinition, functionDefinition, functionType);

            // Remove generated function's parameters from environment
            for (ASTVariableDeclaration parameter : parameterVariablesList) {
                environment.removeVariableValues(parameter.getVariable());
            }
        }

        contractDefinition.finalize();
    }
}
