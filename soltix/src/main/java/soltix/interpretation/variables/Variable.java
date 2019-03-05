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
package soltix.interpretation.variables;

import soltix.ast.ASTNode;
import soltix.ast.ASTVariableDeclaration;
import soltix.ast.ASTVerbatimText;
import soltix.interpretation.values.Value;

import java.util.ArrayList;

/**
 * Class to represent variables
 */
public class Variable extends Value { // A variable also contains a value, just not a constant one
    private ASTVariableDeclaration declaration;
    private ASTVariableDeclaration shadowDeclaration;

    public Variable(ASTVariableDeclaration declaration) {
        //super(declaration.getTypeName());
        this.declaration = declaration;
        shadowDeclaration = null;
    }

    public String getName() { return declaration.getName(); }
    public ASTVariableDeclaration getDeclaration() { return declaration; }

    public ASTVariableDeclaration getShadowNode() { return shadowDeclaration; }
    public void setShadowNode(ASTVariableDeclaration shadowDeclaration) { this.shadowDeclaration = shadowDeclaration; }

    @Override
    public ASTNode getType() { return declaration.getTypeName(); }

    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        return new ASTVerbatimText(0, declaration.getName());
    }

    @Override
    public Object toJSONRepresentation() throws Exception {
        throw new Exception("Variable.toJSONRepresentation is undefined and should not have been called");
    }
}
