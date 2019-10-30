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

import jdk.nashorn.internal.objects.annotations.Function;
import soltix.ast.*;
import soltix.mutation.LiveEMIMutator;
import soltix.synthesis.TransactionGenerator;
import soltix.util.RandomNumbers;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import soltix.input.*;
import soltix.output.*;
import soltix.interpretation.*;
import soltix.profiling.*;
import soltix.synthesis.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Driver {
    final static Logger logger = Logger.getLogger(Driver.class);


    public boolean run(InputStream input, OutputStream output) {
        AST ast = null;
        ASTEditor editor = null;

        if (Configuration.metadataOutputLogFile != null) {
            // Open metadata output file. Any component that needs to write something will
            // do so itself, since this is currently difficult to collect in one place
            if (!openMetadataOutputLogFile()) {
                return false;
            }
        }

        if (Configuration.generateFullContractFile == null) {
            // Read input file
            Parser parser = new ParserASTJSON();

            try {
                ast = parser.parse(input);
            } catch (Exception e) {
                logger.error("Parser exception: " + e.toString());
                e.printStackTrace();
                return false;
            }

            editor = new ASTEditor(ast);

            // Sort functions and contracts by statement size (currently only required by the "reduceFunctions"  and
            // truffle tx generation functions, but inexpensive enough)
            ast.sortFunctionsBySize();
            ast.sortContractsBySize();
        }

        if (Configuration.outputTopLevelContract != null) {
            if (!outputTopLevelContract(ast)) {
                return false;
            }
        }

        if (Configuration.injectLoopLimits) {
            if (Configuration.generateFullContractFile == null) { // if generating, we do it later
                if (!injectLoopLimits(editor)) {
                    return false;
                }
            }
        }


        if (Configuration.interpretationTransactionsFile != null) {
            if (!interpretProgram(ast, Configuration.interpretationTransactionsFile, Configuration.interpretationOutputLogFile)) {
                return false;
            }

            if (Configuration.solidityOutputFile != null) {
                if (!writeSolidityOutput(ast, output, Configuration.solidityOutputFile)) {
                    return false;
                }
            }
            return true;
        }

        if (Configuration.eventsReplaceStorageLog && Configuration.insertLCMSupportCode) {
            if (!generateOutroFunctions(ast)) {
                return false;
            }
        }

        JSONObject tempTransactionsJSONObject = new JSONObject();
        String tempConstructedObjectName = null;
        if (Configuration.generateTruffleConstructorInvocationContract != null
                && Configuration.generateFullContractFile == null) { // done later for this setting
            tempConstructedObjectName = generateTruffleConstructorInvocation(ast,
                    tempTransactionsJSONObject,
                    ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_EXCLUSIVELY_SMALL);
            if (tempConstructedObjectName == null) {
                return false;
            }
        }
        if (Configuration.generateTruffleTransactionContract != null
                && Configuration.generateFullContractFile == null) { // done differently for this setting
            if (!generateTruffleTransaction(ast, tempTransactionsJSONObject, tempConstructedObjectName, ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_EXCLUSIVELY_SMALL)) {
                return false;
            }
        }

        if (Configuration.generateContractFunctionsFile != null) {
            if (!generateContractFunctionsFile(ast)) {
                return false;
            }
        }
        if (Configuration.reduceFunctions != null) {
            if (!reduceFunctions(ast, editor, new RandomNumbers(Configuration.randomNumbersSeed))) {
                return false;
            }
        }

        // Compute profiling event information and, if we're generating an instrumented contract, also
        // edit the AST to contain event definitions and emissions.
        // If we're only reading a recorded profiling log, we still wish to compute the event information
        // in order to interpret the log file - but the Profiler class won't edit the AST if the
        // "profiling" config option isn't set
        if (Configuration.profiling || Configuration.loadProfilingLog != null) {
            // TODO It's unclear whether:
            //    1. It makes sense to have one ProfilingLog for each contract, or one for all contracts.
            //       This will also depend on whether a single log will contain results for multiple
            //       transactions/contracts.
            //    2. Re-computing the event info is really needed or may be superfluous due to self-contained
            //       log information

            // Generate profiling instrumentation separately for each contract, and for each function
            if (!performProfilingInstrumentations(ast, editor)) {
                return false;
            }

            // If available, load profiling log as well
            if (Configuration.loadProfilingLog != null) {
                if (!loadProfilingLog(ast)) {
                    return false;
                }
            }
        }

        // Rename exceptional events if requested (after all other AST processing operations to
        // avoid interferences)
        /*if (Configuration.renameExceptionalEvents)*/ if (Configuration.generateFullContractFile == null) {
            if (!renameExceptionalEvents(editor)) {
                return false;
            }
        }

        if (Configuration.applyLiveEMIMutations) {
            for (int i = 0; i < Configuration.mutatedOutputProgramsCount; ++i) {
                try {
                    if (!applyLiveCodeMutations(ast, editor)) {
                        return false;
                    }
                } catch (Exception e) {
                    logger.error("Exception during live code mutation: " + e.toString());
                    e.printStackTrace();
                    return false;
                }

                ++Configuration.randomNumbersSeed;
                // Write debug graph plot, if requested
                if (Configuration.debugGraphOutputFile != null) {
                    String outputFile = Configuration.debugGraphOutputFile.replace("#", String.valueOf(i));
                    writeDebugGraphOutput(ast, outputFile); // ignore return, keep going on error:
                }
                // Write Solidity output file
                String solidityOutputFile = Configuration.solidityOutputFile != null
                        ? Configuration.solidityOutputFile.replace("#", String.valueOf(i))
                        : null;

                //  System.exit(1);
                if (!writeSolidityOutput(ast, output, solidityOutputFile)) {
                    return false;
                }
            }
        } else {
            // A single non-mutated output file
            // TODO merge this with the above - generate one base contract and multiple mutations
            String solidityOutputFile = Configuration.solidityOutputFile;

            if (Configuration.generateFullContractFile != null) {
                // Generate contract and corresponding constructor invocation plus transactions
                solidityOutputFile = Configuration.generateFullContractFile;

                RandomNumbers prng = new RandomNumbers(Configuration.randomNumbersSeed);
                ast = new AST();
                AST transactionsAST = new AST();
                JSONObject transactionsJSONObject = new JSONObject();

                // Combined contract and transactions generation
                try {
                    ContractGenerator contractGenerator = new ContractGenerator(ast, transactionsAST, transactionsJSONObject, prng);
                    contractGenerator.run();

                    editor = new ASTEditor(ast);

                    Configuration.insertLCMSupportCode = true;
                    if (!generateOutroFunctions(ast)) {
                        return false;
                    }
                    if (!renameExceptionalEvents(editor)) { // Generate mutations support code/variables first
                        return false;
                    }


                    if (Configuration.injectLoopLimits) {
                        // Inject loop limits for original contract, so that loops generated in the mutation stage
                        // below
                        editor = new ASTEditor(ast);
                        if (!injectLoopLimits(editor)) {
                            return false;
                        }
                    }

                    // Write Solidity output file
                    if (!writeSolidityOutput(ast, output, solidityOutputFile)) {
                        return false;
                    }

                    // Now that the contract is available, we can generate a truffle invocation file
                    // TODO support multi-contract instantiations
                    String constructedObjectName = generateTruffleConstructorInvocation(ast,
                            transactionsJSONObject,
                            ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL);

                    if (constructedObjectName == null) {
                        return false;
                    }

                    // Quick and dirty: Always generate 4 mutated contracts to get this up and running.
                    // TODO As stated above, this needs to be merged properly with non-generated contracts
                    // At this point VariableEnvironment snapshots at locations which may be mutated have already
                    // been collected by the code generation/interpretation
                    for (int i = 0; i < 4; ++i) {
                        System.out.println("-----applying mutation " + i);
                        if (!applyLiveCodeMutations(ast, editor)) {
                            return false;
                        }
                        ++Configuration.randomNumbersSeed;

                        System.out.println("writing to /tmp/mutated" + i + ".sol");
                        if (!writeSolidityOutput(ast, output, "/tmp/mutated" + i + ".sol")) {
                            return false;
                        }
                    }

                    // Write previously generated transactions to file. First add the outro() calls
                    // TODO this probably needs to be merged with the construction above for multi-contracts
                    for (ASTContractDefinition contract : ast.getContracts()) {
                        ASTFunctionDefinition outroFunction = contract.getOutroFunction();
                        TransactionGenerator transactionGenerator = contractGenerator.getTransactionGenerator();
                        logger.info("Generating tx for result analysis " + outroFunction.getName() + " in " + contract.getName());
                        transactionGenerator.generateTruffleTransaction(ast, transactionsAST, transactionsJSONObject, constructedObjectName, contract, outroFunction, ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL);
                    }

                    FileOutputStream file = new FileOutputStream(Configuration.generateTruffleTransactionFile);
                    Emitter.emitJavaScriptCode(file, transactionsAST); //.emitGraphPlot(file, ast);
                    file.close();

                    writeJSONFile(Configuration.generateTruffleTransactionFileJSON, transactionsJSONObject);

                    return true; // TODO Unify the above operations properly with the output operations below as well
                } catch (Exception e) {
                    logger.error("Contract generation exception: " + e.toString());
                    e.printStackTrace();
                    return false;
                }
            }



            //Emitter emitter = new Emitter();

            // Write debug graph plot, if requested
            if (Configuration.debugGraphOutputFile != null) {
                writeDebugGraphOutput(ast, Configuration.debugGraphOutputFile); // ignore return, keep going on error:
            }
            // Write Solidity output file
            if (!writeSolidityOutput(ast, output, solidityOutputFile)) {
                return false;
            }
        }

        printMemoryUsageStatistics();
        return true;
    }

    protected void writeJSONFile(String jsonFilePath, JSONObject transactionsJSONObject) throws Exception {
        FileWriter file = new FileWriter(jsonFilePath);
        file.write(transactionsJSONObject.toJSONString());
        file.flush();
    }

    protected boolean injectLoopLimits(ASTEditor editor) {
        try {
            editor.injectLoopLimits();
        } catch (Exception e) {
            logger.error("Exception while injecting loop limits: " + e.toString());
            e.printStackTrace();;
            return false;
        }
        return true;
    }

    protected boolean generateOutroFunctions(AST ast) {
        for (ASTContractDefinition contract : ast.getContracts()) {
            try {
                if (contract.getContractKind() == Definitions.ContractKind.CONTRACT_KIND_CONTRACT) {
                    StateEventOutputGenerator.generateOutroFunction(ast, contract);
                }
            } catch (Exception e) {
                logger.error("Cannot generate outro function: " + e.toString());
                return false;
            }
       }
       return true;
   }

    protected boolean openMetadataOutputLogFile() {
        try {
            FileOutputStream file = new FileOutputStream(Configuration.metadataOutputLogFile);
            PrintWriter printWriter = new PrintWriter(file, true);
            Configuration.metadataOutputLogWriter = printWriter;
        } catch (Exception e) {
            logger.error("Cannot open metadata output file " + Configuration.metadataOutputLogFile + ": " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    protected boolean outputTopLevelContract(AST ast) {
        //ASTContractDefinition topLevelContract;
        try {
            //topLevelContract = TransactionGenerator.findTopLevelContract(ast, true, 0);
            ArrayList<ASTContractDefinition> transactableContracts = TransactionGenerator.getTransactableContracts(ast);

            FileOutputStream file = new FileOutputStream(Configuration.outputTopLevelContract);
            PrintWriter printWriter = new PrintWriter(file);
            if (transactableContracts == null) {
                logger.error("Cannot find transactable contract");
                return false;
            }
            for (ASTContractDefinition contract : transactableContracts) {
                printWriter.println(contract.getName());
            }
            printWriter.flush();
            file.close();
        } catch (Exception e) {
            logger.error("Cannot output top-level contract: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected boolean generateContractFunctionsFile(AST ast) {
        try {
            FileOutputStream file = new FileOutputStream(Configuration.generateContractFunctionsFile);
            PrintWriter writer = new PrintWriter(file);
            // Output every function with the format <contract-name>:<function-name>, ordered descending by size (in
            // AST nodes)
            for (FunctionScope function : ast.getFunctions()) {
                ASTContractDefinition contract = (ASTContractDefinition)function.getParent();
                writer.println(contract.getName() + ":" + function.getName());
            }
            writer.flush();
            file.close();
        } catch (Exception e) {
            logger.error("Exception: Cannot generate contract functions file "
                    + Configuration.generateContractFunctionsFile
                    + ": " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected String generateTruffleConstructorInvocation(AST ast, JSONObject transactionsJSONObject, ValueGenerator.IntegerGenerationPolicy integerGenerationPolicy) {
        AST constructorInvocationAST = new AST();
        ASTContractDefinition contract = ast.getContract(Configuration.generateTruffleConstructorInvocationContract);
        String constructedObjectName = "c"; // TODO

        if (contract == null) {
            logger.error("generateTruffleConstructorInvocation: Cannot locate requested contract " +
                Configuration.generateTruffleConstructorInvocationContract);
            return null;
        }
        try {
            TransactionGenerator transactionGenerator = new TransactionGenerator(new RandomNumbers(Configuration.randomNumbersSeed));
            transactionGenerator.generateTruffleConstructorInvocation(ast, constructorInvocationAST, transactionsJSONObject, constructedObjectName, contract, integerGenerationPolicy);
            //Emitter emitter = new Emitter();
            FileOutputStream file = new FileOutputStream(Configuration.generateTruffleConstructorInvocationFile);
            Emitter.emitJavaScriptCode(file, constructorInvocationAST); //.emitGraphPlot(file, ast);
            file.close();
        } catch (Exception e) {
            logger.error("Exception: Cannot generate truffle constructor invocation for "
                    + Configuration.generateTruffleConstructorInvocationContract
                    + ": " + e.toString());
            e.printStackTrace();
            return null;
        }
        return constructedObjectName;
    }

    protected void generateOutroCall(AST ast, 
                                     ASTContractDefinition contract, 
                                     TransactionGenerator transactionGenerator, 
                                     FileOutputStream file,
                                     JSONObject transactionsJSONObject,
				     String calledObjectName,
                                     ValueGenerator.IntegerGenerationPolicy integerGenerationPolicy) throws Exception {
        if (contract.getOutroFunction() != null) {
            // Terminate transactions list with final state output
            AST transactionAST = new AST();
            ASTFunctionDefinition outroFunction = contract.getOutroFunction();
            logger.info("Generating tx for result analysis " + outroFunction.getName() + " in " + contract.getName());
            transactionGenerator.generateTruffleTransaction(ast, transactionAST, transactionsJSONObject, calledObjectName, contract, outroFunction, integerGenerationPolicy);
            Emitter.emitJavaScriptCode(file, transactionAST);
        }
    }

    protected boolean generateTruffleTransaction(AST ast, JSONObject transactionsJSONObject, String calledObjectName, ValueGenerator.IntegerGenerationPolicy integerGenerationPolicy) {
        //AST transactionAST = new AST();
        //ASTEditor constructorInvocationEditor = new ASTEditor(transactionAST);
        ASTContractDefinition contract = ast.getContract(Configuration.generateTruffleTransactionContract);

        try {
            // Determine largest function to call
            ArrayList<ASTFunctionDefinition> callableFunctions = null;

            // Generate tx for largest function - currently preferring one that doesn't
            // take any address arguments, since these typically result in revert() calls
            // due to unmatched identity expectations
            callableFunctions = ast.getTransactableFunctions(contract, true);
            if (callableFunctions == null) {
                throw new Exception("Contract has no function to generate a transaction for");
            }

            //Emitter emitter = new Emitter();
            FileOutputStream file = new FileOutputStream(Configuration.generateTruffleTransactionFile);

            TransactionGenerator transactionGenerator = new TransactionGenerator(new RandomNumbers(Configuration.randomNumbersSeed));

            for (ASTFunctionDefinition functionToCall : callableFunctions) {
                AST transactionAST = new AST();
                logger.info("Generating tx for " + functionToCall.getName() + " in " + contract.getName());
                transactionGenerator.generateTruffleTransaction(ast, transactionAST, transactionsJSONObject, calledObjectName, contract, functionToCall, integerGenerationPolicy);
                Emitter.emitJavaScriptCode(file, transactionAST); //.emitGraphPlot(file, ast);
            }
            generateOutroCall(ast, contract, transactionGenerator, file, transactionsJSONObject, calledObjectName, integerGenerationPolicy);
            file.close();

            writeJSONFile(Configuration.generateTruffleTransactionFileJSON, transactionsJSONObject);
        } catch (Exception e) {
            logger.error("Exception: Cannot generate truffle tx for "
                    + Configuration.generateTruffleConstructorInvocationContract
                    + ": " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected boolean reduceFunctions(AST ast, ASTEditor editor, RandomNumbers prng) {
        ArrayList<String> reduceFunctions;

        if (Configuration.reduceFunctions != null
                && Configuration.reduceFunctions.size() == 1
                && Configuration.reduceFunctions.get(0).equals("all")) {
            reduceFunctions = new ArrayList<String>();
            for (ASTContractDefinition contract : ast.getContracts()) {
                for (ASTFunctionDefinition function : contract.getFunctions()) {
                    String reduceFunction = contract.getName() + ":" + function.getName();
                    reduceFunctions.add(reduceFunction);
                }
            }
        } else {
            reduceFunctions = Configuration.reduceFunctions;
        }

        for (String requestedRemoval : reduceFunctions) {
            // Assume format "<contract>:<function>"
            String[] s = requestedRemoval.split(":");
            ASTContractDefinition contract = ast.getContract(s[0]);
            if (contract == null) {
                logger.error("Function removal request for unknown contract " + s[0]);
                return false;
            }
            FunctionScope functionScope = null;
            ASTFunctionDefinition function = contract.getFunction(s[1]);
            if (function == null) {
                ASTModifierDefinition modifierDefinition = contract.getModifier(s[1]);
                if (modifierDefinition == null) {
                    logger.error("Function removal request for unknown function " + s[1]);
                    return false;
                } else {
                    functionScope = modifierDefinition;
                }
            } else {
                functionScope = function;
            }
            try {
                editor.emptyFunctionBody(functionScope, prng);
            } catch (Exception e) {
                logger.error("Exception while trying to reduce function " + function.getName() + ": " + e.toString());
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    protected boolean performProfilingInstrumentations(AST ast, ASTEditor editor) {
        HashMap<ASTContractDefinition, ProfilingLogInstrumentation> profilingInstrumentations = null;
        profilingInstrumentations = new HashMap<ASTContractDefinition, ProfilingLogInstrumentation>();

        RandomNumbers prng = new RandomNumbers(Configuration.randomNumbersSeed);

        for (ASTContractDefinition contract : ast.getContracts()) {
            ProfilingInstrumenter profiler = new ProfilingInstrumenter(contract, editor, prng);
            ASTInterpreter interpreter = new ASTInterpreter(ast, profiler);

            for (ASTFunctionDefinition function : contract.getFunctions()) {
                // Avoid instrumenting (and thus mutating) constructors, since we currently have no way to collect
                // constructor events in truffle. Consequently, due to lack of variable information, mutations may
                // generate invalid instructions
                // TODO Fix this if possible, e.g. in the other framework
                if (function.isConstructor()) {
                    continue;
                }
                try {
                    interpreter.run(contract, function);
                    profilingInstrumentations.put(contract, profiler.getProfilingLog());
                } catch (Exception e) {
                    logger.error("Cannot interpret function " + function.getNonNullName() + ": " + e.toString());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean loadProfilingLog(AST ast) {
        // See TODO above comments on log instantiation/separation (separate/do not separate by contracts? transactions?)
        ProfilingLogRecording log = new ProfilingLogRecording(ast);
        try {
            log.loadEventLog(Configuration.loadProfilingLog);
        } catch (Exception e) {
            logger.error("Cannot load profiling log " + Configuration.loadProfilingLog + ": " + e.toString());
            e.printStackTrace();
            return false;
        }

        ProfilingResultLoader loader = new ProfilingResultLoader(log);
        ASTInterpreter interpreter = new ASTInterpreter(ast, loader);

        if (log.getContract() == null) {
            // This probably means that the log is empty, which can happen if transactions fail silently.
            // For example, this occurred for a call to an internal function, which is not allowed but
            // didn't throw a catchable exception or otherwise produce any truffle error output either.
            //
            // Another case - TODO: unsolved, e.g. 01309807ccf34109bcdf49ab0acead2e8db4620c76c11cd269062593e2566dd9 -
            // is contract function name clashes with truffle transaction invocations.
            // E.g.  instance.send.sendTransaction();   where send() represents an ordinary contract function
            // that is apparently misinterpreted as a meta command like sendTransaction()
            logger.error("Cannot locate data in profiling log - possibly empty due to invalid transaction aborting the test");
            return false;
        }
        try {
            interpreter.run(log.getContract(), log.getFirstEventFunction());
        } catch (Exception e) {
            logger.error("Cannot interpret profiling log (contract "
                    + log.getContract().getName()
                    + ", first function " + log.getFirstEventFunction().getName()
                    + "): " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    protected boolean applyLiveCodeMutations(AST ast, ASTEditor editor) throws Exception {
        LiveEMIMutator mutator = new LiveEMIMutator(ast, editor);
        try {
            mutator.run();
        } catch (Exception e) {
            logger.error("EMI live code mutation failed: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected boolean renameExceptionalEvents(ASTEditor editor) {
        try {
            editor.insertGeneratedSupportCode();
        } catch (Exception e) {
            logger.error("Cannot insert custom exception handlers: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected boolean writeDebugGraphOutput(AST ast, String outputFile) {
        FileOutputStream file = null;

        try {
            file = new FileOutputStream(outputFile);
            Emitter.emitGraphPlot(file, ast);
            file.close();
            file = null;
        } catch (Exception e) {
            logger.error("Cannot open debug graph output file " + outputFile + ": " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected boolean writeSolidityOutput(AST ast, OutputStream consoleOutputStream, String outputFilePath) {
        // Write Solidity output - if an output file is specified, write to that, otherwise to
        // the same supplied (console) output stream to which AST debug output is also written
        //  OutputStream solidityOutputStream = output;

        // Save size metric
        // TODO Make this useful in the presence of multiple output operations
        Configuration.writeMetadataLog("totalCodeSize", String.valueOf(ast.getTotalFunctionBodyNodeCount()));

        FileOutputStream file = null;
        if (outputFilePath != null) {
            try {
                file = new FileOutputStream(outputFilePath);
            } catch (Exception e) {
                logger.error("Cannot open Solidity output file " + Configuration.solidityOutputFile + ": " + e.toString());
                return false;
            }
        }
        try {
            Emitter.emitSolidityCode(file != null ? file : consoleOutputStream, ast);
        } catch (Exception e) {
            logger.error("Solidity code output error: " + e.toString());
            e.printStackTrace();
            return false;
        }
        if (file != null) {
            try {
                file.flush();
                file.close();
                file = null;
            } catch (Exception e) {
                logger.error("Cannot flush/close Solidity output file: " + e.toString());
                return false;
            }
        }

        return true;
    }


    protected boolean interpretProgram(AST ast, String transactionsJSONFile, String outputLogFile) {
        JSONParser parser = new JSONParser();
        JSONObject transactionsJSONObject;

        try {
            transactionsJSONObject = (JSONObject) parser.parse(new FileReader(transactionsJSONFile));
        } catch (Exception e) {
            logger.error("Driver.interpretProgram: Exception while loading tx-json file " + transactionsJSONFile + ": " + e.toString());
            return false;
        }

        JSONArray transactionsJSONArray = (JSONArray)transactionsJSONObject.get("transactions");
        FullInterpreter fullInterpreter = new FullInterpreter(transactionsJSONArray);
        ASTInterpreter interpreter = new ASTInterpreter(ast, fullInterpreter);
        try {
            interpreter.run();
        } catch (Exception e) {
            logger.error("Interpretation exception: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }



    protected String toMegaBytesString(long bytes) {
        return String.valueOf(bytes / 1000000) + " MB";
    }
    protected void printMemoryUsageStatistics() {
        MemoryUsage mu =ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage muNH = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        System.out.println("Memory Usage: \n"
                        + "\tInit :"+ toMegaBytesString(mu.getInit()) +
                        "\n\tMax :" + toMegaBytesString(mu.getMax()) +
                        "\n\tUsed :" + toMegaBytesString(mu.getUsed()) +
                        "\n\tCommited :" + toMegaBytesString(mu.getCommitted()) +
                        "\n\tInit NH :" + toMegaBytesString(muNH.getInit()) +
                        "\n\tMax NH :" + toMegaBytesString(muNH.getMax()) +
                        "\n\tUsed NH:" + toMegaBytesString(muNH.getUsed()) +
                        "\n\tCommited NH:" + toMegaBytesString(muNH.getCommitted()));
        System.out.println("\t----------");
        System.out.println("\tTotal: " + toMegaBytesString(Runtime.getRuntime().totalMemory()));
        System.out.println("\tFree: " + toMegaBytesString(Runtime.getRuntime().freeMemory()));
        System.out.println("\tUsed: " + toMegaBytesString(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
    }
}
