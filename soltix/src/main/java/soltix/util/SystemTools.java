/*
 * SOLTIX - Scalable automated framework for testing Solidity compilers.
 *
 * Author: Nils Weller <nweller@uni-bremen.de>
 *
 * Copyright (C) 2019 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
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

import org.apache.log4j.Logger;
import soltix.Driver;

import java.lang.Process;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemTools {
    final static Logger logger = Logger.getLogger(SystemTools.class);

    static public boolean runCommand(String command) {
        BufferedReader b = null;
        System.out.println("running " + command);
        Runtime r = Runtime.getRuntime();
        try {
            Process p = r.exec(command);
            boolean rc = p.waitFor() == 0;
            if (!rc) {
                logger.error("SystemTools.runCommand returned nonzero exit value for " + command);
                return false;
            }
            b = new BufferedReader(new InputStreamReader(p.getInputStream()));
        } catch (Exception e) {
            logger.error("SystemTools.runCommand failed for for '" + command + "': " + e.toString());
            return false;
        }
        String line = "";

        try {
            while ((line = b.readLine()) != null) {
                //System.out.println(line);
            }
            b.close();

        } catch (Exception e) {
            logger.error("SystemTools.runCommand I/O exception for '" + command + "': " + e.toString());
            return false;
        }
        return true;
    }
}
