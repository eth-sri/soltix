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
package soltix.interpretation.variables;

import soltix.Configuration;
import soltix.ast.AST;
import soltix.interpretation.Type;
import soltix.interpretation.expressions.Expression;
import soltix.util.JSONValueConverter;
import org.apache.log4j.Logger;


import soltix.profiling.*;
import soltix.interpretation.values.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to represent a variable environment (variables for which profiling or interpretation result values
 * are available - pertinent to one statement
 */
public class VariableEnvironment {
    final static Logger logger = Logger.getLogger(VariableEnvironment.class);

    private AST ast;

    // Statement to which this environment pertains
    private long statementID;
    // All variables
    private HashMap<String, VariableValues> variables = null;
    private ArrayList<VariableValues> variablesList = null;
    private ArrayList<VariableValues> integerVariablesList = null;
    // Number of values taken by each variable
    private int valueCount = 0;

    //private int currentValueSetIndex = -1;
    public static final int NO_VALUE_SET_SELECTED = -1;



    private boolean recordingChanges = false;

    // Integer variables (of interest for relational expression generation, unlike the other types)

    public VariableEnvironment(AST ast, boolean recordingChanges) throws Exception {
        this(ast, null, 0, recordingChanges);
    }

    public VariableEnvironment(AST ast, ArrayList<ProfilingEvent> events, long statementID, boolean recordingChanges) throws Exception {
        this.ast = ast;
        this.statementID = statementID;
        this.recordingChanges = recordingChanges;
        if (recordingChanges) {
            // For now we assume a single value for each variable (subsequent profiling extensions may change this)
            valueCount = 1;
        }
        variables = new HashMap<String, VariableValues>();
        variablesList = new ArrayList<VariableValues>();
        integerVariablesList = new ArrayList<VariableValues>();
        if (events != null) {
            for (ProfilingEvent event : events) {
                if (event.getStatementID() != statementID) {
                    throw new Exception("VariableEnvironment: Event with inconsistent statement ID: Expected " + statementID + " got " + event.getStatementID());
                }
                if (event.getArguments() != null) {
                    for (Variable var : event.getArguments()) {
                        VariableValues values = new VariableValues(var, statementID);
                        /*variables.put(var.getName(), values);
                        variablesList.add(values);
                        if (var.getDeclaration().getTypeName().getName().startsWith("int")
                                || var.getDeclaration().getTypeName().getName().startsWith("uint")) {
                            integerVariablesList.add(values);
                        }*/
                        addVariableValues(var, values);
                    }
                }
            }
        }
    }

    public Variable getVariable(String name) {
        return variables.get(name).getVariable();
    }

    public VariableEnvironment createSingleValueSnapshot(int index) throws Exception {
        VariableEnvironment snapshotVariableEnvironment = new VariableEnvironment(ast, false);
        for (VariableValues variableValues : variablesList) {
            VariableValues snapshotVariableValues = new VariableValues(variableValues.getVariable(), 0);

            Value currentValue = variableValues.getValue(index);
            if (currentValue instanceof StructValue) {
                // Create a deep copy, otherwise member values can still get updated
                currentValue = ((StructValue)currentValue).createDeepValueCopy(ast);
            } else if (currentValue instanceof ArrayValue) {
                // As for structs, we need a copy here
                throw new Exception("createSingleValueSnapshot unimplemnted for arrays");
            }
            snapshotVariableValues.addValue(currentValue);
            snapshotVariableEnvironment.addVariableValues(snapshotVariableValues.getVariable(), snapshotVariableValues);
        }
        snapshotVariableEnvironment.finishAddingValues(); // to update the value count to 1
        return snapshotVariableEnvironment;
    }


    public boolean isRecordingChanges() {
        return recordingChanges;
    }

    public void addVariableValues(Variable var, VariableValues values) throws Exception {
        // Check type consistency of one representative value (if available) with variable type
        if (var.getType() == null) throw new Exception("Variable type is null for " + var.getName());

        if (values.getValueCount() > 0 && values.getValue(0).getType() == null) throw new Exception("Value type for variable " + var.getName() + " is null");
        if (values.getValueCount() > 0
            && !Type.isSameType(ast, var.getType(), values.getValue(0).getType())) {
            throw new Exception("VariableEnvironment.addVariableValues: Added value with type "
                    + values.getValue(0).getType().toSolidityCode()
                    + " which is inconsistent with variable type " + var.getType().toSolidityCode()
                    + " - classes " + values.getValue(0).getType().getClass().getName()
                    + " vs " + var.getType().getClass().getName());
        }
        variables.put(var.getName(), values);
        variablesList.add(values);
        if (Type.isIntegerType(var.getDeclaration().getTypeName())) {
            integerVariablesList.add(values);
        }
    }

    // For a sequence of member access operations ".", resolve the final struct's value, such that the new member
    // value can be updated in it.
    // The structure is an arbitrary sequence of v.m1.m2.m3... which is represented as a parse tree
    //
    //      expr:                    firstOperand                  member access = m3
    //                              /            \
    //      expr:       firstOperand        member access = m2
    //                  /          \
    //      expr:    value = v    member access = m1
    //
    // So we untangle this by descending the firstOperand node until reaching the variable.
    //
    // TODO Completeness not clear yet
    // TODO Arrays would need to be mixed in here
    protected Value resolveContainingValue(Variable variable, Expression expression, StructValue value) throws Exception {
        if (variable == expression.getFirstOperand().getValue()) {
            // We're processing "v.m" with v's value - it can be kept
            Value memberValue = value.getMemberValue(expression.getMemberAccess().getName());
            if (memberValue instanceof StructValue) {
                // OK - dereference it
                return memberValue;
            } else {
                return value;
            }
        } else {
            Value resolvedLHS = resolveContainingValue(variable, expression.getFirstOperand(), value);
            if (!(resolvedLHS instanceof StructValue)) {
                throw new Exception("VariableEnvironment.resolveContainingValue on unexpected non-struct type");
            }
            Value memberValue = ((StructValue)resolvedLHS).getMemberValue(expression.getMemberAccess().getName());
            // TODO this double resolution here and at the terminal variable looks awkward/wrong. something is off
            // about the recursion
            if (memberValue instanceof StructValue) {
                // OK - dereference it
                return memberValue;
            } else {
                // This is the "terminal" member access, i.e. the last "." in "a.b.c.d" - keep the containing struct type
                return resolvedLHS;
            }
        }
    }

    public Variable tryLookupVariableByExpression(Expression expression) {
        Expression tempExpression = expression;

        // Navigate past struct member access to get access to the base variable
        while (tempExpression.getMemberAccess() != null) {
            tempExpression = tempExpression.getFirstOperand();
        }
        if (tempExpression.getValue() instanceof Variable) {
            return (Variable)tempExpression.getValue();
        }
        return null;
    }

    public void updateVariableValue(Expression expression, Value value) throws Exception {
        /*
        Expression tempExpression = expression;

        // Navigate past struct member access to get access to the base variable
        while (tempExpression.getMemberAccess() != null) {
            tempExpression = tempExpression.getFirstOperand();
        }
        if (!(tempExpression.getValue() instanceof Variable)) {
            throw new Exception("updateVariableValue for unimplemented expression type");
        }*/


        Variable variable = tryLookupVariableByExpression(expression);
        if (variable == null) {
            throw new Exception("updateVariableValue for unimplemented expression type");
        }
        updateVariableValue(variable, expression, value);
    }

    // TODO This desperately needs unit tests for partial value updates
    public void updateVariableValue(Variable variable, Expression expression, Value value) throws Exception {
        boolean debugOutput = false;

        if (debugOutput) {
            System.out.println("update on " + variable.getName() + " prior value " +
                    variables.get(variable.getName()).getValue(0).toASTNode(false).toSolidityCode());
        }

        if (expression.getValue() instanceof Variable) {
            updateVariableValue((Variable)expression.getValue(), value);

            if (debugOutput) {
                System.out.println("update on " + variable.getName() + " after value(1) " +
                        variables.get(variable.getName()).getValue(0).toASTNode(false).toSolidityCode());
            }

            return;
        }

        // Partial update on a struct member (TODO: Array elements)

        VariableValues variableValues = variables.get(variable.getName());
        if (variableValues.getValueCount() != 1) {
            // We could use the most recent value, and that may even be desirable if we're generating profiling
            // logs during execution. But for now a single value is assumed
            throw new Exception("VariableEnvironment.updateVariableValue called on more than one value");
        }

        if (expression.getMemberAccess() != null) {
            StructValue containingStructValue = (StructValue)resolveContainingValue(variable, expression, (StructValue)variableValues.getValue(0));
            containingStructValue.updateMemberValue(expression.getMemberAccess().getName(), value);
        } else {
            throw new Exception("Unexpected updateVariableValue expression argument " + expression.toASTNode().toSolidityCode());
        }


        if (debugOutput) {
            System.out.println("update on " + variable.getName() + " after value(2) " +
                    variables.get(variable.getName()).getValue(0).toASTNode(false).toSolidityCode());
            System.out.println("           expr " + expression.toASTNode().toSolidityCode());
        }
    }

    public void updateVariableValue(Variable variable, Value value) throws Exception {
        VariableValues variableValues = variables.get(variable.getName());
        if (variableValues.getValueCount() != 1) {
            // We could use the most recent value, and that may even be desirable if we're generating profiling
            // logs during execution. But for now a single value is assumed
            throw new Exception("VariableEnvironment.updateVariableValue called on more than one value");
        }
        variableValues.updateValue(0, value);
    }

    public void removeVariableValues(Variable var) {
        VariableValues values = variables.get(var.getName());
        variables.remove(var.getName());
        variablesList.remove(values);
        if (Type.isIntegerType(var.getDeclaration().getTypeName())) {
            integerVariablesList.remove(values);
        }
    }

    public long getStatementID() { return statementID; }

    public int getVariableCount() {
        return variables == null? 0: variablesList.size();
    }
    public VariableValues getVariableValues(int index) {
        return variablesList.get(index);
    }

    public int getIntegerVariableCount() {
        return integerVariablesList == null? 0: integerVariablesList.size();
    }
    public VariableValues getIntegerVariableValues(int index) {
        return integerVariablesList.get(index);
    }


    public void storeVariableValue(String name, Object valueObject, long statementID) throws Exception {
        VariableValues variableValues = variables.get(name);
        if (variableValues == null) {
            if (Character.isDigit(name.charAt(0)) || name.equals("__length__")) {
                // New truffle/ganache version metadata - ignore
                return;
            }
            throw new Exception("Cannot map profiling event argument name '"
                    + name + "' to variable environment for statement " + statementID);
        }

        Variable variable = variableValues.getVariable();
        Value value = JSONValueConverter.valueFromObject(variable.getType(), valueObject);

        if (Configuration.verboseDebugOutput) {
            System.out.println("   STORED TO var " + variable.getName() + " (type " + variable.getType().toSolidityCode() + ") = "
                    + value.toASTNode(false).toSolidityCode() + " (type " + value.getType().toSolidityCode() + ")");
        }
        variableValues.addValue(value);
    }

    public void finishAddingValues() throws Exception {
        for (VariableValues variableValues : variables.values()) {
            int currentVariableValueCount = variableValues.finishAddingValues();
            // Make sure that this environment has the same number of values for each included variable
            if (valueCount == 0) {
                valueCount = currentVariableValueCount;
            } else if (valueCount != currentVariableValueCount) {
                throw new Exception("Inconsistent variable environment variable count");
            }
        }
    }

    public int getValueCount() { return valueCount; }

    public Value resolveVariableValue(int valueSetIndex, /*Variable variable*/ String variableName) throws Exception {
        if (valueSetIndex == NO_VALUE_SET_SELECTED) {
            throw new Exception("Variable.resolveVariableValue called without selected value set index");
        }
        VariableValues variableValues = variables.get(variableName);
        if (variableValues == null) {
            throw new Exception("VariableEnvironment.resolveVariableValue: Cannot locate variable " + variableName + " in variable environment");
        }
        return variableValues.getValue(valueSetIndex);
    }

    public void printLatestToLogger(String variableName) throws Exception {
        // Show latest set of values
        if (variables != null) {
            for (VariableValues values : variables.values()) {
                Variable variable = values.getVariable();

                if (variableName != null && !variable.getName().equals(variableName)) {
                    continue;
                }

                if (values.getValueCount() > 0) {
                    int latestIndex = values.getValueCount() - 1;
                    Value latestValue = values.getValue(latestIndex);
                    logger.info(variable.getName() + " = " + "[" + latestIndex + "] " + latestValue.toASTNode(false).toSolidityCode());
                } else {
                    logger.error("VariableEnvironment.printLatestToLogger: No values for " + values.getVariable().getName());
                }
            }
        } else {
            logger.error("VariableEnvironment.printLatestToLogger: HAVE NO VARIABLES");
        }
    }


    public void printLatestToLogger() throws Exception {
        printLatestToLogger(null);
    }

    public void printValueSetToLogger(int valueSetIndex) throws Exception {
        if (valueSetIndex == NO_VALUE_SET_SELECTED) {
            throw new Exception("VariableEnvironment.printCurrentValueSetToLogger called without current selection");
        }
        logger.info("===== Value set number " + valueSetIndex + " ====");
        for (VariableValues values : variables.values()) {
            logger.info("    " + values.getVariable().getName() + "   =   "
                    + values.getValue(valueSetIndex).toASTNode(false).toSolidityCode()
                    + (values.haveMinMaxValues()? "    min/max interval  [" + values.getSmallestValue().toASTNode(false).toSolidityCode(): "")
                    + (values.haveMinMaxValues()? "," + values.getLargestValue().toASTNode(false).toSolidityCode() + "]": ""));
        }
    }
}
