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

import soltix.ast.ASTNode;
import soltix.interpretation.TypeContainer;
import soltix.synthesis.FunctionGenerator;
import soltix.util.RandomNumbers;

import java.io.PrintWriter;

/**
 * Configuration options for hardcoded as well as command-line-controllable settings.
 */
public class Configuration {
    // Print structured AST-JSON output for debugging
    static public boolean debugASTOutput = false;
    // Collect AST-JSON line number info
    static public boolean collectInputTokenPositions = false;
    // Don't build AST - only print AST-JSON input (avoids stopping on errors)
    static public boolean skipASTProcessing = false;
    static public String debugGraphOutputFile = null;
    static public String solidityOutputFile = null;
    static public String outputTopLevelContract = null;
    static public boolean profiling = false;
    static public boolean showCoverage = false;
    static public String loadProfilingLog = null;
    static public String[] reduceFunctions = null;
    static public String generateTruffleConstructorInvocationContract = null;
    static public String generateTruffleConstructorInvocationFile = null;
    static public String generateTruffleTransactionContract = null;
    static public String generateTruffleTransactionFile = null;
    static public String generateTruffleTransactionFileJSON = null;
    // Output all (reducible) functions in all contracts to a file, enabling the caller to request reductions
    // Format:
    //   - One line per contract
    //   - Line format: <contract-name> <func1-name> <func2-name> ...
    // Functions are ordered in descending size (metric: AST node count)
    static public String generateContractFunctionsFile = null;

    static public String generateFullContractFile = null;
    static public FunctionGenerator.FunctionType generateFullContractFunctionType = null;

    // To avoid uncontrolled test case abortion for revert()/require()/assert() invocations in our
    // test framework, we replace these calls with our own versions as follows:
    //
    //   revert() -> _myrevert(callid)
    //   assert(foo) ->  _myassert(foo, callid)
    //   require(foo) -> _myrequire(foo, callid)      TODO multi-arg require()?!
    //
    // Our current policy is to emit an event (EXCEPTION_REVERTED / EXCEPTION_ASSERT_FAILURE / EXCEPTION_REQUIRE_FAILURE)
    // in these replacement functions instead of aborting the test case. This achieves better test case coverage, as it
    // will just continue as if the condition were fulfilled, but could potentially execute undefined constructs
    // TODO Do undefined constructs that must be avoided exist in EVM and what are they? Div-by-0? Integer overflow? ...
    static public boolean renameExceptionalEvents = false;
    static public int renameNowTimestamps = 0;
    static public boolean renameThrowStatements = false;

    static public boolean injectArraySubscriptChecks = false;
   // static public boolean injectNullContractChecks = false;

    static public boolean applyLiveEMIMutations = false;
    static public int mutatedOutputProgramsCount = 0;
    static public boolean includeAllNodesForInstrumentation = false;

    static public boolean checkRuntimeTautologyCorrectness = false;
    static public boolean checkCompileTimeTautologyCorrectness = true;

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // The settings below define the main properties for the generation of any code (expressions, predicates,
    // contracts, etc.).
    //
    // Without intervening software changes or different mutation/test policy changes, a set of these values uniquely
    // identifies a contract that can be generated with these settings
    //
    // It would be nice to wrap this up in a static class but it's not clear how
    //static public class GenerationProperties {
        static public int randomNumbersSeed = 0x3f2a7b4e;

        static public int randomPredicateDepth(RandomNumbers prng) {
            final int lowerDepthBound = 2;
            final int upperDepthBound = 5;
            return (int) prng.generateLongInteger(lowerDepthBound, upperDepthBound);
        }

        static public int contractGenerationFunctionCount = 10;
        static public int contractGenerationVariableCount = 20;
        static public int contractGenerationFunctionStatementsLowerBound = 1;
        static public int contractGenerationFunctionStatementsUpperBound = 100;
    //};

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // Request insertion of helper storage variables for Live Code Mutation. This should already be done when
    // preparing the original contract, since we want to avoid false positives caused by storage reorganization
    static public boolean insertLCMSupportCode = false;


    // Ignore "view" keyword (thrown away on code output as well). See also comments in ASTFunctionDefinition.toSolidityCode
    static public boolean dropViewMutability = true;

    // The truffle framework has a problem with unnamed event definition parameters, as in
    //    event foo(uint);
    // rather than
    //    event foo(uint param);
    // which causes tests to fail - see https://github.com/trufflesuite/truffle/issues/494.
    // With this option, we insert generated names for unnamed parameters
    static public boolean nameUnnamedEventDefinitionParameters = true;

    static public boolean verboseDebugOutput = false;

    static public boolean avoidGeneratingSubexpressionSideEffects = false;

    // To avoid generating more exponential operator cases unil the bugfixes on exponential operations are integrated:
    // Fixed in 0.4.25
    // ... disabled again due to ganache-cli showstopper runtime errors
    static public boolean avoidExponentialOperator = false;
    static public boolean avoidShiftOperators = false; //false;
    // For some generation purposes, short-circuiting operators are avoided to ensure side effect manifestation for
    // each subexpression
    // TODO Remove this or take care of the conditional operator as well. Currently an expression
    static public boolean avoidLogicalAndConditionalOperators = false;

    // Enable higher-level tracking of variables: Rather than observe storage write operations, we have a final event
    // emission outro routine to transfer values of storage values to the event log.
    // This option should be used only once for the original contract, since instrumented and mutated contracts can
    // reuse the outro code and events
    static public boolean eventsReplaceStorageLog = true;

    // File to write metadata to for further (generated) contract analysis
    static public String metadataOutputLogFile = null;
    static public PrintWriter metadataOutputLogWriter = null;
    static public void writeMetadataLog(String key, String value) {  // TODO JSON? E.g. to support logs for mutated contracts as well?
        if (metadataOutputLogWriter != null) {
            metadataOutputLogWriter.println(key + "=" + value);
        }
    }

    static public int languageVersionMajor = 0;
    static public int languageVersionMinor = 5;


    static public String interpretationTransactionsFile = null;
    static public String interpretationOutputLogFile = null;


    static public boolean injectLoopLimits = false;
    static public boolean guaranteeSafeExecution = false;

    static public boolean ganacheOptimizationWorkarounds = false;

    // TODO For now we don't use structs because this requires the currently disbaled super slow ABIEncoderV2. It should also be tested, however
    static public boolean allowStructsInFunctionABI = false; // affects args + return value

    static public boolean useABIEncoderV2 = false;
}
