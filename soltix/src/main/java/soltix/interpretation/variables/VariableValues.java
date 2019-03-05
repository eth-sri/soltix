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

import soltix.interpretation.values.IntegerValue;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;

import soltix.interpretation.values.Value;

/**
 * Class to represent multiple values taken on by one variable.
 *
 * This is not stored in the Variable class itself, since one variable may be shared by multiple
 * statements.
 * TODO Variable may be an unnecessary level of indirection between VariableValues and ASTNode
 */
public class VariableValues {
    final static Logger logger = Logger.getLogger(VariableValues.class);

    private long statementID;
    private Variable variable;
    private ArrayList<Value> values;
    private int observedValuesCount = 0;

    public VariableValues(Variable variable, long statementID) {
        this.variable = variable;
        this.statementID = statementID;
    }

    public long getStatementID() { return statementID; }
    public Variable getVariable() { return variable; }

    public void addValue(Value value) {
        if (values == null) {
            values = new ArrayList<Value>();
        }
        values.add(value);

        /*
        System.out.println("----- values for " + variable.getName() + " now -----");
        for (Value v : values) {
            System.out.println( " ............ " + v.get());
        }
        */
    }

    public void updateValue(int index, Value newValue) {
        values.set(index, newValue);
    }

    // If this is a recording for an integer value, these are the smallest and largest encountered values:
    // TODO Only recording min/max values may be too coarse-grained in general, and maybe there's a better representation
    private IntegerValue smallestIntegerValue = null;
    private IntegerValue largestIntegerValue = null;

    public int finishAddingValues() throws Exception {
        if (values == null) {
            // TODO Fix this: No values were produced for this variable, even though this whole value processing
            // should be triggered by the presence of events with values.
            // In the case 8bde9c4def35aef9780e70654bd8cd6a9ded3d90cc570b0dd72eba8d200887f9 the variable is
            // constant, which could have something to do with it. Including constant variables in profifing
            // probably doesn't make sense (but then they should still be involved in the variable environment,
            // and their initializers should be considered instead)
            throw new Exception("Unexpected finishAddingValues for " + variable.getName() + ", type "
                    + variable.getType().toSolidityCode() + " without any values");
        }
        if (values.get(0) instanceof IntegerValue) { // At least one is available
            // Find min/max values

            //Collections.sort(values, Collections.reverseOrder());
            for (Value value : values) {
                IntegerValue currentIntegerValue = (IntegerValue)value;
                if (smallestIntegerValue == null || smallestIntegerValue.isGreaterThan(currentIntegerValue).getValue()) {
                    smallestIntegerValue = currentIntegerValue;
                }
                if (largestIntegerValue == null || largestIntegerValue.isSmallerThan(currentIntegerValue).getValue()) {
                    largestIntegerValue = currentIntegerValue;
                }
            }
        }
        return values.size();
    }

    public boolean haveMinMaxValues() { return values.get(0) instanceof IntegerValue; }
    public IntegerValue getLargestValue() throws Exception {
        if (!(values.get(0) instanceof IntegerValue)) {
            throw new Exception("getLargestValue called on non-integer type");
        }
        if (values.size() == 1) {
            // For single-value recordings, small/large integer values do not need to be tracked
            return (IntegerValue)values.get(0);
        }
        return largestIntegerValue;
    }

    public IntegerValue getSmallestValue() throws Exception {
        if (!(values.get(0) instanceof IntegerValue)) {
            throw new Exception("getLargestValue called on non-integer type " + values.get(0).getClass().getName());
        }
        if (values.size() == 1) {
            // For single-value recordings, small/large integer values do not need to be tracked
            return (IntegerValue)values.get(0);
        }
        return smallestIntegerValue;
    }

    public int getValueCount() { return values == null? 0: values.size(); }

    public Value getValue(int index) {
        return values.get(index);
    }

    /* too error-prone:
    public boolean haveNextValue() {
        return observedValuesCount < values.size();
    }
    public int nextValueIndex() {
        return observedValuesCount;
    }
    public Value nextValue() {
        return values.get(observedValuesCount++);
    }*/

}
