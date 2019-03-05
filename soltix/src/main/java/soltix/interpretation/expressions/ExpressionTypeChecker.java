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

import soltix.Configuration;
import soltix.ast.AST;
import soltix.ast.ASTBinaryOperation;
import soltix.ast.ASTUnaryOperation;
import soltix.interpretation.Type;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;

import static soltix.ast.ASTBinaryOperation.Operator.OP_EXP;

/**
 * Class to check whether a proposed operator can be applied to the given argument expression(s)
 */
public class ExpressionTypeChecker {
    private AST ast;

    public ExpressionTypeChecker(AST ast) {
        this.ast = ast;
    }

    // TODO Other operations not covered here: function calls, array and mapping subscripts
    public boolean isUndefinedUnaryOperation(VariableEnvironment environment, Expression operand, ASTUnaryOperation.Operator operator) throws Exception {

        if (Type.isVariableByteType(operand.getType())) {
            // The "bytes" type can't do anything apart from subscript?!
            return true; // undefined
        }

        // TODO: implement this
        // TODO: 1. represent const'ness and/or lvalue/rvalue type info 2. use it to e.g. avoid ++/-- on const identifiers
        switch (operator) {
            case OP_DEC_POST:
            case OP_DEC_PRE:
            case OP_INC_POST:
            case OP_INC_PRE:
                if (Configuration.avoidGeneratingSubexpressionSideEffects) {
                    return true; // we don't want side effects - undefined
                }
                // These operators may only be applied to modifiable (non-costant) variables or array subscripts on
                // a modifiable object
                // TODO Address this properly for all types. We currently only allow non-const variables.
                // TODO Other constellations: array elements (single- or multi-dimensional) standalone or in structs
                // Note that const'ness does not appear to be a property that is generally expressible and propagated
                // in the type system - it may only be bound to variable declarations
            {
                // Try to associate the expression to which this operator is applied with a variable. If we can't
                // do this, the item is inappropriate (e.g. an integer literal) or a currently unsupported operation like
                // an array subscript is involved
                Variable variable = environment.tryLookupVariableByExpression(operand);

                if (variable == null) {
                    return true; // NOT defined
                }
                if (!Type.isIntegerType(operand.getType())) {
                    return true; // NOT defined
                }
                // TODO check variable environment to catch overflows
                return ! variable.getDeclaration().isModifiable();
            }
            /*
                if (!(operand.getValue() instanceof Variable)) {
                    return true; // NOT defined
                } else {
                    Variable v = (Variable)operand.getValue();
                    if (!Type.isIntegerType(v.getType())) {
                        return true; // NOT defined
                    }
                    // TODO check variable environment to catch overflows
                    return ! v.getDeclaration().isModifiable();
                }*/
                // NOTREACHED
            case OP_LNEG:
                // This requires a bool argument
                return ! Type.isBoolType(operand.getType());
            case OP_DELETE:
                // delete is disabled completely for now, since it yields an empty tuple result and thus isn't
                // embeddable in more complex expressions
                // TODO Find a way to apply this usefully
                return true; // not defined
                /*
                // May only be applied to lvalues (non-constant variables)
                if (!(operand.getValue() instanceof Variable)) {
                    return true; // NOT defined
                } else {
                    Variable v = (Variable)operand.getValue();
                    return v.getDeclaration().isConstant();
                }*/
            case OP_PLUS:
                if (Configuration.languageVersionMinor >= 5) {
                    return true; // Unary plus is no longer allowed in 0.5.x
		}
            case OP_MINUS:
                // This requires an integer argument
                return ! Type.isIntegerType(operand.getType());
            case OP_BNEG:
                // Requires integer or bytes
                return ! Type.isIntegerType(operand.getType()) && ! Type.isByteType(operand.getType());
        }

        // TODO Take values from variable environment into account as well (undefined ops: signed integer overflow?!) - but maybe better do this during evaluation, not here
        return false;
    }

    public boolean isUndefinedBinaryOperation(VariableEnvironment environment,
                                              Expression firstOperand,
                                              ASTBinaryOperation.Operator operator,
                                              Expression secondOperand) throws Exception {
        // Perform initial type checking before taking this further. The grammar from the Solidity documentation
        // ( https://github.com/ethereum/solidity/blob/develop/docs/grammar.txt ) does not take type constraints
        // into account for operators, so our best guesses/experimentation may not be correct for all cases.
        //
        // JavaScript is not a viable reference, since e.g. its allowing of relational operators for boolean ("true > false")
        // values transfers to Solidity, but arithmetic on booleans ("true + true" = 2) doesn't.
        //
        // TODO This may later be more appropriately expressed in terms of categories (isBitwise(), etc.) but potential
        // categories aren't clear yet
        // TODO These definedness checks will duplicate actual evaluation eventually - should we merge both?
        // (speculative evaluation -> exception on type error)

        // Note: Integer decisions can still fail for mixed signed and unsigned integer types later - these are not
        // conerted implicitly in arithmetic operations. We pass this constellation on so that the caller of failed
        // expression evaluation will have achance to make this work by e.g. inserting a cast
        // Update: Unified signedness is already ensured in in expression generation now/?!

        if (Type.isVariableByteType(firstOperand.getType()) || Type.isVariableByteType(secondOperand.getType())) {
            // The "bytes" type can't do anything apart from subscript?!
            return true; // undefined
        }

        switch (operator) {
            // Arithmetic
            case OP_PLUS: // +   note: this is counter-intuitively not supported on strings
            case OP_MINUS: // -
            case OP_MUL: // *
            case OP_DIV: // /
            case OP_MOD: // %
            case OP_EXP: // **
                if (!Type.isIntegerType(firstOperand.getType()) || !Type.isIntegerType(secondOperand.getType())) {
                    return true; // is undefined
                }

                if (operator == OP_EXP) {
                    if (Configuration.ganacheOptimizationWorkarounds || Configuration.avoidExponentialOperator) {
                        return true;
                    }
                    // The operands must be unigned
                    if (!Type.isUnsignedIntegerType(firstOperand.getType()) || !Type.isUnsignedIntegerType(secondOperand.getType())) {
                        return true; // is undefined
                    }
                }

                // Types OK so far - continue below.
                break;

                // Bitwise, non-shift
            case OP_BOR: // |
            case OP_BAND: // &
            case OP_BXOR: // ^
                // Allowed: Either both are integers, or both are (fixed) byte arrays
                if (!Type.isIntegerType(firstOperand.getType()) || !Type.isIntegerType(secondOperand.getType())) {
                    if (!Type.isByteType(firstOperand.getType()) || !Type.isByteType(secondOperand.getType())) {
                        return true; // is undefined
                    }
                }
                // Types OK so far - continue below.
                break;
                // Bitwise, shift
            case OP_BSHL: // <<
            case OP_BSHR: // >>
                if (Configuration.ganacheOptimizationWorkarounds || Configuration.avoidShiftOperators) {
                    return true; //if (operator == OP_BSHR) return true; // is undefined
                }

                // Second operand mus always be an integer
                if (!Type.isIntegerType(secondOperand.getType())) {
                    return true; // is undefined
                }

                // First operand may be integer or (fixed) byte array
                if (!Type.isIntegerType(firstOperand.getType()) && !Type.isByteType(firstOperand.getType())) {
                    return true; // is undefined
                }
                // Types OK so far - continue below.
                break;
            // Equality
            case OP_EQ: // ==
            case OP_NEQ: // !=
                // Must be compatible integer type or same type - with the following unsupported exceptions:
                //    - strings         (counter-intuitive)
                //    - structures
                //    - arrays
                //    - mappings
                // (Function types are notably allowed)
                if (Type.isStringType(firstOperand.getType())
                        || Type.isStructType(ast, firstOperand.getType())
                        || Type.isArrayType(firstOperand.getType())
                        || Type.isMappingType(firstOperand.getType())) { // TODO Function types?
                    return true; // is undefined
                }
                // Note: bytes comparisons are allowed even between types of distinct sizes (TODO what do they mean?)
                if (Type.isByteType(firstOperand.getType()) && Type.isByteType(secondOperand.getType())) {
                    ; // OK
                } else if (!Type.isSameType(ast, firstOperand.getType(), secondOperand.getType())) {
                    return true; // is undefined
                }
                // Types OK so far - continue below
                break;
            // Relational
            case OP_GT: // >
            case OP_LT: // <
            case OP_GTE: // >=
            case OP_LTE: // <=
                // These operators are defined for integer, bool and bytes types
                if (!Type.isIntegerType(firstOperand.getType()) || !Type.isIntegerType(secondOperand.getType())) { // not both int
                    if (!Type.isBoolType(firstOperand.getType()) || !Type.isBoolType(secondOperand.getType())) { // not both bool
                        if (!Type.isByteType(firstOperand.getType()) || !Type.isByteType(secondOperand.getType())) { // not both bytes
                            return true; // is undefined
                        }
                    } else {
                        // Relational operator applied to booleans - this is OK only prior to 0.5.x
                        if (Configuration.languageVersionMinor >= 5) {
                            return true; // is undefined 
		        }
                    }
                }
                // Types OK so far - continue below
                break;
            // Logical
            case OP_LOR: // ||
            case OP_LAND: // &&
                if (Configuration.avoidLogicalAndConditionalOperators) {
                    return true;
                }
                if (!Type.isBoolType(firstOperand.getType()) || !Type.isBoolType(secondOperand.getType())) {
                    return true; // is undefined
                }
                // Types OK so far - continue below
        }

        // TODO Take values from variable environment into account as well  - but maybe better do this during evaluation, not here
        return false;
    }
}
