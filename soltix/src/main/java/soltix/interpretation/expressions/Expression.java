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

import soltix.ast.*;
import soltix.interpretation.Type;
import soltix.interpretation.values.Value;
import soltix.interpretation.variables.Variable;
import soltix.synthesis.ExpressionGenerator;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

/**
 * Class to represent an expression in a format more suitable to work with than ASTNode items
 */
public class Expression {
    static private int idCounter = 0;
    private int id = -1;

    public int getID() { return id; }
    public void assignID() { id = ++idCounter; }

    // A code snippet that can be prefixed to code generated (via toASTNode()) for this expression
    // This allows for the insertion of e.g. value checks:
    //     if (subexpr1 != expectedValue1) throw Exception;
    //     if (subexpr2 != expectedValue2) throw Exception;
    //     if ((subexpr1 <op> subexpr2>) != expectedValue3) throw Exception;
    //     subexpr1 <op> subexpr2
    // ... where the first three checks were embedded as debugPrefixCode snippets in the Expression
    // and sub-Expressions of "subexpr1 <op> subexpr2".

    private String debugPrefixCode = "";

    // Operators - non-null if used and mutually exclusive
    private ASTBinaryOperation.Operator binaryOperator = null; // Binary computation
    private ASTAssignment.Operator assignmentOperator = null; // Binary assignment
    private ASTUnaryOperation.Operator unaryOperator = null; // Unary computation
    private boolean conditionalOperator = false;  // Conditional operator
    private ASTVariableDeclaration memberAccess = null;  // Struct member access
    private Expression indexAccess = null; // Array or mapping index access (ASTIndexAccess isn't really useful here)
    private ASTFunctionCall functionCall = null; // Function call
    private ASTNode castExpressionType = null; // Cast-to type
    private ArrayList<Expression> functionCallArguments = null; // ... function call arguments

    // Operator operands - non-null if used
    private Expression firstOperand = null;
    private Expression secondOperand = null;
    private Expression thirdOperand = null; // for conditional operator

    private boolean containsExpressionAlias = false;

    protected void markContained(ArrayList<Expression> checkContained, Expression expression) {
        if (checkContained != null && checkContained.contains(expression)) {
            checkContained.remove(expression);
        }
    }

    public String debugIntermediateValuesString(ArrayList<Expression> checkContained) throws Exception {
        String result = "";
        markContained(checkContained, this);
        if (conditionalOperator) {
            result += "(";

            result += firstOperand.debugIntermediateValuesString(checkContained);
            markContained(checkContained, firstOperand);
            result += "? ";
            result += secondOperand.debugIntermediateValuesString(checkContained);
            markContained(checkContained, secondOperand);
            result += ": ";
            result += thirdOperand.debugIntermediateValuesString(checkContained);
            markContained(checkContained, thirdOperand);
            result += ")";
        } else if (binaryOperator != null) {
            result += "(";
            result += firstOperand.debugIntermediateValuesString(checkContained);
            markContained(checkContained, firstOperand);
            result += binaryOperator;
            result += secondOperand.debugIntermediateValuesString(checkContained);
            markContained(checkContained, secondOperand);
            result += ")";

            if (binaryOperator == ASTBinaryOperation.Operator.OP_BSHR) {
                System.out.println("binary shift right applied to "
                    + (firstOperand.getComputedValues() != null
                            ? firstOperand.getComputedValues().values.get(0).toASTNode(false).toSolidityCode()
                                : "unevaluated")
                    + " and "
                    + (secondOperand.getComputedValues() != null
                            ? secondOperand.getComputedValues().values.get(0).toASTNode(false).toSolidityCode()
                                : "unevaluated")
                        + " result = "
                        + (getComputedValues() != null
                                ? getComputedValues().values.get(0).toASTNode(false).toSolidityCode()
                                    : "unevaluated"));
                System.out.println("expr code " + toASTNode().toSolidityCode());
            }
        } else if (unaryOperator != null) {
            result = "(";
            result += unaryOperator;

            System.out.println("unary application of " + unaryOperator + " to "
                            + (firstOperand.getComputedValues() != null
                                ? firstOperand.getComputedValues().values.get(0).toASTNode(false).toSolidityCode()
                                    : "unevaluated")
                            + " result = "
                            + (getComputedValues() != null
                                ? getComputedValues().values.get(0).toASTNode(false).toSolidityCode()
                                    : "unevaluated"));
            System.out.println("expr code " + toASTNode().toSolidityCode());
            result += firstOperand.debugIntermediateValuesString(checkContained);
            markContained(checkContained, firstOperand);
            result += ")";
        } else if (castExpressionType != null) {
            result += "(" + castExpressionType.toSolidityCode() + "(" + firstOperand.debugIntermediateValuesString(checkContained) + "))";
            markContained(checkContained, firstOperand);
        } else if (getValue() != null) {
            result += getValue().toASTNode(false).toSolidityCode();
            if (getValue() instanceof Variable) {
                System.out.println("   variable " +
                        getValue().toASTNode(false).toSolidityCode() +
                        " = "
                        + (getComputedValues() != null ? getComputedValues().values.get(0).toASTNode(false).toSolidityCode() : "unevaluated"));
            }
        } else if (memberAccess != null) {
            result += "(" + firstOperand.debugIntermediateValuesString(checkContained) + "." + memberAccess.toSolidityCode() + ")";
            markContained(checkContained, firstOperand);
        } else {
            assignOutputAlias(null);
            throw new Exception("debugIntermediateValuesString: Unimplemented operation in expression " + toASTNode().toSolidityCode());
        }

        result += "[";
        if (getComputedValues() != null) {
            result += getComputedValues().values.get(0).toASTNode(false).toSolidityCode();
        } else {
            result += "unevaluated";
        }

        result += "]";
        return result;
    }


    // Value item (constant value or Variable) if non-null and mutually exclusive with operators above
    private Value value = null;

    // The expression's type, regardless of its structure
    private ASTNode type;

    // Computed values for this whole expression for a set of recorded variable value sets
    // Stored here for incremental evaluation
    private /*ArrayList<Value>*/ExpressionEvaluator.ComputedValues computedValues;
    //private ArrayList<Expression> computedUndoExpressions;


    public ASTNode getType() { return type; }

    public ASTBinaryOperation.Operator getBinaryOperator() { return binaryOperator; }
    public ASTUnaryOperation.Operator getUnaryOperator() { return unaryOperator; }
    public Expression getFirstOperand() { return firstOperand; }
    public Expression getSecondOperand() { return secondOperand; }
    public Expression getThirdOperand() { return thirdOperand; }
    public Value getValue() { return value; }

    public boolean getConditionalOperator() { return conditionalOperator; }
    public ASTVariableDeclaration getMemberAccess() { return memberAccess; }
    public Expression getIndexAccess() { return indexAccess; }
    public ASTFunctionCall getFunctionCall() { return functionCall; }
    public ASTNode getCastExpressionType() { return castExpressionType; }
    public ArrayList<Expression> getFunctionCallArguments() { return functionCallArguments; }

    public /*ArrayList<Value>*/ExpressionEvaluator.ComputedValues getComputedValues() { return computedValues; }
    public void setComputedValues(/*ArrayList<Value>*/ExpressionEvaluator.ComputedValues computedValues) { this.computedValues = computedValues; }

    public void addDebugPrefixCode(String debugPrefixCode) {
        this.debugPrefixCode += debugPrefixCode;
    }
    public String getDebugPrefixCode() { return debugPrefixCode; }

    // Replace the existing second operand expression with a new one to adapt expressions for evaluation errors.
    // The new expression is assumed to be well-formed and of the correct type
    public void updateSecondOperand(Expression secondOperand) throws Exception {
        if (!Type.isSameType(null, secondOperand.getType(), secondOperand.getType())) {
            throw new Exception("Exception.updateSecondOperand received argument with unexpected type "
                    + secondOperand.getType().toSolidityCode());
        }
        this.secondOperand = secondOperand;
        containsExpressionAlias = containsExpressionAlias || secondOperand.containsExpressionAlias;
    }

    protected void inferTypeFromOperands(Expression firstOperand, Expression secondOperand) throws Exception {
        if (firstOperand.getType() instanceof ASTElementaryTypeName
                && ((ASTElementaryTypeName)firstOperand.getType()).isIntegerType()) {
            // Infer compatible integer type
            
            /*System.out.println("first  operand = " + firstOperand.toASTNode().toSolidityCode());
            System.out.println("    and type   = " + firstOperand.getType().toSolidityCode());
            System.out.println("second operand = " + secondOperand.toASTNode().toSolidityCode());
            System.out.println("    and type   = " + secondOperand.getType().toSolidityCode());*/
            type = Type.getCompatibleIntegerType(firstOperand.getType(), secondOperand.getType());
        } else {
            // Must be the same type for both sides
            type = firstOperand.getType();
        }
    }

    public Expression(Expression firstOperand, ASTBinaryOperation.Operator operator, Expression secondOperand) throws Exception {
        this.firstOperand = firstOperand;
        this.binaryOperator = operator;
        this.secondOperand = secondOperand;
        if (operator.yieldsBoolResult()) {
            // TODO merge this with BoolValue.getType() and/or put a cached instance to ASTElementaryTypeName
            type = new ASTElementaryTypeName(0, "bool");
        } else {
            inferTypeFromOperands(firstOperand, secondOperand);
        }
        debugPrefixCode = firstOperand.getDebugPrefixCode() + " " + secondOperand.getDebugPrefixCode();
        containsExpressionAlias = firstOperand.containsExpressionAlias || secondOperand.containsExpressionAlias;
    }

    public Expression(Expression firstOperand, ASTAssignment.Operator operator, Expression secondOperand) {
        this.firstOperand = firstOperand;
        this.assignmentOperator = operator;
        this.secondOperand = secondOperand;
        type = firstOperand.getType();
        debugPrefixCode = firstOperand.getDebugPrefixCode() + " " + secondOperand.getDebugPrefixCode() + " " + thirdOperand.getDebugPrefixCode();
        containsExpressionAlias = firstOperand.containsExpressionAlias || secondOperand.containsExpressionAlias;
    }

    public Expression(Expression firstOperand, Expression secondOperand, Expression thirdOperand) throws Exception {
        this.firstOperand = firstOperand;
        this.secondOperand = secondOperand;
        this.thirdOperand = thirdOperand;
        conditionalOperator = true;
        // The conditional operator requires compatible types for its second and third operand
        inferTypeFromOperands(secondOperand, thirdOperand);
        debugPrefixCode = firstOperand.getDebugPrefixCode() + " " + secondOperand.getDebugPrefixCode() + " " + thirdOperand.getDebugPrefixCode();
        containsExpressionAlias = firstOperand.containsExpressionAlias
                                || secondOperand.containsExpressionAlias
                                || thirdOperand.containsExpressionAlias;
    }

    public Expression(ASTUnaryOperation.Operator operator, Expression operand) {
        this.unaryOperator = operator;
        this.firstOperand = operand;
        // All unary operators preserve the type of the operand
        this.type = operand.getType();
        debugPrefixCode = operand.getDebugPrefixCode();
        containsExpressionAlias = operand.containsExpressionAlias;
    }

    public Expression(Expression operand, ASTVariableDeclaration memberAccess) {
        this.firstOperand = operand;
        this.memberAccess = memberAccess;
        this.type = memberAccess.getTypeName();
        debugPrefixCode = operand.getDebugPrefixCode();
        containsExpressionAlias = operand.containsExpressionAlias;
    }

    public Expression(Expression operand, Expression indexAccess) throws Exception {
        this.firstOperand = operand;
        this.indexAccess = indexAccess;
        if (!(operand.getType() instanceof ASTArrayTypeName)) { // TODO mappings?
            throw new Exception("Expression constructor: Index access applied to non-array expression");
        }
        ASTArrayTypeName arrayType = (ASTArrayTypeName)operand.getType();
        this.type = arrayType.getBaseType();
        debugPrefixCode = operand.getDebugPrefixCode();
        containsExpressionAlias = operand.containsExpressionAlias;
    }

    public Expression(ASTFunctionCall functionCall, ArrayList<Expression> arguments, ASTNode returnType) throws Exception {
        this.functionCall = functionCall;
        this.functionCallArguments = arguments;
        this.type = returnType;
        for (Expression e: arguments) {
            if (e.containsExpressionAlias) {
                containsExpressionAlias = true;
                break;
            }
        }
    }

    // Special call expression:
    // Simulates a function call for code output, but really makes the expression evaluator work on some other supplied
    // expression - an "expression alias" - instead to evaluate the function call.
    private Expression expressionAlias = null;
    public Expression(ASTFunctionCall functionCall, ArrayList<Expression> arguments, ASTNode returnType,
                      Expression expressionAlias) throws Exception {
        this(functionCall, arguments, returnType);
        this.expressionAlias = expressionAlias;
        containsExpressionAlias = true;
    }
    public Expression getExpressionAlias() { return expressionAlias; }
    public boolean getContainsExpressionAlias() { return containsExpressionAlias; }

    // Related to the constructor above, the opposite direction: A code output alias rather than an evaluation alias
    private String outputAlias = null;
    public void assignOutputAlias(String outputAlias) {
        this.outputAlias = outputAlias;
    }
    public String getOutputAlias() {
        return outputAlias;
    }



    public Expression(Expression operand, ASTNode castExpressionType) {
        this.firstOperand = operand;
        this.castExpressionType = castExpressionType;
        this.type = castExpressionType;
        debugPrefixCode = operand.getDebugPrefixCode();
        containsExpressionAlias = operand.containsExpressionAlias;
    }

    public Expression(Value value) throws Exception {
        this.value = value;
        this.type = value.getType();
    }


    public ASTNode toASTNode() throws Exception {
        return toASTNode(null);
    }

    public ASTNode toASTNode(ArrayList<Expression> undoExpressions) throws Exception { // TODO undoExpressions stuff isn't used anymore?! Remove it
        if (outputAlias != null) {
            // Override expression with some alias, e.g. output "arg1" instead of "x + y"
            return new ASTVerbatimText(0, outputAlias);
        }
        if (expressionAlias != null) {
            // This must be a function call with some arguments.
            // This isn't handled consistently here yet - ASTFunctionCall works on ASTNode items internally, but
            // in this instance we have Expression items as arguments instead. We use this knowledge to generate
            // the call ad-hoc
            if (functionCall == null) {
                throw new Exception("Expression.toASTNode invalid combination of functionCall and expressionAlias");
            }


            ASTIdentifier callee = (ASTIdentifier)functionCall.getCalled();
            String callCode = callee.toSolidityCode() + "(";

            for (int i = 0; i < functionCallArguments.size(); ++i) {
                Expression argument = functionCallArguments.get(i);
                if (i > 0) {
                    callCode += ", ";
                }

                // We also need to temporarily remove the alias
                String oldAlias = argument.getOutputAlias();
                if (oldAlias == null) {
                    throw new Exception("Expression.toASTNode invalid expressionAlias function argument without alias for " + argument.toASTNode().toSolidityCode());
                }
                argument.assignOutputAlias(null);
                callCode += argument.toASTNode().toSolidityCode();
                argument.assignOutputAlias(oldAlias);
            }
            callCode += ")";
            return new ASTVerbatimText(0, callCode);
        }

        ASTNode result;

        if (binaryOperator != null) {
            String resultString;
            resultString = "(" +
                    firstOperand.toASTNode(undoExpressions).toSolidityCode() + " " + binaryOperator.toString() + " " + secondOperand.toASTNode(undoExpressions).toSolidityCode()
                    + ")";
            result = new ASTVerbatimText(0, resultString);
        } else if (assignmentOperator != null) {
            String resultString;
            resultString = "(" +
                    firstOperand.toASTNode(undoExpressions).toSolidityCode() + " " + assignmentOperator.toString() + " " + secondOperand.toASTNode(undoExpressions).toSolidityCode()
                    + ")";
            result = new ASTVerbatimText(0, resultString);
        } else if (conditionalOperator) {
            String resultString;
            resultString = "(" +
                    firstOperand.toASTNode(undoExpressions).toSolidityCode() + "? " +
                    secondOperand.toASTNode(undoExpressions).toSolidityCode() + ": " +
                    thirdOperand.toASTNode(undoExpressions).toSolidityCode()
                    + ")";
            result = new ASTVerbatimText(0, resultString);
        } else if (unaryOperator != null) {
            // Note: unary operators must also be enclosed between parentheses for cases like "---var", where a maximum
            // bite parses this as "--(-var)" when "-(--var)" was really intended
            String resultString = "(";
            if (unaryOperator.isPrefix()) {
                resultString += unaryOperator.toString();
                if (unaryOperator == ASTUnaryOperation.Operator.OP_DELETE) {
                    resultString += " ";
                }
            }
            resultString += firstOperand.toASTNode(undoExpressions).toSolidityCode();
            if (!unaryOperator.isPrefix()) {
                resultString += unaryOperator.toString();
            }
            resultString += ")";
            result = new ASTVerbatimText(0, resultString);

            // If requested, and if this expression has a side effect (--/++), build an "undo" expression
            if (undoExpressions != null) {
                ASTUnaryOperation.Operator undoOperator = null;
                if (unaryOperator == ASTUnaryOperation.Operator.OP_INC_PRE
                        || unaryOperator == ASTUnaryOperation.Operator.OP_INC_POST) {
                    undoOperator = ASTUnaryOperation.Operator.OP_DEC_PRE;
                } else if (unaryOperator == ASTUnaryOperation.Operator.OP_DEC_PRE
                        || unaryOperator == ASTUnaryOperation.Operator.OP_DEC_POST) {
                    undoOperator = ASTUnaryOperation.Operator.OP_INC_PRE;
                }
                if (undoOperator != null) {
                    Expression undoExpression = new Expression(undoOperator, firstOperand); // Note: not evaluated
                    undoExpressions.add(undoExpression);
                }
            }
        } else if (memberAccess != null) {
            String resultString;
            resultString = firstOperand.toASTNode(undoExpressions).toSolidityCode() + "." + memberAccess.getName();
            result = new ASTVerbatimText(0, resultString);
        } else if (indexAccess != null) {
            String resultString;
            resultString = firstOperand.toASTNode(undoExpressions).toSolidityCode() + "[" + indexAccess.toASTNode(undoExpressions).toSolidityCode() + "]";
            result = new ASTVerbatimText(0, resultString);
        } else if (functionCall != null) {
            if (functionCall.getCalled() instanceof ASTIdentifier
                    && ((ASTIdentifier)functionCall.getCalled()).getName().equals("keccak256")
                && (functionCall.getArguments() == null || functionCall.getArguments().size() == 0)) {
		// We can only now produce the argument AST node for the function call, because the argument expression
		// evaluation may have resulted in fix-up operations, e.g. for division by zero.
		// Apparent 0.5.x change: keccak256 only now(?!) requires "bytes" argument, so we convert to that first
		// TODO proper modeling of "bytes" type
		// TODO not passing "undoExpressions", thereby introducing inc/dec bugs, is an easy mistake to make
                functionCall.addArgumentNode(new ASTVerbatimText(0, "bytes(" + functionCallArguments.get(0).toASTNode(undoExpressions).toSolidityCode() + ")"));
            }
            result = new ASTVerbatimText(0, functionCall.toSolidityCode());
        } else if (castExpressionType != null) {
            result = new ASTVerbatimText(0, castExpressionType.toSolidityCode() + "(" + firstOperand.toASTNode(undoExpressions).toSolidityCode() + ")");
        } else {
            result = value.toASTNode(false);
        }
        if (true && id != -1) {
            // For debugging: Output expression ID as a comment prefix as well. This way we can 
            // trace faulty expressions, e.g. in the expression evalautor
            // Note that this changes the md5 sum of the contract, but not its non-comment code
            String resultString = "//" + id + "\n" + result.toSolidityCode();
            result = new ASTVerbatimText(0, resultString);
        }
        return result;
    }
}
