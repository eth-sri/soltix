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

import java.util.HashMap;

abstract public class FunctionScope extends ASTNode implements Comparable {
    protected ASTParameterList parameters = null;
    protected ASTParameterList returnList = null;
    protected HashMap<String, ASTVariableDeclaration> variableDeclarations = null;
    private int bodyNodeCount = 0;
    protected ASTBlock body = null;

    public FunctionScope(long id, String name) throws Exception {
        super(id, name);
    }

    public int getReturnValuesCount() { return returnList == null? 0: returnList.getChildCount(); }
    public ASTParameterList getReturnList() { return returnList; }
    public ASTParameterList getParameterList() { return parameters; }
    public int getParameterCount() { return parameters == null? 0: parameters.getChildCount(); }
    public void setBody(ASTBlock body) { this.body = body; }
    public ASTBlock getBody() { return body; }

    public void addVariable(ASTVariableDeclaration variable) {
        if (variableDeclarations == null) {
            variableDeclarations = new HashMap<String, ASTVariableDeclaration>();
        }
        variableDeclarations.put(variable.getName(), variable);
    }

    public int getLocalDeclarationsCount() { return variableDeclarations == null? 0: variableDeclarations.size(); }

    public boolean containsVariable(String name) {
        return variableDeclarations != null && variableDeclarations.containsKey(name);
    }

    public ASTNode currentFunctionVariableLookup(String name) {
        if (variableDeclarations == null) {
            return null;
        }
        return variableDeclarations.get(name);
    }

    public void countNode(ASTNode node) {
        ++bodyNodeCount; /* count all for now */
    }
    public int getBodyNodeCount() { return bodyNodeCount; }
    public int compareTo(Object other) {
        FunctionScope otherFunction = (FunctionScope) other;
        if (bodyNodeCount > otherFunction.bodyNodeCount) {
            return 1;
        } else if (bodyNodeCount == otherFunction.bodyNodeCount) {
            return 0;
        } else {
            return -1;
        }
    }


}
