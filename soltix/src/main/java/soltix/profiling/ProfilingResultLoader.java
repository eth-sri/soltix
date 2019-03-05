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

import soltix.Configuration;
import soltix.ast.ASTFunctionDefinition;
import soltix.ast.ASTNode;
import soltix.interpretation.*;
import soltix.interpretation.variables.*;
import soltix.output.*;
import org.apache.log4j.Logger;

/**
 * Class to load profiling event data from a log file
 */
public class ProfilingResultLoader implements IInterpreterCallback {
    final static Logger logger = Logger.getLogger(ProfilingResultLoader.class);

    private ASTInterpreter astInterpreter;
    private ASTFunctionDefinition currentFunction;
    private ProfilingLogRecording recordedLog;
    private CodeOutputWriter codeOutput;

    public ProfilingResultLoader(ProfilingLogRecording recordedLog) {
        this.recordedLog = recordedLog;
    }

    public void initialize(ASTInterpreter astInterpreter) {
        this.astInterpreter = astInterpreter;

        // Produce in-memory Solidity code representation for tracing output
        codeOutput = new CodeOutputWriter(null /* store to memory */);
        boolean showCoverage = Configuration.showCoverage;
        Configuration.showCoverage = false; // not useful for code tracing

        try {
            Emitter.emitSolidityCode(codeOutput, astInterpreter.getAST());
        } catch (Exception e) {
            logger.error("Solidity code output error: " + e.toString());
        }
        Configuration.showCoverage = showCoverage;
    }

    public ASTInterpreter.NavigationPolicy getNavigationPolicy() { return ASTInterpreter.NavigationPolicy.NAVIGATION_POLICY_TARGETED; }

    public ASTNode nextTargetStatement() throws Exception {
        while (recordedLog.haveNextEvent()) {
            ProfilingEvent nextEvent = recordedLog.nextEvent();
            if (nextEvent.getIsOrdinaryUserEvent()) {
                // Only pass profiling events
                continue;
            }

            if (nextEvent == null) {
                return null;
            } else {
                ASTNode statement = nextEvent.getStatement(); //astInterpreter.getAST().getNodeById(nextEvent.getStatementID());
                for (;;) {
                    evaluateEvent(statement, nextEvent);
                    // Collect further parts as well, if any. This also serves to correctly return only a single
                    // statement ID to the caller
                    if (recordedLog.haveNextEvent()
                            && recordedLog.peekNextEvent().getStatementID() == nextEvent.getStatementID()
                            && recordedLog.peekNextEvent().getPartNumber() > 0) {
                        nextEvent = recordedLog.nextEvent();
                    } else {
                        break;
                    }
                }
                return statement;
            }
        }
        return null;
    }

    public void start(ASTFunctionDefinition function) {
        currentFunction = function;
    }

    public void visitNodeBeforeProcessing(ASTNode node) throws Exception {
        node.setCovered(true);
        if (Configuration.verboseDebugOutput) {
            codeOutput.printLineToLogger(node.getOutputCodeLineNumber(), node.getID());
            VariableEnvironment environment = node.getVariableEnvironment();
            if (environment != null) {
                environment.printLatestToLogger();
            }
        }
    }

    public void visitNodeAfterProcessing(ASTNode node) throws Exception {
    }

    public void finish() throws Exception {

    }

    protected void evaluateEvent(ASTNode statement, ProfilingEvent event) throws Exception {
        // Note: variable values are already stored to the variable environment in ProfilingEvent.fromJSON
        ;
    }

    public void run() throws Exception {
        throw new Exception("Invalid call to ProfilingResultLoader.run");
    }
}
