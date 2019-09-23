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
package soltix.interpretation.values;

import soltix.Configuration;
import soltix.ast.ASTElementaryTypeName;
import soltix.ast.ASTNode;
import soltix.ast.ASTVerbatimText;
import soltix.interpretation.TypeContainer;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * Class to represent integer values
 */
public class IntegerValue extends Value implements Comparable, IIntegerOperations {
    // AST type representation
    private ASTElementaryTypeName type;
    private boolean isIndeterminateType = false; // for temporarily untyped 256-bit items
    // ... extracted main type info
    /*private int bits;
    private boolean signed;*/

    // Value representations for small (<= 64bit and large >= 64 and <= 256) numbers
    // TODO Do away with this to avoid escalating case differentiations or be more clever about it (we probably construct too many temorary objects now too)
    private long smallValue = 0;
    private BigInteger bigValue = null;

    // Constructor for constant value type inference
    // TODO We currently assign the type immediately to get up and running but the correct behavior is likely:
    //    - The constant value starts out with no particular type, any operations are just performed on BigIntegers
    // without restrictions
    //    - The unknown type function "collapses" as soon as needed, e.g. if the other argument in an operation
    // already has a type, or the value is assigned to something
    public IntegerValue(BigInteger bigInteger) throws Exception {
        bigValue = bigInteger;

        if (bigInteger.compareTo(BigInteger.ZERO) < 0) {
            throw new Exception("IntegerValue constructor: Type inference is only allowed for positive constants");
        }

        // Count bits needed to represent integer
        int count = 0;
        while (bigInteger.compareTo(BigInteger.ZERO) != 0) {
            bigInteger = bigInteger.shiftRight(1);
            ++count;
        }

        int byteCount = count / 8;
        if (byteCount == 0 || byteCount * 8 != count) ++byteCount;

        type = TypeContainer.getIntegerType(true, byteCount*8);
    }


    public void setIsIndeterminateType(boolean value) {
        isIndeterminateType = value;
    }
    public boolean getIsIndeterminateType() {
        return isIndeterminateType;
    }


    private IntegerValue(ASTNode type) throws Exception {
        //super(type);
        this.type = (ASTElementaryTypeName)type;
        /*this.bits = extractBitsFromType(type);
        this.signed = extractIsSignedFromType(type);*/
    }

    public boolean haveSmallValue() {
        return bigValue == null;
    }

    protected long getSmallValue() { return smallValue; }
    public BigInteger getBigValue() { return bigValue; }

    public IntegerValue(ASTNode type, long value) throws Exception {

        /*
        TODO Re-enable "small" integers if needed or remove all associated disabled processing

        this(type);
        if (this.type.getBits() > 64) { // TODO proper unsigned treatment
            //throw new Exception("Small IntegerValue constructed for > 64bit item");

            // We allow this scenario - an integer type with many bits, but with a small 64bit value representation -
            // since small values will be assigned to large integers in many cases, where this is sufficient.
        }
        smallValue = value;
        */

        this(type, BigInteger.valueOf(value));
    }

    public IntegerValue(String type, long value) throws Exception {
        this(new ASTElementaryTypeName(0, type), value);
    }

    public IntegerValue(ASTNode type, BigInteger value) throws Exception {
        this(type);
        if (this.type.getBits() <= 64) {
            //throw new Exception("Big IntegerValue constructed for <= 64bit item");
            // Allow this for now, since a big representation may be needed for changes later
        }
        bigValue = value;
    }

    public IntegerValue(ASTNode type, String value) throws Exception {
        this(type);
        /*
         TODO Re-enable "small" integers if needed or remove all associated disabled processing


        if (this.type.getBits() <= 64) {
            smallValue = Long.valueOf(value);
        } else {
            bigValue = new BigInteger(value);
        }*/
        bigValue = new BigInteger(value);
    }

    private void updateValue(BigInteger value) {
        this.bigValue = value;
    }

    @Override
    public ASTNode getType() { return type; }

    static private boolean suppressTypeAnnotations = false;
    static public void setSuppressTypeAnnotations(boolean value) { suppressTypeAnnotations = value; }
    static public boolean getSuppressTypeAnnotations() { return suppressTypeAnnotations; }
    @Override
    public ASTNode toASTNode(boolean forJavaScript) throws Exception {
        if (haveSmallValue()) {
            //return new ASTVerbatimText(0, String.valueOf(smallValue));
            throw new Exception("IntegerValue.toASTNode: Small values are unsupported");
        } else {
            if (!forJavaScript) {
                // For Solidity: always use an explicit cast to ensure the correct type
                // TODO Implement type inference for constants (= smallest fit?! signed or unsigned?), then randomly
                // switch between explicit and implicit typing
                if (getSuppressTypeAnnotations()) {
                    return new ASTVerbatimText(0, bigValue.toString());
                } else {
                    return new ASTVerbatimText(0, getType().toSolidityCode() + "(" + bigValue.toString() + ")");
                }
            } else {
                // For JavaScript: type cannot be specified.
                // To avoid silent errors introduced by JavaScript number size limitations, we need to enclose the
                // number between quotation marks. Otherwise, if e.g. an int184 with value
                // -2399511538002730253041114307794119136137714540074305396 is passed to a function, the lower part
                // will be chopped off and padded with zeroes in truffle. Remix detects the issue and produces an
                // error, which - as discussed in https://github.com/ethereum/remix/issues/801 - can be addressed
                // using quotes.
                return new ASTVerbatimText(0, "\"" + bigValue.toString() + "\"");
            }
        }
    }

    @Override
    public Object toJSONRepresentation() {
        return bigValue.toString(16); // like truffle, output hex number without 0x prefix
    }

    @Override
    public String toString() {
        return bigValue.toString();
    }

    public int compareTo(Object other) {
        IntegerValue otherValue = (IntegerValue)other;
        try {
            if (isGreaterThan(otherValue).getValue()) {
                return 1;
            } else if (isEqualTo(otherValue).getValue()) {
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            // Shouldn't happen - value constructor exception propagation
            return 0;
        }
    }

    // Globally shared min/max values for various integer types
    private static HashMap<Integer, IntegerValue> signedMinValueByBits = new HashMap<Integer, IntegerValue>();
    private static HashMap<Integer, IntegerValue> signedMaxValueByBits = new HashMap<Integer, IntegerValue>();
    private static HashMap<Integer, IntegerValue> unsignedMinValueByBits = new HashMap<Integer, IntegerValue>();
    private static HashMap<Integer, IntegerValue> unsignedMaxValueByBits = new HashMap<Integer, IntegerValue>();

    protected void computeLimits() throws Exception {
        if (type.getBits() % 8 != 0) {
            throw new Exception("Integer type with invalid non-multiple of 8: " + type.getBits());
        }

        /*if (type.getBits() >= 64) {
            computeLargeLimits();
        } else {
            computeSmallLimits();
        }*/
        byte[] bitArray = new byte[type.getBits()/8];

        if (type.isSigned()) {
            // Min value - the sign bit is 1, all other bits aren't
            bitArray[0] = (byte)0x80;
            for (int i = 1; i < bitArray.length; ++i) {
                bitArray[i] = 0;
            }
            BigInteger minValue = new BigInteger(bitArray);

            signedMinValueByBits.put(type.getBits(), ValueContainer.getBigIntegerValue(type, minValue));

            // Max value - all bits except for the sign bit are 1
            bitArray[0] = (byte)0x7f;
            for (int i = 1; i < bitArray.length; ++i) {
                bitArray[i] = (byte)0xff;
            }
            BigInteger maxValue = new BigInteger(bitArray);

            signedMaxValueByBits.put(type.getBits(), ValueContainer.getBigIntegerValue(type, maxValue));
        } else {
            // unsigned min is known to be 0, but we still want Value instances that
            // are properly typed
            for (int i = 0; i < bitArray.length; ++i) {
                bitArray[i] = 0;
            }
            BigInteger minValue = new BigInteger(bitArray);
            unsignedMinValueByBits.put(type.getBits(), ValueContainer.getBigIntegerValue(type, minValue));

            // Max value - all bits are 1
            for (int i = 0; i < bitArray.length; ++i) {
                bitArray[i] = (byte)0xff;
            }
            BigInteger maxValue = new BigInteger(1, bitArray); // sign (1) + magnitude

            unsignedMaxValueByBits.put(type.getBits(), ValueContainer.getBigIntegerValue(type, maxValue));
        }
    }

    public int toInt() throws Exception {
        if (haveSmallValue()) {
            if (getSmallValue() > Integer.MAX_VALUE || getSmallValue() < Integer.MIN_VALUE) {
                throw new Exception("IntegerValue.toInt failed: value of range");
            }
            return (int)getSmallValue();
        } else {
            return getBigValue().intValue();
        }
    }


    public IntegerValue getMinValue() throws Exception {
        if (type.isSigned()) {
            if (!signedMinValueByBits.containsKey(type.getBits())) {
                computeLimits();
            }
            return signedMinValueByBits.get(type.getBits());
        } else {
            // unsigned
            if (!unsignedMinValueByBits.containsKey(type.getBits())) {
                computeLimits();
            }
            return unsignedMinValueByBits.get(type.getBits());
        }
    }

    public IntegerValue getMaxValue() throws Exception {
        if (type.isSigned()) {
            if (!signedMaxValueByBits.containsKey(type.getBits())) {
                computeLimits();
            }
            return signedMaxValueByBits.get(type.getBits());
        } else {
            // unsigned
            if (!unsignedMaxValueByBits.containsKey(type.getBits())) {
                computeLimits();
            }
            return unsignedMaxValueByBits.get(type.getBits());
        }
    }


    protected IntegerValue invariantChecked(IntegerValue value) throws Exception {
        if (!value.type.isSigned()
                && value.isSmallerThan(new IntegerValue(value.getType(), 0)).getValue()) {
            throw new Exception("Unsigned value unexpectedly turned negative");
        }
        if (value.isSmallerThan(getMinValue()).getValue()) {
            throw new Exception("Value " + value.toASTNode(false).toSolidityCode()
                + " smaller than minimum limit for type " + value.getType().toSolidityCode()
                    + ": " + getMaxValue().toASTNode(false).toSolidityCode());
        }

        if (value.isGreaterThan(getMaxValue()).getValue()) {
            throw new Exception("Value " + value.toASTNode(false).toSolidityCode()
                    + " exceeds maximum limit for type " + value.getType().toSolidityCode()
                    + ": " + getMaxValue().toASTNode(false).toSolidityCode());
        }
        return value;
    }

    public IntegerValue negate() throws Exception {
        IntegerValue result;

        // TODO handle type size specifics
        if (haveSmallValue()) {
            throw new Exception("Small values are unsupported");
            /*
            if (type.isSigned()) {
                result = ValueContainer.getSmallIntegerValue(getType(), -getSmallValue());
            } else {
                long negated = -getSmallValue() + 0x100;   // TODO proper sign change value for all types, 0x100 = 8 bits
                result = ValueContainer.getSmallIntegerValue(getType(), negated);
            }*/
        } else {
            BigInteger invertedBigInteger;

            if (!type.isSigned()) {
                // BigInteger.not() doesn't do the expected thing. xor with a bitmask to invert instead
                prepareBitmasks();
                // Compute a 2's complement negation
                invertedBigInteger = getBigValue().xor(negationMask).add(BigInteger.ONE);

                // Make negative value positive, limit value range with AND
                // (e.g. for uint8: -0 = ~0+1 = 0xff+1 = 0x100 -> AND with 0xff)
                invertedBigInteger = makeUnsigned(invertedBigInteger).and(negationMask);
            } else {
                if (!getMinValue().getBigValue().equals(getBigValue())) {
                    invertedBigInteger = getBigValue().negate();
                }  else {
                    // if val = MIN, then -val = MIN -> no-op
                    invertedBigInteger = getBigValue();
                }
            }

            result = ValueContainer.getBigIntegerValue(getType(), invertedBigInteger);
        }
        return invariantChecked(result);
    }

    // BigInteger does not seem to offer a consistently unsigned interpretation of its value. This poses
    // a problem when an operation (such as bitwise negation) on a positive BigInteger results in a value
    // that is internally interpreted as negative for purposes such as toString() output.
    //
    // Our awkward workaround for now is to OR a positive larger value with the value under consideration,
    // producing a positive total result, and then XOR the larger value off again. This increases the
    // BigInteger size by 1 bit the first time
    BigInteger negationMaskLimitBit; // = BigInteger.ONE.shiftLeft(((ASTElementaryTypeName)getType()).getBits());
    BigInteger negationMask; // = negationMaskLimitBit.subtract(BigInteger.ONE);

    protected void prepareBitmasks() throws Exception {
        if (negationMaskLimitBit == null) { // TODO static/cached for all
            negationMaskLimitBit = BigInteger.ONE.shiftLeft(type.getBits());
            negationMask = negationMaskLimitBit.subtract(BigInteger.ONE);
        }
    }


    protected BigInteger makeUnsigned(BigInteger value) {
        // BigInteger uses an internal sign-magnitude representation. If it holds the correct magnitude
        // but the wrong sign, our naive (and slow) solution is to flip the sign and construct a new value:
        String s = value.toString().replace('-', '+');
        BigInteger newInteger = new BigInteger(s);
        return newInteger;
    }

    protected BigInteger makeSigned(BigInteger value) throws Exception {
        if (value.compareTo(getMaxValue().getBigValue()) > 0) {
            // Value exceeds maximum (but we assume it doesn't have more bits than this type) -
            // shift into the negative part of the type's range
            BigInteger deltaValue = value.subtract(getMaxValue().getBigValue());
            value = getMinValue().getBigValue().add(deltaValue.subtract(BigInteger.ONE)); // -1 since delta=1 wraps to MIN
        } else if (value.compareTo(getMinValue().getBigValue()) < 0) {
            // Value is smaller than minimum (but we assume it doesn't have more bits than this type) -
            // shift into the positive part of the type's range
            BigInteger deltaValue = getMinValue().getBigValue().subtract(value);
            value = getMaxValue().getBigValue().subtract(deltaValue.subtract(BigInteger.ONE));  // -1 since delta=1 wraps to MAX
        }
        return value;
    }

    public IntegerValue negateBitwise() throws Exception {
        // TODO handle type size specifics
        IntegerValue result;

        // Perform 2's complement negation, undo the +1
        if (getBigValue().equals(getMinValue().getBigValue())) {
            result = getMaxValue();
        } else {
            result = negate().sub(ValueContainer.getBigIntegerValue(getType(), BigInteger.ONE));
        }
        return invariantChecked(result);
    }

    public IntegerValue add(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;
        BigInteger bigIntegerResult;

        // Note: Overflows are allowed in EVM (TODO but could usefully be recorded for other analysis purposes)
        // TODO: Handle proper wraparound for all sizes
        if (haveSmallValue() && otherValue.haveSmallValue()) {
            throw new Exception("Small values are unsupported");
           // result = ValueContainer.getSmallIntegerValue(type, getSmallValue() + otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            bigIntegerResult = BigInteger.valueOf(getSmallValue()).add(otherValue.getBigValue());
        } else if (otherValue.haveSmallValue()) {
            bigIntegerResult = getBigValue().add(BigInteger.valueOf(otherValue.getSmallValue()));
        } else {
            // Both are large
            bigIntegerResult = getBigValue().add(otherValue.getBigValue());
        }

        if (!type.isSigned()) {
            // AND with value mask to limit range
            prepareBitmasks();
            bigIntegerResult = bigIntegerResult.and(negationMask);
        } else {
            // Work out signedness in case of overflows
            bigIntegerResult = makeSigned(bigIntegerResult);
        }

        result = ValueContainer.getBigIntegerValue(type, bigIntegerResult);
        return invariantChecked(result);
    }
    public IntegerValue sub(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;
        BigInteger bigIntegerResult;

        // Note: Underflows are allowed in EVM (TODO but could usefully be recorded for other analysis purposes)
        // TODO: Handle proper wraparound for all sizes
        if (haveSmallValue() && otherValue.haveSmallValue()) {
            throw new Exception("Small values are unsupported");
            //result = ValueContainer.getSmallIntegerValue(type, getSmallValue() - otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            bigIntegerResult = BigInteger.valueOf(getSmallValue()).subtract(otherValue.getBigValue());
        } else if (otherValue.haveSmallValue()) {
            bigIntegerResult = getBigValue().subtract(BigInteger.valueOf(otherValue.getSmallValue()));
        } else {
            // Both are large
            bigIntegerResult = getBigValue().subtract(otherValue.getBigValue());
        }

        if (!type.isSigned()) {
            // AND with value mask to limit range
            prepareBitmasks();
            bigIntegerResult = bigIntegerResult.and(negationMask);
        } else {
            // Work out signedness in case of overflows
            bigIntegerResult = makeSigned(bigIntegerResult);
        }

        result = ValueContainer.getBigIntegerValue(type, bigIntegerResult);
        return invariantChecked(result);
    }
    public IntegerValue div(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;

        if (haveSmallValue() && otherValue.haveSmallValue()) {
            if (otherValue.getSmallValue() == 0) {
                throw new Exception("IntegerValue.div: Divison by zero");
            }
            result = ValueContainer.getSmallIntegerValue(type, getSmallValue() / otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, BigInteger.valueOf(getSmallValue()).divide(otherValue.getBigValue()));
        } else if (otherValue.haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().divide(BigInteger.valueOf(otherValue.getSmallValue())));
        } else {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().divide(otherValue.getBigValue()));
        }
        return invariantChecked(result);
    }
    public IntegerValue mul(Value value) throws Exception {
        // TODO handle type size specifics, wraparound
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;
        BigInteger bigIntegerResult;

        if (haveSmallValue() && otherValue.haveSmallValue()) {
            throw new Exception("Small values are unsupported");
            //result = ValueContainer.getSmallIntegerValue(type, getSmallValue() * otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            bigIntegerResult = BigInteger.valueOf(getSmallValue()).multiply(otherValue.getBigValue());
        } else if (otherValue.haveSmallValue()) {
            bigIntegerResult = getBigValue().multiply(BigInteger.valueOf(otherValue.getSmallValue()));
        } else {
            bigIntegerResult = getBigValue().multiply(otherValue.getBigValue());
        }

        // Always AND with value mask to limit range
        prepareBitmasks();
        bigIntegerResult = bigIntegerResult.and(negationMask);

        if (!type.isSigned()) {
            /*
            // AND with value mask to limit range
            prepareBitmasks();
            bigIntegerResult = bigIntegerResult.and(negationMask);
            */
        } else {
            // Work out signedness in case of overflows
            bigIntegerResult = makeSigned(bigIntegerResult);
        }

        result = ValueContainer.getBigIntegerValue(type, bigIntegerResult);
        return invariantChecked(result);
    }

    public IntegerValue bitwiseOr(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;

        if (haveSmallValue() && otherValue.haveSmallValue()) {
            result = ValueContainer.getSmallIntegerValue(type, getSmallValue() | otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, BigInteger.valueOf(getSmallValue()).or(otherValue.getBigValue()));
        } else if (otherValue.haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().or(BigInteger.valueOf(otherValue.getSmallValue())));
        } else {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().or(otherValue.getBigValue()));
        }
        return invariantChecked(result);
    }

    public IntegerValue bitwiseAnd(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;

        if (haveSmallValue() && otherValue.haveSmallValue()) {
            result = ValueContainer.getSmallIntegerValue(type, getSmallValue() & otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, BigInteger.valueOf(getSmallValue()).and(otherValue.getBigValue()));
        } else if (otherValue.haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().and(BigInteger.valueOf(otherValue.getSmallValue())));
        } else {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().and(otherValue.getBigValue()));
        }
        return invariantChecked(result);
    }

    public IntegerValue bitwiseXor(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;

        if (haveSmallValue() && otherValue.haveSmallValue()) {
            result = ValueContainer.getSmallIntegerValue(type, getSmallValue() ^ otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, BigInteger.valueOf(getSmallValue()).xor(otherValue.getBigValue()));
        } else if (otherValue.haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().xor(BigInteger.valueOf(otherValue.getSmallValue())));
        } else {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().xor(otherValue.getBigValue()));
        }

        return invariantChecked(result);
    }

    public IntegerValue bitwiseShiftLeft(Value value) throws Exception {
        // TODO get rid of shortValue(), which is wrong if truncation takes place.
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;
        BigInteger bigIntegerResult;

        boolean skipMasking = false;
        if (haveSmallValue() && otherValue.haveSmallValue()) {
            throw new Exception("Small values are unsupported");
            //result = ValueContainer.getSmallIntegerValue(type, getSmallValue() << otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            bigIntegerResult = BigInteger.valueOf(getSmallValue()).shiftLeft(otherValue.getBigValue().shortValue());
        } else if (otherValue.haveSmallValue()) {
            bigIntegerResult = getBigValue().shiftLeft(BigInteger.valueOf(otherValue.getSmallValue()).shortValue());
        } else {
            if (otherValue.getBigValue().compareTo(bigInteger256) >= 0) {
                // Shift is at least 256 bits - result will be 0
                bigIntegerResult = BigInteger.ZERO;
                skipMasking = true;
            } else {
                bigIntegerResult = getBigValue().shiftLeft(otherValue.getBigValue().shortValue());
            }
        }

        if (!skipMasking) {
            // Both for signed and unsigned, AND with value mask to limit range
            prepareBitmasks();
            bigIntegerResult = bigIntegerResult.and(negationMask);

            if (type.isSigned()) {
                // Work out new sign
                bigIntegerResult = makeSigned(bigIntegerResult);
            }
        }

        result = ValueContainer.getBigIntegerValue(type, bigIntegerResult);
        return invariantChecked(result);
    }

    final private BigInteger bigInteger256 = new BigInteger("256");

    public IntegerValue bitwiseShiftRight(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        ASTElementaryTypeName integerType = (ASTElementaryTypeName)value.getType();

        if (Configuration.languageVersionMinor >= 5) {
            // Address breaking change as per https://solidity.readthedocs.io/en/v0.5.3/050-breaking-changes.html :
            //
            //     Signed right shift now uses proper arithmetic shift, i.e. rounding towards negative infinity,
            //     instead of rounding towards zero. Signed and unsigned shift will have dedicated opcodes in
            //     Constantinople, and are emulated by Solidity for the moment.
            if (otherValue.isGreaterThanOrEqualTo(ValueContainer.getSmallIntegerValue(value.getType(), integerType.getBits())).getValue()) {
                // Shift bits exceed range
                if (!((ASTElementaryTypeName) value.getType()).isSigned()
                    || this.isGreaterThanOrEqualTo(ValueContainer.getSmallIntegerValue(integerType, 0)).getValue()) {
                    // Type is unsigned or shifted value is positive - result is 0 in any case
                    return ValueContainer.getSmallIntegerValue(integerType, 0);
                } else {
                    // Type is signed and value is negative - sign-extend to -1
                    return ValueContainer.getSmallIntegerValue(integerType, -1);
                }
            } else {
                // Perform normal shift
                return ValueContainer.getBigIntegerValue(integerType,
                        getBigValue().shiftRight(otherValue.getBigValue().shortValue()));
            }
        } else {
            if (otherValue.isGreaterThanOrEqualTo(ValueContainer.getSmallIntegerValue(value.getType(), integerType.getBits())).getValue()) {
                // Result is 0 in any case
                return ValueContainer.getSmallIntegerValue(integerType, 0);
            }

            if (otherValue.isEqualTo(ValueContainer.getSmallIntegerValue(value.getType(), 0)).getValue()) {
                // Apparently a zero exponent yield a zero result - handle that case explicitly
                return this;
            }

            // Compute divisor. Due to signedness issues for small types, we use a large uint256 type now that should be
            // sufficient (there's a dubious potential type mismatch here, but it happens to work for now)
            IntegerValue divisor = ValueContainer.getSmallIntegerValue(TypeContainer.getIntegerType(false, 256), 2).exp(otherValue);
            return div(divisor);
        }
    }

    public IntegerValue mod(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;

        // TODO remainder may need negations for some sign combinations?
        if (haveSmallValue() && otherValue.haveSmallValue()) {
            if (otherValue.getSmallValue() == 0) {
                throw new Exception("IntegerValue.mod: Division by zero");
            }
            result = ValueContainer.getSmallIntegerValue(type, getSmallValue() % otherValue.getSmallValue());
        } else if (haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, BigInteger.valueOf(getSmallValue()).remainder(otherValue.getBigValue()));
        } else if (otherValue.haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().remainder(BigInteger.valueOf(otherValue.getSmallValue())));
        } else {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().remainder(otherValue.getBigValue()));
        }
        return invariantChecked(result);
    }

    public IntegerValue exp(Value value) throws Exception {
        IntegerValue otherValue = (IntegerValue)value;
        IntegerValue result;

        if (haveSmallValue() && otherValue.haveSmallValue()) {
            result = ValueContainer.getSmallIntegerValue(type, (long)Math.pow(getSmallValue(), otherValue.getSmallValue()));
        } else if (haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, BigInteger.valueOf(getSmallValue()).modPow(otherValue.getBigValue(), getMaxValue().getBigValue()));
        } else if (otherValue.haveSmallValue()) {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().modPow(BigInteger.valueOf(otherValue.getSmallValue()), getMaxValue().getBigValue()));
        } else {
            result = ValueContainer.getBigIntegerValue(type, getBigValue().modPow(otherValue.getBigValue(), getMaxValue().getBigValue().add(BigInteger.ONE)));
        }
        return invariantChecked(result);
    }

    public BoolValue isSmallerThan(Value value) throws Exception {
        IntegerValue otherIntegerValue = (IntegerValue)value;
        boolean result;
        if (haveSmallValue() && otherIntegerValue.haveSmallValue()) {
            result = getSmallValue() < otherIntegerValue.getSmallValue();
        } else if (haveSmallValue()) {
            result = BigInteger.valueOf(getSmallValue()).compareTo(otherIntegerValue.getBigValue()) < 0;
        } else if (otherIntegerValue.haveSmallValue()) {
            result = getBigValue().compareTo(BigInteger.valueOf(otherIntegerValue.getSmallValue())) < 0;
        } else {
            // Both are large
            result = getBigValue().compareTo(otherIntegerValue.getBigValue()) < 0;
        }
        return ValueContainer.getBoolValue(result);
    }
    public BoolValue isSmallerThanOrEqualTo(Value value) throws Exception {
        return ValueContainer.getBoolValue(isSmallerThan(value).getValue() || isEqualTo(value).getValue());
    }
    public BoolValue isGreaterThan(Value value) throws Exception {
        IntegerValue otherIntegerValue = (IntegerValue)value;
        boolean result;
        if (haveSmallValue() && otherIntegerValue.haveSmallValue()) {
            result = getSmallValue() > otherIntegerValue.getSmallValue();
        } else if (haveSmallValue()) {
            result = BigInteger.valueOf(getSmallValue()).compareTo(otherIntegerValue.getBigValue()) > 0;
        } else if (otherIntegerValue.haveSmallValue()) {
            result = getBigValue().compareTo(BigInteger.valueOf(otherIntegerValue.getSmallValue())) > 0;
        } else {
            // Both are large
            result = getBigValue().compareTo(otherIntegerValue.getBigValue()) > 0;
        }
        return ValueContainer.getBoolValue(result);
    }
    public BoolValue isGreaterThanOrEqualTo(Value value) throws Exception {
        return ValueContainer.getBoolValue(isGreaterThan(value).getValue() || isEqualTo(value).getValue());
    }
    public BoolValue isEqualTo(Value value) throws Exception {
        IntegerValue otherIntegerValue = (IntegerValue)value;
        boolean result;
        if (haveSmallValue() && otherIntegerValue.haveSmallValue()) {
            result = getSmallValue() == otherIntegerValue.getSmallValue();
        } else if (haveSmallValue()) {
            result = BigInteger.valueOf(getSmallValue()).equals(otherIntegerValue.getBigValue());
        } else if (otherIntegerValue.haveSmallValue()) {
            result = getBigValue().equals(BigInteger.valueOf(otherIntegerValue.getSmallValue()));
        } else {
            // Both are large
            result = getBigValue().equals(otherIntegerValue.getBigValue());
        }
        return ValueContainer.getBoolValue(result);
    }
    public BoolValue isNotEqualTo(Value value) throws Exception {
        return ValueContainer.getBoolValue(!isEqualTo(value).getValue());
    }

    public BytesValue convertToBytesType(ASTElementaryTypeName newType) throws Exception {
        int ownBytes = type.getBits() / 8;
        BigInteger integerValue = bigValue;

        // Convert to unsigned analog of bytes type first, if needed. This also takes care of sign extensions
        IntegerValue unsignedValue;
        if (ownBytes == newType.getBytes() && !type.isSigned()) {
            unsignedValue = this;
        } else {
            ASTElementaryTypeName unsignedType = TypeContainer.getIntegerType(false, newType.getBytes()*8);
            unsignedValue = convertToIntegerType(unsignedType);
        }

        byte[] result = new byte[newType.getBytes()];
        byte[] bytesRepresentation = unsignedValue.bigValue.toByteArray();

        for (int i = bytesRepresentation.length-1, j = result.length-1; i >= 0 && j >= 0; --i, --j) {
            result[j] = bytesRepresentation[i];
        }
        return new BytesValue(newType, result);
    }

    public AddressValue convertToAddressType() throws Exception {
        IntegerValue unsignedValue;

        // Convert this item to uint160 (= 20 bytes) first
        if (type.getBits() == 160 && !type.isSigned()) {
            unsignedValue = this;
        } else {
            ASTElementaryTypeName unsignedType = TypeContainer.getIntegerType(false, 160);
            unsignedValue = convertToIntegerType(unsignedType);
        }

        String stringValue = unsignedValue.bigValue.toString(16);
        if (stringValue.length() % 2 != 0) {
            // Add leading 0 for consistency with Solidity compiler
            stringValue = "0" + stringValue;
        }
        return new AddressValue("0x" + stringValue);
    }

    public IntegerValue convertToIntegerType(ASTElementaryTypeName newType) throws Exception {
        IntegerValue result;
        // TODO actually adjust value.
        if (haveSmallValue()) {
            throw new Exception("Small values are unsupported");
            //result = new IntegerValue(otherType, getSmallValue());
        }

        BigInteger bigIntegerResult = getBigValue();

        // First construct a dummy value (which cannot be cached) with the result type so that proper bitmasks for these
        // conversions are already available within that object
        result = new IntegerValue(newType, bigIntegerResult);

        if (!newType.isSigned()) {
            // Mask off excess bits (truncate)
            result.prepareBitmasks();
            bigIntegerResult = bigIntegerResult.and(result.negationMask);
        } else {
            // Mask off excess bits (truncate)
            result.prepareBitmasks();
            bigIntegerResult = bigIntegerResult.and(result.negationMask);

            // Treat sign
            bigIntegerResult = result.makeSigned(bigIntegerResult);
        }

        result.updateValue(bigIntegerResult);

        return result.invariantChecked(result);
    }
}
