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

import soltix.ASTEditor;
import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.*;
import soltix.interpretation.variables.*;
import soltix.util.RandomNumbers;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Amend AST with profiling information. We first descend the AST with the interpreter
 * class and collect all relevant node information. To simplify AST traversal, we
 * perform changes after, not during interpretation
 */
public class ProfilingInstrumenter implements IInterpreterCallback {
    private ASTInterpreter astInterpreter;
    private ASTEditor astEditor;
    private RandomNumbers prng;
    private ASTContractDefinition profiledContract;
    private ASTFunctionDefinition currentFunction;
    private ProfilingLogInstrumentation profilingLog;
    private HashMap<ASTNode, ArrayList<ProfilingEvent>> modifications = null;

    public ProfilingInstrumenter(ASTContractDefinition profiledContract, ASTEditor astEditor, RandomNumbers prng) {
        this.profiledContract = profiledContract;
        this.profilingLog = new ProfilingLogInstrumentation(astEditor.getAST(), profiledContract);
        this.astEditor = astEditor;
        this.prng = prng;
    }

    public ProfilingLogInstrumentation getProfilingLog() { return profilingLog; }

    public ASTInterpreter.NavigationPolicy getNavigationPolicy() { return ASTInterpreter.NavigationPolicy.NAVIGATION_POLICY_DESCEND_ALL; }
    public ASTNode nextTargetStatement() { return null; }

    public void initialize(ASTInterpreter astInterpreter) {
        this.astInterpreter = astInterpreter;
    }

    protected boolean includeNode(ASTNode node) {
        if (!Configuration.includeAllNodesForInstrumentation && !prng.flipCoin()) {
            return false;
        }
        return node instanceof ASTIfStatement
                || node instanceof ASTWhileStatement
                || node instanceof ASTDoWhileStatement
                || node instanceof ASTForStatement

                || node instanceof ASTExpressionStatement
                || node instanceof ASTBinaryOperation

                || node instanceof ASTReturnStatement
                // WRONG: || node instanceof ASTFunctionDefinition // Falling off the end of a function, i.e. a final state result as in return

                // Falling off the end of a function, i.e. a final state result as in return
           /*     || (node.getParent() instanceof ASTBlock
                &&  node.getParent().getParent() instanceof ASTFunctionDefinition
                &&  node == node.getParent().getChild( node.getParent().getChildCount() - 1) )*/ // Last child in function body?

                // Top of function
                || (node.getParent() instanceof ASTBlock
                &&  node.getParent().getParent() instanceof ASTFunctionDefinition
                &&  node == node.getParent().getChild(0)) // First child in function body?
                || node instanceof ASTVerbatimText; // Presumably a complex generated statement
    }

    /*
    public void start(ASTFunctionDefinition function) {
        currentFunction = function;
    }*/

    public void visitNodeAfterProcessing(ASTNode node) throws Exception { }

    // We have to prepend the statement before processing the node to avoid including variables
    // that may be introdced by it, e.g. in
    //     emit e(); // don't want i
    //     for (uint i = 0; ...
    public void visitNodeBeforeProcessing(ASTNode node) throws Exception {
        if (!includeNode(node)) {
            return;
        }

        // Inject emit statements where desirable. To avoid interfering with the tree traversal,
        // we record all changes first and apply them once the interpreter is done
        ArrayList<ProfilingEvent> events = profilingLog.constructEventList(node, astInterpreter.getScope());

        //node.setProfilingEvents(events);
        node.setVariableEnvironment(new VariableEnvironment(astEditor.getAST(), events, node.getID(), false));
        if (modifications == null) {
            modifications = new HashMap<ASTNode, ArrayList<ProfilingEvent>>();
        }
        // Note: We unconditionally include the event - even if no variables are visible - to obtain
        // coverage information
        modifications.put(node, events);
    }

    public void finish() throws Exception {
        if (Configuration.profiling && modifications != null) {
            if (Configuration.loadProfilingLog != null) {
                // We only re-ran the instrumentation to obtain event and event argument type info for
                // the interpretation of an event log. Thus we can skip the AST output (which is beneficial
                // to avoid event "noise" for the code trace output)
                return;
            }

            for (HashMap.Entry<ASTNode, ArrayList<ProfilingEvent>> entry : modifications.entrySet()) {
                ASTNode instrumentedStatement = entry.getKey();
                ArrayList<ProfilingEvent> instrumentationEvents = entry.getValue();

                if (instrumentedStatement instanceof ASTFunctionDefinition) {
                    // Special case: Falling off the end of a function. It must already have a block, and
                    // the emit statement should be the last child node of that block
                    ASTFunctionDefinition function = (ASTFunctionDefinition)instrumentedStatement;
                    ASTBlock body = function.getBody();
                    // Only do this if the last statement isn't a return statement anyway
                    // (note that this doesn't cover cases like a block at the end, possibly as control structure
                    // bodoy, conaining a return)
                    if (body == null) {
                        // This is named as a function "definition" in the AST, but is really only
                        // a declaration - with no body and thus no need to instrumetn anything
                        ;
                    } else if (body.getChildCount() == 0 || !(body.getChild(body.getChildCount()-1) instanceof ASTReturnStatement)) {
                        astEditor.appendProfilingEmitStatementToBlock(function.getBody(), instrumentationEvents);
                    }
                } else {
                    astEditor.prependProfilingEmitStatement(instrumentedStatement, instrumentationEvents);
                }

                // Generate event definitions
                for (ProfilingEvent event : instrumentationEvents) {
                    astEditor.generateProfilingEventDefinition(profiledContract, event);
                }
            }
            modifications = null;
        }
    }


    public void run() throws Exception {
        throw new Exception("Invalid call to ProfilingInstrumenter.run");
    }
}
