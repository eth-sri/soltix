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
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.synthesis.ExpressionGenerator;
import soltix.synthesis.ValueGenerator;
import soltix.util.RandomNumbers;

public class ASTIndexAccess extends ASTNode {
    private ASTNode accessedItem = null;
    private ASTNode index = null;
    private ASTStructDefinition structAccess = null;
    private String cachedSolidityCode = null;
    private ValueGenerator valueGenerator = null;

    public ASTIndexAccess(long id) throws Exception {
        super(id);
    }

    public ASTStructDefinition getStructAccess() { return structAccess; }
    public ASTNode getAccessedItem() { return accessedItem; }
    public ASTNode getIndex() { return index; }

    protected String baseSolidityCode() throws Exception {
        // Basic subscript access to the item
        return accessedItem.toSolidityCode() + "[" + index.toSolidityCode() + "]";
    }

    @Override
    public String toSolidityCode() throws Exception {
        if (cachedSolidityCode != null) {
            return cachedSolidityCode;
        }

        String result;

        if (Configuration.injectArraySubscriptChecks) {
            if (valueGenerator == null) {
                valueGenerator = new ValueGenerator(new RandomNumbers(Configuration.randomNumbersSeed));
            }
            // Don't allow array subscripts that access non-existent array elements.
            long referencedDeclarationId = accessedItem.getReferencedDeclarationId();
            ASTVariableDeclaration declaration = ast.getVariableById(referencedDeclarationId);


            if (declaration == null) {
                result = //"/*checked6*/"+
                         baseSolidityCode();
            } else if (declaration.getTypeName() instanceof ASTMapping) {
                // Mapping
                result = //"/*checked5*/"+
                         baseSolidityCode();
            } else if (getParent() instanceof ASTUnaryOperation
                    && ((ASTUnaryOperation)getParent()).getOperator() == ASTUnaryOperation.Operator.OP_DELETE) {
                // Operand must be lvalue
                result = //"/*checked4*/"+
                         baseSolidityCode();
            } else {
                // Array
                // TODO fix 00b2a833f02b5ea33fb8af7af2e39ddf2eebacb53490c65a990e770560fbcca1 lvalue issue
                if (!(declaration.getTypeName() instanceof ASTArrayTypeName)) { // TODO Fix this
                    result = //"/*checked3*/"+
                            baseSolidityCode();
                }

                ASTArrayTypeName arrayType = null;

                boolean isBytesType = Type.isByteType(declaration.getTypeName());
                if (!isBytesType) {
                    // Must be an array
                    arrayType = (ASTArrayTypeName)declaration.getTypeName();
                }

                if (arrayType != null && !ExpressionGenerator.isUsableType(ast, arrayType.getBaseType()) /*arrayType.getBaseType() instanceof ASTUserDefinedTypeName
                        && ast.getContract(ar)*/) { // TODO fix this
                        //&& ast.getContract(arrayType.getBaseType().getName()) != null) {
                    result = //"/*checked2*/"+
                                baseSolidityCode();
                } else if (getParent() instanceof ASTAssignment && this == ((ASTAssignment)getParent()).getLHS()) {
                    // Array or bytes element assignment:
                    //     x[y] = value;
                    // Transform to:
                    //     if (x.length > y) x[y] = value;
                    //     ^^^^^^^^^^^^^^^^^^^^^^ this.toSolidityCode()
                    result = "if (" + accessedItem.toSolidityCode() + ".length > (" + index.toSolidityCode() + ")) " // check:   if (x.length > y)
                            + baseSolidityCode();                                                                  // access:  x[y]
                } else {
                 //   return accessedItem.toSolidityCode() + "[" + index.toSolidityCode() + "]";
                    // Array or bytes element read access, e.g.:
                    //     value = x[y];
                    // Transform to:
                    //     value = (x.length > y? x[y]: randomValue);
                    //             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ this.toSolidityCode()
                    ASTStructDefinition structDefinition = null;
                    if (arrayType != null && arrayType.getBaseType() instanceof ASTUserDefinedTypeName) {
                        structDefinition = ast.getStructDefinition(arrayType.getBaseType().getName());
                    }

                    //./run-one-test.sh /home/nweller/securify-contracts/018bb35f4994699bc72790fbf0df1b3ffcb542626c4c38762c03c69e00fcc1d8/contract.sol

                    // Generate first part:
                    //    (x.length > y? x[y]:
                    result = //"/*checked7-parent "  +getParent().getClass().getName() +  "*/" +
                            "(" + accessedItem.toSolidityCode() + ".length > (" + index.toSolidityCode() + ")? "
                            + accessedItem.toSolidityCode() + "[" + index.toSolidityCode() + "]: ";

                    if (structDefinition != null) {
                        // We cannot generate a value in-place for structs, since this provokes memory/storage type
                        // incompatibility errors. Instead, a pre-defined storage struct variable in the contract can
                        // be used as rhs operand
                        result += "_internalStructValue" + structDefinition.getName();
                        structAccess = structDefinition;
                    } else {
                        ASTNode elementType;
                        if (isBytesType) {
                            elementType = TypeContainer.getByteType(1);
                        } else {
                            elementType = arrayType.getBaseType();
                        }

                        // Generate in-place value
                        result += valueGenerator.generateRandomValue(ast, elementType, ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL).toASTNode(false/*TODO*/).toSolidityCode();
                    }
                    result += ")";
                }
            }
        } else {
            result = //"/*checked1*/"+
                     baseSolidityCode();
        }
        cachedSolidityCode = result;
        return result;
    }

    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 2) {
            throw new Exception("Unexpected index access child count " + getChildCount() + " = " + childrenToText());
        }
        // TODO: type-checking?
        accessedItem = getChild(0);
        index = getChild(1);
        setChildren(null);
    }
}
