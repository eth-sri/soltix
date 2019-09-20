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

import java.util.HashMap;

public class ASTAssignment extends ASTNode {
    private Operator operator;
    private ASTNode lhs;
    private ASTNode rhs;
    private boolean outputIsBlock = false;

    public ASTAssignment(long id, String operatorName) throws Exception {
        super(id);
        if ((operator = operators.get(operatorName)) == null) {
            throw new Exception("Unknown assignment operator " + operatorName);
        }
    }

    public Operator getOperator() { return operator; }
    public ASTNode getLHS() { return lhs; }
    public ASTNode getRHS() { return rhs; }
    public boolean getOutputIsBlock() { return outputIsBlock; }

    // TODO The distiction between ordinary and compound assignments is probably ignored
    // pretty much everywhere now (i.e. assumes ASTAssignment = "="). Once compound 
    // assignments are supported, this needs to be fixed
    public enum Operator {
        OP_ASSIGN("="),
        OP_ASSIGN_COMP_PLUS("+="),
        OP_ASSIGN_COMP_MINUS("-="),
        OP_ASSIGN_COMP_MUL("*="),
        OP_ASSIGN_COMP_DIV("/="),
        OP_ASSIGN_COMP_MOD("%="),
        OP_ASSIGN_COMP_BOR("|="),
        OP_ASSIGN_COMP_BAND("&="),
        OP_ASSIGN_COMP_BXOR("^="),
        OP_ASSIGN_COMP_SHL("<<="),
        OP_ASSIGN_COMP_SHR(">>=");

        private String name;

        Operator(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final HashMap<String, ASTAssignment.Operator> operators;
    static {
        operators = new HashMap<String, ASTAssignment.Operator>();
        for (ASTAssignment.Operator op : ASTAssignment.Operator.values()) {
            operators.put(op.toString(), op);
        }
    }

    protected String createMappingEventName(ASTNode lhs, String initialName) throws Exception {
        if (lhs instanceof ASTMemberAccess) {
            ASTMemberAccess memberAccess = (ASTMemberAccess)lhs;
            return createMappingEventName(memberAccess.getAppliedTo(), "") + "_" + memberAccess.getName(); //initialName;
        } else if (lhs instanceof ASTIdentifier) {
            return lhs.toSolidityCode();
        } else {
            throw new Exception("createMappingEventName for unsupported type " + lhs.getClass().getName());
        }
    }

    @Override
    public String toSolidityCode() throws Exception {
        String assignmentCode = lhs.toSolidityCode() + " " + operator.toString() + " " + rhs.toSolidityCode();

        if (Configuration.eventsReplaceStorageLog && Configuration.insertLCMSupportCode) {
            // Catch and instrument mapping assignments:     m[idx] = value;
            if (lhs instanceof ASTIndexAccess
                && Configuration.useABIEncoderV2) { // TODO Only disable it if we're assigning a struct (which requires encoder V2)
                ASTIndexAccess indexAccess = (ASTIndexAccess)lhs;
                ASTNode accessedItem = indexAccess.getAccessedItem();

                String indexExpressionListString = indexAccess.getIndex().toSolidityCode();

                // For multiple indices, skip to the top one
                while (accessedItem instanceof ASTIndexAccess) {
                    indexAccess = (ASTIndexAccess)accessedItem;
                    indexExpressionListString = indexAccess.getIndex().toSolidityCode() + ", " + indexExpressionListString;
                    accessedItem = indexAccess.getAccessedItem();
                }

                long referencedDeclarationId = accessedItem.getReferencedDeclarationId();
                ASTVariableDeclaration declaration = ast.getVariableById(referencedDeclarationId);

                // We suppress struct values containing mappings. These are unsupported recursive types, which would
                // require field-wise emission.
                // TODO support this here and in FunctionGenerator (see isRecursiveMappingalueType() calls)
                if (declaration != null && declaration.getTypeName() instanceof ASTMapping
                        && !Type.isRecursiveMappingValueType(ast, ((ASTMapping)declaration.getTypeName()).getDestinationType())) {
                    //System.out.println("lhs type " + indexAccess.getAccessedItem().getClass().getName() + " in " + lhs.toSolidityCode());

                    // The above isRecursiveMappingValueType() does not catch constellations like
                    //   m1[idx].m2[idx] = val;
                    // where m1 and m2 are mappings, and m1's value is a struct type. As a workaround we
                    // just catch the exception generated by createMappingEventName() and give up on the
                    // instrumentation.
                    // TODO proper support for mixed struct/mapping types
                    try {
                        String name;
                        name = createMappingEventName(indexAccess.getAccessedItem(), declaration.getName());
                        String emitCode = "emit finalResult_Mapping_" + name + "(" + indexExpressionListString /*indexAccess.getIndex().toSolidityCode()*/ + ", " + rhs.toSolidityCode() + ");";
                        assignmentCode = "{ " + emitCode + " " + assignmentCode + "; }";


                        // This is currently unused and wrong (__annotate() sometimes gets embedded in otherwise empty
                        // blocks, which breaks any obvious association with the next AST node)
                        //assignmentCode = annotateSolidityCode(assignmentCode, "mapping_assignment_block");
                        outputIsBlock = true;
                    } catch (Exception e) {
                        ;
                    }
                }
            }
        }

        return assignmentCode;
    }
    
    @Override
    public String toSolidityCodePostfix() { return null; }

    @Override
    public void finalize() throws Exception {
        if (getChildCount() != 2) {
            throw new Exception("Assignment with unexpected operand count " + getChildCount() + " = " + childrenToText());
        }
        lhs = getChild(0);
        rhs = getChild(1);
        removeChildNode(1);
        removeChildNode(0);
    }
}
