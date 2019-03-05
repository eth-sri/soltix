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
package soltix.mutation;

import soltix.ASTEditor;
import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.Type;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.expressions.ExpressionEvaluationErrorHandler;
import soltix.interpretation.expressions.ExpressionEvaluator;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.interpretation.variables.VariableValues;
import soltix.synthesis.*;
import soltix.util.RandomNumbers;

import java.util.ArrayList;

/**
 * Implementation of Live EMI mutations.
 */
public class LiveEMIMutator implements IMutator {
    private AST ast;
    private ASTEditor editor;
    private RandomNumbers prng;
    private ExpressionGenerator expressionGenerator;
    private PredicateGenerator predicateGenerator;
    private VariableGenerator variableGenerator;
    private StructTypeGenerator structTypeGenerator;
    private ExpressionEvaluator expressionEvaluator;
    private TypeConverter typeConverter;
    private StatementGenerator statementGenerator;
    private FunctionGenerator functionGenerator;
    private ASTContractDefinition currentContract = null;
    private ASTFunctionDefinition currentFunction = null;

    public LiveEMIMutator(AST ast, ASTEditor editor) throws Exception {
        this.ast = ast;
        this.editor = editor;
        prng = new RandomNumbers(Configuration.randomNumbersSeed);
        predicateGenerator = new PredicateGenerator(ast, editor, prng);
        structTypeGenerator = new StructTypeGenerator(prng); // TODO fix awkward circular StructTypeGenerator-VariableGenerator dependency
        variableGenerator = new VariableGenerator(ast, prng, structTypeGenerator);
        structTypeGenerator.setVariableGenerator(variableGenerator);
        // Function generator to allow expression generator to generate callable functions of a suitable structure
        // on the fly
        functionGenerator = new FunctionGenerator(ast, prng, null /*not needed*/, null /*not needed*/);
        expressionGenerator = new ExpressionGenerator(ast, predicateGenerator, functionGenerator, prng);

        expressionEvaluator = new ExpressionEvaluator(new ExpressionEvaluationErrorHandler(prng));
        typeConverter = new TypeConverter(ast, prng, expressionEvaluator);
        statementGenerator = new StatementGenerator(ast, prng, expressionGenerator, structTypeGenerator, typeConverter, false);
    }

    public void run() throws Exception {
        processASTNode(ast.getRoot());
    }

    protected void processASTNode(ASTNode node) throws Exception {
        VariableEnvironment environment = node.getVariableEnvironment();
        final boolean mutateAllStatements = true; // for debugging

        if (node instanceof ASTContractDefinition) {
            currentContract = (ASTContractDefinition)node;
        }
        if (node instanceof ASTFunctionDefinition) {
            currentFunction = (ASTFunctionDefinition)node;
        }

        if (environment != null && (mutateAllStatements || prng.flipCoin())) {
            if (currentContract == null) {
                throw new Exception("LiveEMIsoltix.processASTNode: Mutation without containing contract");
            }

            // We have state and can thus prepend synthesized code to this node
            switch ((int)prng.generateLongInteger(0, 2)) {
                case 0:
                    // Apply "Always True Guard" mutation
                    synthesizeAlwaysTrueGuard(node, environment);
                    break;
                case 1:
                    // Apply "Always False Conditional Block"  mutation
                    // (Only in normal contracts, since library/interface contracts don't allow mutable storage)
                    if (currentContract.getContractKind() == Definitions.ContractKind.CONTRACT_KIND_CONTRACT) {
                        synthesizeAlwaysFalseConditionalBlock(node, environment);
                    }
                    break;
                case 2:
                    // (Only in normal contracts, since library/interface contracts don't allow mutable storage)
                    if (currentContract.getContractKind() == Definitions.ContractKind.CONTRACT_KIND_CONTRACT) {
                        // Apply "Always True Conditional Block"  mutation

                        // Disallow function calls for now, since side effect undo handling (see Expression.toASTNode()
                        // undo argument) currently doesn't work for side effects in return function arguments:
                        //   __returnFunction(x++). TODO Fix it
                        boolean oldAllowFunctionCalls = expressionGenerator.getAllowFunctionCalls();
                        expressionGenerator.setAllowFunctionCalls(false);

                        synthesizeAlwaysTrueConditionalBlock(node, environment);

                        expressionGenerator.setAllowFunctionCalls(oldAllowFunctionCalls);
                    }
                    break;
            }
        }

        // Process children, depth-first
        if (node.getChildren() != null) {
            //for (ASTNode child : node.getChildren()) {
            for (int i = 0; i < node.getChildren().size(); ++i) {
                ASTNode child = node.getChild(i);
                processASTNode(child);
            }
        }
    }

    protected void synthesizeAlwaysTrueGuard(ASTNode node, VariableEnvironment environment) throws Exception {
        //Expression generatedTrueCondition = expressionGenerator.generateBooleanExpression(ast, true);
        //editor.embedInIfStatement(generatedTrueCondition.toASTNode(), node);
        int depth = Configuration.randomPredicateDepth(prng); //(int)prng.generateLongInteger(2, 5);

        Expression generatedTrueCondition = predicateGenerator.synthesizePredicate(environment, true, depth);

        ASTIfStatement generatedIfStatement;

        generatedIfStatement = editor.embedInIfStatement(generatedTrueCondition.toASTNode(), node);

        // For debugging: check whether the generated expression is really true
        if (Configuration.checkRuntimeTautologyCorrectness) {
            String code = "if ( (" + generatedTrueCondition.toASTNode().toSolidityCode() + ") != true) emit TAUTOLOGY_ERROR(" + node.getID() + ");";
            ASTVerbatimText checkedGeneratedTrueCondition = new ASTVerbatimText(0, code );

            //ASTVerbatimText checkedGeneratedTrueCondition = editor.createConditionValueCheck(generatedTrueCondition, true);
            ASTBlock block = editor.embedInBlock(generatedIfStatement);
            block.insertChildNode(0, checkedGeneratedTrueCondition);
        }
    }

    protected ASTBlock startConditionalBlock() throws Exception {
        // To prepend a conditional block to a given statement, first embed that statement in a new block
        ASTBlock block = new ASTBlock(0);

        if (!Configuration.eventsReplaceStorageLog) { // Only needed for low-level storage tracing
            // Mark the beginning of a conditional block through special storage write value
            ASTVerbatimText startMarker = new ASTVerbatimText(0, "_internalMutationsMarkerStorage = _internalMutationsMarkerValue;");
            block.addChildNode(startMarker);
        }
        return block;
    }

    protected void endConditionalBlock(ASTBlock block) throws Exception {
        if (!Configuration.eventsReplaceStorageLog) { // Only needed for low-level storage tracing
            // Mark end of conditional block through special storage write value
            ASTVerbatimText endMarker = new ASTVerbatimText(0, "_internalMutationsMarkerStorage = _internalMutationsMarkerValue;");
            block.addChildNode(endMarker);
        }
    }

    private int currentBackupVariableId = 0; // backup variable name counter to avoid name clashes

    protected String buildBackupVariableName(Variable overwrittenVariable) {
        return "backup_var" + String.valueOf(currentBackupVariableId++) + "_" + overwrittenVariable.getName();
    }

    // Always False Conditional block.
    //
    // x     -- containing node (block or parent control structure)
    //    y  -- statement to prepend with generated block
    //
    //    =>
    //
    // x     -- containing node (block or parent control structure)
    // {
    //    ms;
    //    y;
    // }
    protected void synthesizeAlwaysTrueConditionalBlock(ASTNode node, VariableEnvironment environment) throws Exception {
        if (environment.getVariableCount() == 0) {
            // No variables to work on
            return;
        }

        ASTBlock conditionalBlock = startConditionalBlock();

        // We synthesize an expression and build a block with side effects on one variable as follows:
        //
        //     int backup = synthesizeExpression();
        //     if (synthesizedTruePredicte()) {
        //         backup = variable;
        //         variable = ......
/*
        int backup_v = h synthesized valid expression i ;
        if ( h synthesized true predicate i ) {
            backup_v = v;
            v = h synthesized valid expression i ;
            if/while( h synthesized false predicate i ) {
                print v;
            }
        }
        v = backup_v;
  */

//
        // int backup_v = <synthesized-expression>;

        // TODO Merge with StatementGenerator.pickLValueExpression()?
        Variable overwrittenVariable = expressionGenerator.pickRandomUsableVariable(environment);
        if (overwrittenVariable == null) {
            // Give up - no free variable
            return;
        }
        String backupVariableName = null;
        Expression expression = expressionGenerator.synthesizeExpression(currentContract, environment, overwrittenVariable.getType());
        if (expression == null) {
            // Give up (currently this means we have no variables to work with - TODO maybe including constants would be
            // an alternative)
            return;
        }

        // If we picked a constant variable, we cannot assign to it. For now we work around this by simply
        // disabling the assignments to it (potentially weakening TCB/FCB mutation effects significantly).
        // TODO Decide on whether to keep this or exclude constant variables from the selection process instead
        boolean avoidConstantVariableAssignment = ! overwrittenVariable.getDeclaration().isModifiable();

        // "bytes" types are currently an unsolved problem which is also addressed by skipping the variable
        // assignment. They may not be declared with a "memory" storage location, but assignings values to the
        // default location "storage" yields errors
        // TODO Fix this. As a potential awkward workaround, transform
        //    backup_bytes_var = bytes_value;
        // to
        //    backup_string_var = bytes_to_string(bytes_value);    and string_to_bytes()

        boolean avoidByteVariableAssignment = false;
        if (Type.isByteType(overwrittenVariable.getDeclaration().getTypeName())) {
            avoidByteVariableAssignment = true;
        }

        // We want the storage location to be "memory" at least for strings and structs, since e.g. string literal
        // assignments fail otherwise (this supposedly depends on the Solidity version to some extent as well).
        // The storage location cannot be specified for elementary types.
        //
        // TODO Arrays?
        boolean isIntegerAssignment = false;
        String backupVariableAssignmentCastType = null;
        if (!avoidByteVariableAssignment) { // For byte variables even the initializer for a new variable already fails
            ArrayList<Expression> undoSideEffectsExpressions = new ArrayList<Expression>();


            // Note: When converting the generated expression to an AST node, its potential side effects (++/--
            // operators) are recorded so that we can undo them.
            //
            // The easiest way to address this is to store the original values of affected variables and restore the
            // original value after executing the expression statement
            //
            //   backup_int = x;
            //   expr(++x);
            //   x = backup_int;
            //
            // TODO The "x = backup_int" assignment could be wrapped in an ATG as well
            //
            // (This is the easiest way to address the problem that a change is notably not guaranteed to occur, e.g. in
            //      cond? ++x: --y
            // the taken and thus to-be-undone operand can vary depending on the value of "cond". This also affects
            // short-circuiting logical operators TODO ... that should generally be handled more accurately)
            ASTNode expressionNode = expression.toASTNode(undoSideEffectsExpressions);


            // Save integer variables that will be modified by increment/decrement operators
            for (int i = 0; i < undoSideEffectsExpressions.size(); ++i) {
                Expression undoExpression = undoSideEffectsExpressions.get(i);
                String backupVariable = "_internalBackupSideEffectVariable" + String.valueOf(i);
                ASTNode backupAssignment = new ASTVerbatimText(0, backupVariable + " = uint256("
                                                    + undoExpression.getFirstOperand().toASTNode().toSolidityCode() + ");");
                conditionalBlock.addChildNode(backupAssignment);
            }

            if (Type.isIntegerType(overwrittenVariable.getType())) {
                // We already have a declared variable
                backupVariableName = "_internalBackupInteger";
                backupVariableAssignmentCastType = "uint256"; // We use the same uint256 for all integer types - need casts

                isIntegerAssignment = true;
                // Generate an assignment
                ASTVerbatimText backupVariableAssignment = new ASTVerbatimText(0,
                        backupVariableName + " = " + backupVariableAssignmentCastType + "(" + expressionNode.toSolidityCode() + ");");
                conditionalBlock.addChildNode(backupVariableAssignment);
            } else if (Type.isAddressType(overwrittenVariable.getType())
                    || Type.isBoolType(overwrittenVariable.getType())) {
                // We already have a declared variable
                backupVariableName = "_internalBackup" + (Type.isAddressType(overwrittenVariable.getType()) ? "Address" : "Bool");
                // Generate an assignment
                ASTVerbatimText backupVariableAssignment = new ASTVerbatimText(0,
                        backupVariableName + " = " + expressionNode.toSolidityCode() + ";");
                conditionalBlock.addChildNode(backupVariableAssignment);
            } else if (Type.isContractType(ast, overwrittenVariable.getType())) {
                // We already have a declared variable
                ASTContractDefinition contract = ast.getContract(overwrittenVariable.getType().getName());
                backupVariableName = "_internalBackupContract" + contract.getName();
                // Generate an asignment
                ASTVerbatimText backupVariableAssignment = new ASTVerbatimText(0,
                        backupVariableName + " = " + expressionNode.toSolidityCode() + ";");
                conditionalBlock.addChildNode(backupVariableAssignment);
            } else if (Type.isStructType(ast, overwrittenVariable.getType())) {
                // We already have a declared variable
                ASTStructDefinition structDefinition = ast.getStructDefinition(overwrittenVariable.getType().getName());
                backupVariableName = "_internalBackupStruct" + structDefinition.getName();
                // Generate an assignment
                ASTVerbatimText backupVariableAssignment = new ASTVerbatimText(0,
                        backupVariableName + " = " + expressionNode.toSolidityCode() + ";");
                conditionalBlock.addChildNode(backupVariableAssignment);
            } else if (Type.isStringType(overwrittenVariable.getType())) {
                // We already have a declared variable
                backupVariableName = "_internalBackupString";
                // Generate an assignment
                ASTVerbatimText backupVariableAssignment = new ASTVerbatimText(0,
                        backupVariableName + " = " + expressionNode.toSolidityCode() + ";");
                conditionalBlock.addChildNode(backupVariableAssignment);
            } else {
                // Declare new variable
                String storageLocation = (Type.isStringType(overwrittenVariable.getType())
                        || Type.isStructType(ast, overwrittenVariable.getType())) ? " memory " : " ";

                backupVariableName = buildBackupVariableName(overwrittenVariable);
                backupVariableAssignmentCastType = null;

                // Generate a declaration with initializer
                ASTVerbatimText backupVariableDeclaration = new ASTVerbatimText(0, overwrittenVariable.getType().toSolidityCode()
                        + storageLocation + backupVariableName + " = " + expressionNode.toSolidityCode() + ";");
                conditionalBlock.addChildNode(backupVariableDeclaration);
            }

            // Restore integers that were modified by increment/decrement operators
            for (int i = 0; i < undoSideEffectsExpressions.size(); ++i) {
                Expression undoExpression = undoSideEffectsExpressions.get(i);
                String backupVariable = "_internalBackupSideEffectVariable" + String.valueOf(i);
                String castBackupVariable = undoExpression.getFirstOperand().getType().toSolidityCode() + "(" + backupVariable + ")";
                ASTNode backupAssignment = new ASTVerbatimText(0,
                        // TODO instead of  if (x != oldvalue) x = oldvalue;  use a tautology as condition instead to
                        // make it less clear whether the assignment is performed. Note that this requires some ugly
                        // extra work to exclude all variables that were modified here from the tautology generation.
                        // Maybe some synthesizePredicate() variant with passed list of excluded variables
                        "if (" + undoExpression.getFirstOperand().toASTNode().toSolidityCode() + " != " + castBackupVariable + ") "
                        + undoExpression.getFirstOperand().toASTNode().toSolidityCode() + " = "
                        + castBackupVariable + ";");
                conditionalBlock.addChildNode(backupAssignment);
            }
        }

        // if (synthesizedTruePredicate) {
        Expression truePredicate = predicateGenerator.synthesizePredicate(environment, true, Configuration.randomPredicateDepth(prng));
        ASTIfStatement outerIfStatement = new ASTIfStatement(0);
        outerIfStatement.addChildNode(truePredicate.toASTNode());
        ASTBlock outerIfBody = new ASTBlock(0);
        outerIfStatement.addChildNode(outerIfBody);
        //     backup_v = v;
        if (!avoidByteVariableAssignment && !avoidConstantVariableAssignment) {
            String rhs = "";
            if (backupVariableAssignmentCastType != null) {
                rhs = backupVariableAssignmentCastType + "(";
            }
            rhs += overwrittenVariable.getName();
            if (backupVariableAssignmentCastType != null) {
                rhs += ")";
            }
            ASTVerbatimText backupAssignment = new ASTVerbatimText(0, backupVariableName + " = " + rhs + ";");
            outerIfBody.addChildNode(backupAssignment);
        }

                //     v = <synthesized-expression>;
        // TODO

        //     if/while (<synthesized-false-predicate>) {
        //        ;  // TODO print v
        //     }
        ASTIfStatement innerIfStatement = new ASTIfStatement(0);
        ASTBlock body = new ASTBlock(0);
        Expression falsePredicate = predicateGenerator.synthesizePredicate(environment, false,
                Configuration.randomPredicateDepth(prng));
        innerIfStatement.addChildNode(falsePredicate.toASTNode());
        innerIfStatement.addChildNode(body);
        body.finalize();
        innerIfStatement.finalize();
        outerIfBody.addChildNode(innerIfStatement);
        outerIfBody.finalize();

        outerIfStatement.finalize();
        String debugPrefix = truePredicate.getDebugPrefixCode();
        if (!debugPrefix.equals("")) {
            conditionalBlock.addChildNode(new ASTVerbatimText(0, debugPrefix));
        }
        conditionalBlock.addChildNode(outerIfStatement);

        // v = backup_v;
        if (!avoidByteVariableAssignment && !avoidConstantVariableAssignment) {
            String rhs = "";
            if (isIntegerAssignment) {
                // A uint256 is used as storage for all integers - use cast to convert back to original type
                rhs = overwrittenVariable.getType().toSolidityCode() + "(";
            }
            rhs += backupVariableName;
            if (isIntegerAssignment) {
                rhs += ")";
            }
            ASTVerbatimText restoreAssignment = new ASTVerbatimText(0, overwrittenVariable.getName() + " = " + rhs + ";");
            conditionalBlock.addChildNode(restoreAssignment);
        }

        endConditionalBlock(conditionalBlock);

        // Replace parent with synthesized block
        editor.replaceParentNode(node.getParent(), conditionalBlock, node);
        // Append original statement at end of block
        conditionalBlock.addChildNode(node);
    }

    protected void synthesizeAlwaysFalseConditionalBlock(ASTNode node, VariableEnvironment environment) throws Exception {
        ASTBlock conditionalBlock = startConditionalBlock();

        // Build "if" with false condition. TODO Switch between if and while and for?
        Expression falsePredicate = predicateGenerator.synthesizePredicate(environment, false, Configuration.randomPredicateDepth(prng));
        ASTIfStatement outerIfStatement = new ASTIfStatement(0);
        outerIfStatement.addChildNode(falsePredicate.toASTNode());

        // Defer body generation until the AST structure is set up. statementGenerator.generate() needs to
        // traverse parent nodes up to the function definition node
        conditionalBlock.addChildNode(outerIfStatement);
        endConditionalBlock(conditionalBlock);

        // Replace parent with synthesized block
        editor.replaceParentNode(node.getParent(), conditionalBlock, node);
        // Append original statement at end of block
        conditionalBlock.addChildNode(node);

        // Generate body
        statementGenerator.generate(environment, currentContract, currentFunction, outerIfStatement);
        outerIfStatement.finalize();
    }
}
