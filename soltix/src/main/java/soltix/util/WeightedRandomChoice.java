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
package soltix.util;

import java.util.ArrayList;

/**
 * Class to choose randomly among multiple items with non-uniform distribution
 */
public class WeightedRandomChoice {
    private ArrayList<Integer> choices = new ArrayList<Integer>();

    public WeightedRandomChoice() {
    }

    private int totalPercentage = 0;
    public void addItem(int percentage) throws Exception {
        if (totalPercentage + percentage > 100 || percentage < 0) {  // allow 0 to disable items temporarily
            throw new Exception("WeightedRandomChoce: total percentage exceeds 100 or invalid individual percentage, " + totalPercentage + " + " + percentage);
        }
        totalPercentage += percentage;
        choices.add(percentage);
    }

    public int choice(RandomNumbers prng) throws Exception {
        if (totalPercentage != 100) {
            throw new Exception("WeightedRandomChoice: total percentage does not add up to 100");
        }

        int randomValue = (int)prng.generateLongInteger(0, 99);
        int currentTotal = 0;
        for (int i = 0; i < choices.size(); ++i) {
            int nextTotal = currentTotal + choices.get(i);
            if (currentTotal <= randomValue && nextTotal > randomValue) {
                return i;
            }
            currentTotal = nextTotal;
        }
        throw new Exception("Unexpected choice value " + randomValue + " outside of range 0 to " + currentTotal);
    }
}
