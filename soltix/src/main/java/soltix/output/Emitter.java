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
package soltix.output;

import java.io.OutputStream;
import java.io.PrintWriter;

import soltix.Configuration;
import soltix.ast.*;

/**
 * Class to generate output in various forms from input AST, currently:
 *   - Solidity code
 *   - GraphVIZ dot file for graph plot
 */
public class Emitter {
    private Emitter() {}
    static protected void defineASTVerticesForGraphPlot(PrintWriter writer, ASTNode currentNode) throws Exception {
        String code = currentNode.getID() + ": "; // Prefix code with node ID
        String codeStart = currentNode.toSolidityCode();
        String codeEnd = currentNode.toSolidityCodePostfix();

        if (codeStart != null) {
            code += codeStart;
        } else {
            code += currentNode.getClass().getName();
        }
        if (codeEnd != null) {
            code += " ... " + codeEnd;
        }

        // Escape quotes and escape chars to prevent GraphViz errors
        code = ASTLiteral.escapeString(code);


        // Define each vertex with its ID as name and Solidity code as label
        String colorString = "";
        if (Configuration.showCoverage) {
            colorString = ",color=" + (currentNode.getCovered()? "green": "red");
        }
        writer.println("    " + currentNode.getID() + "[label=\"" + code + "\" " + colorString + "];");
        for (int i = 0; i < currentNode.getChildCount(); ++i) {
            defineASTVerticesForGraphPlot(writer, currentNode.getChild(i));
        }
    }

    static protected void defineASTEdgesForGraphPlot(PrintWriter writer, ASTNode currentNode) {
        // Draw current node -> children edges
        for (int i = 0; i < currentNode.getChildCount(); ++i) {
            writer.println("    " + currentNode.getID() + " -> " + currentNode.getChild(i).getID() + ";");
        }
        // Process childern
        for (int i = 0; i < currentNode.getChildCount(); ++i) {
            defineASTEdgesForGraphPlot(writer, currentNode.getChild(i));
        }
    }

    static public void emitGraphPlot(OutputStream output, AST ast) throws Exception {
        PrintWriter writer = new PrintWriter(output);
        writer.println("digraph {");
        defineASTVerticesForGraphPlot(writer, ast.getRoot());
        defineASTEdgesForGraphPlot(writer, ast.getRoot());
        writer.println("}");
        writer.flush();
    }




    static protected void printNestingLevel(CodeOutputWriter writer, ASTNode node) throws Exception {
        int depth = node.getDepth();
        // Indent blocks at same level as their parent statements
        if (node instanceof ASTBlock) {
            --depth;
        }
        for (int i = 0; i < depth; ++i) {
            writer.print("    ");
        }
    }

    static private int callDepth = 0;

    static protected void outputASTSolidityCode(CodeOutputWriter writer, ASTNode currentNode) throws Exception {
        ++callDepth;
        if (currentNode.toSolidityCode() != null) {
            if (Configuration.showCoverage && currentNode.getCovered()) {
                writer.print("/*#*/");
            }
            printNestingLevel(writer, currentNode);

            String termination = "";
            if (currentNode instanceof ASTVariableDeclaration // Declaration which is not part of a declaration statement (?)
                || (currentNode instanceof ASTFunctionDefinition && currentNode.getChildCount() == 0) // Function declaration without body
                || currentNode instanceof ASTExpressionStatement
                || currentNode instanceof ASTVariableDeclarationStatement
                || currentNode instanceof ASTPlaceholderStatement
                || currentNode instanceof ASTUsingForDirective
                || currentNode instanceof ASTPragmaDirective
                || currentNode instanceof ASTEventDefinition) {

                boolean skipTermination = false;

                if (currentNode instanceof ASTExpressionStatement) {
                    ASTExpressionStatement expressionStatement = (ASTExpressionStatement)currentNode;

                    if (expressionStatement.getBody() != null
                        && expressionStatement.getBody() instanceof ASTAssignment
                        && ((ASTAssignment)expressionStatement.getBody()).getOutputIsBlock()) {
                        // This is an assignment embedded in an expressin statement -
                        //      x = y
                        // which has been rewritten to a block
                        //      { ... ; x = y; }
                        // and thus we have to suppress the semicolon to make the block an atomic
                        // item e.g. for an if statement body
                        skipTermination = true;
                    }
                }
                if (!skipTermination) {
                    termination = ";";
                }
            }

            int lineNumber = writer.getCurrentLineNumber();
            int positionInLine = writer.getCurrentLinePosition();

            // Output solidity code
            writer.println(currentNode.toSolidityCode() + termination);

            // Accounting in node
            int outputLength = writer.getCurrentLinePosition() - positionInLine;
            currentNode.setOutputCodeLineNumber(lineNumber);
            currentNode.setOutputCodeLinePosition(positionInLine);
            currentNode.setOutputCodeLength(outputLength);
        }

        if (currentNode instanceof ASTIfStatement) {
            // The if and else bodies are stored as child nodes for graph visualization,
            // but we have to process them consciously to insert the else part
            ASTIfStatement ifStatement = (ASTIfStatement) currentNode;
            outputASTSolidityCode(writer, ifStatement.getIfBranch());
            if (ifStatement.getElseBranch() != null) {
                printNestingLevel(writer, currentNode);
                writer.println("else");
                outputASTSolidityCode(writer, ifStatement.getElseBranch());
            }
        } else {
            for (int i = 0; i < currentNode.getChildCount(); ++i) {
                outputASTSolidityCode(writer, currentNode.getChild(i));
            }
        }

        if (currentNode.toSolidityCodePostfix() != null) {
            if (Configuration.showCoverage && currentNode.getCovered()) {
                writer.print("/*#*/");
            }
            printNestingLevel(writer, currentNode);
            writer.println(currentNode.toSolidityCodePostfix());
        }
        --callDepth;
    }

    static public void emitSolidityCode(OutputStream output, AST ast) throws Exception {
        PrintWriter writer = new PrintWriter(output);
        CodeOutputWriter codeWriter = new CodeOutputWriter(writer);

        if (Configuration.insertLCMSupportCode && Configuration.useABIEncoderV2) {
            // To allow for structs containing strings (clashes for contracts which already contain this line):
            // TODO: This encoder adds a 20x performance penalty for large contracts. Re-enable once fixed or generate V2-specific contracts
            codeWriter.println("pragma experimental ABIEncoderV2;");
        }

        outputASTSolidityCode(codeWriter, ast.getRoot());

        codeWriter.flush();
    }

    static public void emitSolidityCode(CodeOutputWriter writer, AST ast) throws Exception {

        if (Configuration.insertLCMSupportCode && Configuration.useABIEncoderV2) {
            // To allow for structs containing strings (clashes for contracts which already contain this line):
            // TODO: This encoder adds a 20x performance penalty for large contracts. Re-enable once fixed or generate V2-specific contracts
            writer.println("pragma experimental ABIEncoderV2;");
        }

        // Output code
        outputASTSolidityCode(writer, ast.getRoot());
    }

    // Only for limited transactions code for now: emit truffle JavaScript code
    static public void emitJavaScriptCode(OutputStream output, AST ast) throws Exception {
        PrintWriter writer = new PrintWriter(output);
        CodeOutputWriter codeWriter = new CodeOutputWriter(writer);

        outputASTSolidityCode(codeWriter, ast.getRoot());

        codeWriter.flush();
    }
}
