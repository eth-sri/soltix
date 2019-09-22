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

import java.math.BigInteger;
import java.util.Random;

/**
 * Class for pseudo-random number generation
 */
public class RandomNumbers {
    // TODO Replace Random with a custom PRNG implementation? To guarantee that the same code revision + configuration
    // + PRNG seed reproduce the program's behavior, which may not be the case for Java's PRNG across different versions
    private Random prng;
    private boolean generateZerosOnly = false;

    public RandomNumbers(int seed) {
        prng = new Random(seed);
    }

    public void setGenerateZerosOnly(boolean generateZerosOnly) { this.generateZerosOnly = generateZerosOnly; }
    public boolean getGenerateZerosOnly() { return generateZerosOnly; }

    public double generateDouble() {
        return generateZerosOnly? 0.0: prng.nextDouble();
    }

    public long generateLongInteger(long minValue, long maxValue) {
        long range = maxValue - minValue; // +1 to include the max value itself
        double prngValue = generateDouble();

        // Scale to range
        long scaledLongValue = (long)(Math.round(prngValue * range));

        // Map to min/max value window
        return minValue + scaledLongValue;
    }

    public BigInteger generatePositiveBigInteger(BigInteger limit) {
        if (generateZerosOnly) {
            return BigInteger.valueOf(0);
        }
        BigInteger result;
        do {
            result = new BigInteger(limit.bitLength(), prng);
        } while (result.compareTo(limit) >= 0);
        return result;
    }


    public boolean flipCoin() {
        return generateLongInteger(0, 1) == 1;
    }
}
