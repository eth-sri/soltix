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

import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.expressions.ExpressionBuilder;
import soltix.interpretation.expressions.ExpressionEvaluationErrorHandler;
import soltix.interpretation.expressions.ExpressionEvaluator;
import soltix.interpretation.values.*;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.interpretation.variables.VariableValues;
import soltix.output.Console;
import soltix.synthesis.ValueGenerator;
import soltix.util.JSONValueConverter;
import soltix.util.RandomNumbers;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Stack;

// Full interpretation of transactions applied to contract functions.
//
// Unlike other IInterpreterCallback implementations, we are passed control on a per-transaction rather than
// per-node basis (therer are some ugly distinct interfaces, but ASTInterpreter is still involved for now, since it does
// some generally useful things like modifier invocation to implementation resolutions)
public class FullInterpreter implements IInterpreterCallback {
    private AST ast;
    private ASTInterpreter astInterpreter;
    private ArrayList<Transaction> transactions;
    private ExpressionEvaluator expressionEvaluator;
    private ValueGenerator valueGenerator;

    private class BreakException extends Exception {
        public BreakException(String text) {
            super(text);
        }
    }
    private class ContinueException extends Exception {
        public ContinueException(String text) {
            super(text);
        }
    }
    private class ReturnException extends Exception {
        private Value returnValue;

        public ReturnException(String text, Value returnValue) {
            super(text);
            this.returnValue = returnValue;
        }

        public Value getReturnValue() { return returnValue; }
    }

    public FullInterpreter(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
        // TODO Supply proper error handler policy to ensure stability for preceding iterations
        expressionEvaluator = new ExpressionEvaluator(new ExpressionEvaluationErrorHandler(new RandomNumbers(Configuration.randomNumbersSeed)), this);
    }

    public void initialize(ASTInterpreter astInterpreter) {
        this.astInterpreter = astInterpreter;
        ast = astInterpreter.getAST();
        emittedEventsJSONObjectList = new ArrayList<JSONObject>();
        valueGenerator = new ValueGenerator(new RandomNumbers(Configuration.randomNumbersSeed));
    }

    public ValueGenerator getValueGenerator() {
        return valueGenerator;
    }

    public ASTInterpreter.NavigationPolicy getNavigationPolicy() {
        return ASTInterpreter.NavigationPolicy.NAVIGATION_POLICY_FULL_INTERPRETATION;
    }

    public ASTNode nextTargetStatement() throws Exception {
        throw new Exception("Invalid call to FullInterpreter.nextTargetStatement");
    }

    private ArrayList<JSONObject> emittedEventsJSONObjectList;

    //public void start(ASTContractDefinition contract, ASTFunctionDefinition function, JSONObject transaction);
    public void finish() throws Exception {
        // Write event results
        FileWriter file = new FileWriter(Configuration.interpretationOutputLogFile);

        // TODO this currently produces a single huge line, which should be pretty-printed (maybe just use a nodejs
        // script to clean up the produced file)
        for (JSONObject object : emittedEventsJSONObjectList) {
            file.write(object.toJSONString().replace("\\\\", "\\") + "\n"); // undo JSON \-escape
        }
        file.flush();
    }

    public void visitNodeBeforeProcessing(ASTNode node) throws Exception {
        throw new Exception("Invalid call to FullInterpreter.visitNodeBeforeProcessing");
    }
    public void visitNodeAfterProcessing(ASTNode node) throws Exception {
        throw new Exception("Invalid call to FullInterpreter.visitNodeBeforeProcessing");
    }


    private /*ASTContractDefinition*/ContractValue currentContractValueContext = null;

    public void run() throws Exception {
        //initializeContractEnvironment(transactions.get(0).getContract()); // TODO multiple contracts?
        createGlobalEnvironment(); // TODO multiple contracts?



        if (transactions.size() == 0) {
            throw new Exception("Empty transactions list");
        }

        /*
        // TODO unify with ContractValue
        ASTContractDefinition contractContext = transactions.get(0).getContract();
        currentContractValueContext = contractContext;
        if (!initializedGlobalEnvironment) {
            // Due to expression evaluation for initializers requiring a variable environment and potentially other
            // stack frame elements, we initialize storage variables at the first function call.
            // An unpleasant example that requires variable processing is:
            //    int x = 5;
            //    int y = x;
            initializeContractEnvironment(contractContext);
            initializedGlobalEnvironment = true;
        }*/
        currentContractValueContext = interpretNewExpression(transactions.get(0).getContract()/*TODO*/,
                                                        new ArrayList<Value>() /* TODO */);





        for (Transaction transaction : transactions) {
            Value result = interpretTransaction(transaction);
            // TODO use result
        }
        //globalEnvironment = null;
        globalInterpreterEnvironment = null;
    }


    private Stack<SolidityStackFrame> callStack = new Stack<SolidityStackFrame>();
    protected SolidityStackFrame currentStackFrame() { return callStack.empty()? null: callStack.peek(); }

    public Value interpretTransaction(Transaction transaction) throws Exception {

       // Value result = interpretNode();

        ASTFunctionDefinition calledFunction = transaction.getFunction();
        calledFunction.setInterpretationArguments(transaction.getArguments());
        //Value result = interpretFunctionCall(transaction.getFunction(), transaction.getArguments());
        Value result = interpretNode(transaction.getFunction());

                /*
        uninitializeLocalFunctionEnvironment(stackFrame, transaction.getFunction()); //transaction);
        callStack.pop();
        */

        return result;
    }



    private VariableEnvironment globalInterpreterEnvironment;
   // private boolean initializedGlobalEnvironment = false;


    protected void createGlobalEnvironment() throws Exception {
        // Prepare variable environment, which will hold a single, continuously updated value set while synthesizing
        // expressions
        globalInterpreterEnvironment = new VariableEnvironment(ast, true);
    }

    protected void initializeContractEnvironment(ContractValue contractValue, //VariableEnvironment environment,
                                                 ASTContractDefinition currentContract) throws Exception {
        // Generate storage variables
        for (ASTNode tmp : currentContract.getVariables()) {
            ASTVariableDeclaration variableDeclaration = (ASTVariableDeclaration)tmp;

            Variable variable  = new Variable(variableDeclaration);
            VariableValues variableValues = new VariableValues(variable, 0);

            // Start out with initializer value
            Value initializerValue = variableDeclaration.getInitializerValue();
            if (initializerValue == null) {
                ASTNode initializerCode = variableDeclaration.getInitializer();
                if (initializerCode != null) {
                    initializerValue = interpretNode(initializerCode);
                }

                if (initializerValue == null) {
                    // No initializer given - construct default (zero) value for this item, but only if it is not a
                    // contract type, because contract values are initialized to null
                    if (Type.isContractType(ast, variableDeclaration.getTypeName())) {
                        ;
                    } else {
                        initializerValue = ValueContainer.getDefaultStorageValue(valueGenerator, ast, variableDeclaration.getTypeName());
                    }
                }
            }

            variableValues.addValue(initializerValue);
            contractValue.getInterpretationEnvironment().addVariableValues(variable, variableValues);
        }

        // Include functions as well - treated as "variables" with a FunctionValue as implicit initializer
        for (ASTFunctionDefinition function : currentContract.getFunctions()) {
            Variable variable = new Variable(function);
            VariableValues variableValues = new VariableValues(variable, 0);
            FunctionValue value = new FunctionValue(currentContract, function, function.getFunctionType());
            variableValues.addValue(value);
            contractValue.getInterpretationEnvironment().addVariableValues(variable, variableValues);
        }

        // Add "this" variable TODO make it immutable, and function value variables too
        ASTVariableDeclaration thisDeclaration = new ASTVariableDeclaration(0, "this",
                "contract", "storage", "internal", true, false);
        thisDeclaration.addChildNode(new ASTUserDefinedTypeName(0, currentContract.getName()));
        thisDeclaration.finalize();
        Variable thisVariable = new Variable(thisDeclaration);
        VariableValues thisValues = new VariableValues(thisVariable, 0);
        thisValues.addValue(contractValue);
        contractValue.getInterpretationEnvironment().addVariableValues(thisVariable, thisValues);
    }

    protected void initializeLocalFunctionEnvironment(SolidityStackFrame stackFrame,
                                                      ASTFunctionDefinition functionDefinition,
                                                      ArrayList<Value> arguments) throws Exception {
        ArrayList<ASTVariableDeclaration> parameterList = functionDefinition.getParameterList().toArrayList();
        for (int i = 0; i < parameterList.size(); ++i) {
            ASTVariableDeclaration parameter = parameterList.get(i);
           // Variable variable = new Variable(parameter);
           // VariableValues variableValues = new VariableValues(variable, 0);

            // Initialize parameter with corresponding argument (no type conversion - assume correct type)
            // TODO this probably does require a conversion due to the lack of tx input format expressiveness
           // variableValues.addValue();
           // stackFrame.getScope().enterNode(parameter, transaction.getArguments().get(i));
            parameter.setInitializerValue(/*transaction.getArguments()*/ arguments.get(i));
        }
    }

    protected void uninitializeLocalFunctionEnvironment(SolidityStackFrame stackFrame,
                                                        ASTFunctionDefinition functionDefinition) throws Exception {
        ArrayList<ASTVariableDeclaration> parameterList = functionDefinition.getParameterList().toArrayList();
        for (int i = 0; i < parameterList.size(); ++i) {
            ASTVariableDeclaration parameter = parameterList.get(i);
            stackFrame.getScope().leaveNode(parameter);
        }
    }



    public ContractValue interpretNewExpression(ASTContractDefinition contractType,
                                                ArrayList<Value> arguments) throws Exception {
        ContractValue value = new ContractValue(contractType);

        currentContractValueContext = value; // assign here to resolve another circular reference when evaluating
                                             // initializers. TODO fix this, maybe pass proper context objects where needed

        VariableEnvironment environment = new VariableEnvironment(ast, true);
        //if (!initializedGlobalEnvironment) {
            // Due to expression evaluation for initializers requiring a variable environment and potentially other
            // stack frame elements, we initialize storage variables at the first function call.
            // An unpleasant example that requires variable processing is:
            //    int x = 5;
            //    int y = x;

        value.setInterpretationEnvironment(environment); // Again, already set this due to circular references in initializeContractEnvironment(). TODO Fix this



        // TODO remove all "global" things, associate them with ContractValues; call constructor with args
            initializeContractEnvironment(/*environment*/value, contractType);
        //    initializedGlobalEnvironment = true;
       // }

        // Call constructor, if any
        ASTFunctionDefinition constructor = contractType.getConstructor();
        if (constructor != null) {
            constructor.setInterpretationArguments(arguments);
            interpretNode(constructor);
        }
        return value;
    }



    public Value interpretNode(ASTNode currentNode) throws Exception {
        ast.setCurrentNode(currentNode);
        Scope currentScope = currentStackFrame() != null? currentStackFrame().getScope(): null;

        currentNode.setCovered(true);

        // Process the current item for the scope to e.g. introduce a declaration if it is a variable.
        // If this is a function, the step will be performed as part of the function interpretation because
        // it first needs to set up a stack frame with a new scope.
        // The first interpreted node must be a function definition to ensure that a stack + scope exists.
        if (!(currentNode instanceof ASTFunctionDefinition)) {
            if (currentScope != null) { // may still be null during storage initializer evaluation
                currentScope.enterNode(currentNode, null); // TODO initializer?
            }
        }

        Value returnValue = null;

        if (currentNode instanceof ASTFunctionDefinition) {
            ASTFunctionDefinition functionDefinition = (ASTFunctionDefinition) currentNode;
            returnValue = interpretFunctionCall(functionDefinition, functionDefinition.getInterpretationArguments());
        } else if (currentNode instanceof ASTBlock) {
            returnValue = interpretBlock((ASTBlock)currentNode);
        } else if (currentNode instanceof ASTVariableDeclarationStatement) {
            ; // This is already processed by currentScope.enterNode(). TODO move initializer processing here?
        } else if (currentNode instanceof ASTEmitStatement) {
            interpretEmitStatement((ASTEmitStatement) currentNode);
        } else if (currentNode instanceof ASTReturnStatement) {
            interpretReturnStatement((ASTReturnStatement) currentNode);
        } else if (currentNode instanceof  ASTIfStatement) {
            interpretIfStatement((ASTIfStatement) currentNode);
        } else if (currentNode instanceof ASTWhileStatement) {
            interpretWhileStatement((ASTWhileStatement) currentNode);
        } else if (currentNode instanceof ASTDoWhileStatement) {
            interpretDoWhileStatement((ASTDoWhileStatement) currentNode);
        } else if (currentNode instanceof ASTForStatement) {
            interpretForStatement((ASTForStatement) currentNode);
        } else if (currentNode instanceof ASTBreakStatement) {
            interpretBreakStatement((ASTBreakStatement) currentNode);
        } else if (currentNode instanceof ASTContinueStatement) {
            interpretContinueStatement((ASTContinueStatement) currentNode);
        } else if (currentNode instanceof ASTExpressionStatement) {
            interpretExpressionStatement((ASTExpressionStatement) currentNode);
        //} else if (currentNode instanceof ASTNewExpression) {
            //returnValue = interpretNewExpression((ASTNewExpression)currentNode);
        } else if (currentNode instanceof ASTLiteral
                || currentNode instanceof ASTFunctionCall
                || currentNode instanceof ASTBinaryOperation
                || currentNode instanceof ASTTupleExpression
                || currentNode instanceof ASTNewExpression) {
            returnValue = interpretExpression(currentNode);
        } else {
            throw new Exception("FullInterpreter.interpretNode for unimplemented node type "
                    + currentNode.getClass().toString()
                    + " for " + currentNode.toSolidityCode());
        }

        // TODO Restore position?
        //ast.setCurrentNode(currentNode);
        //currentScope.leaveNode(currentNode);
        return returnValue;
    }



    protected Value interpretFunctionCall(ASTFunctionDefinition functionDefinition,
                                          ArrayList<Value> arguments) throws Exception {


        // Set up stack frame
        SolidityStackFrame stackFrame = new SolidityStackFrame(functionDefinition.getContract(),
                functionDefinition,
                currentContractValueContext.getInterpretationEnvironment(), /*TODO*/    /*globalEnvironment,*/ // global env is always the fallback, otherwise we would run into local vars clashes
                this,
                arguments,
                ast);



        stackFrame.getScope().enterNode(functionDefinition, null);
        callStack.push(stackFrame);
        ast.setCurrentNode(functionDefinition);

        /*
        if (!initializedGlobalEnvironment) {
            // Due to expression evaluation for initializers requiring a variable environment and potentially other
            // stack frame elements, we initialize storage variables at the first function call.
            // An unpleasant example that requires variable processing is:
            //    int x = 5;
            //    int y = x;
            initializeContractEnvironment(functionDefinition.getContract());
            initializedGlobalEnvironment = true;
        }*/

        initializeLocalFunctionEnvironment(stackFrame, functionDefinition, arguments);

        // Perform interpretation
        /*
        ASTBlock body = functionDefinition.getBody();
        body.setCovered(true);
        Value result = interpretChildNodes(body);
        */
        Value result = null;

        try {
            interpretNode(functionDefinition.getBody());
        } catch (ReturnException exception) {
            result = exception.getReturnValue();
        }

        // Cleanup
        uninitializeLocalFunctionEnvironment(stackFrame, functionDefinition);
        callStack.pop();

        return result;
    }

    protected Value interpretBlock(ASTBlock block) throws Exception {
        return interpretChildNodes(block);
    }

    // Depth-first child node traversal for all paths
    protected Value interpretChildNodes(ASTNode currentNode) throws Exception {
        Value result = null;
        for (int i = 0; i < currentNode.getChildCount(); ++i) {
            result = interpretNode(currentNode.getChild(i));
            if (currentStackFrame().getHaveReturnValue()) {
                // Have return value (null if "empty"/"void")- stop
                result = currentStackFrame().getReturnValue();
                break; // TODO what about break/continue?
            }
        }
        return result;
    }


    protected void interpretEmitStatement(ASTEmitStatement emitStatement) throws Exception {
        JSONObject eventObject = new JSONObject();
        eventObject.put("event", emitStatement.getName());
        emittedEventsJSONObjectList.add(eventObject);

        JSONObject argsObject = new JSONObject();

        // Evaluate arguments
        ASTFunctionCall functionCall = emitStatement.getFunctionCall();
        VariableEnvironment currentVariableEnvironment = currentStackFrame().getScope().getVariableEnvironment();
        ArrayList<Expression> arguments = functionCall.getExpressionArguments(currentStackFrame().getContract(), currentVariableEnvironment);

        ASTEventDefinition eventDefinition = currentStackFrame().getContract().getEventDefinition(emitStatement.getName());
        ArrayList<ASTVariableDeclaration> eventParameters = eventDefinition.getParameterList().toArrayList();

        for (int i = 0 ; i < arguments.size(); ++i) {
            Value result = expressionEvaluator.evaluateForAll(currentVariableEnvironment, arguments.get(i)).values.get(0);
            argsObject.put(eventParameters.get(i).getName(),
                    JSONValueConverter.objectFromValue(result));
        }

        eventObject.put("args", argsObject);
    }

    protected Value interpretExpressionStatement(ASTExpressionStatement statement) throws Exception {
        return interpretExpression(statement.getBody());
    }

    protected Value interpretExpression(ASTNode node) throws Exception {
        // There may be no stack frame when we're evaluating things while initializing a contract object
        // TODO store globalEnvironment in ContractValue, retrieve it from there

        VariableEnvironment currentVariableEnvironment =
                currentStackFrame() != null? currentStackFrame().getScope().getVariableEnvironment(): currentContractValueContext.getInterpretationEnvironment() /*TODO*/; // globalEnvironment;
        ASTContractDefinition currentContract =
                currentStackFrame() != null? currentStackFrame().getContract(): currentContractValueContext.getContractDefinition();


        Expression expression = ExpressionBuilder.fromASTNode(ast,
                                                              currentContract,
                                                              currentVariableEnvironment,
                                                              node);
        Value result = expressionEvaluator.evaluateForAll(currentVariableEnvironment,
                ExpressionBuilder.fromASTNode(ast,
                        currentContract,
                        currentVariableEnvironment,
                        node)).values.get(0);
        return result;
    }

    protected void interpretReturnStatement(ASTReturnStatement returnStatement) throws Exception {
        // Evaluate argument, if any
        if (returnStatement.getArgument() != null) {
            VariableEnvironment currentVariableEnvironment = currentStackFrame().getScope().getVariableEnvironment();
            Value result = expressionEvaluator.evaluateForAll(currentVariableEnvironment,
                    ExpressionBuilder.fromASTNode(ast,
                            currentStackFrame().getContract(),
                            currentVariableEnvironment,
                            returnStatement.getArgument())).values.get(0);

            currentStackFrame().setReturnValue(result);
        } else {
            currentStackFrame().setReturnValue(null);
        }
        // TODO proper error handling
        throw new ReturnException("Invalid return statement on line  " + returnStatement.getInputCodeLineNumber(),
                currentStackFrame().getReturnValue());
    }

    protected void interpretIfStatement(ASTIfStatement ifStatement) throws Exception {
        // Interpret condition
        VariableEnvironment currentVariableEnvironment = currentStackFrame().getScope().getVariableEnvironment();
        Value result = interpretNode(ifStatement.getCondition());/*expressionEvaluator.evaluateForAll(currentVariableEnvironment,
                ExpressionBuilder.fromASTNode(currentStackFrame().getContract(),
                        currentVariableEnvironment,
                        ifStatement.getCondition())).values.get(0);*/



        // Interpret body
        if (((BoolValue) result).getValue() == true) {
            interpretNode(ifStatement.getIfBranch());
        } else if (ifStatement.getElseBranch() != null) {
            interpretNode(ifStatement.getElseBranch());
        }
    }

    protected void interpretWhileStatement(ASTWhileStatement whileStatement) throws Exception {
        for (;;) {
            // Interpret condition
            Value result = interpretNode(whileStatement.getCondition());

            if (!(result instanceof BoolValue)) {
                Console.error(whileStatement, "Unexpected while condition type - not bool for "
                        + whileStatement.getCondition().toSolidityCode());
                throw new Exception("interpretIfStatement failed");
            }

            // Interpret body
            if (((BoolValue) result).getValue() == true) {
                try {
                    interpretNode(whileStatement.getBody());
                } catch (BreakException breakException) {
                    break;
                } catch (ContinueException continueException) {
                    continue;
                }
            } else {
                break;
            }
        }
    }

    protected void interpretForStatement(ASTForStatement forStatement) throws Exception {
        if (forStatement.getInitPart() != null) {
            // Interpret initializer part
            interpretNode(forStatement.getInitPart());
        }

        for (;;) {
            // Interpret condition
            Value result = interpretNode(forStatement.getCondPart());

            if (!(result instanceof BoolValue)) {
                Console.error(forStatement, "Unexpected for condition type - not bool for "
                        + forStatement.getCondPart().toSolidityCode());
                throw new Exception("interpretForStatement failed");
            }

            // Interpret body
            if (((BoolValue) result).getValue() == true) {
                try {
                    interpretNode(forStatement.getBody());
                } catch (BreakException breakException) {
                    break;
                } catch (ContinueException continueException) {
                    ; // OK, continue with post-body statement below
                }
            } else {
                break;
            }

            // Interpret post-body statement
            interpretNode(forStatement.getLoopPart());
        }
    }

    protected void interpretDoWhileStatement(ASTDoWhileStatement doWhileStatement) throws Exception {
        for (;;) {
            // Interpret loop body
            try {
                interpretNode(doWhileStatement.getBody());
            } catch (BreakException breakException) {
                break;
            } catch (ContinueException continueException) {
                ; // OK, continue with condition below
            }

            // Interpet condition
            Value result = interpretNode(doWhileStatement.getCondition());

            if (!(result instanceof BoolValue)) {
                Console.error(doWhileStatement, "Unexpected do-while condition type - not bool for "
                        + doWhileStatement.getCondition().toSolidityCode());
                throw new Exception("interpretDoWhileStatement failed");
            }

            if (((BoolValue) result).getValue() == false) {
                break;
            }
        }
    }

    protected void interpretBreakStatement(ASTBreakStatement breakStatement) throws Exception {
        // Outer loops catch the exception or we'll error out. TODO proper error handling
        throw new BreakException("Invalid break statement on line  " + breakStatement.getInputCodeLineNumber());
    }

    protected void interpretContinueStatement(ASTContinueStatement breakStatement) throws Exception {
        // Outer loops catch the exception or we'll error out. TODO proper error handling
        throw new ContinueException("Invalid continue statement on line  " + breakStatement.getInputCodeLineNumber());
    }
}
