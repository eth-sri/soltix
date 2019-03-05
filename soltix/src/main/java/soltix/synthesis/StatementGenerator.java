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

package soltix.synthesis;

import soltix.ast.*;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.util.RandomNumbers;
import soltix.util.WeightedRandomChoice;

/**
 * Class to generate randomized statements.
 */
public class StatementGenerator {
    private AST ast;
    private RandomNumbers prng;
    private ExpressionGenerator expressionGenerator;
    private StructTypeGenerator structTypeGenerator;
    private TypeConverter typeConverter;
    private WeightedRandomChoice weightedLoopStatementRandomChoice;
    private boolean allowReturnStatements;

    public StatementGenerator(AST ast,
                              RandomNumbers prng,
                              ExpressionGenerator expressionGenerator,
                              StructTypeGenerator structTypeGenerator,
                              TypeConverter typeConverter,
                              boolean allowReturnStatements) throws Exception {

        this.ast = ast;
        this.prng = prng;
        this.expressionGenerator = expressionGenerator;
        this.structTypeGenerator = structTypeGenerator;
        this.typeConverter = typeConverter;
        weightedLoopStatementRandomChoice = new WeightedRandomChoice();
        weightedLoopStatementRandomChoice.addItem(50); // expression statement
        if (allowReturnStatements) {
            weightedLoopStatementRandomChoice.addItem(16); // return
            weightedLoopStatementRandomChoice.addItem(17); // break
            weightedLoopStatementRandomChoice.addItem(17); // continue
        } else {
            weightedLoopStatementRandomChoice.addItem(25); // break
            weightedLoopStatementRandomChoice.addItem(25); // continue
        }
        this.allowReturnStatements = allowReturnStatements;
    }

    // Check whether there's a parent loop anywhere between the given node and the containing function
    // definition node.
    // Note that we don't recognize the parent node for which we generate the statement as a border, so
    // for e.g. FCB generation on:
    //     if (false-expr)
    //          ...generate()'d statement...
    // there could be a loop above the if statement to which we continue/break. For our FCB use case this is
    // no problem, since the body isn't actually executed
    protected boolean haveParentLoop(ASTNode parentNode) throws Exception {
        if (parentNode instanceof ASTWhileStatement
            || parentNode instanceof ASTForStatement
            || parentNode instanceof ASTDoWhileStatement) {
            return true;
        } else if (parentNode instanceof ASTFunctionDefinition) {
            // Stop - no loop encountered
            return false;
        } else {
            if (parentNode.getParent() == null) {
                throw new Exception("StatementGenerator.haveParentLoop: node " + parentNode.getClass().getName()
                        + " has no parent (getParent() is null). Make sure to properly embed any node for which "
                        + "you call StatementGenerator.generate() into the AST by using addChildNode() prior to the "
                        + "call");
            }
            return haveParentLoop(parentNode.getParent());
        }
    }

    protected void generateIfStatement(VariableEnvironment environment,
                                       ASTContractDefinition contract,
                                       ASTFunctionDefinition function,
                                       ASTNode parentNode) throws Exception {

        ASTIfStatement ifStatement = new ASTIfStatement(0);
        parentNode.addChildNode(ifStatement); // Do this early for availability in haveParentLoop()
        Expression condition = expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, TypeContainer.getBoolType());

        // Condition child node
        ifStatement.addChildNode(condition.toASTNode());

        // Generate the "if" body
        generate(environment, contract, function, ifStatement);

        // With 0.5 probability, also generate an "else" body
        if (prng.flipCoin()) {
            generate(environment, contract, function, ifStatement);
        }

        ifStatement.finalize();
    }

    protected void generateWhileLoop(VariableEnvironment environment,
                                     ASTContractDefinition contract,
                                     ASTFunctionDefinition function,
                                     ASTNode parentNode) throws Exception {

        ASTWhileStatement whileStatement = new ASTWhileStatement(0);
        parentNode.addChildNode(whileStatement); // Do this early for availability in haveParentLoop()
        Expression condition = expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, TypeContainer.getBoolType());

        // Condition child node
        whileStatement.addChildNode(condition.toASTNode());

        // Generate the "while" body
        generate(environment, contract, function, whileStatement);

        whileStatement.finalize();
    }

    protected void generateForLoop(VariableEnvironment environment,
                                   ASTContractDefinition contract,
                                   ASTFunctionDefinition function,
                                   ASTNode parentNode) throws Exception {

        ASTForStatement forStatement = new ASTForStatement(0, false /*TODO*/, true, true);
        parentNode.addChildNode(forStatement); // Do this early for availability in haveParentLoop()
        Expression condition = expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, TypeContainer.getBoolType());
        Expression loopPart = expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, TypeContainer.getBoolType());

        // Condition child node
        forStatement.addChildNode(condition.toASTNode());
        // Loop part child node
        forStatement.addChildNode(loopPart.toASTNode());

        // Generate the "for" body
        generate(environment, contract, function, forStatement);

        forStatement.finalize();
    }

    protected Expression pickLValueExpression(VariableEnvironment environment) throws Exception {
        Variable variable = expressionGenerator.pickRandomUsableVariable(environment);
        if (variable == null || variable.getDeclaration().isModifiable()) {
            return null;
        }

        Expression lhsVariableExpression = new Expression(variable);

        // For structs, the assignment would work, but the result comparison would need to be
        // performed member-wise.
        // To address thus, we access a non-struct member of v instead if it is a struct type
        if (Type.isStructType(ast, variable.getType())) {
            lhsVariableExpression = typeConverter.accessRandomStructMember(environment,
                    lhsVariableExpression,
                    ast.getStructDefinition(variable.getType().getName()));
        }

        return lhsVariableExpression;
    }


    protected void generateAssignment(VariableEnvironment environment, ASTContractDefinition contract, ASTNode parentNode) throws Exception {
        Expression lhsExpression = pickLValueExpression(environment);
        if (lhsExpression == null) {
            // No lvalue available in context - generate standalone expression instead
            generateExpressionStatement(environment, contract, parentNode);
            return;
        }
        Expression rhsExpression = expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, lhsExpression.getType());

        ASTVerbatimText assignment = new ASTVerbatimText(0, lhsExpression.toASTNode().toSolidityCode()
                + " = " + rhsExpression.toASTNode().toSolidityCode() + ";");

        parentNode.addChildNode(assignment);
    }

    protected void generateExpressionStatement(VariableEnvironment environment, ASTContractDefinition contract, ASTNode parentNode) throws Exception {
        // TODO: Maybe get in-place struct type generation to work. Disabled for now (structTypeGenerator = null), since
        // the AST type adding operation seems to trash an outside iterator and results in an exception. But maybe we
        // already have enough struct types from the original program
        ASTNode resultType = TypeContainer.chooseRandomType(ast, contract, true, /*structTypeGenerator*/null, prng);

        // Note: We do not avoid generating expressions without side effects
        // TODO: Lower probability of side-effect-free statements or ensure side effects via mixed in inc/dec/compound assignments
        Expression expression = expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, resultType);
        ASTVerbatimText statement = new ASTVerbatimText(0, expression.toASTNode().toSolidityCode() + ";");
        parentNode.addChildNode(statement);
    }

    protected void generateReturnStatement(VariableEnvironment environment,
                                           ASTContractDefinition contract,
                                           ASTFunctionDefinition function,
                                           ASTNode parentNode) throws Exception {
        Expression expression = null;

        if (function.getReturnList() != null) {
            if (function.getReturnList().getChildCount() > 1) {
                throw new Exception("Unsupported return item count > 1");
            } else if (function.getReturnList().getChildCount() == 1) {
                ASTNode resultType = function.getReturnList().getChild(0);
                expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, resultType);
            }
        }

        ASTVerbatimText returnNode;

        String returnCode = "return";
        if (expression != null) {
            returnCode += " " + expression.toASTNode().toSolidityCode();
        }
        returnCode += ";";
        returnNode = new ASTVerbatimText(0, returnCode);

        parentNode.addChildNode(returnNode);
    }

    protected void generateBreakStatement(ASTNode parentNode) throws Exception {
        ASTBreakStatement breakStatement = new ASTBreakStatement(0);
        parentNode.addChildNode(breakStatement);
    }

    protected void generateContinueStatement(ASTNode parentNode) throws Exception {
        ASTContinueStatement continueStatement = new ASTContinueStatement(0);
        parentNode.addChildNode(continueStatement);
    }

    protected void generateSingleStatement(VariableEnvironment environment,
                                           ASTContractDefinition contract,
                                           ASTFunctionDefinition function,
                                           ASTNode parentNode) throws Exception {
        switch ((int)prng.generateLongInteger(0, 4)) {
            case 0:
                generateIfStatement(environment, contract, function, parentNode);
                break;
            case 1:
                generateWhileLoop(environment, contract, function, parentNode);
                break;
            case 2:
                generateForLoop(environment, contract, function, parentNode);
                break;
            case 3:
                generateAssignment(environment, contract, parentNode);
                break;
            case 4:
                if (haveParentLoop(parentNode)) {
                    // This allows us to mix in "continue" or "break" statements - choose
                    // between these and an expression statement
                    switch (weightedLoopStatementRandomChoice.choice(prng)) {
                        case 0: // expression statement
                            generateExpressionStatement(environment, contract, parentNode);
                            break;
                        case 1: // break
                            generateBreakStatement(parentNode);
                            break;
                        case 2: // continue
                            generateContinueStatement(parentNode);
                            break;
                        case 3: // return  TODO: does it even make sense for this to be associated with the loop check?
                            generateReturnStatement(environment, contract, function, parentNode);
                            break;
                    }
                } else {
                    generateExpressionStatement(environment, contract, parentNode);
                }
                break;
            default:
                throw new Exception("generateSingleStatement for undefined type");
        }
    }

    private int depth = 0;
    private final int depthLimit = 3;

    // Generates a randomized sequence of statements - possibly including blocks as well - and appends it as
    // child nodes to the supplied parent node
    public void generate(VariableEnvironment environment,
                         ASTContractDefinition contract,
                         ASTFunctionDefinition function,
                         ASTNode parentNode) throws Exception {
        ++depth;

        if (depth >= depthLimit || prng.flipCoin()) { // Note: depth does not enforce a terminal statement, just avoids multi-statements
            // Generate a single statement
            generateSingleStatement(environment, contract, function, parentNode);
        } else {

            // Generate multiple statements
            int count = (int)prng.generateLongInteger(1,3); // Small count, even allowing 1, to avoid escalating size. TODO best values? Parameterize?
            ASTNode containingBlock;

            if (!(parentNode instanceof ASTBlock)) {
                containingBlock = new ASTBlock(0);
                parentNode.addChildNode(containingBlock);
            } else {
                containingBlock = parentNode;
            }
            for (int i = 0; i < count; ++i) {
                generateSingleStatement(environment, contract, function, containingBlock);
            }
        }
        --depth;
    }
}
