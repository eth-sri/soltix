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

package soltix.interpretation;


import soltix.Configuration;
import soltix.ast.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Class to perform various semantic interpretations of a specified contract's function within the AST,
 * and use a specified callback to use the information (for profiling instrumentation and result loading)
 */
public class ASTInterpreter {
    final static Logger logger = Logger.getLogger(ASTInterpreter.class);

    private boolean firstRun = true;
    private AST ast;
    private Scope scope;
    private IInterpreterCallback callbackObject;

    public enum NavigationPolicy {
        // (depth-first) traversal of all nodes - currently for profiling instrumentation
        NAVIGATION_POLICY_DESCEND_ALL,
        // request next target statement from callback object, follow path (which must be unambiguous, i.e. not pass
        // conditional statements)
        NAVIGATION_POLICY_TARGETED,
        // Full contract interpretation
        NAVIGATION_POLICY_FULL_INTERPRETATION
    }

    public ASTInterpreter(AST ast, IInterpreterCallback callbackObject) {
        this.ast = ast;
        this.callbackObject = callbackObject;
        callbackObject.initialize(this);
    }

    public AST getAST() { return ast; }
    public Scope getScope() { return scope; }

    protected void resolveInheritanceSpecifiers(ASTContractDefinition contract) throws Exception {
        ArrayList<ASTInheritanceSpecifier> inheritanceSpecifiers = contract.getInherits();
        if (inheritanceSpecifiers != null) {
            for (ASTInheritanceSpecifier inherits : inheritanceSpecifiers) {
                if (inherits.getContract() == null) {
                    ASTContractDefinition definition = ast.getContract(inherits.getName());
                    if (definition == null) {
                        throw new Exception("Contract definition for " + inherits.getName() + " not available");
                    }
                    inherits.setContract(definition);
                    definition.addInheritedBy(contract);

                    // Process this contract as well
                    resolveInheritanceSpecifiers(definition);
                }
            }
        }
    }

    protected void resolveModifierInvocations(ASTContractDefinition contract, ASTFunctionDefinition function) {
        ArrayList<ASTModifierInvocation> modifierInvocations = function.getModifiers();
        if (modifierInvocations != null) {
            for (ASTModifierInvocation invocation : modifierInvocations) {
                if (invocation.getDefinition() == null) {
                    ASTModifierDefinition definition = contract.getModifierDefinition(invocation.getName());
                    if (definition == null) {
                        // TODO: This is probably not really a modifier invocation, but a base contract
                        // constructor call - deal with that. For now just assume that's the case

                        //throw new Exception("Modifier definition for " + invocation.getName() + " not available");
                    } else {
                        invocation.setDefinition(definition);
                    }
                }
            }
        }
    }

    // Perform first interpretation pass, involving the resolution of various data structure references in the AST
    public void run(ASTContractDefinition contract, ASTFunctionDefinition function) throws Exception { // TODO Optionally supply a transaction
        if (firstRun) {
            // Perform various variable reference resolutions. This cannot be done earlier (at least
            // not so easily) since forward references are allowed
            scope = new Scope(contract);
            // Resolve contract inheritance specifiers to contract definitions. The modifier resolution
            // below already depends on the availability of base contract information
            resolveInheritanceSpecifiers(contract);
            // Resolve modifier invocation references to their corresponding definitions, unless this
            // has already been done earlier
            resolveModifierInvocations(contract, function);
            firstRun = false;
        }

        ast.setCurrentNode(function);

        interpret(ast);
        callbackObject.finish();
    }

    public void run() throws Exception {
        for (ASTContractDefinition contract : ast.getContracts()) {
            resolveInheritanceSpecifiers(contract);
        }
        for (ASTContractDefinition contract : ast.getContracts()) {
            for (ASTFunctionDefinition function : contract.getFunctions()) {
                resolveModifierInvocations(contract, function);
            }
        }

        interpret(ast);
        callbackObject.finish();
    }


    protected ArrayList<ASTNode> constructStatementPath(ASTNode sourceStatement,
                                                        ASTFunctionDefinition sourceFunction,
                                                        ASTNode targetStatement,
                                                        ASTFunctionDefinition targetFunction) throws Exception {
        ArrayList<ASTNode> path = new ArrayList<ASTNode>();

        // We expect the profiling instrumentation to generate events which are sprinkled over the
        // code with sufficient density that any two distinct events that may be emitted successively
        // at runtime can be connected through a path as follows:
        //
        //    - If source and target statement are located in different functions, we presume that the
        // source statement caused this transition (for an "if" or "while" statement that would mean
        // a call in the condition expression, "for" loops haven't been worked out yet).
        // TODO We currently don't distinguish between a inter-function transition caused by a call or a return,
        // but this may later become desirable to trace across AST nodes that aren't prepended with profiling
        // event emissions - but a better case can probably be made for more aggressive instrumentation on
        // every line
        //    - Otherwise, a path between the source and target statement can be found by starting
        // from the statement which is located lower down the AST (i.e. has a higher tree depth value)
        // up to the other statement.
        //    - If both statements are located at the same depth, they are assumed to be children
        // of the same containing AST item (typically or maybe always an ASTBlock)
        if (sourceFunction != targetFunction) {
            return path;
        }

        ASTNode upperNode;
        ASTNode lowerNode;

        if (sourceStatement.getDepth() == targetStatement.getDepth()) {
            upperNode = sourceStatement.getID() < targetStatement.getID()? sourceStatement: targetStatement;
            lowerNode = sourceStatement.getID() > targetStatement.getID()? sourceStatement: targetStatement;
        } else {
            upperNode = sourceStatement.getDepth() < targetStatement.getDepth() ? sourceStatement : targetStatement;
            lowerNode = sourceStatement.getDepth() > targetStatement.getDepth() ? sourceStatement : targetStatement;
        }
        boolean lowerNodeIsSource = lowerNode == sourceStatement;

        while (lowerNode.getDepth() != upperNode.getDepth()) {
            if (lowerNode.getParent() == null) {
                throw new Exception("Cannot find path between statements " + lowerNode.getID() + " and " + upperNode.getID());
            } else if (lowerNode.getParent().getID() == upperNode.getID()) {
                // OK - we have a path
                return path;
            } else {
                if (lowerNode.getParent() instanceof ASTBlock) {
                    // The lower node is embedded in a block - process all subsequent child nodes
                    // before we can leave that block
                    // TODO: Include block's children in statement list. Take care of source/target distinction
                    // (this affects the path segment's position - start/end)
                    ASTBlock parentBlock = (ASTBlock)lowerNode.getParent();
                    int statementIndex = parentBlock.getChildNodeIndex(lowerNode);
                    if (lowerNode == sourceStatement) {
                        // {  { a; b; c; }    { d; e; f; }  { g; h; i; } }
                        // For a source of "e", the first sub-path is "f", followed by "g; h; i;", both
                        // of which components are already in the correct order and are followed by the
                        // remainder of the path to the target. All other parent components, such as
                        // control structures, must also be appended as they are encountered
                        for (int i = statementIndex + 1; i < parentBlock.getChildCount(); ++i) {
                            path.add(parentBlock.getChild(i));
                        }
                    } else { // lowerNode == targetStatement
                        // {  { a; b; c; }    { d; e; f; }  { g; h; i; } }
                        // For a target of "e", the firs sub-path is "d", followed by "a; b; c;", both of
                        // which components must be flipped for the correct order, and prepended with the
                        // remainder of the path starting from the source. All other parent components,
                        // such as control structures, must also be prepended as they are encountered
                        for (int i = 0; i < statementIndex - 1; ++i) {
                            path.add(i, parentBlock.getChild(i));
                        }
                        // Knowing that something in the block was executed, the block node can also be marked
                        // as covered
                        path.add(0, parentBlock);
                    }
                } else { // parent isn't block
                    // As described in the block branch above, this node must be prepended for the
                    // lower = target case and may be appended for the lower = source case
                    if (lowerNode == targetStatement) {
                        path.add(0, lowerNode.getParent());
                    } else {
                        path.add(lowerNode.getParent());
                    }
                }

                lowerNode = lowerNode.getParent();
                if (lowerNodeIsSource) {
                    sourceStatement = lowerNode;
                } else {
                    targetStatement = lowerNode;
                }
            }
        }

        // Both statements are at the same depth - but are they in the same parent block? This may not be
        // the case for our mapping assignment instrumentation, which generates standalone blocks that
        // result in unexpected statement paths e.g. from source to target in:
        //     {   { source; }    { target; } }
        // Our solution to this is a temporary workaround (TODO) skipping one block level to address only
        // the exact case given in the example above.
        // TODO This whole path finding routine should be rewritten or disabled, since it's not general
        // enough and currently not really needed (but it does make for nice colored coverage graph plots)
        //
        // It should be fairly straightforward to:
        //     - Search the common block parent
        //     - Use the statement IDs to determine whether source/target is on the left/right in the tree
        //     - Use the source/target distinction to determine whether we're moving forward
        //       (left-to-right in the tree) or backward (right-to-left - moving up again in a loop)
        //     - Process all intermediate nodes accordingly
        if (lowerNode.getParent() != upperNode.getParent()) {
            if (lowerNode.getParent() instanceof ASTBlock
                    && upperNode.getParent() instanceof ASTBlock) {
                ASTBlock lowerBlock = (ASTBlock)lowerNode.getParent();
                ASTBlock upperBlock = (ASTBlock)upperNode.getParent();
                if (lowerBlock.getParent() == upperBlock.getParent()) {
                    lowerNode = lowerBlock;
                    upperNode = upperBlock;
                    if (lowerNodeIsSource) {
                        sourceStatement = lowerNode;
                        targetStatement = upperNode;
                    } else {
                        targetStatement = lowerNode;
                        sourceStatement = upperNode;
                    }
                }
            }
        }


        assert sourceStatement.getParent() == targetStatement.getParent();
        assert sourceStatement.getParent() instanceof ASTBlock;
        ASTBlock containingBlock = (ASTBlock)sourceStatement.getParent();

/*
        System.out.println("source stmt id " + sourceStatement.getID() + " dest stmt od " + targetStatement.getID());
        System.out.println("source stmt " + sourceStatement.toSolidityCode() + " dest stmt " + targetStatement.toSolidityCode());
        System.out.println("containing block (assumed) " + containingBlock.toSolidityCode());
        System.out.println("parent of that block " + containingBlock.getParent().toSolidityCode());
*/

        int sourceStatementIndex = containingBlock.getChildNodeIndex(sourceStatement);
        int targetStatementIndex = containingBlock.getChildNodeIndex(targetStatement);

        if (sourceStatementIndex < targetStatementIndex) {
            // Straightforward case: from a to b in "{ ... a; ... b; ...}"
            for (int i = sourceStatementIndex + 1; i < targetStatementIndex; ++i) {
                path.add(containingBlock.getChild(i));
            }
        } else {
            // The case of a to b in "{ ... b; ... a; ... }" - we revisited the parent node of the block
            // (probably a control structure) prior to visiting b after a
            for (int i = sourceStatementIndex + 1; i < containingBlock.getChildCount(); ++i) {
                path.add(containingBlock.getChild(i));
            }
            path.add(containingBlock.getParent());
            for (int i = 0; i < targetStatementIndex - 1; ++i) {
                path.add(containingBlock.getChild(i));
            }
        }

        return path;
    }

    protected void interpret(AST ast) throws Exception {
        ASTNode currentNode = ast.getCurrentNode();

        if (callbackObject.getNavigationPolicy() == NavigationPolicy.NAVIGATION_POLICY_DESCEND_ALL) {
            callbackObject.visitNodeBeforeProcessing(currentNode);

            scope.enterNode(currentNode, null); // We do not record values here, so no initializer is given

            // Depth-first child node traversal for all paths
            for (int i = 0; i < currentNode.getChildCount(); ++i) {
                ast.setCurrentNode(currentNode.getChild(i));
                interpret(ast);
            }

            callbackObject.visitNodeAfterProcessing(currentNode);

            // Restore position
            ast.setCurrentNode(currentNode);

            scope.leaveNode(currentNode);
        } else if (callbackObject.getNavigationPolicy() == NavigationPolicy.NAVIGATION_POLICY_TARGETED) {
            ASTFunctionDefinition currentFunction = currentNode instanceof ASTFunctionDefinition
                    ? (ASTFunctionDefinition) currentNode
                    : ast.getFunctionByStatementId(currentNode.getID());

            callbackObject.visitNodeBeforeProcessing(currentNode);
            if (currentNode instanceof ASTFunctionDefinition) {
                // Mark the body as covered as well
                callbackObject.visitNodeBeforeProcessing(currentFunction.getBody());
            }

            boolean followStatementPaths = true;

            // The next statement target is given by the callback
            for (; ; ) {
                ASTNode nextStatement = callbackObject.nextTargetStatement();
                if (nextStatement == null) {
                    // Stop processing
                    break;
                }
                ASTFunctionDefinition nextFunction = ast.getFunctionByStatementId(nextStatement.getID());


                if (Configuration.verboseDebugOutput) {
                    logger.info("### coverage " + currentNode.getID() + " at line " + currentNode.getOutputCodeLineNumber() +
                            " -> " + nextStatement.getID() + " " + nextFunction.getName() + " at " + nextStatement.getOutputCodeLineNumber());
                }

                if (nextStatement == currentNode) { //nextStatementId == currentNode.getID()) {
                    // An empty path - previous event was repeated
                    ;
                } else {
                    try {
                        // To construct the path, we can follow parent node references
                        ArrayList<ASTNode> statementPath = constructStatementPath(currentNode, currentFunction, nextStatement, nextFunction);

                        // Traverse path - assume proper ascending order (though this currently doesn't matter)
                        for (int i = 0; i < statementPath.size(); ++i) {
                            callbackObject.visitNodeBeforeProcessing(statementPath.get(i));
                        }
                    } catch (Exception e) {
                        // Bug. give up, since we don't really need this now
                        followStatementPaths = false;
                        logger.error("constructStatementPath exception - giving up on path tracing");
                    }
                }

                // TODO process source/dest for scope and callbacks
                currentNode = nextStatement;
                currentFunction = nextFunction;

                callbackObject.visitNodeBeforeProcessing(currentNode);
            }
        } else if (callbackObject.getNavigationPolicy() == NavigationPolicy.NAVIGATION_POLICY_FULL_INTERPRETATION) {
            callbackObject.run();
        } else {
            throw new Exception("Unsupported interpreter AST navigation policy " + callbackObject.getNavigationPolicy());
        }
    }
}
