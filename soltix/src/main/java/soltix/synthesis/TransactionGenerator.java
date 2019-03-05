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

import soltix.ast.*;
import soltix.interpretation.Transaction;
import soltix.interpretation.values.Value;
import soltix.util.JSONValueConverter;
import soltix.util.RandomNumbers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to generate constructor invocations and function transactions with randomized arguments.
 *
 * The output format is truffle-compatible AST code and the JSON format (for transactions only)
 */
public class TransactionGenerator {
    private ValueGenerator valueGenerator;

    public TransactionGenerator(RandomNumbers prng) {
        valueGenerator = new ValueGenerator(prng);
    }

    static protected boolean parameterListContainsContractArgument(AST ast, ASTParameterList parameterList) {
        for (ASTNode node : parameterList.getChildren()) {
            ASTVariableDeclaration argument = (ASTVariableDeclaration)node;
            if (argument.getTypeName() instanceof ASTUserDefinedTypeName
                    && ast.getContract(argument.getTypeName().getName()) != null) {
                return true;
            } else if (argument.getTypeName() instanceof ASTArrayTypeName) {
                ASTArrayTypeName array = (ASTArrayTypeName)argument.getTypeName();
                if (array.getBaseType() instanceof ASTUserDefinedTypeName
                        && ast.getContract(array.getBaseType().getName()) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    static public boolean isTransactableFunction(AST ast, ASTFunctionDefinition function) {
        // Don't call constructors or fallback function
        //
        // ... plus workaround for functions that take other contracts as constructor argument: Don't
        // transact with them for now.
        //
        // TODO fix this
        return !function.isConstructor()
                && function.getVisibility() == Definitions.Visibility.VISIBILITY_PUBLIC
                && !function.getName().equals("")
                && !parameterListContainsContractArgument(ast, function.getParameterList());
    }

    static protected boolean isTransactableContract(AST ast, ASTContractDefinition contract) {
        boolean haveTransactableFunction = false;
        boolean haveFunctionDeclarationWithoutBody = false;

        for (ASTFunctionDefinition function : contract.getFunctions()) {
            if (function.getBody() == null) {
                haveFunctionDeclarationWithoutBody = true;
            } else if (isTransactableFunction(ast, function)) {
                haveTransactableFunction = true;
            }
        }

        // Workaround for contracts that take other contracts as constructor argument: Don't
        // transact with them for now.
        //
        // TODO fix this
        // See test-env/test-env-truffle/udt-constructor-case for an example that is runnable,
        // but only logs events for contract b - not for contract a functions called by b
        ASTFunctionDefinition constructor = contract.getConstructor();
        boolean haveUninstantiableConstructor = false;
        if (constructor != null) {
            if (parameterListContainsContractArgument(ast, constructor.getParameterList())) {
                haveUninstantiableConstructor = true;
            }
        }

        // We need a function to transact with, and a function declaration without body apparently
        // renders the contract abstract (may not be instantiated), and we currently can't instantiate
        // some constructors
        return haveTransactableFunction && !haveFunctionDeclarationWithoutBody && !haveUninstantiableConstructor;
    }

    static public ArrayList<ASTContractDefinition> getTransactableContracts(AST ast) {
        ArrayList<ASTContractDefinition> result = null;

        for (ASTContractDefinition contract : ast.getContracts()) {
            if (TransactionGenerator.isTransactableContract(ast, contract)) {
                if (result == null) {
                    result = new ArrayList<ASTContractDefinition>();
                }
                result.add(contract);
            }
        }
        return result;
    }

    protected void generateFunctionCallArgumentList(AST contractAST,
                                                    ASTFunctionCall functionCall,
                                                    ASTParameterList parameterList,
                                                    ValueGenerator.IntegerGenerationPolicy integerGenerationPolicy,
                                                    boolean forJavaScript) throws Exception {

        for (ASTNode node : parameterList.getChildren()) {
            ASTVariableDeclaration declaration = (ASTVariableDeclaration)node;
            Value value = valueGenerator.generateRandomValue(contractAST, declaration.getTypeName(), integerGenerationPolicy); // TODO JS vs Solidity?

            declaration.setInitializerValue(value); // (technically not an "initializer" but an argument)
            ASTNode valueNode = value.toASTNode(forJavaScript);
            functionCall.addChildNode(valueNode);
        }
    }


    protected String generateFunctionCallArgumentList2(AST contractAST,
                                                       ASTParameterList parameterList,
                                                       ValueGenerator.IntegerGenerationPolicy integerGenerationPolicy,
                                                       Transaction transaction
                                                       /*JSONObject transactionJSONObject*/) throws Exception {
        String resultArgumentListString = "";
        //for (ASTNode node : parameterList.getChildren()) {

        //JSONArray jsonArgumentArray = new JSONArray();
        for (int i = 0; i < parameterList.getChildCount(); ++i) {
            ASTNode node = parameterList.getChild(i);
            ASTVariableDeclaration declaration = (ASTVariableDeclaration)node;
            Value value = valueGenerator.generateRandomValue(contractAST, declaration.getTypeName(), integerGenerationPolicy); // TODO JS vs Solidity?

            declaration.setLastTransactionValue(value);

            /*ASTNode valueNode = value.toASTNode(forJavaScript);
            functionCall.addChildNode(valueNode);*/
            if (i > 0) {
                resultArgumentListString += ", ";
            }
            resultArgumentListString += value.toASTNode(true).toSolidityCode();
            //jsonArgumentArray.add(JSONValueConverter.objsoltixromValue(value));
            transaction.addArgumentValue(value);
        }
        //transactionJSONObject.put("args", jsonArgumentArray);
        return resultArgumentListString;
    }


    // TODO Use ASTVerbatimText or make the complex construction operations generally reusable
    public void generateTruffleConstructorInvocation(AST contractAST,
                                                     AST constructorAST,
                                                     ASTContractDefinition contract,
                                                     ValueGenerator.IntegerGenerationPolicy integerGenerationPolicy) throws Exception {
        if (constructorAST.getRoot() != null) {
            throw new Exception("generateTruffleConstructorInvocation: Received non-empty AST");
        }

        // We need to generate a call of this form:
        //     deployer.deploy(<Contract>, <constructor-arg1>, ...);
        //
        // which translates to:
        //
        //                 ExpressionStatement
        //                          |
        //                    FunctionCall
        //                      /   |   \
        //          MemberAccess   Arg1  ...
        //                |
        //           Identifier
        //           (deployer)
        ASTExpressionStatement expressionStatement = new ASTExpressionStatement(0);
        constructorAST.addInnerNode(expressionStatement);

        ASTFunctionCall functionCall = new ASTFunctionCall(0, false, null);
        ASTMemberAccess memberAccess = new ASTMemberAccess(0, "deploy", 0);
        ASTIdentifier identifierDeployer = new ASTIdentifier(0, "deployer", 0);

        memberAccess.addChildNode(identifierDeployer);
        functionCall.addChildNode(memberAccess);

        // Add function call arguments
        ASTIdentifier identifierContractName = new ASTIdentifier(0, contract.getName(), 0);
        functionCall.addChildNode(identifierContractName);
        ASTFunctionDefinition constructor = contract.getConstructor();

        if (constructor != null) {
            generateFunctionCallArgumentList(contractAST, functionCall, constructor.getParameterList(), integerGenerationPolicy,true);
        }

        expressionStatement.addChildNode(functionCall);

        identifierContractName.finalize();
        identifierDeployer.finalize();
        memberAccess.finalize();
        functionCall.finalize();
        expressionStatement.finalize();
    }

    // TODO Use ASTVerbatimText or make the complex construction operations generally reusable
    // TODO Contract variables referenced by the function could be set to random values prior to running the function ("instance.var = value;" in truffle)
    // Generates a truffle-compatible transaction for the contract given by "contractAST" and "contract", on the function
    // given by "function", to "transactionAST".
    // The function argument values are stored to the ASTVariableDeclaration initializer field of the corresponding
    // function parameter list entry
    public void generateTruffleTransaction(AST contractAST,
                                           AST transactionAST,
                                           JSONObject allTransactionsJSONObject,
                                           ASTContractDefinition contract, // TODO: Use or remove this
                                           ASTFunctionDefinition function,
                                           ValueGenerator.IntegerGenerationPolicy integerGenerationPolicy) throws Exception {
        /*if (transactionAST.getRoot() != null) {
            throw new Exception("generateTruffleTransaction: Received non-empty AST");
        }*/

        // Transaction structure:
        //     logEvents(await instance.<function>(<arg1>, ...));

        //JSONObject currentTransactionJSONObject = new JSONObject();
        Transaction currentTransaction = new Transaction(contract, function);

        //currentTransactionJSONObject.put("contract", contract.getName());
        //currentTransactionJSONObject.put("function", function.getName());



        String generatedArgumentList = generateFunctionCallArgumentList2(contractAST, function.getParameterList(), integerGenerationPolicy,
                                                                        currentTransaction); //currentTransactionJSONObject);
        ASTNode callNode = new ASTVerbatimText(0, "logEvents(await instance." + function.getName() + "(" + generatedArgumentList + "));");
        transactionAST.addInnerNode(callNode);

        JSONArray transactionsArray =  allTransactionsJSONObject.containsKey("transactions")
                                            ? (JSONArray)allTransactionsJSONObject.get("transactions")
                                                : new JSONArray();
        transactionsArray.add(currentTransaction.toJSONObject()); //currentTransactionJSONObject);
        allTransactionsJSONObject.put("transactions", transactionsArray);
    }
}
