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

import soltix.Configuration;
import soltix.ast.*;
import soltix.interpretation.Type;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.expressions.Expression;
import soltix.interpretation.expressions.ExpressionEvaluator;
import soltix.interpretation.values.IntegerValue;
import soltix.interpretation.values.ValueContainer;
import soltix.interpretation.variables.VariableEnvironment;
import soltix.util.RandomNumbers;

import java.util.ArrayList;

import static soltix.ast.ASTBinaryOperation.Operator.OP_EQ;
import static soltix.ast.ASTBinaryOperation.Operator.OP_MOD;
import static soltix.synthesis.ValueGenerator.IntegerGenerationPolicy.INTEGER_GENERATION_POLICY_FAVOR_SMALL;

public class TypeConverter {
    private AST ast;
    private RandomNumbers prng;
    private ExpressionEvaluator expressionEvaluator;
    private ValueGenerator valueGenerator;

    public TypeConverter(AST ast, RandomNumbers prng, ExpressionEvaluator expressionEvaluator) {
        this.ast = ast;
        this.prng = prng;
        this.expressionEvaluator = expressionEvaluator;
        valueGenerator = new ValueGenerator(prng);
    }

    // TODO Handle tuples? Must be done everywhere else too
    public Expression convertToCompatibleType(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        ASTNode currentType = expressionToConvert.getType();

        if (currentType instanceof ASTElementaryTypeName) {
            ASTElementaryTypeName elementaryTypeName = (ASTElementaryTypeName) currentType;
            switch (elementaryTypeName.getElementaryType()) {
                case ELEMENTARY_TYPE_INT:
                    return convertFromInt(environment, expressionToConvert, desiredType);
                case ELEMENTARY_TYPE_UINT:
                    return convertFromUint(environment, expressionToConvert, desiredType);
                case ELEMENTARY_TYPE_BOOL:
                    return convertFromBool(environment, expressionToConvert, desiredType);
                case ELEMENTARY_TYPE_BYTE:
                    return convertFromByte(environment, expressionToConvert, desiredType);
                case ELEMENTARY_TYPE_ADDRESS:
                    return convertFromAddress(environment, expressionToConvert, desiredType);
                case ELEMENTARY_TYPE_STRING:
                    return convertFromString(environment, expressionToConvert, desiredType);
                default:
                    throw new Exception("ExpressionGenerator.convertToCompatibleType: Unsupported elementary type " + elementaryTypeName.getElementaryType());
            }
        } else if (currentType instanceof ASTArrayTypeName) {
            return convertFromArray(environment, expressionToConvert, desiredType);
        } else if (currentType instanceof ASTUserDefinedTypeName) {
            ASTStructDefinition structDefinition = ast.getStructDefinition(currentType.getName());
            if (structDefinition != null) {
                return convertFromStruct(environment, expressionToConvert, structDefinition, desiredType);
            } else {
                ASTEnumDefinition enumDefinition = ast.getEnumDefinition(currentType.getName());
                return convertFromEnum(environment, expressionToConvert, enumDefinition, desiredType);
            }
            //throw new Exception("convertToCompatibleType for unsupported current type "
            //        + currentType.toSolidityCode() + " to desired type " + desiredType.toSolidityCode());
        } else {
            throw new Exception("convertToCompatibleType for unsupported current type "
                    + currentType.toSolidityCode() + ", " + currentType.getClass().getName());
        }
    }

    protected Expression convertComparableExpressionUsingConditionalOperator(Expression expressionToConvert,
                                                                             ASTNode desiredType) throws Exception {
        Expression conditionComparisonValue = new Expression(valueGenerator.generateRandomValue(ast, expressionToConvert.getType(),
                INTEGER_GENERATION_POLICY_FAVOR_SMALL));

        Expression condition = new Expression(expressionToConvert, OP_EQ, conditionComparisonValue);
        Expression trueBranch = new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));
        Expression falseBranch = new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));

        return new Expression(condition, trueBranch, falseBranch);
    }

    protected Expression genericConvertFrom(VariableEnvironment environment,
                                            Expression expressionToConvert,
                                            ASTNode desiredType) throws Exception {
        if (Type.isSameType(ast, expressionToConvert.getType(), desiredType)) {
            // Nothing to change
            return expressionToConvert;
        }

        if (Type.isStructType(ast, desiredType)) {
            // The below doesn't work, since we're working at the expression rather than value level at this point,
            // and embedding things in struct literals doesn't mixes both.
            // TODO fix this
            // For now we discard the input item as before, since the only context where retaining expressions is
            // mandatory is in return function call generation

            /* The below is probably better (less restrictively) handled by debugIntermediateValuesString() after
               generating a return function - which should in that case always be enabled

            if (Configuration.allowStructsInFunctionABI) {
                throw new Exception("Discarding struct item -");
            }*/

            //return new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));

            // Another attempt, use condop again, original expression vs. randomly generated as condition - ensuring
            // evaluation:
            //    x == generate(T_x)? generate(T_Y): generate(T_Y)

            /*
            Expression conditionComparisonValue = new Expression(valueGenerator.generateRandomValue(ast, expressionToConvert.getType(),
                                                            INTEGER_GENERATION_POLICY_FAVOR_SMALL));
            Expression condition = new Expression(expressionToConvert, OP_EQ, conditionComparisonValue);
            Expression trueBranch = new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));
            Expression falseBranch = new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));

            return new Expression(condition, trueBranch, falseBranch);
*/
            // Convert
            return convertComparableExpressionUsingConditionalOperator(expressionToConvert, desiredType);

            //throw new Exception("botched");

            // Target type is structure - we solve this by building a struct literal and embedding the old
            // value in place of one member (with a suitable conversion to its type).
            // Take the first one for now, since unfortunately accessRandomStructMember() is not easily
            // applicable in this context.
            /*StructValue structValue = (StructValue)valueGenerator.generateDefaultUDTValue(ast, (ASTUserDefinedTypeName)desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL);
            // TODO arrays
            StructValue tempStructValue = structValue;
            while (Type.isStructType(ast, tempStructValue.getMemberValueList().getDeclarations().get(0).getTypeName())) {
                tempStructValue = (StructValue)tempStructValue.getMemberValueList().getValues().get(0);
            }
            tempStructValue.updateMemberValue();*/
        }

        System.out.println(" ####### CONV " + expressionToConvert.getType().toSolidityCode() + " TO " + desiredType.toSolidityCode());
        if (true) throw new Exception("Invalid genericConvertFrom call - obsolete. From " +
                    expressionToConvert.getType().toSolidityCode() + " to " + desiredType.toSolidityCode());

        // As a generic catch-all conversion approach to convert an expression of type T1 to a specific type T2, we
        // can use the conditional operator to select an expression, such that (with casts added for clarity):
        //      (T1)e
        // becomes
        //      ((T1)e <op> (T1)value?  (T2)newValue1:  (T2)newValue2)
        // For now, we simply use the tautology generator to create an always-true expression, thereby selecting the
        // first operand of the conditional operator.
        //
        // TODO 1: Actually do what's described above. See PredicateGenerator.synthesizeRelationalPredicateOneExpression
        // TODO 2: Apply cast operations where applicable as described by
        //    http://solidity.readthedocs.io/en/v0.4.24/types.html#conversions-between-elementary-types
        // TODO 3: Another arbitrary type conversion is enabled by function calls (argument -> return)
        // TODO Choose one of the options above with weighted probability for each conversion
        return new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));
    }

    protected Expression integerToBool(Expression expressionToConvert, ASTNode desiredType) throws Exception {
        // Return true if the lowest bit is set, false otherwise
        Expression conditionFirstTerm = new Expression( // expr % 2
                expressionToConvert,
                OP_MOD,
                new Expression(ValueContainer.getSmallIntegerValue(expressionToConvert.getType(), 2))
        );
        Expression condition = new Expression(conditionFirstTerm, // (expr % 2) == 1
                OP_EQ,
                new Expression(ValueContainer.getSmallIntegerValue(expressionToConvert.getType(), 1))
        );

        return new Expression(condition, new Expression(ValueContainer.getBoolValue(true)), new Expression(ValueContainer.getBoolValue(false)));
    }

    protected Expression convertFromIntOrUint(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        
        // TODO Determine type conversion rules for integer types with different sizes and sign here and for ExpressionEvaluator

        // int to bytes, int to uint can be handled with a cast
        if (Type.isIntegerType(desiredType)) {
            return new Expression(expressionToConvert, desiredType);
	} else if (Type.isByteType(desiredType)) {
            // Starting with 0.5.x, a direct conversion pathway between integer and bytes types is apparently only
            // available for the maximum-sized types, such as int256 to bytes32 and vice-versa.
            // For now, we use intermediate conversions to these types where needed. 
	    ASTElementaryTypeName sourceType = (ASTElementaryTypeName)expressionToConvert.getType();
	    if (Configuration.languageVersionMinor >= 5 && sourceType.getBits() < 256) {
                ASTElementaryTypeName tempTargetType = TypeContainer.getIntegerType(sourceType.isSigned(), 256);
                expressionToConvert = new Expression(expressionToConvert, tempTargetType);
            }

            ASTElementaryTypeName destType = (ASTElementaryTypeName)desiredType;
            if (Configuration.languageVersionMinor >= 5 && destType.getBytes() < 32) {
                ASTElementaryTypeName tempTargetType = TypeContainer.getByteType(32);
                expressionToConvert = new Expression(expressionToConvert, tempTargetType);
            }

            return new Expression(expressionToConvert, desiredType);
        } else if (Type.isBoolType(desiredType)) {
            return integerToBool(expressionToConvert, desiredType);
        } else if (Type.isStringType(desiredType)) {
            return convertComparableExpressionUsingConditionalOperator(expressionToConvert, desiredType);
        } else if (Type.isAddressType(desiredType)) {
            // Curiously, a direct conversion exists, regardless of the input type
            return new Expression(expressionToConvert, desiredType);
        }
        return genericConvertFrom(environment, expressionToConvert, desiredType);
    }


    protected Expression convertFromInt(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        return convertFromIntOrUint(environment, expressionToConvert, desiredType);
    }

    protected Expression convertFromUint(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        return convertFromIntOrUint(environment, expressionToConvert, desiredType);
    }
    
    protected Expression convertFromBool(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        if (Type.isIntegerType(desiredType)) {
            // bool to integer.
            // The initial version mapped true -> 1, false ->0 which can be viewed as an idiomatic preservation
            // of the original value, but this produces results which are unesirably biased to a tiny part of
            // the result type's domain.
            // Keeping a bias to 0 may be desirable due to its special natue, so we include it in one branch
            // of the conditional operator. But the other branch receives a random value (which is in turn
            // biased to small values because they are more meaningful for various operations, e.g. as shift
            // bits)
            /*
            // true -> 1, false -> 0 per conditional operator
            return new Expression(expressionToConvert, new Expression(new IntegerValue(desiredType, 1)), new Expression(new IntegerValue(desiredType, 0)));
            */
            Expression trueBranch;
            Expression falseBranch;
            if (prng.flipCoin()) {
                trueBranch = new Expression((IntegerValue)valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));
                falseBranch = new Expression(new IntegerValue(desiredType, 0));
            } else {
                falseBranch = new Expression((IntegerValue)valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL));
                trueBranch = new Expression(new IntegerValue(desiredType, 0));
            }
            return new Expression(expressionToConvert, trueBranch, falseBranch);

        } else {
            // bool to anything else: random value in both cases
            // could be done for int as well, but 0/1 may be more intuitive/traditional...?
            return new Expression(expressionToConvert,
                                  new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL)),
                                  new Expression(valueGenerator.generateRandomValue(ast, desiredType, INTEGER_GENERATION_POLICY_FAVOR_SMALL)));
        }
        //return genericConvertFrom(environment, expressionToConvert, desiredType);
    }
    protected Expression convertFromByte(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        if (Type.isByteType(desiredType)) {
            // A cast is sufficient
            return new Expression(expressionToConvert, desiredType);
        } else if (Type.isIntegerType(desiredType)) {
            // Starting with 0.5.x, a direct conversion pathway between integer and bytes types is apparently only
            // available for the maximum-sized types, such as int256 to bytes32 and vice-versa.
            // For now, we use intermediate conversions to these types where needed. 
	    ASTElementaryTypeName sourceType = (ASTElementaryTypeName)expressionToConvert.getType();

	    if (Configuration.languageVersionMinor >= 5 && sourceType.getBytes() < 32) {
                ASTElementaryTypeName tempTargetType = TypeContainer.getByteType(32);
                expressionToConvert = new Expression(expressionToConvert, tempTargetType);
            }

            ASTElementaryTypeName destType = (ASTElementaryTypeName)desiredType;
            if (Configuration.languageVersionMinor >= 5 && destType.getBits() < 256) {
                ASTElementaryTypeName tempTargetType = TypeContainer.getIntegerType(destType.isSigned(), 256);
                expressionToConvert = new Expression(expressionToConvert, tempTargetType);
            }

            return new Expression(expressionToConvert, desiredType);
        } else if (Type.isBoolType(desiredType)) {
            // Convert to some integer type, then to bool. We call this function recursively to properly
	    // address the conversion indirections required by solc 0.5+ (if branch above)
            ASTNode integerType = TypeContainer.getRandomIntegerType(prng);
            //expressionToConvert = new Expression(expressionToConvert, integerType);
	    expressionToConvert = convertFromByte(environment, expressionToConvert, integerType);
            return convertFromInt(environment, expressionToConvert, desiredType);
        } else if (Type.isStringType(desiredType)) {
            return convertComparableExpressionUsingConditionalOperator(expressionToConvert, desiredType);
        } else if (Type.isAddressType(desiredType)) {
            return new Expression(expressionToConvert, desiredType);
        }
        return genericConvertFrom(environment, expressionToConvert, desiredType);
    }
    protected Expression convertFromAddress(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        if (Type.isIntegerType(desiredType) || Type.isByteType(desiredType)) {
            // A direct conversion exists
            return new Expression(expressionToConvert, desiredType);
        } else if (Type.isBoolType(desiredType)) {
            ASTElementaryTypeName randomIntegerType = TypeContainer.getRandomIntegerType(prng);
            Expression intermediateExpression = new Expression(expressionToConvert, randomIntegerType);
            return randomIntegerType.isSigned()
                    ? convertFromInt(environment, intermediateExpression, desiredType)
                    : convertFromUint(environment, intermediateExpression, desiredType);
        } else if (Type.isStringType(desiredType)) {
            return convertComparableExpressionUsingConditionalOperator(expressionToConvert, desiredType);
        }

        return genericConvertFrom(environment, expressionToConvert, desiredType);
    }
    protected Expression convertFromString(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        // String-to-anything requires us to go through a bytes type
        Expression bytesExpression = hashString(environment, expressionToConvert);
        return convertFromByte(environment, bytesExpression, desiredType);

        //return genericConvertFrom(environment, expressionToConvert, desiredType);
    }


    protected Expression convertFromEnum(VariableEnvironment environment,
                                         Expression expressionToConvert,
                                         ASTEnumDefinition enumDefinition,
                                         ASTNode desiredType) throws Exception {
        if (Type.isIntegerType(desiredType)) {
            // A direct conversion exists
            return new Expression(expressionToConvert, desiredType);
        } else if (Type.isByteType(desiredType) || Type.isBoolType(desiredType)) {
            ASTElementaryTypeName randomIntegerType = TypeContainer.getRandomIntegerType(prng);
            Expression intermediateExpression = new Expression(expressionToConvert, randomIntegerType);
            return randomIntegerType.isSigned()
                    ? convertFromInt(environment, intermediateExpression, desiredType)
                    : convertFromUint(environment, intermediateExpression, desiredType);
        } else if (Type.isStringType(desiredType)) {
            return convertComparableExpressionUsingConditionalOperator(expressionToConvert, desiredType);
        }

        return genericConvertFrom(environment, expressionToConvert, desiredType);
    }



    public Expression hashString(VariableEnvironment environment, Expression stringExpression) throws Exception {
        // s -> keccak256(s)
        ASTFunctionCall functionCall = new ASTFunctionCall(0, false, null);
        ASTIdentifier identifier = new ASTIdentifier(0, "keccak256", 0);
        functionCall.addChildNode(identifier);
        functionCall.addChildNode(stringExpression.toASTNode());
        identifier.finalize();
        functionCall.finalize();

        ASTNode returnType = TypeContainer.getByteType(32);

        // TODO: Re-enable the expression creation and find a way to evaluate this.
        // keccak256 implementations exist, though not in Maven Central.
        // Since keccak256 is only used for string comparisons, we could also compute some other hash
        ArrayList<Expression> arguments = new ArrayList<Expression>();
        arguments.add(stringExpression);
        return new Expression(functionCall, arguments, returnType);
    }

    public Expression accessRandomStructMember(VariableEnvironment environment,
                                                  Expression expressionToConvert,
                                                  ASTStructDefinition structDefinition) throws Exception {
        // Struct values cannot be used as operands for any computational (as opposed to assignment) binary operators.
        // Thus we pick a random struct member first - involving an intermediate struct member access operator -, and
        // then convert that one to the desired type.
        if (structDefinition.getMembers().size() == 0) {
            throw new Exception("convertFromStruct applied to empty struct type");
        }

        // Random member selection
        int randomMemberindex = (int)prng.generateLongInteger(0, structDefinition.getMembers().size()-1);
        ASTVariableDeclaration memberVariable = structDefinition.getMembers().get(randomMemberindex);
        ASTNode memberType = memberVariable.getTypeName();

        // Dereference to obtain selected member
        expressionToConvert = new Expression(expressionToConvert, memberVariable);

        // Handle nested structs
        if (memberType instanceof ASTUserDefinedTypeName) {
            ASTStructDefinition memberStructDefinition = ast.getStructDefinition(memberType.getName());
            if (memberStructDefinition != null) {
                // Another nested struct definition that must be resolved
                expressionToConvert = accessRandomStructMember(environment, expressionToConvert, memberStructDefinition);
            } else {
                throw new Exception("TypeConverter: Cannot resolve struct type " + memberType.getName());
            }
        } else if (memberType instanceof ASTArrayTypeName) {
            // Member is an array - obtain an element from it
            expressionToConvert = accessRandomArrayElement(environment, expressionToConvert);
        }


        //System.out.println("structmember result expr >"
        //        + expressionToConvert.toASTNode().toSolidityCode()
        //        + "< member type "
        //        + memberType.toSolidityCode());
        return expressionToConvert;
    }

    protected Expression convertFromStruct(VariableEnvironment environment,
                                           Expression expressionToConvert,
                                           ASTStructDefinition structDefinition,
                                           ASTNode desiredType) throws Exception {
        // Get a random non-struct, non-array expression - descending with "." and "[]" operators as deeply as needed
        Expression randomMember = accessRandomStructMember(environment, expressionToConvert, structDefinition);

        // Convert to desired final result
        return convertToCompatibleType(environment, randomMember, desiredType);
    }

    public Expression accessRandomArrayElement(VariableEnvironment environment, Expression expressionToConvert) throws Exception {
        // Obtain a random element from the array first
        // TODO: The index creation should be more sophisticated - possibly using other variables as index, and taking
        // VariableEnvironment state into account to otbain the range of valid array sizes.
        int index = (int)prng.generateLongInteger(0, 4);
        // Note: the subscript technically doesn't need to be evaluated for all elements now, since we only have a single
        // constant, but we may later use variables instead
        Expression subscriptExpression = new Expression(new IntegerValue("uint", index));
        expressionToConvert = new Expression(expressionToConvert, subscriptExpression);

        ASTNode elementType = expressionToConvert.getType();
        if (elementType instanceof ASTArrayTypeName) {
            expressionToConvert = accessRandomArrayElement(environment, expressionToConvert);
        } else if (elementType instanceof ASTUserDefinedTypeName) {
            ASTUserDefinedTypeName userDefinedTypeName = (ASTUserDefinedTypeName)elementType;
            ASTStructDefinition structDefinition = ast.getStructDefinition(userDefinedTypeName.getName());
            if (structDefinition != null) {
                expressionToConvert = accessRandomStructMember(environment, expressionToConvert, structDefinition);
            }
        }
        return expressionToConvert;
    }

    protected Expression convertFromArray(VariableEnvironment environment, Expression expressionToConvert, ASTNode desiredType) throws Exception {
        // Get a random non-array, non-struct expression - descending with "." and "[]" operators as deeply as needed
        Expression randomElement = accessRandomArrayElement(environment, expressionToConvert);

        // Convert to desired final result
        return convertToCompatibleType(environment, randomElement, desiredType);
    }
}
