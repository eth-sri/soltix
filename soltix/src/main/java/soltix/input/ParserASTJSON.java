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

package soltix.input;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import soltix.Configuration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import soltix.ast.*;

/**
 * Parser of Solidity code supplied in "--ast-json" solc JSON output format
 */
public class ParserASTJSON extends Parser {
    protected void debugPrint(int depth, String s) {
        if (!Configuration.debugASTOutput) return;

        for (int i = 0; i < depth; ++i) {
            System.out.print("\t");
        }
        System.out.println(s);
    }

    protected boolean nameEquals(String name, String value) {
        return name != null && name.equals(value);
    }

    protected void processSourceUnit(long id, AST ast, JSONObject attributes) throws Exception {
        if (ast.getRoot() != null) {
            throw new Exception("Unexpected SourceUnit item");
        }
        String codeFile = (String)attributes.get("absolutePath");
        if (Configuration.debugASTOutput && codeFile != null) {
            if (Configuration.debugASTOutput) {
                // Read source file into byte array to extract code referenced by JSON nodes
                Path path = Paths.get(codeFile);
                codeArray = Files.readAllBytes(path);
            }
        }
        ast.addInnerNode(new ASTSourceUnit(id));
    }

    protected void processContractDefinition(long id, AST ast, JSONObject attributes, String name) throws Exception {
        if (ast.getCurrentNode() == null || !(ast.getCurrentNode() instanceof ASTSourceUnit)) {
            throw new Exception("Unexpected ContractDefinition item");
        }
        String contractKind = (String)attributes.get("contractKind");
        if (contractKind == null) {
            throw new Exception("Contract definition has no contractKind field");
        }
        ast.addInnerNode(new ASTContractDefinition(id, name, contractKind));
    }

    protected void processFunctionDefinition(long id, AST ast, String attrName, JSONObject attributes) throws Exception {
        if (ast.getCurrentNode() == null || !(ast.getCurrentNode() instanceof ASTContractDefinition)) {
            throw new Exception("Unexpected FunctionDefinition item for parent item " + ast.getCurrentNode().getClass().getName());
        }
        String visibility = (String)attributes.get("visibility");
        if (visibility == null) {
            throw new Exception("Function definition without visibility attribute");
        }
        String stateMutability = (String)attributes.get("stateMutability");
        Boolean isConstructor = (Boolean)attributes.get("isConstructor");
        Boolean isConstant = (Boolean)attributes.get("constant"); // note: does not exist (= null) in 0.5.x, TODO handle pure/view?
        if (isConstant == null) {
            isConstant = Boolean.valueOf(false);
        }

        ast.addInnerNode(new ASTFunctionDefinition(id, attrName, visibility, stateMutability, isConstructor, isConstant));
    }

    protected void processParameterList(long id, AST ast, JSONObject object) throws Exception {
        if (ast.getCurrentNode() == null) {
            throw new Exception("Unexpected ParameterList item - root is null");
        } else if (!(ast.getCurrentNode() instanceof ASTFunctionDefinition)
                && !(ast.getCurrentNode() instanceof ASTEventDefinition)
                && !(ast.getCurrentNode() instanceof ASTModifierDefinition)
                && !(ast.getCurrentNode() instanceof ASTFunctionTypeName)) {
            throw new Exception("Unexpected ParameterList item for parent item " + ast.getCurrentNode().getClass().getName());
        }
        ast.addInnerNode(new ASTParameterList(id));
    }

    protected void processBlock(long id, AST ast, JSONObject object) throws Exception {
        if (ast.getCurrentNode() == null) {
            throw new Exception("Unexpected block item - no root");
        } else if (!(ast.getCurrentNode() instanceof ASTFunctionDefinition)
                && !(ast.getCurrentNode() instanceof ASTIfStatement)
                && !(ast.getCurrentNode() instanceof ASTForStatement)
                && !(ast.getCurrentNode() instanceof ASTWhileStatement)
                && !(ast.getCurrentNode() instanceof ASTDoWhileStatement)
                && !(ast.getCurrentNode() instanceof ASTModifierDefinition)
                && !(ast.getCurrentNode() instanceof ASTBlock)) {
            throw new Exception("Unexpected block item for parent item " + ast.getCurrentNode().getClass().getName());
        }

        ASTBlock block = new ASTBlock(id);
        if (ast.getCurrentNode() instanceof ASTIfStatement) {
            ASTIfStatement ifStatement = (ASTIfStatement)ast.getCurrentNode();
            if (ifStatement.getIfBranch() == null) {
                ifStatement.setIfBranch(block);
            } else {
                assert ifStatement.getElseBranch() == null;
                ifStatement.setElseBranch(block);
            }
        }

        ast.addInnerNode(block);
    }

    protected void processIfStatement(long id, AST ast, JSONObject object) throws Exception {
        if (ast.getCurrentNode() == null) {
            throw new Exception("Unexpected if statement item - no root");
        } else if (!(ast.getCurrentNode() instanceof ASTBlock)
                && !(ast.getCurrentNode() instanceof ASTForStatement)
                && !(ast.getCurrentNode() instanceof ASTIfStatement)
                && !(ast.getCurrentNode() instanceof ASTWhileStatement)
                && !(ast.getCurrentNode() instanceof ASTDoWhileStatement)) {
            throw new Exception("Unexpected if statement for parent item " + ast.getCurrentNode().getClass().getName());
        }
        ast.addInnerNode(new ASTIfStatement(id));
    }

    protected void processWhileStatement(long id, AST ast, JSONObject object) throws Exception {
        if (ast.getCurrentNode() == null) {
            throw new Exception("Unexpected while statement item - no root");
        } else if (!(ast.getCurrentNode() instanceof ASTBlock)
                && !(ast.getCurrentNode() instanceof ASTForStatement)
                && !(ast.getCurrentNode() instanceof ASTIfStatement)
                && !(ast.getCurrentNode() instanceof ASTWhileStatement)
                && !(ast.getCurrentNode() instanceof ASTDoWhileStatement)) {
            throw new Exception("Unexpected while statement for parent item" + ast.getCurrentNode().getClass().getName());
        }
        ast.addInnerNode(new ASTWhileStatement(id));
    }

    protected void processDoWhileStatement(long id, AST ast) throws Exception {
        if (ast.getCurrentNode() == null) {
            throw new Exception("Unexpected while statement item - no root");
        } else if (!(ast.getCurrentNode() instanceof ASTBlock)
                && !(ast.getCurrentNode() instanceof ASTForStatement)
                && !(ast.getCurrentNode() instanceof ASTIfStatement)
                && !(ast.getCurrentNode() instanceof ASTWhileStatement)
                && !(ast.getCurrentNode() instanceof ASTDoWhileStatement)) {
            throw new Exception("Unexpected while statement for parent item" + ast.getCurrentNode().getClass().getName());
        }
        ast.addInnerNode(new ASTDoWhileStatement(id));
    }

    protected void processForStatement(long id, AST ast, JSONObject attributes) throws Exception {
        if (ast.getCurrentNode() == null) {
            throw new Exception("Unexpected for statement item - no root");
        } else if (!(ast.getCurrentNode() instanceof ASTBlock)
                && !(ast.getCurrentNode() instanceof ASTForStatement)
                && !(ast.getCurrentNode() instanceof ASTIfStatement)
                && !(ast.getCurrentNode() instanceof ASTWhileStatement)
                && !(ast.getCurrentNode() instanceof ASTDoWhileStatement)) {
            throw new Exception("Unexpected for statement for parent item" + ast.getCurrentNode().getClass().getName());
        }

        // The attributes specify which components in for(x;y;z) *aren't* available
        boolean initPart = true, condPart = true, loopPart = true;
        if (attributes != null) {
            if (attributes.containsKey("initializationExpression")) initPart = false;
            if (attributes.containsKey("condition")) condPart = false;
            if (attributes.containsKey("loopExpression")) loopPart = false;
        }

        ast.addInnerNode(new ASTForStatement(id, initPart, condPart, loopPart));
    }

    protected void processEventDefinition(long id, AST ast, JSONObject attributes, String name) throws Exception {
        if (ast.getCurrentNode() == null || !(ast.getCurrentNode() instanceof ASTContractDefinition)) {
            throw new Exception("Unexpected EventDefinition item for parent item " + ast.getCurrentNode().getClass().getName() + ", name = " + ast.getCurrentNode().getNonNullName());
        }
        boolean isAnonymous = false;
        if (attributes.containsKey("anonymous")) {
            isAnonymous = (Boolean)attributes.get("anonymous");
        }
        ast.addInnerNode(new ASTEventDefinition(id, name, isAnonymous));
    }

    protected void processModifierDefinition(long id, AST ast, JSONObject object, String name) throws Exception {
        if (ast.getCurrentNode() == null || !(ast.getCurrentNode() instanceof ASTContractDefinition)) {
            throw new Exception("Unexpected ModifierDefinition item for parent item " + ast.getCurrentNode().getClass().getName() + ", name = " + ast.getCurrentNode().getNonNullName());
        }
        ast.addInnerNode(new ASTModifierDefinition(id, name));
    }

    protected void processBinaryOperation(long id, AST ast, JSONObject attributes) throws Exception {
        String operator = (String)attributes.get("operator");
        if (operator == null) {
            throw new Exception("Binary operator name not defined");
        }
        ast.addInnerNode(new ASTBinaryOperation(id, operator));
    }

    protected void processIdentifier(long id, AST ast, JSONObject attributes) throws Exception {
        String name = (String)attributes.get("value");
        if (name == null) {
            throw new Exception("Identifier name not defined for src  " + (String)attributes.get("src"));
        }
        long referencedDeclarationId = (Long)attributes.get("referencedDeclaration");
        ast.addInnerNode(new ASTIdentifier(id, name, referencedDeclarationId));
    }

    protected ArrayList<String> getOptionalArgumentNames(JSONObject attributes) throws Exception {
        ArrayList<String> argumentNames = null;

        if (attributes != null) { // this applies not just to struct construction:   isStructConstructorCall) {
            JSONArray namesArray = (JSONArray)attributes.get("names");
            for (int i = 0; i < namesArray.size(); ++i) {
                if (argumentNames == null) {
                    argumentNames = new ArrayList<String>();
                }
                if ((String)namesArray.get(i) == null) {
                    // A null "names" array entry for a struct constructor indicates that
                    // no struct member names - as in "S({a: 1, b: 2, c: 3})" are specified,
                    // but instead the default argument assignment is assumed as in "S(1,2,3)"
                    if (i != 0 || namesArray.size() != 1) {
                        throw new Exception("Unexpected struct constructor name attribute contents");
                    }
                    argumentNames = null;
                    break;
                }
                argumentNames.add((String)namesArray.get(i));
            }
        }

        return argumentNames;
    }

    protected void processFunctionCall(long id, AST ast, JSONObject attributes) throws Exception {
        Boolean isStructConstructorCall = (Boolean)attributes.get("isStructConstructorCall");
        ArrayList<String> argumentNames = getOptionalArgumentNames(attributes);
        ast.addInnerNode(new ASTFunctionCall(id, isStructConstructorCall, argumentNames));
    }

    protected void processExpressionStatement(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTExpressionStatement(id));
    }

    protected void processVariableDeclarationStatement(long id, AST ast, JSONObject attributes) throws Exception {
        JSONArray assignments = (JSONArray)attributes.get("assignments");
        ArrayList<Integer> assignmentIds = null;
        if (assignments != null) {
            assignmentIds = new ArrayList<Integer>();
            for (int i = 0; i < assignments.size(); ++i) {
                //if (assignments.get)
                if (assignments.get(i) == null) {
                    // This is an empty slots, e.g. the first part in "var (,x) = (123, 456);"
                    assignmentIds.add(-1);
                } else {
                    //int value = 0;
                    //Object object = (Object)assignments.get(i);

                    //if (object instanceof Integer) {
                    //    value = (Integer)object;
                    //} else if (value instanceof Long) {
                    //    value = (int)(Long)object;
                    //}
                    //assignments.add((Integer)assignments.get(i));

                    // ID typing and access are unclear - apparently sometimes int, sometimes long. Maybe
                    // we don't need IDs, so just mark this item as present (=1)
                    assignmentIds.add(1);
                }
            }
        } else System.out.println(" !!!! no assignments");
        ast.addInnerNode(new ASTVariableDeclarationStatement(id, assignmentIds));
    }

    protected void processVariableDeclaration(long id, AST ast, String name, JSONObject attributes) throws Exception {
        String type = (String)attributes.get("type");
        if (type == null) {
            throw new Exception("Variable declaration without type attribute");
        }
        String storageLocation = (String)attributes.get("storageLocation");
        String visibility = (String)attributes.get("visibility");
        boolean isConstant = (Boolean)attributes.get("constant");
        boolean isIndexed = false;
        if (attributes.containsKey("indexed")) {
            isIndexed = (Boolean)attributes.get("indexed");
        }
        ast.addInnerNode(new ASTVariableDeclaration(id, name, type, storageLocation, visibility, isConstant, isIndexed));
    }

    protected void processLiteral(long id, AST ast, JSONObject attributes) throws Exception {
        String value = (String)attributes.get("value");
        String type = (String)attributes.get("type");
        String token = (String)attributes.get("token");
        String subdenomination = (String)attributes.get("subdenomination");

        ASTLiteral.LiteralType literalType = null;

        if (value == null) { // Order matters: if this is a hexvalue, it can still be a "string" token
            value = (String) attributes.get("hexvalue");
            literalType = ASTLiteral.LiteralType.LITERAL_TYPE_HEXADECIMAL;
        } else if (token.equals("string" )) {
            literalType = ASTLiteral.LiteralType.LITERAL_TYPE_STRING;
            if (value == null) {
                throw new Exception("String literal without value");
            }
        } else if (token.equals("bool")) {
            literalType = ASTLiteral.LiteralType.LITERAL_TYPE_BOOL;
            if (value == null) {
                throw new Exception("Bool literal without value");
            }
        } else {
            // This should be an integer constant that may be decimal or hexadecimal (with 0x prefix) - TODO: others?
            if (value.startsWith("0x")) {
                literalType = ASTLiteral.LiteralType.LITERAL_TYPE_INTEGER_HEXADECIMAL;
            } else {
                literalType = ASTLiteral.LiteralType.LITERAL_TYPE_INTEGER_DECIMAL;
            }
            if (value == null /*|| type == null*/) { // type may not be defined for array sizes: "uint[3]"
                throw new Exception("Literal without value attribute");
            }
        }
        ast.addInnerNode(new ASTLiteral(id, type, value, literalType, subdenomination));
    }

    protected void processElementaryTypeName(long id, AST ast, String name) throws Exception {
        ast.addInnerNode(new ASTElementaryTypeName(id, name));
    }

    protected void processAssignment(long id, AST ast, JSONObject attributes) throws Exception {
        String operator = (String)attributes.get("operator");
        ast.addInnerNode(new ASTAssignment(id, operator));
    }

    protected void processModifierInvocation(long id, AST ast, JSONObject attributes) throws Exception {
        boolean argumentsSet = false;
        if (attributes != null &&
                attributes.get("arguments") != null) {
            argumentsSet = true;
        }
        ast.addInnerNode(new ASTModifierInvocation(id, argumentsSet));
    }

    protected void processElementaryTypeNameExpression(long id, AST ast, JSONObject attributes) throws Exception {
        String value = (String)attributes.get("value");
        if (value == null) {
            throw new Exception("Elementary type name expression without value attribute");
        }
        ast.addInnerNode(new ASTElementaryTypeNameExpression(id, value));
    }

    protected void processMapping(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTMapping(id));
    }

    protected void processIndexAccess(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTIndexAccess(id));
    }

    protected void processMemberAccess(long id, AST ast, JSONObject attributes) throws Exception {
        String memberName = (String)attributes.get("member_name");
        if (memberName == null) {
            throw new Exception("Member access has no member_name attribute");
        }
        Long referencedDeclarationId = (Long)attributes.get("referencedDeclaration");
        ast.addInnerNode(new ASTMemberAccess(id, memberName, referencedDeclarationId == null? 0: (long)referencedDeclarationId));
    }

    protected void processUserDefinedTypeName(long id, AST ast, String name) throws Exception {
        ast.addInnerNode(new ASTUserDefinedTypeName(id, name));
    }

    protected void processStructDefinition(long id, AST ast, JSONObject attributes, String name) throws Exception {
        String canonicalName = (String)attributes.get("canonicalName");
        ast.addInnerNode(new ASTStructDefinition(id, name, canonicalName));
    }

    protected void processInheritanceSpecifier(long id, AST ast, JSONObject attributes) throws Exception {
        boolean argumentsSet = false;
        if (attributes != null &&
                attributes.get("arguments") != null) {
            argumentsSet = true;
        }
        ast.addInnerNode(new ASTInheritanceSpecifier(id, argumentsSet));
    }

    protected void processConditional(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTConditional(id));
    }

    protected void processTupleExpression(long id, AST ast, JSONObject attributes) throws Exception {
        Object isInlineArray = attributes.get("isInlineArray");
        String tupleType = (String)attributes.get("type");
        JSONArray components = (JSONArray)attributes.get("components");
        ArrayList<Boolean> tupleComponents = null;
        ArrayList<ASTNode> replacementChildren = null;

        if (components != null) {
         //   System.out.println( " !!!!!!!!!!!!!!! " + tupleType + " HAS " + components.size() + " COMPONENTS ");

            // See comments in ASTTupleExpression on the ultra-awkward tuple handling.
            //
            // If the components attribute is set, it contains the JSON objects (identifiers
            // that would otherwise be contained in the children item. So we go over the
            // components and apply our parse tree construction functionfor each of them
            tupleComponents = new ArrayList<Boolean>();
            for (int i = 0; i < components.size(); ++i) {
                JSONObject currentComponent = (JSONObject)components.get(i);
                if (currentComponent == null) {
                    tupleComponents.add(false);
                } else {
                    tupleComponents.add(true);
                    AST tmpAST = new AST();
                    //processJSONObject(tmpAST, currentComponent, 0);
                    JSONObjectToAST(tmpAST, currentComponent, 0);
                    //System.out.println( ">>>>>>> sub root node = " + tmpAST.getRoot().toSolidityCode());
                    if (replacementChildren == null) {
                        replacementChildren = new ArrayList<ASTNode>();
                    }
                    replacementChildren.add(tmpAST.getRoot());
                }
            }
        }

        ASTNode tupleExpressionNode = new ASTTupleExpression(id, isInlineArray != null? (Boolean)isInlineArray: false, tupleType, tupleComponents);
        if (replacementChildren != null) {
            tupleExpressionNode.setChildren(replacementChildren);
            for (int i = 0; i < tupleComponents.size(); ++i) System.out.println("   " + i + " = " + tupleComponents.get(i));
        }
        ast.addInnerNode(tupleExpressionNode);
    }

    protected void processReturnStatement(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTReturnStatement(id));
    }

    protected void processBreakStatement(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTBreakStatement(id));
    }

    protected void processContinueStatement(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTContinueStatement(id));
    }

    protected void processArrayTypeName(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTArrayTypeName(id));
    }

    protected void processPragmaDirective(long id, AST ast, JSONObject attributes) throws Exception {
        JSONArray literals = (JSONArray)attributes.get("literals");
        if (literals == null) {
            throw new Exception("Pragma directive without literals attribute");
        }
        if (literals.size() < 1) {
            throw new Exception("Pragma directive with empty literals list");
        }
        String type = (String)literals.get(0);
        String args = null;
        if (literals.size() > 1) {
            args = "";
            for (int i = 1; i < literals.size(); ++i) {
                args += (String)literals.get(i);
            }
        }
        ast.addInnerNode(new ASTPragmaDirective(id, type, args));
    }

    protected void processThrow(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTThrow(id));
    }

    protected void processPlaceholderStatement(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTPlaceholderStatement(id));
    }

    protected void processEmitStatement(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTEmitStatement(id));
    }

    protected void processFunctionTypeName(long id, AST ast, JSONObject attributes) throws Exception {
        String visibility = (String)attributes.get("visibility");
        if (visibility == null) {
            throw new Exception("Function type name definition without visibility attribute");
        }
        ast.addInnerNode(new ASTFunctionTypeName(id, visibility));
    }

    protected void processNewExpression(long id, AST ast) throws Exception {
        ast.addInnerNode(new ASTNewExpression(id));
    }

    protected void processUsingForDirective(long id, AST ast, JSONObject attributes) throws Exception {
        boolean emptyTypeName = false;
        if (attributes != null && attributes.containsKey("typeName")) {
            // This should only ever be set to null, and indicates a wildcard?!
            //    using SafeMath for *;
            // Only SafeMath will then be a child node
            emptyTypeName = true;
            assert (String)attributes.get("typeName") == null;
        }
        ast.addInnerNode(new ASTUsingForDirective(id, emptyTypeName));
    }

    protected void processEnumDefinition(long id, AST ast, JSONObject attributes, String attrName) throws Exception {
        String canonicalName = (String)attributes.get("canonicalName");
        ast.addInnerNode(new ASTEnumDefinition(id, attrName, canonicalName));
    }

    protected void processEnumValue(long id, AST ast, String attrName) throws Exception {
        ast.addInnerNode(new ASTEnumValue(id, attrName));
    }

    protected void processUnaryOperation(long id, AST ast, JSONObject attributes) throws Exception {
        String operator = (String)attributes.get("operator");
        boolean isPrefix = (Boolean)attributes.get("prefix");
        ast.addInnerNode(new ASTUnaryOperation(id, operator, isPrefix));
    }

    protected void processInlineAssembly(long id, AST ast, JSONObject attributes) throws Exception {
        String operations = (String)attributes.get("operations");
        ast.addInnerNode(new ASTInlineAssembly(id, operations));
    }


    private byte [] codeArray = null;

    protected void processJSONObject(AST ast, JSONObject jsonObject, int depth) throws Exception {
        JSONObject attributes = (JSONObject)jsonObject.get("attributes");

        // Identify current item - name stored as part of the attributes
        String itemName = null;
        String attrName = null;
        int codeStart = 0;
        long id;
        String code = null;

        itemName = (String)jsonObject.get("name");
        id = (Long)jsonObject.get("id");
        if (attributes != null) {
            attrName = (String)attributes.get("name");
            // Extract corresponding source code for debugging
            if (Configuration.debugASTOutput) {
                String src = (String)jsonObject.get("src");
                if (codeArray != null && src != null) {
                    String[] parts = src.split(":");
                    codeStart = Integer.parseInt(parts[0]);
                    int codeLength = Integer.parseInt(parts[1]);
                    code = new String(codeArray, codeStart, codeLength, "UTF-8").replaceAll("\n", "\\\\n");
                }
            }
        }

        debugPrint(depth, "item " + (itemName == null? "?": itemName) + " = " + (attrName == null? "?": attrName)
                + " " + (code == null? "(no code)": ("              " + code)));

        if (ast == null) {
            // No further processing - we only wanted to print the item
            return;
        }

        // Process current item
        if (nameEquals(itemName, "PragmaDirective")) {
            processPragmaDirective(id, ast, attributes);
        } else if (nameEquals(itemName, "UsingForDirective")) {
            processUsingForDirective(id, ast, attributes);
        } else if (nameEquals(itemName, "SourceUnit")) {
            processSourceUnit(id, ast, attributes);
        } else if (nameEquals(itemName, "ContractDefinition")) {
            processContractDefinition(id, ast, attributes, attrName); //jsonObject, attrName);
        } else if (nameEquals(itemName, "FunctionDefinition")) {
            processFunctionDefinition(id, ast, attrName, attributes);
        } else if (nameEquals(itemName, "ParameterList")) {
            processParameterList(id, ast, jsonObject);
        } else if (nameEquals(itemName, "Block")) {
            processBlock(id, ast, jsonObject);
        } else if (nameEquals(itemName, "IfStatement")) {              // Start of control-structure-related items
            processIfStatement(id, ast, jsonObject);
        } else if (nameEquals(itemName, "ForStatement")) {
            processForStatement(id, ast, attributes);
        } else if (nameEquals(itemName, "WhileStatement")) {
            processWhileStatement(id, ast, jsonObject);
        } else if (nameEquals(itemName, "DoWhileStatement")) {
            processDoWhileStatement(id, ast);
        } else if (nameEquals(itemName, "Return")) {
            processReturnStatement(id, ast);
        } else if (nameEquals(itemName, "Break")) {
            processBreakStatement(id, ast);
        } else if (nameEquals(itemName, "Continue")) {
            processContinueStatement(id, ast);
        } else if (nameEquals(itemName, "Throw")) {
            processThrow(id, ast);
        } else if (nameEquals(itemName, "InlineAssembly")) {
            processInlineAssembly(id, ast, attributes);
        } else if (nameEquals(itemName, "EmitStatement")) {
            processEmitStatement(id, ast);
        } else if (nameEquals(itemName, "ExpressionStatement")) {      // Start of expression-related items
            processExpressionStatement(id, ast);
        } else if (nameEquals(itemName, "BinaryOperation")) {
            processBinaryOperation(id, ast, attributes);
        } else if (nameEquals(itemName, "UnaryOperation")) {
            processUnaryOperation(id, ast, attributes);
        } else if (nameEquals(itemName, "TupleExpression")) {
            processTupleExpression(id, ast, attributes);
        } else if (nameEquals(itemName, "Conditional")) {
            processConditional(id, ast);
        } else if (nameEquals(itemName, "Identifier")) {
            processIdentifier(id, ast, attributes);
        } else if (nameEquals(itemName, "Literal")) {
            processLiteral(id, ast, attributes);
        } else if (nameEquals(itemName, "Assignment")) {
            processAssignment(id, ast, attributes);
        } else if (nameEquals(itemName, "FunctionCall")) {
            processFunctionCall(id, ast, attributes);
        } else if (nameEquals(itemName, "MemberAccess")) {
            processMemberAccess(id, ast, attributes);
        } else if (nameEquals(itemName, "NewExpression")) {
            processNewExpression(id, ast);
        } else if (nameEquals(itemName, "PlaceholderStatement")) {
            processPlaceholderStatement(id, ast);
        } else if (nameEquals(itemName, "ElementaryTypeNameExpression")) {
            processElementaryTypeNameExpression(id, ast, attributes);
        } else if (nameEquals(itemName, "ModifierInvocation")) {
            processModifierInvocation(id, ast, attributes);
        } else if (nameEquals(itemName, "InheritanceSpecifier")) {
            processInheritanceSpecifier(id, ast, attributes);
        } else if (nameEquals(itemName, "EventDefinition")) {       // Start of type-related items
            processEventDefinition(id, ast, attributes, attrName);
        } else if (nameEquals(itemName, "StructDefinition")) {
            processStructDefinition(id, ast, attributes, attrName);
        } else if (nameEquals(itemName, "EnumDefinition")) {
            processEnumDefinition(id, ast, attributes, attrName);
        } else if (nameEquals(itemName, "EnumValue")) {
            processEnumValue(id, ast, attrName);
        } else if (nameEquals(itemName, "VariableDeclarationStatement")) {
            processVariableDeclarationStatement(id, ast, attributes);
        } else if (nameEquals(itemName, "VariableDeclaration")) {
            processVariableDeclaration(id, ast, attrName, attributes);
        } else if (nameEquals(itemName, "ElementaryTypeName")) {
            processElementaryTypeName(id, ast, attrName);
        } else if (nameEquals(itemName, "Mapping")) {
            processMapping(id, ast);
        } else if (nameEquals(itemName, "IndexAccess")) {
            processIndexAccess(id, ast);
        } else if (nameEquals(itemName, "ArrayTypeName")) {
            processArrayTypeName(id, ast);
        } else if (nameEquals(itemName, "UserDefinedTypeName")) {
            processUserDefinedTypeName(id, ast, attrName);
        } else if (nameEquals(itemName, "ModifierDefinition")) {
            processModifierDefinition(id, ast, jsonObject, attrName);
        } else if (nameEquals(itemName, "FunctionTypeName")) {
            processFunctionTypeName(id, ast, attributes);
        } else {
            throw new Exception("Unknown JSON object: " + (itemName == null? "no name": itemName));
        }
    }

    protected void JSONObjectToAST(AST ast, JSONObject jsonObject, int depth) throws Exception {
        descendJSONObject(ast, jsonObject, depth);
    }
    protected void printJSONObject(JSONObject jsonObject, int depth) throws Exception {
        descendJSONObject(null, jsonObject, depth);
    }

    protected void descendJSONObject(AST ast, JSONObject jsonObject, int depth) throws Exception {
        ASTNode firstCurrentNode = ast != null? ast.getCurrentNode(): null;

        // Process item
        processJSONObject(ast, jsonObject, depth);
        ASTNode secondCurrentNode = ast != null? ast.getCurrentNode(): null;


        // Process child nodes
        JSONArray children = (JSONArray)jsonObject.get("children");
        if (children == null) {
            debugPrint(depth, "no children");
        } else {
            for (int i = 0; i < children.size(); ++i) {
                JSONObject childObject = (JSONObject)children.get(i);
                JSONObjectToAST(ast, childObject, depth+1);
            }
        }

        if (ast != null) {
            // If child nodes processing properly cleaned up after itself, all children have
            // been completed
            assert secondCurrentNode == ast.getCurrentNode();

            // Complete item if necessary
            if (firstCurrentNode != secondCurrentNode) {
                // This item indeed introduced a new node that can now be completed
                ast.completeInnerNode(); // TODO what about leaves?
            }
        }
    }




    public AST parse(InputStream input) throws Exception {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject)jsonParser.parse(
                new InputStreamReader(input, "UTF-8"));

        AST ast = new AST();

        if (!Configuration.skipASTProcessing) {
            // Process JSON object to build AST
            JSONObjectToAST(ast, jsonObject, 0);
        } else {
            // For debugging: only print, don't interpret
            printJSONObject(jsonObject, 0);
        }
        return ast;
    }
}
