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
package soltix.profiling;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Comparison of two event logs from the execution of two contracts that are expected to be semantically equivalent
 * for the input data given in the test.
 *
 * (Potentially but not necessarily containing Profiling-specific events as well)
 */
public class EventLogComparator {
    final static Logger logger = Logger.getLogger(EventLogComparator.class);

    static public boolean equal(String firstLogPath, String secondLogPath, boolean ignoreProfilingEvents) throws Exception {
        // Perform a light-weight line-by-line event comparison. No AST or type info is needed, only event
        // names and argument names/values
        BufferedReader firstLogReader = new BufferedReader(new FileReader(firstLogPath));
        BufferedReader secondLogReader = new BufferedReader(new FileReader(secondLogPath));
        boolean result = true;

        String firstLine = null, secondLine = null;
        JSONParser jsonParser = new JSONParser();

        int firstLineNumber = 0, secondLineNumber = 0;
        for (;;) {
            String firstEventName = null, secondEventName = null;
            String firstEventArguments = null, secondEventArguments = null;
            while (firstLogReader != null) {
                firstLine = firstLogReader.readLine();
                if (firstLine == null) {
                    firstLogReader.close();
                    firstLogReader = null;
                } else {
                    ++firstLineNumber;
                    JSONObject firstLineJSON = (JSONObject) jsonParser.parse(firstLine);
                    firstEventName = (String) firstLineJSON.get("event");
                    if (firstEventName == null) {
                        // Some test cases, such as 01fa569a017e8d103f8c9c0b5fecaad0a3d4b28ffc465f63195754c5257947d0,
                        // apparently result in events that were not emitted by the Solidity program, the nature of which is unclear:
                        //      {"logIndex":0,"transactionIndex":0,"transactionHash":"0x99f8cae7fd148ac7b8ef615113c89d1065a098eabe9ad25f0ee17b23a5043b8d","blockHash":"0x9a1b8f13df9f406267b8f30a9650405d8d377b6e7b01611645ada6cda1d2553    a","blockNumber":5,"address":"0x802042f99aed5e1d327ddb2aaf82790d89c154bd","data":"0x0","topics":["0x0cb5c216387887a30c1b56bfd6d4562eaf53b432f9a7d3a8bde0298f3f130fa3"],"type":"mined"}
                        continue;
                    }
                    firstEventArguments = ((JSONObject) firstLineJSON.get("args")).toJSONString();
                    if (ignoreProfilingEvents && firstEventName.startsWith(ProfilingEvent.profilingEventPrefix)) {
                        // Skip profiling events
                        firstEventName = null;
                        continue;
                    } else {
                        break;
                    }
                }
            }
            while (secondLogReader != null) {
                secondLine = secondLogReader.readLine();
                if (secondLine == null) {
                    secondLogReader.close();
                    secondLogReader = null;
                } else {
                    ++secondLineNumber;
                    JSONObject secondLineJSON = (JSONObject) jsonParser.parse(secondLine);
                    secondEventName = (String) secondLineJSON.get("event");
                    if (secondEventName == null) {
                        continue;
                    }
                    secondEventArguments = ((JSONObject) secondLineJSON.get("args")).toJSONString();
                    if (ignoreProfilingEvents && secondEventName.startsWith(ProfilingEvent.profilingEventPrefix)) {
                        // Skip profiling events
                        secondEventName = null;
                        continue;
                    } else {
                        break;
                    }
                }
            }

            //System.out.println( "     " + (firstEventName!=null? firstEventName: " (none) ") + " vs " +
            //        (secondEventName!=null? secondEventName: " (none) ") );

            if (firstEventName != null && secondEventName != null) {
                // Both sides have an event
                if (!firstEventName.equals(secondEventName)
                        || !firstEventArguments.equals(secondEventArguments)) {
                    result = false;
                    break;
                }
            } else if (firstEventName == null && secondEventName == null) {
                // Neither side has an event - done
                result = true;
                break;
            } else {
                // One is null - since we've already excluded profiling events, this is not permissible
                result = false;
                break;
            }
        }
        if (!result) {
            logger.error("Error: Log difference on line " + firstLineNumber + " (" + firstLogPath + ") vs line " + secondLineNumber + " (" + secondLogPath + ")");
        }
        return result;
    }
}
