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

import soltix.ast.AST;
import soltix.ast.ASTContractDefinition;
import soltix.ast.ASTFunctionDefinition;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Class to represent a loaded recording of profiling events
 */
public class ProfilingLogRecording {
    // Contract to which this log pertains - extracted from the event information, and currently
    // limited to a single contract per log
    private ASTContractDefinition contract = null;
    // Function containing the first encountered profiling event
    private ASTFunctionDefinition firstEventFunction = null;

    // List of all events actually recorded during execution (profiling-events.log)
    private ArrayList<ProfilingEvent> recordedResultList = null;
    private AST ast;
    private int currentReplayIndex = -1;

    public ProfilingLogRecording(AST ast) {
        this.ast = ast;
    }

    public ASTContractDefinition getContract() { return contract; }
    public ASTFunctionDefinition getFirstEventFunction() { return firstEventFunction; }

    public void loadEventLog(String path) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;

        recordedResultList = new ArrayList<ProfilingEvent>();
        while ((line = br.readLine()) != null) {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject)jsonParser.parse(line);

            ProfilingEvent event = ProfilingEvent.fromJSON(ast, jsonObject);
            if (event == null) {
                continue;
            }

            recordedResultList.add(event);

            if (!event.getIsOrdinaryUserEvent()) {
                /*
                // A log for multiple contracts must be allowed at least for inter-contract
                // calls
                if (contract != null) {
                    if (contract != event.getContract()) {
                        throw new Exception("Event log contains events from more than one contract");
                    }
                } else*/ {
                    contract = event.getContract();
                    // Infer the containing function of this first event as well
                    firstEventFunction = ast.getFunctionByStatementId(event.getStatementID());
                }
            }
        }
        currentReplayIndex = 0;

        // Finalize environment variables
        for (ProfilingEvent eventData : recordedResultList) {
            if (eventData.getStatement() != null) {
                eventData.getStatement().getVariableEnvironment().finishAddingValues();
            }
        }
    }

    public boolean haveNextEvent() throws Exception {
        if (currentReplayIndex == -1) {
            throw new Exception("ProfilingLogRecording.haveNextEvent without preceding loadEventLog");
        }
        return currentReplayIndex < recordedResultList.size();
    }

    public ProfilingEvent peekNextEvent() {
        return recordedResultList.get(currentReplayIndex);
    }

    public ProfilingEvent nextEvent() {
        return recordedResultList.get(currentReplayIndex++);
    }
}
