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
package soltix;

import soltix.profiling.EventLogComparator;
import soltix.synthesis.FunctionGenerator;
import soltix.util.Hash;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.ArrayList;

public class Main {
    final static Logger logger = Logger.getLogger(Main.class);

    protected static void usage() {
        // TODO update this
        logger.info("Usage: soltix [--debugASTOutput] [--skipASTProcessing] [--debugGraphPlot=output-dotfile] [--instrument] [--replay=event-log]");
        System.exit(1);
    }
    public static void main(String [] args) {
        BasicConfigurator.configure();

        String logPath = System.getenv("LOG4J_CONFIG");
        if (logPath == null) {
            logPath = "log4j.properties";
        }
        PropertyConfigurator.configure(logPath);

        // Process program arguments
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("--debugASTOutput")) {
                Configuration.debugASTOutput = true;
                Configuration.collectInputTokenPositions = true;
            } else if (args[i].equals("--skipASTProcessing")) {
                Configuration.skipASTProcessing = true;
            } else if (args[i].startsWith("--debugGraphPlot=")) {
                String [] s = args[i].split("=");
                Configuration.debugGraphOutputFile = s[1];
            } else if (args[i].startsWith("--solidityOutput=")) {
                String [] s = args[i].split("=");
                Configuration.solidityOutputFile = s[1];
            } else if (args[i].startsWith("--outputTopLevelContract=")) {
                String[] s = args[i].split("=");
                Configuration.outputTopLevelContract = s[1];
            } else if (args[i].equals("--instrument")) {
                Configuration.profiling = true;
            } else if (args[i].startsWith("--replay")) {
                String [] s = args[i].split("=");
                Configuration.loadProfilingLog = s[1];
                Configuration.showCoverage = true;
            } else if (args[i].startsWith("--generateTruffleConstructorInvocation")) {
                String[] s = args[i].split("=");
                Configuration.generateTruffleConstructorInvocationContract = s[1];
                Configuration.generateTruffleConstructorInvocationFile = s[2];
            } else if (args[i].startsWith("--generateTruffleTransaction")) {
                String[] s = args[i].split("=");
                // For now: choose contract to generate tx for, invoke largest (in terms of
                // statements) function automatically
                if (s.length != 4) {
                    logger.error("--generateTruffleTransaction requires a .tx and a .tx-json output file argument");
                    System.exit(1);
                }
                Configuration.generateTruffleTransactionContract = s[1];
                Configuration.generateTruffleTransactionFile = s[2];
                Configuration.generateTruffleTransactionFileJSON = s[3];
            } else if (args[i].startsWith("--generateContractFunctionsFile")) {
                String[] s = args[i].split("=");
                Configuration.generateContractFunctionsFile = s[1];
            } else if (args[i].startsWith("--reduceFunctions")) {
                String[] s = args[i].split("=");
                // Assumed format:
                //    --reduceFunctions=<contract>:<function>,<contract>:<function>, ...
                String[] s2 = s[1].split(",");
                Configuration.reduceFunctions = s2;
            } else if (args[i].startsWith("--generateFullContract")) {
                String[] s = args[i].split("=");

                Configuration.generateFullContractFile = s[1];

                if (s.length < 3
                        || (Configuration.generateFullContractFunctionType = FunctionGenerator.FunctionType.fromName(s[2])) == null) {
                    logger.error("--generateFullContract requires two '='-separated arguments: output file and function type");
                    logger.error("--generateFullContract=file.sol=[random|assignmentSequence|singleReturn");
                    System.exit(1);
                }
            } else if (args[i].equals("--showCoverage")) {
                Configuration.showCoverage = true;
            } else if (args[i].equals("--renameExceptionalEvents")) {
                Configuration.renameExceptionalEvents = true;
            } else if (args[i].startsWith("--renameNowTimestamps")) {
                String[] s = args[i].split("=");
                Configuration.renameNowTimestamps = Integer.parseInt(s[1]);
            } else if (args[i].equals("--renameThrowStatements")) {
                Configuration.renameThrowStatements = true;
            } else if (args[i].startsWith("--testEventLogEquivalence")) {
                String[] s = args[i].split("=");
                // Only compare two event logs and return 0 if equal, 1 if not equal or an error occurred
                try {
                    System.exit(EventLogComparator.equal(s[1], s[2], true /*TODO - pass this*/, true /* TODO */) ? 0 : 1);
                } catch (Exception e) {
                    logger.error("Exception while trying to compare logs " + s[1] + " and " + s[2] + ": " + e.toString());
                    e.printStackTrace();
                    System.exit(1);
                }
            } else if (args[i].startsWith("--applyLiveEMIMutations")) {
                // Argument: Count of mutated programs to generate
                String[] s = args[i].split("=");
                if (s.length == 1) {
                    logger.error("--applyLiveEMIMutations requires the number of mutated programs to generate as argument");
                    System.exit(1);
                }
                Configuration.applyLiveEMIMutations = true;
                Configuration.mutatedOutputProgramsCount = Integer.parseInt(s[1]);
                if (Configuration.mutatedOutputProgramsCount < 1) {
                    logger.error("Invalid output program count " + Configuration.mutatedOutputProgramsCount + " for --applyLiveEMIMutations");
                    System.exit(1);
                }
            } else if (args[i].equals("--insertLCMSupportCode")) {
                Configuration.insertLCMSupportCode = true;
            } else if (args[i].equals("--injectArraySubscriptChecks")) {
                Configuration.injectArraySubscriptChecks = true;
            } else if (args[i].startsWith("--prng-seed")) {
                String[] s = args[i].split("=");
                Configuration.randomNumbersSeed = Integer.parseInt(s[1]);
            } else if (args[i].startsWith("--contractGenerationFunctionCount")) {
                String[] s = args[i].split("=");
                Configuration.contractGenerationFunctionCount = Integer.parseInt(s[1]);
            } else if (args[i].startsWith("--contractGenerationVariableCount")) {
                String[] s = args[i].split("=");
                Configuration.contractGenerationVariableCount = Integer.parseInt(s[1]);
            } else if (args[i].startsWith("--contractGenerationFunctionStatementsLowerBound")) {
                String[] s = args[i].split("=");
                Configuration.contractGenerationFunctionStatementsLowerBound = Integer.parseInt(s[1]);
            } else if (args[i].startsWith("--contractGenerationFunctionStatementsUpperBound")) {
                String[] s = args[i].split("=");
                Configuration.contractGenerationFunctionStatementsUpperBound = Integer.parseInt(s[1]);
            } else if (args[i].equals("--eventsReplaceStorageLog")) {
                Configuration.eventsReplaceStorageLog = true;
            } else if (args[i].startsWith("--metadataOutput")) {
                String[] s = args[i].split("=");
                Configuration.metadataOutputLogFile = s[1];
            } else if (args[i].startsWith("--interpret")) {
                // Needs "="-separated arguments:
                //     - transactions file in JSON format
                //     - output log file path
                String[] s = args[i].split("=");
                if (s.length != 3) {
                    logger.error("--interpret needs two '='-separated arguments: input .tx-json file and output log path");
                    System.exit(1);
                }
                Configuration.interpretationTransactionsFile = s[1];
                Configuration.interpretationOutputLogFile = s[2];

                Configuration.collectInputTokenPositions = true; // For line numbers in error/warning messages
            } else if (args[i].equals("--inject-loop-limits")) {
                Configuration.injectLoopLimits = true;
            } else if (args[i].equals("--guaranteeSafeExecution")) {
                Configuration.guaranteeSafeExecution = true;
            } else if (args[i].equals("--ganacheOptimizationWorkarounds")) {
                Configuration.ganacheOptimizationWorkarounds = true;
            } else if (args[i].equals("--avoidExponentialOperator")) {
                Configuration.avoidExponentialOperator = true;
            } else if (args[i].equals("--avoidShiftOperators")) {
                Configuration.avoidShiftOperators = true;
            } else if (args[i].equals("--useABIEncoderV2")) {
                Configuration.useABIEncoderV2 = true;
            } else if (args[i].equals("--allowStructsInFunctionABI")) {
                Configuration.allowStructsInFunctionABI = true;
            } else {
                logger.info("Unknown argument " + i + ": '" + args[i] + "'");
                usage();
            }
        }

        if (Configuration.applyLiveEMIMutations) {
            if (Configuration.loadProfilingLog == null) {
                logger.error("Live code mutations require loading an event log");
                usage();
            }
            if (Configuration.solidityOutputFile == null) {
                if (Configuration.mutatedOutputProgramsCount > 1) {
                    logger.error("Multiple mutated programs requested - this requires file output with --solidityOutput");
                    System.exit(1);
                }
            } else {
                int hashCharCount = 0;
                for (int i = 0; i < Configuration.solidityOutputFile.length(); ++i) {
                    if (Configuration.solidityOutputFile.charAt(i) == '#') {
                        ++hashCharCount;
                    }
                }

                if (hashCharCount != 1) {
                    logger.error("The output file name should contain exactly '#' character to insert the output program counter at");
                    System.exit(1);
                }
            }
        }
        if (Configuration.generateFullContractFile != null) {
            if (Configuration.generateTruffleConstructorInvocationFile == null
                || Configuration.generateTruffleTransactionFile == null) {
                logger.error("--generateFullContractFile currently additionally requires --generateTruffleConstructorInvocation and "
                        + "--generateTruffleTransaction to generate all files in one go");
                System.exit(1);
            }
            if (Configuration.contractGenerationFunctionStatementsUpperBound
                    < Configuration.contractGenerationFunctionStatementsLowerBound) {
                logger.error("Setting for --contractGenerationFunctionStatementsUpperBound is smaller than "
                        + " --contractGenerationFunctionStatementsLowerBound");
                System.exit(1);
            }
        }

        // Start transformation work
        Driver driver = new Driver();

        boolean rc = driver.run(System.in, System.out);
        System.exit(rc? /* success */ 0: /* error */ 1);
    }
}
