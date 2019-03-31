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
package soltix.ast;

import soltix.Configuration;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.expressions.ExpressionBuilder;
import soltix.interpretation.variables.VariableEnvironment;

import java.util.ArrayList;

import static soltix.ast.ASTLiteral.LiteralType.LITERAL_TYPE_INTEGER_DECIMAL;

public class ASTFunctionCall extends ASTNode {
    private ASTNode called = null;
    private ArrayList<ASTNode> arguments;
    private ArrayList<String> argumentNames;
    private ArrayList<Expression> expressionArguments;
    private boolean isStructConstructorCall;
    private boolean isRenamedRevert = false;
    private boolean isRenamedAssert = false;
    private boolean isRenamedRequire = false;
    private ASTContractDefinition contract = null;

    // Pass contract for renaming bookkeeping.
    // TODO Probably finalize() should receive the AST for such state info instead. UPDATE: Probably not, since the finalize() environment is too restricted by a lack of resolved references etc.
    public void setContract(ASTContractDefinition contract) { this.contract = contract; }

    public ASTFunctionCall(long id, boolean isStructConstructorCall, ArrayList<String> argumentNames) throws Exception {
        super(id);
        this.isStructConstructorCall = false; //isStructConstructorCall;
        this.argumentNames = argumentNames;
    }

    public ASTNode getCalled() { return called; }
    public ArrayList<ASTNode> getArguments() { return arguments; }

    // Convert the ASTNode representation of an expression to an Expression object
    public ArrayList<Expression> getExpressionArguments(VariableEnvironment environment) throws Exception {
        if (expressionArguments == null) {
            expressionArguments = new ArrayList<Expression>();
            for (ASTNode argument : arguments) {
                Expression expressionArgument = ExpressionBuilder.fromASTNode(environment, argument);
                expressionArguments.add(expressionArgument);
            }
        }
        return expressionArguments;
    }

    @Override
    public String toSolidityCode() throws Exception {
        String code = called.toSolidityCode() + "(";
        if (/*isStructConstructorCall &&*/ argumentNames != null) {
            code += "{";

            /*
            System.out.println("args count " + arguments.size() + " names count " + argumentNames.size());
            for (int i = 0; i < arguments.size(); ++i) {
                System.out.println( "  arg " + i +  " = " + arguments.get(i).toSolidityCode());
            }
            for (int i = 0; i < argumentNames.size(); ++i) {
                System.out.println("   name " + i + " = " + argumentNames.get(i));
            }*/
        }
        for (int i = 0; i < arguments.size(); ++i) {
            if (i > 0) {
                code += ", ";
            }
            if (argumentNames != null) { //isStructConstructorCall) {
                code += argumentNames.get(i) + ": ";
            }
            code += arguments.get(i).toSolidityCode();
        }
        if (argumentNames != null) { //isStructConstructorCall) {
            code += "}";
        }
        code += ")";
        return code;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }

    public void addArgumentNode(ASTNode argument) {
        if (arguments == null) {
            arguments = new ArrayList<ASTNode>();
        }
        arguments.add(argument);
    }

    public void finalize() throws Exception {
        if (getChildCount() < 1) {
            throw new Exception("Function call has unexpected child count - " + getChildCount());
        }

        if (!(getChild(0) instanceof ASTIdentifier)
                //&& !(getChild(0) instanceof ASTElementaryTypeNameExpression)
                && !getChild(0).isTypeName()
                && !(getChild(0) instanceof ASTMemberAccess)
                && !(getChild(0) instanceof ASTFunctionCall)
                && !(getChild(0) instanceof ASTTupleExpression)/* "siringAuction.bid.value(msg.value - autoBirthFee)(_sireId, msg.sender)" */
                && !(getChild(0) instanceof ASTNewExpression)) {
            throw new Exception("Function call not applied to identifier, but to " + getChild(0).getClass().getName());
        }

        // Extract call name, remove node
        //setName(getChild(0).getName());
        called = getChild(0);
        removeChildNode(0);

        // Remaining children are arguments
        arguments = getChildren();
        setChildren(null);

        if (Configuration.renameExceptionalEvents && called instanceof ASTIdentifier) {
            if (called.getName().equals("revert")) {
                called.setName("_myrevert");
                isRenamedRevert = true;
                contract.setContainsRenamedRevertReferences(true);
            }
            if (called.getName().equals("assert")) {
                called.setName("_myassert");
                isRenamedAssert = true;
                contract.setContainsRenamedAssertReferences(true);
            }
            if (called.getName().equals("require")) {
                called.setName("_myrequire");
                isRenamedRequire = true;
                contract.setContainsRenamedRequireReferences(true);
            }

            /* Deactivate this for now, since it's currently not used and the additional argument causes a "stack too deep" error
               for at least one Securify test suite case

            if (isRenamedRevert || isRenamedAssert || isRenamedRequire) {
                // Pass call AST ID as argument to identify the location
                // TODO Probably currently useless due to renaming additions messing up all IDs
                ASTLiteral idLiteral = new ASTLiteral(0, "", String.valueOf(getID()), LITERAL_TYPE_INTEGER_DECIMAL, null);
                idLiteral.finalize();
                arguments.add(idLiteral);
            }*/
        }
    }
}
