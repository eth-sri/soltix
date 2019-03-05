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

import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;

/**
 * Class to compute a keccak256() hash
 */
public class Hash {
    public static byte[] keccak256(byte[] input) {
        Keccak.DigestKeccak keccak = new Keccak.Digest256();
        keccak.update(input, 0, input.length);
        return keccak.digest();
    }

    // EIP-55 adds an address checksum scheme that typically leads to randomly generated
    // addresses (including constants used in a bytes20 context) violating the checksum, thereby
    // producing a compiler error in 0.5+.
    //
    // An input address can be converted to a compliant address as per the code given in:
    //
    //      https://github.com/ethereum/EIPs/blob/master/EIPS/eip-55.md
    //
    // Java-specific checksumming adaptations inspired by:
    //
    //      https://www.programcreek.com/java-api-examples/?api=org.bouncycastle.jcajce.provider.digest.Keccak
    public static String toChecksumAddress(String addressString) throws Exception {
        if (addressString.length() != 42) { // 20 times 2 hex digits, 2 prefix bytes
            throw new Exception("Hash.toChecksumAddress called with non-20-bytes argument");
        }

        // Start out with a lower case original string, with the 0x prefix stripped
        addressString = addressString.substring(2).toLowerCase();

        // Compute keccak
        String hash = Hex.toHexString(keccak256(addressString.getBytes()));

        // Build adapted address string based on initial string and hash
        String result = "0x";

        for (int i = 0; i < addressString.length(); ++i) {
            if (i-2 < hash.length() && Character.digit(hash.charAt(i), 16) >= 8) {
                result += Character.toUpperCase(addressString.charAt(i));
            } else {
                result += addressString.charAt(i);
            }
        }

        return result;
    }
}
