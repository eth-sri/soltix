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

package soltix.interpretation.expressions;

import com.sun.org.apache.xml.internal.utils.UnImplNode;
import soltix.ast.*;
import soltix.interpretation.FullInterpreter;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.values.*;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.util.Hash;
import soltix.util.Util;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static soltix.ast.ASTBinaryOperation.Operator.*;
import static soltix.ast.ASTUnaryOperation.Operator.OP_BNEG;
import static soltix.ast.ASTUnaryOperation.Operator.OP_DEC_POST;
import static soltix.interpretation.expressions.ExpressionEvaluationException.ExceptionType.*;

/*
 * Class for epression evaluation.
 *
 * There are two main expression evaluation contexts of interest to us:
 *
 *    - Evaluation of synthesized expressions based on recorded profiling information in order to check
 * their validity with the set of values that has been encountered for some contract and input transactions.
 * This can be done incrementally for each operator using evaluateForAllIncrementally()
 *    - If and when we support full contract interpretation, there should only be a single set of current
 * variable values that is continuously updated as assignment operators are encountered. This ma be more
 * appropriately expressed with a Scope object, not VariableEnvironment
 * This can be done with a single call to evaluateForOne()
 */
public class ExpressionEvaluator {
    //private AST ast;
    static public boolean freeIntermediateResults = false; //true;
    private IExpressionEvaluatorErrorCallback evaluatorErrorCallback;
    private FullInterpreter interpreter = null;

    public ExpressionEvaluator(IExpressionEvaluatorErrorCallback evaluatorErrorCallback,
                               FullInterpreter interpreter) { //AST ast) {
        this.evaluatorErrorCallback = evaluatorErrorCallback;
        this.interpreter = interpreter;
    }


    public class ComputedValues {
        public ComputedValues(int valueCount) {
            values = new ArrayList<Value>();
            for (int i = 0; i < valueCount; ++i) {
                values.add(null);
            }
        }
        public ComputedValues() {
            values = new ArrayList<Value>();
        }

        public ArrayList<Value> values;
        // Where appropriate: Pass source expression info (e.g. variables) along with computation results
        public Expression sourceExpression = null;
    }



    // evaluateForOne() runs on one set of values previously selected with VariableEnvironment.selectCurrentValueSet(),
    // and recursively evaluates all sub-expressions
    public Value evaluateForOne(VariableEnvironment environment, int valueSetIndex, Expression expression) throws Exception {
        //if (!environment.haveCurrentValueSetSelection()) {
        if (valueSetIndex == VariableEnvironment.NO_VALUE_SET_SELECTED) {
            throw new Exception("evaluateForOne called without value set selection");
        }

        return evaluate(environment, valueSetIndex, expression, false).values.get(0);
    }

    // evaluateForAllIncrementally() runs on all sets of values available in the VariableEnvironment and stores all
    // generated values in the corresponding expression.
    //
    // It operates incrementally and expects sub-expressions to have been evaluated (= contain the computed Value
    // list for all value sets) already (this allows fine-grained expression creation, where the application of each
    // new operator can be attempted and discarded if an undefined construct such as div-by-0 is encountered)
    private boolean doingPartialValueSetEvaluation = false;


    public ComputedValues evaluateForAll(VariableEnvironment environment, Expression expression) throws Exception {
        return evaluate(environment, VariableEnvironment.NO_VALUE_SET_SELECTED, expression, false);
    }



    protected Expression fixExpressionEvaluationException(VariableEnvironment environment, Expression originalExpression, ExpressionEvaluationException evaluationException) throws Exception {
        Expression fixedExpression;

        evaluationException.sanityCheckCorrectionContext(originalExpression);

        switch (evaluationException.getType()) {
            case ExceptionType_ValueError_ZeroDivision:
                fixedExpression = evaluatorErrorCallback.fixZeroDivision(this, environment, originalExpression, evaluationException.getValues());
                break;
            case ExceptionType_ValueError_NegativeSecondOperand:
                fixedExpression = evaluatorErrorCallback.fixNegativeOperand(originalExpression, evaluationException.getValues());
                break;
            default:
                throw evaluationException;
        }
        if (fixedExpression == null) {
            throw evaluationException;
        }
        return fixedExpression;
    }


    boolean debugTrace = false;

    protected ComputedValues evaluate(VariableEnvironment environment,
                                      int valueSetIndex,
                                      Expression expression,
                                      boolean reevaluating) throws Exception {
        ComputedValues resultValues;

        if (expression.getExpressionAlias() != null) {
            // This is an override for evaluation
            expression = expression.getExpressionAlias();
        }

        int traceExpressionId = -1; // 209

        if (traceExpressionId != -1 && expression.getID() == traceExpressionId) {
            // Print variable environment. By adding suitable emit statements (currently needs to be done manually)
            // before the expression we're tracing, we can verify that our current evaluation results correspond to
            // the actually computed values.
            // Furthermore, verbose expression evaluation debug output can be added - only the output between this
            // start debug statement and the corresponding end debug statement below needs to be considered
            System.out.println(" ========== eval " + traceExpressionId + " start, code " + expression.toASTNode().toSolidityCode());
            environment.printLatestToLogger();
            debugTrace = true;
        }


        if (reevaluating && expression.getComputedValues() != null) {
            // Return previous result. This prevents side effects from kicking in multiple times when fixing up and
            // reevaluating faulty expressions
            return expression.getComputedValues();
        }

        if (expression.getBinaryOperator() != null) {
            ComputedValues firstOperandValues;
            ComputedValues secondOperandValues;

            // We can definitely evaluate the first argument already, but for some operators the second one
            // may only be needed on demand
            firstOperandValues = evaluate(environment, valueSetIndex, expression.getFirstOperand(), reevaluating);

            /*secondOperandValues = evaluate(environment, expression.getSecondOperand(), forAllValues);*/

            if (Type.isIntegerType(expression.getFirstOperand().getType())
                    && Type.isIntegerType(expression.getSecondOperand().getType())) {
                // Check signed/unsigned type mismatch (independently of the evaluation of value sets, since these may be
                // empty)
                // TODO This can probably be removed again since we proactively avoid this constellation now by inserting casts where needed
                ASTElementaryTypeName firstType = (ASTElementaryTypeName)expression.getFirstOperand().getType();
                ASTElementaryTypeName secondType = (ASTElementaryTypeName)expression.getSecondOperand().getType();
                if (firstType.isSigned() != secondType.isSigned()) {
                    throw new ExpressionEvaluationException(null, ExceptionType_TypeError_SignednessMismatch,
                            "Invalid combination of signed and unsigned integer operands in "
                                    + expression.toASTNode().toSolidityCode(), null);
                } else if (firstType.isSigned() && expression.getBinaryOperator() == OP_EXP) {
                    throw new ExpressionEvaluationException(null, ExceptionType_TypeError_WrongSign,
                            "Invalid signed operands to **", null);
                }
            }

            try {
                resultValues = evaluateBinaryOperatorForAll(environment,
                        firstOperandValues,  // expression.getFirstOperand(),
                        expression.getBinaryOperator(),
                        /*secondOperandValues*/ expression.getSecondOperand(),

                        // This must process all result operands produced above for consistency. Otherwise, if e.g. the
                        // single item at index 10 was processed, we get an out-of-bounds access for the single result
                        // item
                        VariableEnvironment.NO_VALUE_SET_SELECTED,
                        valueSetIndex, // pass original value too

                        reevaluating);

            } catch (ExpressionEvaluationException evaluationException) {
                if (evaluatorErrorCallback == null) {
                    throw evaluationException;
                }


                // Try to fix the issue for value errors (type errors: see above) that may occur at this level
                // (e.g. ArrayBounds should be closed closer to the error source)
                Expression fixedSecondOperandExpression;

                fixedSecondOperandExpression = fixExpressionEvaluationException(environment, expression.getSecondOperand(), evaluationException);

                resultValues = null;
                try {
                    // Try evaluating it again
                    expression.updateSecondOperand(fixedSecondOperandExpression);


/*
                    System.out.println("re-eval for " + expression.getFirstOperand().toASTNode().toSolidityCode()
                        + "       " + expression.getBinaryOperator() + "      " + expression.getSecondOperand().toASTNode().toSolidityCode());
                    System.out.println("full expr " + expression.toASTNode().toSolidityCode());
                    System.out.println("first opernad values " + firstOperandValues.values.size());
*/

                    // Note: Due to fix-up operations, at this point we might have the unintuitive constellation that
                    // even "firstOperandValues" only contains partial results, i.e. with null entries due to error
                    // interruptions, even though the structure is
                    //          evalAll(first)              # ensures all "first" items are good
                    //          evalBinOp(first, second)    # only fails to complete "second" items
                    // This is because in e.g.
                    //       x % y
                    // all "x" items will be computed properly, but "y" may be left incomplete due to division by zero,
                    // and then a fixed version
                    //       x % (y + offset)
                    // actually causes "y" to become the first operand in a binary expression. As usual, this must
                    // probably be resolved entirely on demand, not for all y items, due to short-circuiting and side
                    // effects, e.g. in
                    //    b && (x % y++) == 0
                    // where the y++ side effect must only manifest itself depending on the "b" value.
                    //
                    // This is addressed by trying to preserve the order of fix-up operations where needed, i.e. an
                    // added extra offset becomes the first operand so it can be evaluated entirely, and the second
                    // item in a binary operator evaluation is re-evaluated on demand
                    resultValues = evaluateBinaryOperatorForAll(environment,
                            firstOperandValues,
                            expression.getBinaryOperator(),
                            expression.getSecondOperand(),
                            //valueSetIndex,
                            VariableEnvironment.NO_VALUE_SET_SELECTED,
                            valueSetIndex, // pass original value too

                            true);
                } catch (ExpressionEvaluationException evaluationException2) {
                    // Second exception - give up.
                    // This would probably indicate that the fix didn't work, since there are currently no known
                    // scenarios in which a fixed first exception can result in a second exception (e.g. addressing
                    // div-by-0 resulting in a negative operand error - doesn't happen since both error classes occur
                    // in distinct contexts)
                    System.out.println("SECOND EXCEPTION " + evaluationException2.getType()
                                     + " AFTER FIRST " + evaluationException.getType());
                    evaluationException2.printStackTrace();
                    System.exit(1);
                }
            }
            if (freeIntermediateResults) {
                expression.getFirstOperand().setComputedValues(null);
                expression.getSecondOperand().setComputedValues(null);
            }
        } else if (expression.getUnaryOperator() != null) {
            ComputedValues operandValues;

            operandValues = evaluate(environment, valueSetIndex, expression.getFirstOperand(), reevaluating);
            operandValues.sourceExpression = expression.getFirstOperand();

            resultValues = evaluateUnaryOperatorForAll(environment, expression.getUnaryOperator(), operandValues, expression.getFirstOperand());

            if (freeIntermediateResults) {
                expression.getFirstOperand().setComputedValues(null);
            }
        } else if (expression.getConditionalOperator()) {
            ComputedValues firstOperandValues;
            ComputedValues secondOperandValues;
            ComputedValues thirdOperandValues;

            // We can definitely evaluate the first argument already, but for some operators the second one
            // may only be needed on demand
            firstOperandValues = evaluate(environment, valueSetIndex, expression.getFirstOperand(), reevaluating);
            /*secondOperandValues = evaluate(environment, expression.getSecondOperand(), forAllValues);
            thirdOperandValues = evaluate(environment, expression.getThirdOperand(), forAllValues);*/

            resultValues = evaluateConditionalOperatorForAll(environment,
                                                             firstOperandValues,
                                                             expression.getFirstOperand(), // for debugging
                                                             expression.getSecondOperand(),
                                                             expression.getThirdOperand(),
                                                             reevaluating);

            if (freeIntermediateResults) {
                expression.getFirstOperand().setComputedValues(null);
                expression.getSecondOperand().setComputedValues(null);
                expression.getThirdOperand().setComputedValues(null);
            }
        } else if (expression.getMemberAccess() != null) {
            ComputedValues operandValues;

            operandValues = evaluate(environment, valueSetIndex, expression.getFirstOperand(), reevaluating);

            resultValues = evaluateMemberAccessForAll(operandValues, expression.getMemberAccess());

            if (freeIntermediateResults) {
                expression.getFirstOperand().setComputedValues(null);
            }
        } else if (expression.getIndexAccess() != null) {
            ComputedValues operandValues;
            ComputedValues indexValues;

            operandValues = evaluate(environment, valueSetIndex, expression.getFirstOperand(), reevaluating);
            indexValues = evaluate(environment, valueSetIndex, expression.getIndexAccess(), reevaluating);

            // TODO Fix ArrayBounds exceptions
            resultValues = evaluateIndexAccessForAll(operandValues, indexValues, expression.getIndexAccess());

            if (freeIntermediateResults) {
                expression.getFirstOperand().setComputedValues(null);
                expression.getIndexAccess().setComputedValues(null);
            }
        } else if (expression.getFunctionCall() != null) {
            ASTFunctionCall functionCall = expression.getFunctionCall();
            ASTNode called = functionCall.getCalled();

            if (called instanceof ASTIdentifier) {
                ASTIdentifier identifier = (ASTIdentifier) called;
                if (identifier.getName().equals("keccak256")) {
                    ArrayList<Expression> arguments = expression.getFunctionCallArguments();
                    Expression stringArgument = arguments.get(0);
                    ComputedValues argumentValues = evaluate(environment, valueSetIndex, stringArgument, reevaluating);

                    if (valueSetIndex == VariableEnvironment.NO_VALUE_SET_SELECTED) {
                        // There's only a string argument here
                        resultValues = evaluateKeccak256ForAll(argumentValues);

                        if (freeIntermediateResults) {
                            arguments.get(0).setComputedValues(null);
                        }
                    } else {
                        resultValues = new ComputedValues();

                        // TODO bytes/string interchangeability is unlikely to be correct - how exactly is it defined?
                        byte[] data =  argumentValues.values.get(0) instanceof StringValue?
                                ((StringValue) argumentValues.values.get(0)).getValue().getBytes("UTF-8"):
                                ((BytesValue) argumentValues.values.get(0)).getValue();

                        resultValues.values.add(evaluateKeccak256ForOne(data));
                    }
                } else if (interpreter != null) {
                    // This call can be interpreted - first evaluate function arguments
                    // TODO valueSetIndex distinction ever relevant here?
                    ArrayList<Value> arguments = evaluateFunctionCallArguments(environment,
                                                                                VariableEnvironment.NO_VALUE_SET_SELECTED,
                                                                                expression.getFunctionCallArguments());

                    if (functionCall.getInterpretationFunctionDefinition() != null) {
                        ASTFunctionDefinition calledFunction = functionCall.getInterpretationFunctionDefinition();
                        calledFunction.setInterpretationArguments(arguments);

                        Value returnValue = interpreter.interpretNode(calledFunction);
                        resultValues = new ComputedValues();
                        resultValues.values.add(returnValue); // TODO multi-value?

                        if (returnValue == null) {
                            throw new Exception("Null return value from interpretation");
                        }
                    } else if (functionCall.getInterpretationStructDefinition() != null) {
                        Value structValue =
                                new StructValue((ASTUserDefinedTypeName)expression.getType(),
                                                functionCall.getInterpretationStructDefinition(),
                                        arguments);
                        resultValues = new ComputedValues();
                        resultValues.values.add(structValue); // TODO multi-value?
                    } else {
                        throw new Exception("Call to unknown function object type in " + functionCall.toSolidityCode());
                    }
                } else {
                    throw new Exception("Attempt to evaluate unimplemented function " + identifier.getName());
                }
            } else {
                throw new Exception("Unsupported function call on non-identifier item");
            }
        } else if (expression.getCastExpressionType() != null) {
            ASTNode targetType = (ASTNode)expression.getCastExpressionType();
            Expression operand = expression.getFirstOperand();
            ComputedValues operandValues;

            operandValues = evaluate(environment, valueSetIndex, operand, reevaluating);

            if (Type.isSameType(null/*ast TODO */, operand.getType(), targetType)) {
                // No-op
                resultValues = operandValues;
            } else {
                resultValues = evaluateCastForAll(operandValues, operand.getType(), targetType);

                if (freeIntermediateResults) {
                    operand.setComputedValues(null);
                }
            }
        } else if (expression.getValue() != null) {
            if (expression.getValue() instanceof Variable) {
                ComputedValues result = new ComputedValues();

                result.sourceExpression = expression;

                if (valueSetIndex == VariableEnvironment.NO_VALUE_SET_SELECTED) {
                    if (environment.isRecordingChanges()) {
                        // Store a single value
                        Value v = environment.resolveVariableValueIncludingParentEnvironments(0, ((Variable) expression.getValue()).getName());
                        result.values.add(v);
                    } else {
                        // Resolve all variable values based on recorded profiling results
                        for (int i = 0; i < environment.getValueCount(); ++i) {
                            Value v = environment.resolveVariableValueIncludingParentEnvironments(i, ((Variable) expression.getValue()).getName());
                            //System.out.println("Resolved var " + ((Variable)expression.getValue()).getName());
                            //System.out.println("to " + v.toASTNode(false).toSolidityCode());
                            result.values.add(v);
                        }
                    }
                } else {
                    // Resolve current variable value based on recorded profiling results
                    Value v;
                    v = environment.resolveVariableValueIncludingParentEnvironments(valueSetIndex, ((Variable) expression.getValue()).getName());
                    //System.out.println("Resolved var " + ((Variable)expression.getValue()).getName());
                    //System.out.println("to " + v.toASTNode(false).toSolidityCode());
                    result.values.add(v);
                }

                resultValues = result;
            } else {
                // This is already a constant value
                Value value = expression.getValue();

                ComputedValues result = new ComputedValues();

                if (valueSetIndex == VariableEnvironment.NO_VALUE_SET_SELECTED) {
                    if (environment.isRecordingChanges()) {
                        // Store a single value (count = 1)
                        result.values.add(value);
                    } else {
                        // Duplicate the given value for each variable set instance
                        int valueCount = environment.getValueCount();
                        for (int i = 0; i < valueCount; ++i) {
                            result.values.add(value);
                        }
                    }
                } else {
                    result.values.add(value);
                }
                resultValues = result;
            }
        } else if (expression.getTupleComponents() != null) {
            // Tuple expression - evaluate all components
            ArrayList<Expression> components = expression.getTupleComponents();
            ArrayList<ComputedValues> computedTupleValues = new ArrayList<ComputedValues>();

            for (Expression component : components) {
                ComputedValues operandValues;
                operandValues = evaluate(environment, valueSetIndex, component, reevaluating); // TODO reevaluating dubious here
                computedTupleValues.add(operandValues);
            }
            // Combine sub items:
            // (a,b,c)                                       tuple-expression      Expression,Expression,Expression
            // --->
            // a: [a1,a2,...] b: [b1,b2,...] c: [c1,c2,...]  multi-value resuls    Value,Value,Value.... per Expression
            // --->
            // [   (a1,b1,c1), (a2, b2, c2), ...  ]          tuple multi-values    (Value,Value,Value...) per tuple-expr
            if (computedTupleValues.size() == 0 || computedTupleValues.get(0).values.size() == 0) {
                System.out.println("No evaluation results for " + expression.toASTNode().toSolidityCode());
                Util.unimpl();
            }

            resultValues = new ComputedValues();
            resultValues.sourceExpression = expression;

            for (int i = 0; i < computedTupleValues.get(0).values.size(); ++i) {
                ArrayList<Value> tupleValues = new ArrayList<Value>();
                for (int j = 0; j < computedTupleValues.size(); ++j) {
                    Value value = computedTupleValues.get(j).values.get(i);
                    tupleValues.add(value);
                }
                resultValues.values.add(new TupleValue(tupleValues, expression.getType()));
            }
        } else if (expression.getAssignmentOperator() != null) {
            if (expression.getAssignmentOperator() != ASTAssignment.Operator.OP_ASSIGN) {
                Util.unimpl();
            }

            // Evaluate LHS operand
            // TODO this needs to be done at the latest once we have arrays so the LHS may contain side effects. a[i++] = x; ...
            // Constructs like "f().x = y;" - with f() returning a contract reference - are rejected due to lvalue errors
            //     ComputedValues firstOperandValues;
            //     firstOperandValues = evaluate(environment, valueSetIndex, expression.getFirstOperand(), reevaluating);

            // Adapt RHS operand to include a conversion operation to the LHS operand's type
            Expression convertedRHS = new Expression(expression.getSecondOperand(), expression.getFirstOperand().getType());

            // Evaluate RHS operand
            ComputedValues secondOperandValues;
            secondOperandValues = evaluate(environment, valueSetIndex, convertedRHS, reevaluating);

            // Store assignment
            for (int i = 0; i < secondOperandValues.values.size(); ++i) {
                Value value = secondOperandValues.values.get(i);
                environment.updateVariableValueIncludingParentEnvironments(/*variable*/expression.getFirstOperand(), value); // TODO multi-item computations?
            }
            resultValues = secondOperandValues;
        } else if (expression.getNewContract() != null) {
            if (interpreter != null) {
                // This contract construction call can be interpreted - first evaluate constructor arguments
                ArrayList<Value> arguments = evaluateFunctionCallArguments(environment,
                        VariableEnvironment.NO_VALUE_SET_SELECTED,
                        expression.getFunctionCallArguments());
                ContractValue contractValue = new ContractValue(expression.getNewContract(), arguments);
                resultValues = new ComputedValues();
                resultValues.values.add(contractValue);
            } else {
                throw new Exception("Evaluating 'new' expression without interpreter");
            }
        } else {
            throw new Exception("ExpressionEvaluator.evaluate received malformed Expression");
        }

        if (valueSetIndex == VariableEnvironment.NO_VALUE_SET_SELECTED) {
            // Cache results for all-value evaluation
            expression.setComputedValues(resultValues);
        } else {
            // Cache result for single-value evaluation using the selected value set index
            ComputedValues existingValues = expression.getComputedValues();
            if (existingValues == null) {
                existingValues = new ComputedValues(environment.getValueCount());
                expression.setComputedValues(existingValues);
            }

            existingValues.values.set(valueSetIndex, resultValues.values.get(0));
        }

        if (traceExpressionId != -1 && expression.getID() == traceExpressionId) {
            System.out.println(" ========== eval " + traceExpressionId + " end ");
            environment.printLatestToLogger();

            System.out.println(" ========== final result " + resultValues.values.get(0).toASTNode(false).toSolidityCode());

            debugTrace = false;
        }

        if (debugTrace) {
            System.out.println("cur result " + expression.toASTNode().toSolidityCode() + " = " + resultValues.values.get(0).toASTNode(false).toSolidityCode());
        }

        return resultValues;
    }

    protected ArrayList<Value> evaluateFunctionCallArguments(VariableEnvironment environment,
                                                             int valueSetIndex,
                                                             ArrayList<Expression> argumentExpressions
                                                             /*boolean reevaluating ??? ... won't work so easily with a list*/) throws Exception {
        ArrayList<Value> values = new ArrayList<Value>();
        for (Expression expression : argumentExpressions) {
            ComputedValues computedValues = evaluate(environment,
                valueSetIndex,
                expression,
                false /* TODO proper fixup integration */);
            values.add(computedValues.values.get(0));
        }
        return values;
    }

    protected Value evaluateKeccak256ForOne(byte[] data /*StringValue input*/) throws Exception {
        byte[] hash = Hash.keccak256(data /*input.getValue().getBytes("UTF-8")*/);
        return new BytesValue(TypeContainer.getByteType(32), hash);
    }

    protected ComputedValues evaluateKeccak256ForAll(ComputedValues stringValues) throws Exception {
        ComputedValues result = new ComputedValues();
        for (Value value : stringValues.values) {
            // TODO bytes/string interchangeability is unlikely to be correct - how exactly is it defined?
            byte[] data = value instanceof StringValue?
                                ((StringValue)value).getValue().getBytes("UTF-8"):
                                ((BytesValue)value).getValue();
            Value bytesResult = evaluateKeccak256ForOne(data); //(StringValue)value);
            result.values.add(bytesResult);
        }
        return result;
    }

    protected ComputedValues evaluateCastForAll(ComputedValues operandValues,
                                                ASTNode sourceType,
                                                ASTNode targetType) throws Exception {
        ComputedValues result = new ComputedValues();

        for (Value value : operandValues.values) {
            if (Type.isIntegerType(sourceType) && Type.isIntegerType(targetType)) {
                Value convertedValue = ((IntegerValue) value).convertToIntegerType((ASTElementaryTypeName) targetType);
                result.values.add(convertedValue);
            } else if (Type.isByteType(sourceType) && Type.isByteType(targetType)) {
                Value convertedValue = ((BytesValue) value).convertToBytesType((ASTElementaryTypeName) targetType);
                result.values.add(convertedValue);
            } else if (Type.isByteType(sourceType) && Type.isIntegerType(targetType)) {
                Value convertedValue = ((BytesValue) value).convertToIntegerType((ASTElementaryTypeName) targetType);
                result.values.add(convertedValue);
            } else if (Type.isIntegerType(sourceType) && Type.isByteType(targetType)) {
                Value convertedValue = ((IntegerValue) value).convertToBytesType((ASTElementaryTypeName) targetType);
                result.values.add(convertedValue);
            } else if (Type.isIntegerType(sourceType) && Type.isAddressType(targetType)) {
                Value convertedValue = ((IntegerValue) value).convertToAddressType();
                result.values.add(convertedValue);
            } else if (Type.isStringType(sourceType)
                    && Type.isByteType(targetType)
                    && ((ASTElementaryTypeName)targetType).getBytes() == 0) {
                // bytes(string), typically for keccak256 arguments for now
                Value convertedValue = ((StringValue) value).convertToBytesType();
                result.values.add(convertedValue);
            } else {
                throw new Exception("Unimplemented cast from " + sourceType.toSolidityCode() + " to " + targetType.toSolidityCode());
            }
        }
        return result;
    }

    protected ComputedValues evaluateConditionalOperatorForAll(VariableEnvironment environment,
                                                               ComputedValues firstOperandValues,
                                                               Expression firstOperand,
                                                               Expression secondOperand,
                                                               Expression thirdOperand,
                                                               boolean reevaluating) throws Exception {
        ComputedValues result = new ComputedValues();

        for (int i = 0; i < firstOperandValues.values.size(); ++i) {
            Value conditionValue = firstOperandValues.values.get(0);
            if (!(conditionValue instanceof BoolValue)) {
                throw new Exception("Conditional operator without bool condition " + " operand "
                        + firstOperand.toASTNode().toSolidityCode()
                        + " value " + conditionValue.toASTNode(false).toSolidityCode()
                        + " type " + conditionValue.getType().toSolidityCode());
            }

            if (((BoolValue)conditionValue).getValue()) {
                // Take true branch - evaluate corresponding expression
                // (i = 0 if environment.isRecording() = true)
                ComputedValues secondOperandValues = evaluate(environment, i, secondOperand, reevaluating);
                result.values.add(secondOperandValues.values.get(0));
            } else {
                // Take false branch - evaluate corresponding branch expression
                // (i = 0 if environment.isRecording() = true)
                ComputedValues thirdOperandValues = evaluate(environment, i, thirdOperand, reevaluating);
                result.values.add(thirdOperandValues.values.get(0));
            }
        }
        return result;
    }

    protected ComputedValues evaluateMemberAccessForAll(ComputedValues operandValues,
                                                        ASTVariableDeclaration memberDeclaration) throws Exception {
        ComputedValues result = new ComputedValues();
        for (Value value : operandValues.values) {
            if (!(value instanceof StructValue)) {
                throw new Exception("Invalid member access to non-struct value");
            }
            StructValue structValue = (StructValue)value;

            Value memberValue = structValue.getMemberValue(memberDeclaration.getName());
            if (memberValue == null) {
                throw new Exception("Unknown struct member " + memberDeclaration.getName());
            }

            result.values.add(memberValue);
        }
        return result;
    }

    protected ComputedValues evaluateIndexAccessForAll(ComputedValues operandValues,
                                                       ComputedValues indexValues,
                                                       Expression debugIndexExpression) throws Exception {
        ComputedValues result = new ComputedValues();
        for (int i = 0; i < operandValues.values.size(); ++i) {
            if (!(operandValues.values.get(i) instanceof ArrayValue)) { // TODO Mappings
                throw new Exception("Invalid array subscript access to non-array value");
            }
            if (!(indexValues.values.get(i) instanceof IntegerValue)) {
                throw new Exception("Invalid non-integer array subscript");
            }

            ArrayValue arrayValue = (ArrayValue)operandValues.values.get(i);
            IntegerValue indexValue = (IntegerValue)indexValues.values.get(i);
            if (isNegativeInteger(indexValue) || indexValue.isGreaterThanOrEqualTo(arrayValue.length()).getValue()) {
                throw new ExpressionEvaluationException(debugIndexExpression,
                                    ExpressionEvaluationException.ExceptionType.ExceptionType_ValueError_ArrayBounds,
                                                        "Invalid array index out of bounds", indexValues);
            }

            Value elementValue = arrayValue.indexAccess(indexValue.toInt());
            result.values.add(elementValue);
        }
        return result;
    }

    protected ComputedValues evaluateBinaryOperatorForAll(VariableEnvironment environment,
                                                          ComputedValues firstOperands,
                                                          ASTBinaryOperation.Operator operator,
                                                          Expression secondOperandExpression,
                                                          int valueSetIndex,
                                                          int originalValueSetIndex,
                                                          boolean reevaluating) throws Exception {  //ComputedValues secondOperands) throws Exception {
        ComputedValues result = new ComputedValues();
        ComputedValues secondOperandValues;


        if (operator != OP_LAND && operator != OP_LOR) {
            // This is an operator for which we can evaluate the second operand unconditionally - and this is
            // desirable, since a complete rather than partial set of values may be beneficial when fixing up
            // invalid expressions
            //
            // TODO There's a problem here in that the evaluate() call may actually honor a valueSetIndex setting for
            // one specific element, but - like many other evaluation functions - the binary operator evaluation
            // below just ignores the valueSetIndex and casually processes all firstOperand items.
            // This failed in one case where:
            //    - Some subexpression evaluation worked on a single item due being embedded in a short-cirtuciting
            // operator
            //    - A division-by-zero problem for a divisor operand of x caused an ExpressionEvaluationErrorHandler
            // patch-up operation to change it to (x + constant)
            //    - The reevaluation step for the single item only loaded one value, but the loop below tried to
            // loop over multiple first operand items
            // Workaround for now: evaluate the additive constant for all items after creating it to ensure all
            // elements are available. Better: honor element selection correctly.
            //
            // Mixing single-item and all-item evaluations probably worked so far because assignment sequence
            // contracts (where evaluation semantics are a bit trickier due to side effect requirements) happen
            // to have 1 value item in total (single-item = all-item)
            //
            // TODO update: This should be mostly fixed now, but the valueSetIndex handling is incredibly
            // contrived and incomprehensible. Using originalValueSetIndex here is currently needed to ensure
            // that the same number of items is produced for the second operand if the first operand is a single-value
            // slice of a larger number of items. In that case, NO_VALUE_SET_SELECTED will be passed as valueSetIndex,
            // but it does NOT mean, in contrast to what some part of the code assume, that all values in the
            // environment are to be processed. Instead it means that all values in the slice (= 1 item) are
            // processed. For this reason index values would not be correct either when accessing the variable
            // environment. So this can only work by chance if variables have already been resolved, which may not
            // be possible for reevaluation
            //
            // TODO This probably fixes most or all "complete program" cases, but must still be tested for
            // "assignment sequence" programs, and there should at least be safeguards but preferably it should
            // all be cleaned up
            secondOperandValues = evaluate(environment, originalValueSetIndex,  //VariableEnvironment.NO_VALUE_SET_SELECTED,
                    secondOperandExpression,  reevaluating);
        } else {
            secondOperandValues = null;
        }


        int startIndex;
        int endIndex;

        if (valueSetIndex == VariableEnvironment.NO_VALUE_SET_SELECTED) {
            // Process all items
            startIndex = 0;
            endIndex = firstOperands.values.size();
        } else {
            // Process a single specified item
            startIndex = valueSetIndex;
            endIndex = startIndex + 1;
        }

        //for (int i = 0; i < firstOperands.values.size(); ++i) {
        for (int i = startIndex; i < endIndex; ++i) {
            // Support short-circuiting
            if (operator == OP_LAND && ((BoolValue)firstOperands.values.get(i)).getValue() == false) {
                // Result is already false - don't evaluate second operand
                result.values.add(ValueContainer.getBoolValue(false));
            } else if (operator == OP_LOR && ((BoolValue)firstOperands.values.get(i)).getValue() == true) {
                // Result is already true - don't evaluate second operand
                result.values.add(ValueContainer.getBoolValue(true));
            } else {
                if (secondOperandValues == null || secondOperandValues.values.get(i) == null) {
                    // Evaluate this single current second operand item before we can use its value
                    // (i = 0 for environment.isRecordingChanges() = true)
                    ComputedValues currentSecondOperandValue = evaluate(environment, i, secondOperandExpression, reevaluating);

                    result.values.add(evaluateBinaryOperator(firstOperands.values.get(i), operator, currentSecondOperandValue.values.get(0),
                                                            secondOperandValues, secondOperandExpression));
                } else {
                    result.values.add(evaluateBinaryOperator(firstOperands.values.get(i), operator, secondOperandValues.values.get(i),
                                                            secondOperandValues, secondOperandExpression));
                }
            }
        }

        return result;
    }

    protected boolean isNegativeInteger(IntegerValue value) throws Exception {
        return Type.isSignedIntegerType(value.getType())
            && value.isSmallerThan(ValueContainer.getSmallIntegerValue(value.getType(), 0)).getValue();
    }

    protected boolean isZeroInteger(IntegerValue value) throws Exception {
        return value.isEqualTo(ValueContainer.getSmallIntegerValue(value.getType(), 0)).getValue();
    }

    protected Value evaluateBinaryOperator(Value firstOperand,
                                           ASTBinaryOperation.Operator operator,
                                           Value secondOperand,
                                           ComputedValues debugAllSecondOperandValues,
                                           Expression debugSecondOperandExpression) throws Exception {

        if (firstOperand instanceof IntegerValue && secondOperand instanceof IntegerValue) {
            IntegerValue firstIntegerOperand = (IntegerValue)firstOperand;
            IntegerValue secondIntegerOperand = (IntegerValue)secondOperand;

            // Enforce same type for both operands, so as to simplify evaluation and force the caller to take type
            // conversion issues into account
            ASTElementaryTypeName firstIntegerType = (ASTElementaryTypeName)firstIntegerOperand.getType();
            ASTElementaryTypeName secondIntegerType = (ASTElementaryTypeName)secondIntegerOperand.getType();

            if (firstIntegerType.isSigned() != secondIntegerType.isSigned()) {
                throw new Exception("ExpressionEvaluator.evaluateBinaryOperator: Sign mismatch between "
                    + firstIntegerOperand.toASTNode(false).toSolidityCode() + " and "
                    + secondIntegerOperand.toASTNode(false).toSolidityCode() + " for " +  operator);
            }
            if (firstIntegerType.getBits() != secondIntegerType.getBits()) {
                throw new Exception("ExpressionEvaluator.evaluateBinaryOperator: Type bits mismatch between "
                        + firstIntegerOperand.toASTNode(false).toSolidityCode() + " and "
                        + secondIntegerOperand.toASTNode(false).toSolidityCode() + ": " +
                        + firstIntegerType.getBits() + " vs " + secondIntegerType.getBits());
            }

            switch (operator) {
                // Arithmetic
                case OP_PLUS: /* + */  return firstIntegerOperand.add(secondIntegerOperand);
                case OP_MINUS: /* - */ return firstIntegerOperand.sub(secondIntegerOperand);
                case OP_MUL: /* * */   return firstIntegerOperand.mul(secondIntegerOperand);
                case OP_DIV: /* / */
                    if (isZeroInteger(secondIntegerOperand)) {
                        throw new ExpressionEvaluationException(debugSecondOperandExpression,
                                                                ExceptionType_ValueError_ZeroDivision,
                                                                "Division with zero operand", debugAllSecondOperandValues);
                    }
                    return firstIntegerOperand.div(secondIntegerOperand);
                case OP_MOD: /* % */
                    if (isZeroInteger(secondIntegerOperand)) {
                        throw new ExpressionEvaluationException(debugSecondOperandExpression,
                                                                ExceptionType_ValueError_ZeroDivision /* same for / and % */,
                                                                "Modulo with zero operand", debugAllSecondOperandValues);
                    }
                    return firstIntegerOperand.mod(secondIntegerOperand);
                case OP_EXP: /* ** */
                    // Note: ExceptionType_TypeError_WrongSign has already been caught at this point (independently of
                    // values), operands should both be unsigned unsigned
                    if (isNegativeInteger(secondIntegerOperand)) { // TODO remove this - can't happen?
                        // Shift right is undefined for a negative second operand (but not for huge shift bit counts)
                        throw new ExpressionEvaluationException(debugSecondOperandExpression,
                                                                ExceptionType_ValueError_NegativeSecondOperand,
                                                                "Exponentiation with negative exponent",
                                                                debugAllSecondOperandValues);
                    }
                    return firstIntegerOperand.exp(secondIntegerOperand);
                // Logical
                case OP_EQ: /* == */   return firstIntegerOperand.isEqualTo(secondIntegerOperand);
                case OP_NEQ: /* != */  return firstIntegerOperand.isNotEqualTo(secondIntegerOperand);
                case OP_GT: /* > */    return firstIntegerOperand.isGreaterThan(secondIntegerOperand);
                case OP_LT: /* < */    return firstIntegerOperand.isSmallerThan(secondIntegerOperand);
                case OP_GTE: /* >= */  return firstIntegerOperand.isGreaterThanOrEqualTo(secondIntegerOperand);
                case OP_LTE: /* <= */  return firstIntegerOperand.isSmallerThanOrEqualTo(secondIntegerOperand);
                // Bitwise
                case OP_BOR: /* | */   return firstIntegerOperand.bitwiseOr(secondIntegerOperand);
                case OP_BAND: /* & */  return firstIntegerOperand.bitwiseAnd(secondIntegerOperand);
                case OP_BXOR: /* ^ */  return firstIntegerOperand.bitwiseXor(secondIntegerOperand);
                case OP_BSHL: /* << */
                    if (isNegativeInteger(secondIntegerOperand)) {
                        // Shift right is undefined for a negative second operand (but not for huge shift bit counts)
                        throw new ExpressionEvaluationException(debugSecondOperandExpression,
                                                                ExceptionType_ValueError_NegativeSecondOperand,
                                                                "Shift left with negative value "
                                                                        + debugSecondOperandExpression.toASTNode().toSolidityCode() + " giving " + secondIntegerOperand.toASTNode(false).toSolidityCode()
                                                                , debugAllSecondOperandValues);
                    }

                    return firstIntegerOperand.bitwiseShiftLeft(secondIntegerOperand);
                case OP_BSHR: /* >> */
                    if (isNegativeInteger(secondIntegerOperand)) {
                        // Shift right is undefined for a negative second operand (but not for huge shift bit counts)
                        throw new ExpressionEvaluationException(debugSecondOperandExpression,
                                                                ExceptionType_ValueError_NegativeSecondOperand,
                                                                "Shift right with negative value",
                                                                debugAllSecondOperandValues);
                    }
                    return firstIntegerOperand.bitwiseShiftRight(secondIntegerOperand);
                default:  throw new Exception("Unexpected binary operator " + operator + " for integer operands");
            }
        } else if (firstOperand instanceof BoolValue && secondOperand instanceof BoolValue) {
            BoolValue firstBoolOperand = (BoolValue) firstOperand;
            BoolValue secondBoolOperand = (BoolValue) secondOperand;

            switch (operator) {
                // Logical
                case OP_EQ: /* == */   return firstBoolOperand.isEqualTo(secondBoolOperand);
                case OP_NEQ: /* != */  return firstBoolOperand.isNotEqualTo(secondBoolOperand);
                case OP_GT: /* > */    return firstBoolOperand.isGreaterThan(secondBoolOperand);
                case OP_LT: /* < */    return firstBoolOperand.isSmallerThan(secondBoolOperand);
                case OP_GTE: /* >= */  return firstBoolOperand.isGreaterThanOrEqualTo(secondBoolOperand);
                case OP_LTE: /* <= */  return firstBoolOperand.isSmallerThanOrEqualTo(secondBoolOperand);
                case OP_LOR: /* || */  return firstBoolOperand.logicalOr(secondBoolOperand);
                case OP_LAND: /* && */ return firstBoolOperand.logicalAnd(secondBoolOperand);
                default:
                    throw new Exception("Unexpected binary operator " + operator + " for bool operands");
            }
        } else if (firstOperand instanceof BytesValue && secondOperand instanceof IntegerValue) {
            BytesValue firstBytesOperand = (BytesValue) firstOperand;
            IntegerValue secondIntegerOperand = (IntegerValue) secondOperand;

            switch (operator) {
                case OP_BSHL:
                    if (isNegativeInteger(secondIntegerOperand)) {
                        // Shift right is undefined for a negative second operand (but not for huge shift bit counts)
                        throw new ExpressionEvaluationException(debugSecondOperandExpression,
                                                        ExceptionType_ValueError_NegativeSecondOperand,
                                                        "Shift left with negative value"
                                                                + debugSecondOperandExpression.toASTNode().toSolidityCode() + " giving " + secondIntegerOperand.toASTNode(false).toSolidityCode()
                                                        , debugAllSecondOperandValues);
                    }
                    return firstBytesOperand.bitwiseShiftLeft(secondIntegerOperand);
                case OP_BSHR:
                    if (isNegativeInteger(secondIntegerOperand)) {
                        // Shift right is undefined for a negative second operand (but not for huge shift bit counts)
                        throw new ExpressionEvaluationException(debugSecondOperandExpression,
                                                        ExceptionType_ValueError_NegativeSecondOperand,
                                                        "Shift right with negative value", debugAllSecondOperandValues);
                    }
                    return firstBytesOperand.bitwiseShiftRight(secondIntegerOperand);
                default: throw new Exception("Unexpected binary operator " + operator + " for bytes operands");
            }
        } else if (firstOperand instanceof BytesValue && secondOperand instanceof BytesValue) {
            BytesValue firstBytesOperand = (BytesValue)firstOperand;
            BytesValue secondBytesOperand = (BytesValue)secondOperand;

            if (firstBytesOperand.getBytesCount() != secondBytesOperand.getBytesCount()) {
                throw new Exception("ExpressionEvaluator.evaluateBinaryOperator: Bytes operand size mismatch between "
                        + firstBytesOperand.toASTNode(false).toSolidityCode() + " and "
                        + secondBytesOperand.toASTNode(false).toSolidityCode()
                        + " " + firstBytesOperand.getBytesCount() + " vs " + secondBytesOperand.getBytesCount());
            }
            switch (operator) {
                case OP_EQ:  return firstBytesOperand.isEqualTo(secondBytesOperand);
                case OP_NEQ: return firstBytesOperand.isNotEqualTo(secondBytesOperand);
                case OP_LT:  return firstBytesOperand.isSmallerThan(secondBytesOperand);
                case OP_LTE: return firstBytesOperand.isSmallerThanOrEqualTo(secondBytesOperand);
                case OP_GT:  return firstBytesOperand.isGreaterThan(secondBytesOperand);
                case OP_GTE: return firstBytesOperand.isGreaterThanOrEqualTo(secondBytesOperand);

                case OP_BOR:  return firstBytesOperand.bitwiseOr(secondBytesOperand);
                case OP_BAND: return firstBytesOperand.bitwiseAnd(secondBytesOperand);
                case OP_BXOR: return firstBytesOperand.bitwiseXor(secondBytesOperand);

                default: throw new Exception("Unexpected binary operator " + operator + " for bytes operands");
            }
        } else if (firstOperand instanceof AddressValue && secondOperand instanceof AddressValue) {
            AddressValue firstAddressOperand = (AddressValue)firstOperand;
            AddressValue secondAddressOperand = (AddressValue)secondOperand;
            switch (operator) {
                case OP_EQ:   return firstAddressOperand.isEqualTo(secondAddressOperand);
                case OP_NEQ:  return firstAddressOperand.isNotEqualTo(secondAddressOperand);
                default: throw new Exception("Unexpected binary operator " + operator + " for address operands");
            }
        } else if (firstOperand instanceof StringValue && secondOperand instanceof StringValue) {
            StringValue firstStringOperand = (StringValue)firstOperand;
            StringValue secondStringOperand = (StringValue)secondOperand;
            switch (operator) {
                default: throw new Exception("Unexpected binary operator " + operator + " for string operands in "
                            + firstOperand.toString() + " " + operator + " " + secondOperand.toString());
            }
        } else if (firstOperand instanceof EnumValue && secondOperand instanceof EnumValue) {
            switch (operator) {
                default: throw new Exception("Unexpected binary operator " + operator + " for enum operands");
            }
        } else if (firstOperand instanceof StructValue && secondOperand instanceof StructValue) {
            switch (operator) {
                default: throw new Exception("Unexpected binary operator " + operator + " for struct operands");
            }
        } else if (firstOperand instanceof ArrayValue && secondOperand instanceof ArrayValue) {
            switch (operator) {
                default: throw new Exception("Unexpected binary operator " + operator + " for array operands");
            }
        } else {
            throw new Exception("Unexpected operand type combination "
                    + firstOperand.getType().toSolidityCode()
                    + " (" + firstOperand.toASTNode(false).toSolidityCode() + ")"
                    + " and " + secondOperand.getType().toSolidityCode() +
                    " (" + secondOperand.toASTNode(false).toSolidityCode() + ")"
                    + " for operator " + operator);
        }
    }

    protected ComputedValues evaluateUnaryOperatorForAll(VariableEnvironment environment,
                                                         ASTUnaryOperation.Operator operator,
                                                         ComputedValues operands,
                                                         Expression debugOperandExpression) throws Exception {
        ComputedValues result = new ComputedValues();
        for (Value value : operands.values) {
            result.values.add( evaluateUnaryOperator(environment, operator, value, operands.sourceExpression, debugOperandExpression) );
        }
        return result;
    }

    protected Value evaluateUnaryOperator(VariableEnvironment environment,
                                          ASTUnaryOperation.Operator operator,
                                          Value operand,
                                          Expression operandSourceExpression, // source value (without intermediate unary ops)
                                          Expression debugOperandExpression   // current operator's operand expresson (including possible sub-ops)
                                    ) throws Exception {
        if (operand instanceof BoolValue) {
            BoolValue boolValue = (BoolValue) operand;
            if (operator == ASTUnaryOperation.Operator.OP_LNEG) {
                return ValueContainer.getBoolValue(!boolValue.getValue());
            } else {
                throw new Exception("Unsupported operator " + operator + " for bool value in " + debugOperandExpression.toASTNode().toSolidityCode());
            }
        } else if (operand instanceof IntegerValue) {
            IntegerValue integerValue = (IntegerValue) operand;

            // Note: For all increment/decrement operators, signed over- and underflow wraps around rather
            // than causing an EVM exception
            //
            // Reflect variable changes in variable environment, if needed
            //
            // TODO: Fix commit issues with inc/dec side effects. Known issues:
            //   1. Generated expressions are discarded during expression construction, thereby invalidating any
            // potential side effects that would currently still be recorded below
            //   2. Conditional operator branches that are not executed
            //   3. Logical and/or branches that are not executed
            // Some sort of final commit operation is needed.
            if (operator == ASTUnaryOperation.Operator.OP_DEC_POST
                    || operator == ASTUnaryOperation.Operator.OP_INC_POST) {
                if (environment.isRecordingChanges()) {
                    IntegerValue alteredValue = operator == OP_DEC_POST
                            ? integerValue.sub(ValueContainer.getSmallIntegerValue(integerValue.getType(), 1))
                                : integerValue.add(ValueContainer.getSmallIntegerValue(integerValue.getType(), 1));
                    // Update side effect on operand expression 
                    environment.updateVariableValueIncludingParentEnvironments(operandSourceExpression, alteredValue);
                }
                // TODO The variable change would need to be reflected in the environment for full interpretation
                return integerValue;
            } else if (operator == ASTUnaryOperation.Operator.OP_DEC_PRE) {
                Value newValue = integerValue.sub(ValueContainer.getSmallIntegerValue(integerValue.getType(), 1));
                if (environment.isRecordingChanges()) {
                    // Update side effect on operand expression 
                    environment.updateVariableValueIncludingParentEnvironments(operandSourceExpression, newValue);
                }
                return newValue;
            } else if (operator == ASTUnaryOperation.Operator.OP_INC_PRE) {
                Value newValue = integerValue.add(ValueContainer.getSmallIntegerValue(integerValue.getType(), 1));
                if (environment.isRecordingChanges()) {
                    // Update side effect on operand expression 
                    environment.updateVariableValueIncludingParentEnvironments(operandSourceExpression, newValue);
                }
                return newValue;
            } else if (operator == ASTUnaryOperation.Operator.OP_MINUS) {
                return integerValue.negate();
            } else if (operator == OP_BNEG) {
                return integerValue.negateBitwise();
            } else if (operator == ASTUnaryOperation.Operator.OP_PLUS) {
                return integerValue; // no-op
            } else {
                throw new Exception("ExpressionEvaluator.evaluateUnaryOperator: Unsupported unary operator "
                        + operator + " for " + operand.toASTNode(false).toSolidityCode()
                        + " of type " + operand.getType().toSolidityCode());
            }
        } else if (operand instanceof BytesValue) {
            BytesValue bytesValue = (BytesValue)operand;
            if (operator == OP_BNEG) {
                return bytesValue.negateBitwise();
            } else {
                throw new Exception("ExpressionEvaluator.evaluateUnaryOperator: Unsupported unary operator "
                        + operator + " for " + operand.toASTNode(false).toSolidityCode()
                        + " of type " + operand.getType().toSolidityCode());
            }
        } else {
            throw new Exception("ExpressionEvaluator.evaluateUnaryOperator: Unary operator with unexpected operand type " + operand.getClass().toString());
        }
    }
}
