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

import soltix.ast.*;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.expressions.Expression;
import soltix.profiling.*;
import soltix.interpretation.variables.*;
import soltix.synthesis.ValueGenerator;
import soltix.util.RandomNumbers;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Low-level AST node creation/insertion/modification routines
 * TODO this class does too many different, too high-level things and doesn't really belong anywhere - split up?
 */
public class ASTEditor {
    private AST ast;

    public ASTEditor(AST ast) {
        this.ast = ast;
    }

    public AST getAST() { return ast; }

    public void insertGeneratedSupportCode() throws Exception {
        // As per the Configuration.renameExceptionalEvents option, generate custom functions to which
        // revert()/assert()/require() calls may be rerouted
        //
        // Deactivate callid arguments for now, since they are currently not used and the additional argument
        // causes a "stack too deep" error for at least one Securify test suite case.
        //
        // Also deactivate event emissions since these seemed to break many test cases
        // TODO Reactivate this - the issue was most likely not ignorning the "pure" function state mutability
        String revertCode =
                //"event EXCEPTION_REVERTED(uint callid);"
              //+ "function _myrevert(uint callid) internal { "
                  "event EXCEPTION_REVERTED();"
                + "function _myrevert() internal { "
                +     "emit EXCEPTION_REVERTED();"
                + "}";
        ASTVerbatimText revertCodeNode = new ASTVerbatimText(0, revertCode);
        revertCodeNode.finalize();

        String assertCode =
              //  "event EXCEPTION_ASSERT_FAILURE(uint callid);"
              //+ "function _myassert(bool expr, uint callid) internal { "
                  "event EXCEPTION_ASSERT_FAILURE();"
                + "function _myassert(bool expr) internal { "
                +     "if (!expr) {"
                +         "emit EXCEPTION_ASSERT_FAILURE();"
                +     "}"
                + "}";
        ASTVerbatimText assertCodeNode = new ASTVerbatimText(0, assertCode);
        assertCodeNode.finalize();

        String requireCode =
              //  "event EXCEPTION_REQUIRE_FAILURE(uint callid);"
              //+ "function _myrequire(bool expr, uint callid) internal { "
                  "event EXCEPTION_REQUIRE_FAILURE();"
                + "function _myrequire(bool expr) internal { "
                +     "if (!expr) {"
                +         "emit EXCEPTION_REQUIRE_FAILURE();"
                +     "}"
                + "}";
        ASTVerbatimText requireCodeNode = new ASTVerbatimText(0, requireCode);
        requireCodeNode.finalize();

        // Predictable "now"/"block.timestamp" replacement (constant to allow insertion in "library" contracts -
        // otherwise there's a compiler error):
        String nowCode = "uint constant _mynow = " + String.valueOf(Configuration.renameNowTimestamps) + ";";
        ASTVerbatimText nowCodeNode = new ASTVerbatimText(0, nowCode);
        nowCodeNode.finalize();

        // We define a special storage location and a known constant value that may be assigned to it to transmit
        // OOB information on the program execution. By convention, SSTORE assignments between (and including)
        // two markers may be ignored when comparing logs. This allows us to modify and restore storage in
        // mutated programs without affecting the SSTORE comparison with unmutated programs
        String codeBlockMutationsMarkerCode = "uint256 _internalMutationsMarkerStorage; uint256 constant _internalMutationsMarkerValue = 0xABABABABABABABAB"
                                                                                                                     +    "DCDCDCDCDCDCDCDC"
                                                                                                                     +    "EAEAEAEAEAEAEAEA"
                                                                                                                     +    "BCBCBCBCBCBCBCBC" + ";";
        ASTVerbatimText codeBlockMutationsMarkerNode = new ASTVerbatimText(0, codeBlockMutationsMarkerCode);


        // Alleviate stack space pressure by declaring some temporary variables for mutations in storage as well
        String liveCodeMutationsBackupVariablesCode = "uint256 _internalBackupInteger; address _internalBackupAddress; bool _internalBackupBool; string _internalBackupString;";
        // Introduce another backup variable per known contract. TODO only for referenced contracts
        liveCodeMutationsBackupVariablesCode += "\n";
        for (ASTContractDefinition contract : ast.getContracts()) {
            String code = contract.getName() + " _internalBackupContract" + contract.getName() + "; ";
            liveCodeMutationsBackupVariablesCode += code;
        }
        // Introuce another backup variable per known struct. TODO only for referenced structs
        liveCodeMutationsBackupVariablesCode += "\n";
        for (ASTStructDefinition structDefinition : ast.getStructDefinitions()) {
            String code = structDefinition.getCanonicalName() + " _internalBackupStruct" + structDefinition.getName() + "; "; // TODO name clash potential for struct name
            liveCodeMutationsBackupVariablesCode += code;
        }
        // Add backup variables to store original values of variables in sub-expressions with side effects (++/--)
        for (int i = 0; i < 10; ++i) { // TODO: Declare as many as needed
            String backupVariable = "uint256 _internalBackupSideEffectVariable" + String.valueOf(i) + "; ";
            liveCodeMutationsBackupVariablesCode += backupVariable;
        }

        /*
        if (Configuration.injectNullContractChecks) {
            // Add a backup contract instance to use in place of uninitialized/null contracts of the same type
            for (ASTContractDefinition contract : ast.getContracts()) {
                String code = contract.getName() + " _internalFallbackContract" + contract.getName() + " = new "
                        + valueGenerator.generateRandomValue(ast, contract.getType()).toASTNode(false).toSolidityCode() + "; ";
                liveCodeMutationsBackupVariablesCode += code;
            }
        }*/


        // Introduce another backup variable per known struct
        ASTVerbatimText liveCodeMutationsBackupVariablesNode = new ASTVerbatimText(0, liveCodeMutationsBackupVariablesCode);

        // Event to replace throw statements, such that we can continue to produce test data rather than
        // aborting the test case
        String throwCode = "event EXCEPTION_THROW_STATEMENT();";
        ASTVerbatimText throwCodeNode = new ASTVerbatimText(0, throwCode);
        throwCodeNode.finalize();

        // Event for runtime tautology checking
        String tautologyErrorCode = "event TAUTOLOGY_ERROR(uint statementId);";
        ASTVerbatimText tautologyErrorNode = new ASTVerbatimText(0, tautologyErrorCode);
        tautologyErrorNode.finalize();

        // Append relevant definitions to every contract.
        // TODO putting this into a base contract and inheriting from that would generate less code
        for (ASTContractDefinition contract : ast.getContracts()) {
            if (contract.getContainsRenamedRevertReferences()) {
                contract.addChildNode(revertCodeNode);
            }
            if (contract.getContainsRenamedAssertReferences()) {
                contract.addChildNode(assertCodeNode);
            }
            if (contract.getContainsRenamedRequireReferences()) {
                contract.addChildNode(requireCodeNode);
            }

            if (contract.getContainsRenamedNowReferences()) {
                contract.addChildNode(nowCodeNode);
            }

            if (contract.getContainsRenamedThrows()) {
                contract.addChildNode(throwCodeNode);
            }

            if (Configuration.checkRuntimeTautologyCorrectness && Configuration.insertLCMSupportCode) {
                contract.addChildNode(tautologyErrorNode);
            }

            // TODO: Do what's described below by introducing a command line flag and using it only when processing
            // the original contract - otherwise there will be clashes since the variables are inserted multiple times
            // The mutations marker storage fields are now always inserted, so that original, instrumented and
            // mutated programs all have the same declared storage requirements.
            if (/*Configuration.applyLiveEMIMutations*/ Configuration.insertLCMSupportCode // TODO rename this or split into multiple flags
                    && contract.getContractKind() == Definitions.ContractKind.CONTRACT_KIND_CONTRACT) {
                contract.addChildNode(codeBlockMutationsMarkerNode);
                contract.addChildNode(liveCodeMutationsBackupVariablesNode);

                if (Configuration.injectArraySubscriptChecks) {
                    HashMap<ASTStructDefinition, Boolean> processedStructs = new HashMap<ASTStructDefinition, Boolean>();
                    // Also add storage struct variables
                    for (ASTIndexAccess indexAccess : contract.getIndexAccessList()) {
                        indexAccess.toSolidityCode(); // Determine struct access, if any
                        ASTStructDefinition structDefinition = indexAccess.getStructAccess();
                        if (structDefinition == null || processedStructs.containsKey(structDefinition)) {
                            continue;
                        }
                        processedStructs.put(structDefinition, true);
                        String code = structDefinition.getCanonicalName() + " _internalStructValue" + structDefinition.getName() + ";";
                        ASTNode node = new ASTVerbatimText(0, code);
                        contract.addChildNode(node);
                    }
                }

                // Generate an empty __annotate(string) function. This way we can generate calls to it which are
                // a no-op for third-party Solididy tools such as the compiler, but our own AST parser can extract
                // the argument and add it to the subsequent AST node
                // TODO like all other helper variables this is too ad-hoc
                String annotateCode = "function " + ASTFunctionDefinition.SpecialFunction.SPECIAL_FUNCTION_ANNOTATE.getName() + "(string memory arg) public {}";
                ASTNode annotateNode = new ASTVerbatimText(0, annotateCode);
                contract.addChildNode(annotateNode);
            }
        }
    }

    public ASTVerbatimText generateExpressionValueCheck(Expression expression, boolean expectedValue) throws Exception {
        ASTNode expressionCodeNode = expression.toASTNode();
        String code = "if ( (" + expressionCodeNode.toSolidityCode() + ") != " + String.valueOf(expectedValue)
                + ") emit TAUTOLOGY_ERROR(" + expressionCodeNode.getID() + ");";
        ASTVerbatimText checkedGeneratedCondition = new ASTVerbatimText(0, code );
        return checkedGeneratedCondition;
    }

    public void prependExpressionValueCheck(Expression expression, boolean expectedValue) throws Exception {
        ASTVerbatimText checkedGeneratedCondition = generateExpressionValueCheck(expression, expectedValue);
        expression.addDebugPrefixCode(checkedGeneratedCondition.toSolidityCode());
    }


    public ASTBlock embedInBlock(ASTNode statement) throws Exception {
        ASTNode parent = statement.getParent();
        ASTBlock block = new ASTBlock(0);

        // Locate statement to be embedded
        if (parent instanceof IControlStructure) {
            IControlStructure structure = (IControlStructure)parent;
            structure.replaceChildStatement(statement, block);
        } else if (parent instanceof ASTBlock) {
            ASTBlock parentBlock = (ASTBlock)parent;
            parentBlock.replaceChildNode(statement, block);
        } else {
            if (parent == null) {
                System.out.println("Parent of " + statement.toSolidityCode() + " is null?");
            }
            throw new Exception("Cannot embed AST node [" + statement.toSolidityCode() + "] in unexpected parent node [" + parent.toSolidityCode() + "]");
        }
        statement.setParent(block);

        // Embed old statement in new block
        block.addChildNode(statement);
        block.finalize();

        return block;
    }


    public ASTWhileStatement embedInWhileStatement(ASTNode condition, ASTNode body) throws Exception {
        ASTNode parent = body.getParent();
        ASTWhileStatement whileStatement = new ASTWhileStatement(0);

        whileStatement.setParent(parent);
        whileStatement.addChildNode(condition);
        whileStatement.addChildNode(body);
        whileStatement.finalize();

        replaceParentNode(parent, whileStatement, body);
        return whileStatement;
    }

    public ASTIfStatement embedInIfStatement(ASTNode condition, ASTNode body) throws Exception {
        // Turn statement
        //    x
        // into
        // { if (cond) x }
        // The surrounding block ensures that we do not mess up if-else statement matching,
        // e.g. when prepending to x in
        //    if (...) x else ...
        // and where the insertion would match the else with the newly generated if.
        ASTNode oldParent = body.getParent();
        ASTBlock newBlock = new ASTBlock(0);

        replaceParentNode(oldParent, newBlock, body);

        ASTIfStatement ifStatement = new ASTIfStatement(0);

        ifStatement.addChildNode(condition);
        ifStatement.addChildNode(body);
        ifStatement.finalize();

        newBlock.addChildNode(ifStatement);

        return ifStatement;
    }

    public void replaceParentNode(ASTNode parent, ASTNode newParent, ASTNode embeddedStatement) throws Exception {
        if (parent instanceof IControlStructure) {
            IControlStructure controlStructure = (IControlStructure)parent;
            controlStructure.replaceChildStatement(embeddedStatement, newParent);
        } else if (parent instanceof ASTBlock) {
            ASTBlock parentBlock = (ASTBlock)parent;
            parentBlock.replaceChildNode(embeddedStatement, newParent);
        } else {
            throw new Exception("Cannot embed while statement in unexpected parent structure " + parent.getClass().getName());
        }
        embeddedStatement.setParent(newParent); // parent may be null before this
    }


    public void emptyFunctionBody(FunctionScope function, RandomNumbers prng) throws Exception {
        // Remove all code from function body, inserting a dummy return statement if necessary
        ASTBlock body = function.getBody();
        ValueGenerator valueGenerator = null;

        if (body != null) { // TODO set body for modifier definitions, then remove this check
            // Clear body
            if (function instanceof ASTModifierDefinition) {
                // Modifier: we always need the "_;" statement
                ArrayList<ASTNode> newBlockChildren = new ArrayList<ASTNode>();
                ASTVerbatimText replacement = new ASTVerbatimText(0, "_;");
                newBlockChildren.add(replacement);
                body.setChildren(newBlockChildren);
            } else {
                // Function: we may have a return value
                body.setChildren(null);

                ASTParameterList returnList = function.getReturnList();
                if (returnList != null) {
                    ArrayList<ASTNode> newBlockChildren = new ArrayList<ASTNode>();

                    // Generate return statement
                    String returnStatementString = "return (";
                    for (ASTVariableDeclaration parameter : returnList.toArrayList()) {
                        if (valueGenerator == null) {
                            valueGenerator = new ValueGenerator(prng);
                        }
                        returnStatementString += valueGenerator.generateRandomValue(ast, parameter.getTypeName(),
                                ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_RANDOM).toASTNode(false).toSolidityCode();
                    }
                    returnStatementString += ");";
                    ASTVerbatimText returnNode = new ASTVerbatimText(0, returnStatementString);
                    newBlockChildren.add(returnNode);
                    body.setChildren(newBlockChildren);
                }
            }
        }
    }

    protected ASTEmitStatement generateInstrumentatonEmitStatement(ProfilingEvent event) throws Exception {
        ASTEmitStatement emitStatement = new ASTEmitStatement(0);
        ASTFunctionCall emitCall = new ASTFunctionCall(0, false, null);
        emitStatement.addChildNode(emitCall);
        emitCall.addChildNode(new ASTIdentifier(0, event.getName(), 0));
        if (event.getArguments() != null) {
            for (Variable var : event.getArguments()) {
                // TODO Like generateProfilingEventDefinition(), this doesn't deal gracefully with tuples,
                // since ASTVariableDeclarationStatement.getName() only accesses the first child node.
                ASTIdentifier identifier = new ASTIdentifier(0, var.getName(), 0);
                emitCall.addChildNode(identifier);
            }
        }
        emitCall.finalize();
        emitStatement.finalize();
        return emitStatement;
    }

    public void appendProfilingEmitStatementToBlock(ASTBlock block, ArrayList<ProfilingEvent> instrumentationEvents) throws Exception {
        for (ProfilingEvent event : instrumentationEvents) {
            ASTEmitStatement emitStatement = generateInstrumentatonEmitStatement(event);
            block.addChildNode(emitStatement);
        }
    }

    protected ASTBlock enforceParentBlock(ASTNode node) throws Exception {
        if (!(node.getParent() instanceof ASTBlock)) {
            embedInBlock(node);
        }
        return (ASTBlock)node.getParent();
    }

    public void prependProfilingEmitStatement(ASTNode instrumentedStatement, ArrayList<ProfilingEvent> instrumentationEvents) throws Exception {
        /*
        // First ensure that the event injection does not alter the control flow -
        // embed statements in blocks where necessary (e.g. for (;;) stmt;  becomes  for (;;) { emit e(...); stmt; }
        // This should always be the case unless the parent is already a block
        if (!(instrumentedStatement.getParent() instanceof ASTBlock)) {
            embedInBlock(instrumentedStatement);
        }

        int positionInBlock = -1;
        ASTBlock parent = (ASTBlock)instrumentedStatement.getParent();
        for (int i = 0; i < parent.getChildCount(); ++i) {
            if (parent.getChild(i) == instrumentedStatement) {
                positionInBlock = i;
                break;
            }
        }
        if (positionInBlock == -1) {
            throw new Exception("Cannot find node " + instrumentedStatement.getID() + " in parent block");
        }
        */
        ASTBlock parentBlock = enforceParentBlock(instrumentedStatement);
        int positionInBlock = parentBlock.getChildNodeIndex(instrumentedStatement);

        for (ProfilingEvent event : instrumentationEvents) {
            ASTEmitStatement emitStatement = generateInstrumentatonEmitStatement(event);
            parentBlock.insertChildNode(positionInBlock, emitStatement);
            ++positionInBlock;
        }
    }

    public void generateProfilingEventDefinition(ASTContractDefinition profiledContract, ProfilingEvent event) throws Exception {
        ASTEventDefinition definition = new ASTEventDefinition(0, event.getName(), false);
        ASTParameterList parameterList = new ASTParameterList(0);

        if (event.getArguments() != null) {
            for (Variable var : event.getArguments()) {
                // TODO This doesn't deal gracefully with tuples. We only access the first
                // VariableDeclaration node of a declaration statement
                ASTVariableDeclaration astDeclaration = var.getDeclaration();

                // Create our own object to strip out the initializer and possibly other undesirable
                // settings
                ASTVariableDeclaration copiedDeclaration = new ASTVariableDeclaration(0, astDeclaration.getName(), "(type)",
                        "default", "internal", false, false); // TODO what to index?
                copiedDeclaration.addSharedChildNode(astDeclaration.getTypeName());
                copiedDeclaration.finalize();
                parameterList.addChildNode(copiedDeclaration);
            }
        }

        parameterList.finalize();
        definition.addChildNode(parameterList);
        definition.finalize();

        profiledContract.addChildNode(definition);
    }

    private int loopLimitVariableCounter = 0;

    protected void processForLoopInjection(ASTNode node, ASTContractDefinition currentContract) throws Exception {
        if (node instanceof ASTForStatement
            || node instanceof ASTDoWhileStatement
            || node instanceof ASTWhileStatement) {
            // Create loop counter (at this point currentContract != null can be assumed or we wouldn't be in a loop)
            String loopCounterName = "_internalLoopCounter" + String.valueOf(loopLimitVariableCounter++);
            ASTVerbatimText variableNode = new ASTVerbatimText(0, "int " + loopCounterName + ";");
            currentContract.addChildNode(variableNode);

            // Prepend a "loopCounter = 0;" assignment to the loop, embedding it in a block if necessary
            ASTBlock parentBlock = enforceParentBlock(node);
            int positionInBlock = parentBlock.getChildNodeIndex(node);
            ASTVerbatimText assignmentNode = new ASTVerbatimText(0, loopCounterName + " = 0;");
            parentBlock.insertChildNode(positionInBlock, assignmentNode);

            // Add an "if (++loopCounter == <limit>) break;" as first loop body node, embedding the existing
            // body in a block if necessary
            ASTVerbatimText earlyLoopBreakNode = new ASTVerbatimText(0,
                    "if (++" + loopCounterName + " == 3) break;"); // TODO configurable option. Event production escalates heavily for higher values and things crash, so we only run each loop twice for now
            ASTNode bodyNode;
            if (node instanceof ASTForStatement) {
               bodyNode = ((ASTForStatement)node).getBody();
            } else if (node instanceof ASTDoWhileStatement) {
                bodyNode = ((ASTDoWhileStatement)node).getBody();
            } else {
                bodyNode = ((ASTWhileStatement)node).getBody();
            }
            parentBlock = enforceParentBlock(bodyNode);
            positionInBlock = parentBlock.getChildNodeIndex(bodyNode);
            parentBlock.insertChildNode(positionInBlock, earlyLoopBreakNode);

        } else if (node instanceof ASTContractDefinition) {
            currentContract = (ASTContractDefinition)node;
        }

        if (node.getChildren() != null) {
            // Save child nodes, then work on the copy to avoid trashing the iterator due to AST editing operations
            ArrayList<ASTNode> childNodes = new ArrayList<ASTNode>();
            for (ASTNode childNode : node.getChildren()) {
                childNodes.add(childNode);
            }
            for (ASTNode childNode : childNodes) {
                processForLoopInjection(childNode, currentContract);
            }
        }
    }

    public void injectLoopLimits() throws Exception {
        processForLoopInjection(ast.getRoot(), null);
    }

}
