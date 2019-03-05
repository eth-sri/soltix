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
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.expressions.*;
import soltix.interpretation.values.BoolValue;
import soltix.interpretation.values.IntegerValue;
import soltix.interpretation.values.Value;
import soltix.interpretation.values.ValueContainer;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.interpretation.variables.VariableValues;
import soltix.util.RandomNumbers;
import soltix.util.WeightedRandomChoice;

import java.util.ArrayList;
import java.util.Random;

import static soltix.ast.ASTBinaryOperation.Operator.*;
import static soltix.interpretation.expressions.ExpressionEvaluationException.ExceptionType.ExceptionType_ValueError_NegativeSecondOperand;


/**
 * Class to generate randomized expressions.
 *
 * This follows the outline given in "Finding Compiler Bugs via Live Code Mutation" (Sun et al., 2016)
 */
public class ExpressionGenerator {
    private AST ast; // for user-defined type lookups
    private RandomNumbers prng;
    private WeightedRandomChoice weightedRandomUnaryOperationChoice;
    private PredicateGenerator predicateGenerator;
    private TypeConverter typeConverter;
    private ExpressionTypeChecker expressionTypeChecker;
    private ExpressionTypeConverter expressionTypeConverter;
    private ExpressionEvaluator expressionEvaluator;
    private ValueGenerator valueGenerator;
    private boolean allowFunctionCalls = true;
    private FunctionGenerator functionGenerator;


    public ExpressionGenerator(AST ast, PredicateGenerator predicateGenerator, FunctionGenerator functionGenerator, RandomNumbers prng) throws Exception {
        this.ast = ast;
        this.predicateGenerator = predicateGenerator;
        this.prng = prng;
        expressionTypeChecker = new ExpressionTypeChecker(ast);
        expressionEvaluator = new ExpressionEvaluator(new ExpressionEvaluationErrorHandler(prng)); //ast);
        typeConverter = new TypeConverter(ast, prng, expressionEvaluator);
        expressionTypeConverter = new ExpressionTypeConverter(ast, expressionEvaluator, typeConverter);
        valueGenerator = new ValueGenerator(prng);

        // Unary operator, function call or conversion to a random other type.
        // Unary operators are weighted more heavily, since they allow for more diverse operations.
        // Function calls may be slightly more interesting than their weight suggests, but also carry a
        // higher code generation size overhead due to introducing new functions
        allowFunctionCalls = true;
        weightedRandomUnaryOperationChoice = new WeightedRandomChoice();
        weightedRandomUnaryOperationChoice.addItem(90); // uop
        weightedRandomUnaryOperationChoice.addItem(10); // function call
        //weightedRandomUnaryOperationChoice.addItem(10); // type conversion

        this.functionGenerator = functionGenerator; //new FunctionGenerator(ast, prng, null /*not needed*/, this, null /*not needed*/);
    }

    public boolean getAllowFunctionCalls() { return allowFunctionCalls; }
    public void setAllowFunctionCalls(boolean value) { allowFunctionCalls = value; }

    protected<T> T pickRandomItemWithoutReplacement(ArrayList<T> list) {
        int itemIndex = (int)prng.generateLongInteger(0, list.size()-1);
        T item = list.get(itemIndex);
        list.remove(itemIndex);
        return item;
    }

    protected<T> ArrayList<T> pickMultipleRandomItemsWithoutReplacement(ArrayList<T> list, int count) throws Exception {
        if (count > list.size()) {
            throw new Exception("pickMultipleRandomItemsWithoutReplacement called for more items than are available");
        }

        ArrayList<T> result = new ArrayList<T>();
        while (count-- > 0) {
            T currentItem = pickRandomItemWithoutReplacement(list);
            result.add(currentItem);
        }
        return result;
    }

    // TODO better way to write this or move these to the respective classes
    protected ArrayList<ASTUnaryOperation.Operator> buildUnaryOperatorList() {
        ArrayList<ASTUnaryOperation.Operator> operators = new ArrayList<ASTUnaryOperation.Operator>();
        for (ASTUnaryOperation.Operator operator : ASTUnaryOperation.Operator.values()) {
            operators.add(operator);
        }
        return operators;
    }
    protected ArrayList<ASTBinaryOperation.Operator> buildBinaryOperatorList() {
        ArrayList<ASTBinaryOperation.Operator> operators = new ArrayList<ASTBinaryOperation.Operator>();
        for (ASTBinaryOperation.Operator operator : ASTBinaryOperation.Operator.values()) {
            operators.add(operator);
        }
        return operators;
    }

    // TODO We still run into Securify test suite issues with structs containing mappings.
    static public boolean isUsableType(AST ast, ASTNode type) {
        //ASTNode type = variable.getTypeName();

        if (type instanceof ASTMapping) {
            return false;
        } else if (!(type instanceof ASTUserDefinedTypeName)) {
            return true; // OK
        }

        // We exclude contracts and structs containing contracts for now
        // TODO support this
        ASTStructDefinition structDefinition = ast.getStructDefinition(type.getName());
        if (structDefinition != null) {
            // Each member must allso be a "usable variable" for this to work
            for (ASTVariableDeclaration declaration : structDefinition.getMembers()) {
                if (!isUsableType(ast, declaration.getTypeName())) {
                    return false;
                }
            }
            return true; // OK
        }

        ASTEnumDefinition enumDefinition = ast.getEnumDefinition(type.getName());
        if (enumDefinition != null) {
            return true; // OK
        }


        assert ast.getContract(type.getName()) != null;
        return false;
    }

    public Variable pickRandomUsableVariable(VariableEnvironment environment) {
        if (environment.getVariableCount() == 0) {
            return null;
        }

        int randomVariableIndex = (int)prng.generateLongInteger(0, environment.getVariableCount()-1);

        // Loop, beginning from the initial position, until a suitably typed variable is found
        int currentVariableIndex = randomVariableIndex;

        do {
            VariableValues variableValues = environment.getVariableValues(currentVariableIndex);
            Variable candidateVariable = variableValues.getVariable();
            if (isUsableType(ast, candidateVariable.getDeclaration())) {
                return candidateVariable;
            }
            currentVariableIndex = (currentVariableIndex + 1) % environment.getVariableCount();
        } while (currentVariableIndex != randomVariableIndex);
        return null;
    }

    protected boolean populateWorklist(ASTContractDefinition contract, VariableEnvironment environment, ArrayList<Expression> worklist) throws Exception {
        // Random selection of variables to include
        // TODO Maybe it should be possible to limit the number of variables to generate simpler expressions.
        //Variable backupVariable = null;
        if (environment.getVariableCount() > 0) {
            for (int i = 0; i < environment.getVariableCount(); ++i) {
                Variable variable = environment.getVariableValues(i).getVariable();
                // TODO This rarely seems to pick up struct variables, which haven't been seen together with ++/-- yet
                // TODO ++/-- side effect application may be handled incorrectly too
                if (isUsableType(ast, variable.getDeclaration())) {
                    if (prng.flipCoin() && prng.flipCoin()) { // TODO UNDO THIS?
                        // Include
                        Expression expression = new Expression(variable);

                        if (Type.isStructType(ast, variable.getDeclaration().getTypeName())) {
                            // This is a struct type, to which operators generally can't be applied.
                            //
                            // For some cases, we use the member access operator to obtain an expression that
                            // designates a member - this allows the application of inc/dec, etc. operators.
                            //
                            // For the remaining cases, we just keep the struct type, which may in some cases
                            // later cause a binary operator type conversion of the other operand to this
                            // struct - which is an interesting operation in itself, since it will create a
                            // struct literal (the member access operation is then applied to both structs).
                            if (prng.flipCoin()) {
                                ASTStructDefinition structType = ast.getStructDefinition(variable.getDeclaration().getTypeName().getName());
                                expression = typeConverter.accessRandomStructMember(environment,
                                        expression,
                                        structType);
                            }
                        }

                        worklist.add(expression);
                    } else {
                        // Maybe include later if nothing else comes up
                        //  backupVariable = variable;
                    }
                }
            }
            // TODO add some random constants as well?
        }
        if (worklist.size() == 0) {
            // We need some variable to work with
            //Variable variable = environment.getVariableValues((int)prng.generateLongInteger(0, environment.getVariableCount()-1)).getVariable();
            Variable backupVariable = pickRandomUsableVariable(environment);

            if (backupVariable != null) {
                worklist.add(new Expression(backupVariable));
            } else {
                // Add random constant so as to not return empty-handed - some callers require something to work with
                ASTNode randomType = TypeContainer.chooseRandomType(ast, contract,  true, null, prng);
                Expression constant = new Expression(valueGenerator.generateRandomValue(ast, randomType, ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL));
                worklist.add(constant);
            }
        }

        return true;
    }


    // Produce a valid, already interpreted expression - potentially including operator fix-up operations.
    // Note that functions to call may be generated and added to the contract as a side effect.
    //
    // TODO We may need a more explicit separation between expressions that may and may not be evaluated
    // multiple times. For full contract generations, multiple evaluations of an expression containing an
    // increment/decrement operator will result in multiple side effects
    public Expression synthesizeExpression(ASTContractDefinition contract, VariableEnvironment environment, ASTNode desiredResultType) throws Exception {
        Expression expression = synthesizeBaseExpressionForRandomWorklist(contract, environment, desiredResultType);

        expressionEvaluator.evaluateForAll(environment, expression); // Ignore result

        return expression;
    }

    static public Expression makeOperandSafeForOperation(RandomNumbers prng,
                                                         ASTBinaryOperation.Operator operator,
                                                         Expression secondOperand) throws Exception{
        if (operator == OP_DIV || operator == OP_MOD) {
            // Turn x / y into x / (y != 0? rand1: rand2)
            // We cannot simply reuse "y" in the true branch, because that produces incorrect results for reevaluation
            // of expressions containing side effects (currently only ++/--)
            IntegerValue zeroValue = ValueContainer.getSmallIntegerValue(secondOperand.getType(), 0);
            Expression zeroCheck = new Expression(secondOperand, OP_NEQ, new Expression(zeroValue));

            IntegerValue randomValueTrue = ValueContainer.getSmallIntegerValue(secondOperand.getType(), (int)prng.generateLongInteger(1, 5));
            IntegerValue randomValueFalse = ValueContainer.getSmallIntegerValue(secondOperand.getType(), (int)prng.generateLongInteger(1, 5));

            return new Expression(zeroCheck, new Expression(randomValueTrue), new Expression(randomValueFalse));
        } else if (operator == OP_BSHL || operator == OP_BSHR) {
            // Turn x <shift> y into x <shift> (y < 0? rand1: rand2)
            // We cannot simply reuse "y" in the true branch, because that produces incorrect results for reevaluation
            // of expressions containing side effects (currently only ++/--)
            IntegerValue zeroValue = ValueContainer.getSmallIntegerValue(secondOperand.getType(), 0);
            Expression zeroCheck = new Expression(secondOperand, OP_LT, new Expression(zeroValue));

            IntegerValue randomValueTrue = ValueContainer.getSmallIntegerValue(secondOperand.getType(), (int)prng.generateLongInteger(1, 5));
            IntegerValue randomValueFalse = ValueContainer.getSmallIntegerValue(secondOperand.getType(), (int)prng.generateLongInteger(1, 5));

            return new Expression(zeroCheck, new Expression(randomValueTrue), new Expression(randomValueFalse));
        } else {
            return secondOperand;
        }
    }

    // Synthesize unevaluated base expression, which may later need additional operator injections to fix up
    // exceptions.
    //
    // TODO this seems to pick up contract-typed variables, which is undesirable in at least some contexts because
    // a contract value may not convertible to the desired result type (but if it has accessible fields, these could
    // be accessed like struct fields using the member access operator to obtain a value of some other type), see:
    //   ./run-one-test.sh ~/securify-contracts/0174d4582cca163501d219def808fd4f9a126dd0bf0f7d224020c7d83d585dc5/contract.sol
    protected Expression synthesizeBaseExpressionForRandomWorklist(ASTContractDefinition contract, VariableEnvironment environment, ASTNode desiredResultType) throws Exception {
        ArrayList<Expression> worklist = new ArrayList<Expression>();

        if (!populateWorklist(contract, environment, worklist)) {
            return null;
        }
        return synthesizeBaseExpressionForSpecifiedWorklist(contract, worklist, environment, desiredResultType, false);
    }

    protected Expression synthesizeBaseExpressionForSpecifiedWorklist(ASTContractDefinition contract,
                                                                      ArrayList<Expression> worklist,
                                                                      VariableEnvironment environment,
                                                                      ASTNode desiredResultType,
                                                                      boolean enforceInclusionAndEvaluation) throws Exception {
        if (Configuration.verboseDebugOutput) System.out.println( "=================== new expr gen ==============");

        boolean oldDisallowLogicalAndConditionalOperators = Configuration.avoidLogicalAndConditionalOperators;
        if (enforceInclusionAndEvaluation) {
            Configuration.avoidLogicalAndConditionalOperators = true;
        }

        while (worklist.size() > 1) {
            if (Configuration.verboseDebugOutput) {
                System.out.println("Expr gen worklist:");
                for (Expression expr : worklist) {
                    System.out.println("           > " + expr.toASTNode().toSolidityCode() + " type " + expr.getType().toSolidityCode()
                            + "    inc values " + (expr.getComputedValues() == null ? 0 : expr.getComputedValues().values.size()));
                }
            }


            // TODO: In addition to unary and binary operators we could introduce a third category
            // (flipCoin->0.33 probability choice) for:
            //    - function calls
            //    - cast operations
            //    - index (arrays, bytes*) and member access
            // Type conversions could be generated as needed to make these operations applicable to any type.
            // Similarly, unary and binary operators could be applied to type-converted items to enforce their
            // applicability in new contexts
            if (prng.flipCoin()) {
                boolean allowFunctionCalls2 = allowFunctionCalls;
                int nonStructItemCount = -1;
                int unusableFunctionArgumentsCount = 0;



                if (!Configuration.allowStructsInFunctionABI) {
                    for (int i = 0; i < worklist.size(); ++i) {
                        if (!Type.isStructType(ast, worklist.get(i).getType())) {
                            ++nonStructItemCount;
                        }
                    }
                    if (nonStructItemCount == 0) {
                        // Cannot generate non-empty function argument list
                        allowFunctionCalls2 = false;
                    }
                }

                // Rule out expressions that alf
                for (int i = 0; i < worklist.size(); ++i) {
                    if (worklist.get(i).getContainsExpressionAlias()) {
                        ++unusableFunctionArgumentsCount;
                    }
                }

                // Unary operator, function call or conversion to a random other type
                switch (allowFunctionCalls2? weightedRandomUnaryOperationChoice.choice(prng): 0) {
                    case 0: // uop
                    {
                        // TODO Limit re-application of operators, e.g. for bytes variables we often have a ridiculous sequence
                        // of 5-10 ! operators applied to the same item in succession
                        Expression operand = pickRandomItemWithoutReplacement(worklist);
                        ArrayList<ASTUnaryOperation.Operator> operators = buildUnaryOperatorList();
                        if (Configuration.verboseDebugOutput) System.out.println(" ==== START UNARY OP APP ===");
                        while (operators.size() > 0) {
                            ASTUnaryOperation.Operator operator = pickRandomItemWithoutReplacement(operators);
                            if (expressionTypeChecker.isUndefinedUnaryOperation(environment, operand, operator)) {
                                continue;
                            }
                            if (Configuration.verboseDebugOutput)
                                System.out.println("    APPLYING " + operator + " TO " + operand.toASTNode().toSolidityCode());
                            Expression newExpression = new Expression(operator, operand);
                            if (Configuration.verboseDebugOutput)
                                System.out.println("    BUILT EXPR " + newExpression.toASTNode().toSolidityCode());

                            worklist.add(newExpression);
                            break;
                        }
                        if (Configuration.verboseDebugOutput) System.out.println(" ==== END UNARY OP APP ===");
                        break;
                    }
                    case 1: // function call
                    {
                        int argumentCount = (int)prng.generateLongInteger(1,
                                (Configuration.allowStructsInFunctionABI? worklist.size(): nonStructItemCount) - unusableFunctionArgumentsCount);

                        argumentCount = Math.min(argumentCount, 5); // Avoid stack space limit errors

                        ArrayList<Expression> arguments;

                        // Pick the last argumentCount items, w.l.o.g., from the available expressions.
                        // If structs are allowed as arguments, they are included.
                        arguments = new ArrayList<Expression>();
                        for (int i = worklist.size() - 1; i >= 0; --i) {
                            Expression e = worklist.get(i);
                            boolean isUsableExpression = e.getExpressionAlias() == null;
                            if (!Configuration.allowStructsInFunctionABI && Type.isStructType(ast, e.getType())) {
                                isUsableExpression = false;
                            }

                            if (isUsableExpression) {
                                if (e.getContainsExpressionAlias()) {
                                    // This expression is itself a function call - avoid using this as another
                                    // argument, because it will lead to conflicts
                                    continue;
                                }

                                arguments.add(e);
                                worklist.remove(i);
                                if (arguments.size() == argumentCount) {
                                    // Done
                                    break;
                                }
                            }
                        }

                        Expression functionCallExpression;

                        if (arguments.size() > 0) {
                            functionCallExpression = functionGenerator.generateFunctionWithSingleReturn(contract, environment, arguments);
                            
                            worklist.add(functionCallExpression);
                        }
                        break;
                    }
                    default:
                        throw new Exception("synthesizeBaseExpressionForSpecifiedWorklist: Unimplemented unary operation choice");
                }
            } else {
                // Binary operator - first bring both operands to a common or compatible type
                Expression firstOperand = pickRandomItemWithoutReplacement(worklist);
                Expression secondOperand = pickRandomItemWithoutReplacement(worklist);
                ArrayList<ASTBinaryOperation.Operator> operators = buildBinaryOperatorList();

                expressionTypeConverter.setFirstOperand(firstOperand);
                expressionTypeConverter.setSecondOperand(secondOperand);
                
                if (Configuration.verboseDebugOutput) System.out.println(" === START BINARY OP APP ===");

                expressionTypeConverter.run(environment, prng);

                firstOperand = expressionTypeConverter.getFirstOperand();
                secondOperand = expressionTypeConverter.getSecondOperand();

                while (operators.size() > 0) {
                    ASTBinaryOperation.Operator operator = pickRandomItemWithoutReplacement(operators);

                    if (expressionTypeChecker.isUndefinedBinaryOperation(environment, firstOperand, operator, secondOperand)) {
                        continue;
                    }

                    if (Configuration.guaranteeSafeExecution) {
                        // Catch invalid constructs:
                        //    - div/mod by 0
                        //    - shift by negative bit count
                        secondOperand = makeOperandSafeForOperation(prng, operator, secondOperand);
                    }

                    Expression newExpression = new Expression(firstOperand, operator, secondOperand);
                    worklist.add(newExpression);
                    break;
                }
                if (Configuration.verboseDebugOutput) System.out.println(" === END BINARY OP APP, SIZE =  ===" + operators.size());

                if (worklist.size() == 0) {
                    // Filter out known "bytes" problems first
                    // TODO We could subscript "bytes" variables if they are always non-empty based on the recorded runtime info
                    // TODO If something like this happens, maybe we could pull in more variables that were previously excluded in the random selection
                    // TODO Maybe excluding variables randomly isn't even desirable, since there are not that many in most contexts (though we have to watch out for stack errors)
                    Expression alternativeResult = null;

                    if (Type.isStringType(firstOperand.getType()) && Type.isStringType(secondOperand.getType())) {
                        // There are no defined string operations - convert both items to a random bytes
                        // type
                    }
                    if (Type.isVariableByteType(secondOperand.getType())) { // w.l.o.g.: If at least one is "bytes", the first operand must also be "bytes"
                        Expression temp = firstOperand;
                        firstOperand = secondOperand;
                        secondOperand = temp;
                    }
                    if (Type.isVariableByteType(firstOperand.getType())) {
                        if (!Type.isVariableByteType(secondOperand.getType())) {
                            alternativeResult = secondOperand; // already evaluated
                        } else {
                            // Nothing to work with, include some constant
                            Expression expression = new Expression(valueGenerator.generateRandomValue(ast,
                                    desiredResultType,
                                    ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL));
                            alternativeResult = expression;
                        }
                        worklist.add(alternativeResult);
                        break;
                    }

                    if (!Configuration.ganacheOptimizationWorkarounds) {
                        // No defined operators - probably can't happen!? TODO Keep one of the arguments, drop the other one if this can really happen?
                        throw new Exception("ExpressionGenerator: Unexpected generation failure: no defined binary operator for "
                                + firstOperand.toASTNode().toSolidityCode()
                                + " and " + secondOperand.toASTNode().toSolidityCode() + ", types "
                                + firstOperand.getType().toSolidityCode() + " vs " + secondOperand.getType().toSolidityCode());
                    } else {
                        // Due to optimization workarounds, constellations without valid operations occur.
                        // TODO should probably still be looked into
                        worklist.add(firstOperand);
                        System.out.println("THROWING AWAY " + secondOperand.toASTNode().toSolidityCode());
                        System.out.println(" first " + firstOperand.toASTNode().toSolidityCode());
                        System.out.println(" second " + secondOperand.toASTNode().toSolidityCode());
                        System.out.println(" type first " + firstOperand.getType().toSolidityCode());
                        System.out.println(" type second " + secondOperand.getType().toSolidityCode());
                        throw new Exception("Discarded item - no longer an option since function call generation was added");
                    }
                }
            }
        }

        if (Configuration.verboseDebugOutput) System.out.println("############## RETURNING EXPR " + worklist.get(0).toASTNode().toSolidityCode());
        if (Configuration.verboseDebugOutput) System.out.println("type conv " + worklist.get(0).getType().toSolidityCode() + " -> " + desiredResultType.toSolidityCode());

        Expression ret = typeConverter.convertToCompatibleType(environment, worklist.get(0), desiredResultType);

        if (Configuration.verboseDebugOutput) System.out.println("############## RETURNING CONV " + ret.toASTNode().toSolidityCode());

        if (enforceInclusionAndEvaluation) {
            Configuration.avoidLogicalAndConditionalOperators = oldDisallowLogicalAndConditionalOperators;
        }

        ret.assignID();
        //System.out.println(" " + ret.getID() + " = " + ret.toASTNode().toSolidityCode());
        return ret;
    }
}
