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

import soltix.profiling.ProfilingEvent;
import soltix.interpretation.variables.VariableEnvironment;

import java.util.ArrayList;

abstract public class ASTNode {
    protected AST ast = null;
    protected ASTNode parent;
    private ArrayList<ASTNode> children;
    private int depth;
    private long id;
    private long internalID;
    protected String name;
    static private long currentNodeCounter = 1;
    // Coverage info (if profiling log loaded)
    private boolean covered = false;
    private int coveredCount = 0; // TODO clean up redundancy

    // VariableEnvironment to store results from either:
    //    - Instrumentation (i.e. a log is read to populate this with potentially many values if loops are involved)
    //    - Interpretation (currently limited to single values)
    private VariableEnvironment variableEnvironment = null;


    // If this is an item that may reference a variable (e.g. an identifier or member access operator), this is its ID
    protected long referencedDeclarationId = 0;

    // Solidity transformed output code properties
    private int outputCodeLineNumber = 0;
    private int outputCodeLinePosition = 0;
    private int outputCodeLength = 0;

    public void setOutputCodeLineNumber(int lineNumber) { outputCodeLineNumber = lineNumber; }
    public void setOutputCodeLinePosition(int positionInLine) { outputCodeLinePosition = positionInLine; }
    public void setOutputCodeLength(int outputLength) { outputCodeLength = outputLength; }
    public int getOutputCodeLineNumber() { return outputCodeLineNumber; }
    public int getOutputCodeLinePosition() { return outputCodeLinePosition; }
    public int getOutputCodeLength() { return outputCodeLength; }

    public long getReferencedDeclarationId() { return referencedDeclarationId; }

    public ASTNode(long id) throws Exception {
        setParent(null);
        children = new ArrayList<ASTNode>();

        internalID = currentNodeCounter++;

        final int maximumNodeCount = 1000000;
        if (id == 0) {
            // This is a generated node - we have to assign some ID without clashing with an existing
            // solc ID. While extra nodes are probably only ever generated after the full AST has been
            // constructed - so we could count up from the maximum encountered ID -, we simply use a
            // large value instead for now
            this.id = internalID + maximumNodeCount;
        } else {
            if (id >= maximumNodeCount) {
                throw new Exception("Encountered AST node too large: " + id);
            }
            this.id = id;
        }
    }

    public ASTNode(long id, String name) throws Exception {
        this(id);
        setName(name);
    }

    public void setAST(AST ast) { this.ast = ast; }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public String getNonNullName() { return name == null? "?": getName(); }
    public void setCovered(boolean value) {
        covered = value;
        if (value) {
            ++coveredCount;
        } else {
            coveredCount = 0;
        }
    } // TODO cleanup

    public boolean getCovered() { return covered; }
    public int getCoveredCount() { return coveredCount; } // potentially useful for heat maps

    //public void setProfilingEvents(ArrayList<ProfilingEvent> profilingEvents) { this.profilingEvents = profilingEvents; }
    public void setVariableEnvironment(VariableEnvironment variableEnvironment) { this.variableEnvironment = variableEnvironment; }
    public VariableEnvironment getVariableEnvironment() { return variableEnvironment; }

    //public void setID(int id) { this.id = id; }
    public long getID() { return id; }

    public void setParent(ASTNode parent) {
        this.parent = parent;
    }
    public ASTNode getParent() {
        return parent;
    }

    public void addChildNode(ASTNode node) throws Exception {
        if (node == null) throw new Exception("Attempt to add null child node");
        node.setParent(this);
        children.add(node);
    }
    public void addSharedChildNode(ASTNode node) throws Exception { // don't disrupt existing parent relationship
        if (node == null) throw new Exception("Attempt to add null child node");
        children.add(node);
    }
    public boolean containsChildNode(ASTNode node) {
        return children.contains(node);
    }
    public void insertChildNode(int index, ASTNode node) {
        node.setParent(this);
        children.add(index, node);
    }
    public void removeChildNode(int index) {
        children.remove(index);
    }

    public int getChildCount() {
        // "children" can become null if a finalize() method processes the children and
        // removes them to avoid having them processed later
        if (children == null) {
            return 0;
        }
        return children.size();
    }

    protected String annotateSolidityCode(String code, String annotation) {
        return ASTFunctionDefinition.SpecialFunction.SPECIAL_FUNCTION_ANNOTATE.getName() + "(\"" + annotation + "\"); " + code;
    }

    public ASTNode getChild(int index) {
        return children.get(index);
    }
    public ArrayList<ASTNode> getChildren() { return children; }
    public void setChildren(ArrayList<ASTNode> value) { children = value; }
    public void setChild(int index, ASTNode node) { children.set(index, node); }

    public void setDepth(int depth) { this.depth = depth; }
    public int getDepth() { return depth; }

    public boolean isTypeName() {
        //TODO: most of the additions below and in ASTFunctionCall make more test cases pass parsing, but the
        //       generated function calls probably do not use all relevant fields
        return     this instanceof ASTElementaryTypeName
                || this instanceof ASTElementaryTypeNameExpression
                || this instanceof ASTArrayTypeName
                || this instanceof ASTMapping
                || this instanceof ASTUserDefinedTypeName;
    }

    public String childrenToText() {
        String text ="[";
        for (int i = 0; i < getChildCount(); ++i) {
            if (i > 0) {
                text += " ";
            }
            text += getChild(i).getClass().getName();
        }
        text += "]";
        return text;
    }

    // Method to be called when this node and all child nodes have been read. Nodes that override
    // it can process child nodes to check them and extract relevant information
    public void finalize() throws Exception { /* OK */ }

    // Get first part of Solidity code representing this node, e.g.
    // the opening brace for a block node ("{ ... }")
    abstract public String toSolidityCode() throws Exception;
    public String toSolidityCodeNonNull() throws Exception {
        String code = toSolidityCode();
        return code != null? code: getClass().getName();
    }

    // Get second part of Solidity code representing this node, if any (null otherwise),
    // e.g. the closing brace for a block node
    abstract public String toSolidityCodePostfix() throws Exception;
}
