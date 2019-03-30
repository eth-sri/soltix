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

import soltix.ASTEditor;
import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.expressions.ExpressionEvaluationErrorHandler;
import soltix.interpretation.expressions.ExpressionEvaluator;
import soltix.interpretation.variables.Variable;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.util.RandomNumbers;

import java.util.ArrayList;


/**
 * Class to generate functions (of various available types).
 */
public class FunctionGenerator {
    public enum FunctionType {
        // For full-contract generation (initial version): Function body is sequence of assignmenst
        FUNCTION_TYPE_ASSIGNMENT_SEQUENCE("assignmentSequence"),
        // For type conversion purposes: Body is single return statement with generated expression
        FUNCTION_TYPE_SINGLE_RETURN("singleReturn"),
        // For FCG and full-contract generation: Body is random statement sequence (most diverse code structure)
        // ("random" and "complete" functions are synonymous)
        FUNCTION_TYPE_RANDOM("random");

        private String name;

        FunctionType(String name) {
            this.name = name;
        }

        static public FunctionType fromName(String name) {
            for (FunctionType type : values()) {
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    private AST ast;
    private ASTEditor astEditor;
    private RandomNumbers prng;
    private VariableGenerator variableGenerator;
    private StructTypeGenerator structTypeGenerator;
    private PredicateGenerator predicateGenerator;
    private ExpressionGenerator expressionGenerator;
    private ExpressionEvaluator expressionEvaluator;
    private TypeConverter typeConverter;
    private StatementGenerator statementGenerator;
    private int errorId = 0;

    public FunctionGenerator(AST ast, RandomNumbers prng,
                             VariableGenerator variableGenerator,
                             StructTypeGenerator structTypeGenerator) throws Exception {
        this.ast = ast;
        this.prng = prng;
        this.variableGenerator = variableGenerator;
        this.structTypeGenerator = structTypeGenerator;
        astEditor = new ASTEditor(ast);
        predicateGenerator = new PredicateGenerator(ast, astEditor, prng);
        expressionGenerator = new ExpressionGenerator(ast, predicateGenerator, this, prng);
        expressionEvaluator = new ExpressionEvaluator(new ExpressionEvaluationErrorHandler(prng));
        typeConverter = new TypeConverter(ast, prng, expressionEvaluator);
        statementGenerator = new StatementGenerator(ast, prng, expressionGenerator, structTypeGenerator, typeConverter, true);
    }

    public ASTFunctionDefinition generateTransactableFunctionSignature(ASTContractDefinition containingContract, String name) throws Exception {
        ASTFunctionDefinition functionDefinition = new ASTFunctionDefinition(0, name, "public", "payable",
                false, false);
        int parameterCount = (int) prng.generateLongInteger(0, 5);
        ASTParameterList parameterList = new ASTParameterList(0);

        int currentArgumentIndex = 0;
        for (int i = 0; i < parameterCount; ++i) {
            String argName = "a" + currentArgumentIndex++;

            // Note: In this context struct arguments are not allowed, since they cannot be passed at least by truffle,
            // but may not currently be supported by the ABI either
            ASTVariableDeclaration parameter = variableGenerator.generateRandomVariable(containingContract, argName, false,false, true);

            // TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // Add the parameter! This was missed for some reason, but it's currently beneficial to start with simpler
            // code as we implement interpretation
            parameterList.addChildNode(parameter);
        }
        parameterList.finalize();

        functionDefinition.addChildNode(parameterList);
        ASTParameterList returnList = new ASTParameterList(0);
        // TODO Add support for return values
        functionDefinition.addChildNode(returnList);

        // Return function already finalized, even though the body must still be added later
        // (so signature data shows where expected for transaction generation)
        functionDefinition.finalize();

        return functionDefinition;
    }

    public void generateFunctionBody(VariableEnvironment environment,
                                     ASTContractDefinition contract,
                                     ASTFunctionDefinition function,
                                     FunctionType type) throws Exception {
        switch (type) {
            case FUNCTION_TYPE_ASSIGNMENT_SEQUENCE:
                generateFunctionBodyAssignmentSequence(contract, environment, function);
                break;
            case FUNCTION_TYPE_SINGLE_RETURN:
                // TODO implement this. It should probably be shaped as follows:
                //    function f(arglist...) { return <expr>; }
                // where <expr> is a generated expression that should access every argument.
                throw new Exception("FunctionGenerator.generateFunctionBody for unsupported type " + type);
                //break;
            case FUNCTION_TYPE_RANDOM:
                generateFunctionBodyRandomStatements(environment, contract, function);
                break;
            default:
                throw new Exception("FunctionGenerator.generateFunctionBody called for unsupported body type " + type);
        }
    }

    // Generate a function with >= 1 arguments that returns 1 return value of random type, and only contains a return
    // statement with an expression as argument, e.g.:
    //
    //     function f(t1 arg1, t2, arg2, ...) {
    //        return ...expr involving t1, t2, ....
    //     }
    //
    // This simple structure allows us to simulate function call evaluation using our expression evaluator
    // with only minor additions. Thus, we can generate expressions containing function calls, and still know
    // the expected result value.
    //
    // To make this work, we work with an internal expression representation that can be evaluated as any other
    // expession, but uses two exernal code output aliases. E.g. given 2 expressions
    //
    //    (x+1)    and   --y
    //
    // we can
    //
    //    1. Assign argument name aliases:
    //      (x+1) = arg1     y-- = arg2
    //    2. Use the expression generator to build some expression as usual, such as
    //      (x+1) * --y
    //    3. Assign a function call alias to that constructed expression:
    //      (x+1) * --y = f((x+1), --y)
    //    4. Generate a function and output the expression using our name aliases instead of the actual code:
    //      function f(t1 arg1, t2, arg2) {
    //         return arg1 * arg2;
    //      }
    //    5. Evaluate the call f((x+1), --y) using the underlying full expression
    //    6. Output the code using the call alias f((x+1), --y)
    //
    // Note that the generated expression currently may not contain further calls to preceding function definitions,
    // since that:
    //
    //    - Introducs a clash for items that are already aliased for the underlying expression and then aliased again
    //    - Raises questions on proper evaluation rules (evaluated-value-caching would probably work to avoid side
    // (effect reevaluation issues)
    static public String returnFunctionNamePrefix = "__returnFunction";
    static public int returnFunctionCounter = 0;
    public Expression generateFunctionWithSingleReturn(ASTContractDefinition contract,
                                                 VariableEnvironment environment,
                                                 ArrayList<Expression> arguments) throws Exception{

        // 1. Assign aliases
        for (int i = 0; i < arguments.size(); ++i) {
            String name = "arg" + i;
            arguments.get(i).assignOutputAlias(name);
        }

        // 2. Build expression
        ASTNode randomResultType = TypeContainer.chooseRandomType(ast, contract, Configuration.allowStructsInFunctionABI, structTypeGenerator, prng);
        boolean oldAllowFunctionCalls = expressionGenerator.getAllowFunctionCalls();
        expressionGenerator.setAllowFunctionCalls(false); // don't allow nested calls, as explained above

        // Also disable increment/decrement operators, since these can fool us into thinking side effects took place when
        // they really didn't, e.g. in:
        //      function f(t arg) { return ++arg; }      f(v);
        // we build "++v" as evaluation expression, but the compiler works on an argument that is discarded
        boolean oldAvoidGeneratingSubexpressionSideEffects = Configuration.avoidGeneratingSubexpressionSideEffects;
        Configuration.avoidGeneratingSubexpressionSideEffects = true;

        // Create a copy of the list structure because the expression generation shall not disrupt it
        ArrayList<Expression> worklist = new ArrayList<Expression>();
        for (Expression expression : arguments) {
            worklist.add(expression);
        }

        //Configuration.verboseDebugOutput = true;


        // Evaluator *must*, in order to produce proper side effects for arguments (e.g. f(i++)):
        //   1. include every argument in the resulting expression
        //   2. include it in a subexpression that definitely gets evaluated (may not always be the case for &&, || and ?:)
        Expression generatedExpression = expressionGenerator.synthesizeBaseExpressionForSpecifiedWorklist(contract, worklist, environment, randomResultType, true);

        expressionGenerator.setAllowFunctionCalls(oldAllowFunctionCalls);
        Configuration.avoidGeneratingSubexpressionSideEffects = oldAvoidGeneratingSubexpressionSideEffects;

        // 3. Assign a function call alias to the expression.
        // This is modelled by creating an ordinary function call expression, which is also supplied with the
        // underlying expression to be evaluated.
        // The expression evaluator can pick up on that expression, evaluate that instead of the function call, and
        // the code output for the function call works as usual.
        // It's important to keep working with expressions here rather than say switching to textual expression argument
        // representations already at this point, because the expression evauation may later change it due to fix-up
        // operations
        String functionName = returnFunctionNamePrefix + returnFunctionCounter++;
        ASTFunctionCall functionCall = new ASTFunctionCall(0, false, null);
        functionCall.addChildNode(new ASTIdentifier(0, functionName, 0));
        functionCall.finalize();
        Expression specialCallExpression = new Expression(functionCall, arguments, randomResultType, generatedExpression);

        // 4. Generate function definition.
        StringBuilder sb = new StringBuilder();

        String trace = "";//"returnFunction3";

        if (trace.equals(functionName)) {
            System.out.println("### " + trace + " CODE: " + generatedExpression.toASTNode().toSolidityCode() + "\n");
        }

        // function(args...) returns (T) {


        sb.append("function ");
        sb.append(functionName);
        sb.append("(");
        for (int i = 0; i < arguments.size(); ++i) {
            Expression expression = arguments.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(expression.getType().toSolidityCode());
            sb.append(" ");
            if (Type.isStructType(ast, expression.getType()) || Type.isStringType(expression.getType())) { // TODO more generic solution?
                sb.append("memory ");
            }
            sb.append(expression.getOutputAlias());
        }
        sb.append(") public returns (");
        sb.append(randomResultType.toSolidityCode());
        if (Type.isStructType(ast, randomResultType) || Type.isStringType(randomResultType)) { // TODO more generic solution?
            sb.append(" memory");
        }
        sb.append(") {");
        String firstPart = sb.toString();


        // return <expr>; }
        firstPart += "return ";
        String secondPart = "; }";

        // Save function definition
        // We need to keep the generated expression around - and introduce a new ASTVerbatimText hack for this purpose
        // alone - because fix-up operations may alter the expression and render saved textual code at this point
        // obsolete
        ASTVerbatimText functionNode1 = new ASTVerbatimText(0, firstPart, generatedExpression, secondPart); //sb.toString());
        contract.addChildNode(functionNode1);



/*
        System.out.println(" ---- func " + functionName);
        specialCallExpression.assignID();
        System.out.println("CALL EXPR ID " + specialCallExpression.getID());
        System.out.println("generated expr " + generatedExpression.toASTNode().toSolidityCode());
        System.out.println("ARGS:");
        for (int i = 0; i < arguments.size(); ++i) {
            //arguments.get(i).assignOutputAlias(null);
            System.out.println("   " + i + " = " + arguments.get(i).toASTNode().toSolidityCode());
            if (arguments.get(i).getOutputAlias() == null) System.out.println("               !!!! null alias");
            else System.out.println("            alias " + arguments.get(i).getOutputAlias());
        }*/

      /*  ArrayList<Expression> temp = new ArrayList<Expression>();
        for (Expression e: arguments) {
            temp.add(e);
        }
        generatedExpression.debugIntermediateValuesString(temp);
        if (temp.size() > 0) {
            for (Expression e: temp) {
                System.out.print("   MISSING " + e.toASTNode().toSolidityCode());
                e.assignOutputAlias(null);
                System.out.println(" = " + e.toASTNode().toSolidityCode());
            }
            throw new Exception("ERROR: Not all arguments included in function body for " + functionName);
        }*/

        return specialCallExpression;
    }


    // Generate a function body containing a random sequence of statements.
    // These may not be execute without EVM exception, since no interpretation has taken place yet
    protected void generateFunctionBodyRandomStatements(VariableEnvironment environment,
                                                        ASTContractDefinition contract,
                                                        ASTFunctionDefinition function) throws Exception {
        ASTBlock bodyBlock = new ASTBlock(0);

        assert function.getBody() == null;
        function.setBody(bodyBlock);
        function.addChildNode(bodyBlock); // for output

        int statementCount = (int) prng.generateLongInteger(Configuration.contractGenerationFunctionStatementsLowerBound,
                Configuration.contractGenerationFunctionStatementsUpperBound);

        for (int i = 0; i < statementCount; ++i) {
            statementGenerator.generate(environment, contract, function, bodyBlock);
        }
    }


    // Generate a function body containing:
    //
    //     - A sequence of assignments from random expressions to randomly chosen variables in the environment
    //     - A check of the assignment result vs. the expected (as per expression evaluation) result, and an
    // EXPR_ERROR() event emission in case of mismatch
    //     - Randomly inserted (P=0.5) and generated Always-True-Guard if-statements preceding assignments
    //
    // Thus the structure of a generated function looks like this:
    //        Without ATGs:                               With ATGs:
    //      function f() {                              function f() {
    //         x = <expr>;                                  if (<tautology>)
    //         if (x != <value>) emit EXPR_ERROR(num);          x = <expr>;
    //      }                                                if (x != <value>) emit EXPR_ERROR(num);
    //                                                  }
    // In order to enable Live Code Mutations, variable value snapshots are recorded for the assignment
    // and result check AST nodes. Our advantage here is that we currently only have a single known value
    // in each context, since there are no loops
    protected void generateFunctionBodyAssignmentSequence(ASTContractDefinition contract,
                                                          VariableEnvironment environment,
                                                          ASTFunctionDefinition function) throws Exception {
        ASTBlock bodyBlock = new ASTBlock(0);

        assert function.getBody() == null;
        function.setBody(bodyBlock);
        function.addChildNode(bodyBlock); // for output

        // For now we generate a sequence of assignments of random expressions to randomly chosen variables,
        // with ATG if-statements thrown in occasionally
        int expressionCount = (int) prng.generateLongInteger(Configuration.contractGenerationFunctionStatementsLowerBound,
                Configuration.contractGenerationFunctionStatementsUpperBound);
        for (int i = 0; i < expressionCount; ++i) {
            Variable variable = expressionGenerator.pickRandomUsableVariable(environment);
            Expression lhsVariableExpression = new Expression(variable);

            // For structs, the assignment would work, but the result comparison would need to be
            // performed member-wise.
            // To address thus, we access a non-struct member of v instead if it is a struct type
            if (Type.isStructType(ast, variable.getType())) {
                lhsVariableExpression = typeConverter.accessRandomStructMember(environment,
                                                                                lhsVariableExpression,
                                                                                ast.getStructDefinition(variable.getType().getName()));
            }


            Expression expression = expressionGenerator.synthesizeBaseExpressionForRandomWorklist(contract, environment, lhsVariableExpression.getType());

            // Randomly precede assignment with ATG. Note that we do not generate a block here, even though
            // we subsequently generate two statements, so it will be parsed as:
            //     if (<tautology>)
            //         v = <expr>;
            //     if (v != <value>) emit EVENT();
            // This is fine for ATG guards, since the assignment should always be executed, but would require changes
            // if false guards will be used some time
            boolean generateATG = false; //prng.flipCoin();
            boolean assignmentResultOracle = true; // TODO parameterize + implement alternative "outro" oracle
            ASTVerbatimText currentATGNode = null;
            if (generateATG) {
                Expression tautology = predicateGenerator.synthesizePredicate(environment, true,
                        Configuration.randomPredicateDepth(prng));

                if (Configuration.checkRuntimeTautologyCorrectness) {
                    ASTVerbatimText checkNode = astEditor.generateExpressionValueCheck(tautology, true);

                    // Tautology checks drastically reduce the amount of other code that is allowed before the framework
                    // fails:
                    //   bodyBlock.addChildNode(checkNode);
                }
                currentATGNode = new ASTVerbatimText(0, "if (" + tautology.toASTNode().toSolidityCode() + ")");
                bodyBlock.addChildNode(currentATGNode);
            }

            // Generate assignment and subsequent value check:
            //    v = <expr>; if (v != <interpreted_expr>) emit EXPR_ERROR(<id>);
            // (We already ruled out a struct type for v above)
            String lhsVariableString = lhsVariableExpression.toASTNode().toSolidityCode();

            // Record value prior to computing the expression (which may contain side effects)
            VariableEnvironment preAssignmentValues = environment.createSingleValueSnapshot(0);

            ExpressionEvaluator.ComputedValues evaluationResult = expressionEvaluator.evaluateForAll(environment, expression); //expressionEvaluator.incrementallyEvaluated(environment, expression);

/*
            if (errorId == 21) {
                System.out.println("faulty expr " + expression.debugIntermediateValuesString());
                System.exit(0);
            }*/

            String checkStatement = null;
            if (assignmentResultOracle) {
                String lhsVariableStringForComparison = Type.isStringType(expression.getType())
                                        ? "keccak256(bytes(" + lhsVariableString + "))"
                                            : lhsVariableString;
                String rhsVariableStringForComparison = Type.isStringType(expression.getType())
                                        ? "keccak256(bytes(" + evaluationResult.values.get(0).toASTNode(false).toSolidityCode() + "))"
                                            : evaluationResult.values.get(0).toASTNode(false).toSolidityCode();
                checkStatement = "if (" + lhsVariableStringForComparison + " != " + rhsVariableStringForComparison
                        + ") emit EXPR_ERROR(" + errorId++ + ");";
            }

            if (generateATG) {
                // Indent
                lhsVariableString = "\t" + lhsVariableString;
            }
            ASTVerbatimText assignment = new ASTVerbatimText(0, lhsVariableString
                    + " = " + expression.toASTNode().toSolidityCode() + ";");

            ASTVerbatimText checkStatementNode = checkStatement == null? null: new ASTVerbatimText(0, checkStatement);

            assignment.setVariableEnvironment(preAssignmentValues);

            // TODO Fix parent relationships, which aren't correct yet and lead to a failure to perform an
            // instrumentation of the form
            //     if (true-expr) // ATG
            //        emit PROFILING_EVENT(...);
            //        x = y;
            // with the correct surrounding block, since should really be child to the ATG (which is not so easy
            // apparently)

            /*if (currentATGNode != null) {
                currentATGNode.addChildNode(assignment);
            } else*/
            {
                bodyBlock.addChildNode(assignment);
                if (checkStatementNode != null) {
                    bodyBlock.addChildNode(checkStatementNode);
                }
            }

            // Update assigned-to variable's value
            environment.updateVariableValue(variable, lhsVariableExpression, evaluationResult.values.get(0));

            // Record value after computing the expression (which may contain side effects) and updating the lhs variable
            VariableEnvironment postAssignmentValues = environment.createSingleValueSnapshot(0);

            // TODO Where to store the snapshot if no check node is present? Add an empty expression statement?!
            if (checkStatementNode != null) {
                checkStatementNode.setVariableEnvironment(postAssignmentValues);
            }
        }
    }
}
