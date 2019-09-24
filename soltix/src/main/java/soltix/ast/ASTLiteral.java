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

public class ASTLiteral extends ASTNode {
    private String type; // TODO not actually used and the solc ast-json info looks fairly useless
    private String value;
    //private boolean isHex;
    private LiteralType literalType;
    private String subdenomination = null;

    public enum LiteralType {
        LITERAL_TYPE_INTEGER_DECIMAL, // decimal integral constant - 123
        LITERAL_TYPE_INTEGER_HEXADECIMAL, // hexadecimal integral constant - 0x123
        LITERAL_TYPE_HEXADECIMAL,     // hexadecimal string constant -  hex"123" (TODO distinction from 0x123 unclear)
        LITERAL_TYPE_BOOL,            // boolean constant - true/false
        LITERAL_TYPE_STRING           // string constant - "foo"
    }


    public LiteralType getType() { return literalType; }
    public String getSubdenomination() { return subdenomination; }
    public String getValueString() { return value; }

    public ASTLiteral(long id, String type, String value, LiteralType literalType, String subdenomination) throws Exception {  //}, boolean isHex) {
        super(id);
        this.type = type;
        this.value = value;
        //this.isHex = isHex;
        this.literalType = literalType;
        this.subdenomination = subdenomination;
    }

    static public String escapeString(String value) {
        String result = "";
        for (int i = 0; i < value.length(); ++i) {
            Character    c = value.charAt(i);
            if (c == '"') {
                result += "\\\"";
            } else if (value.charAt(i) == '\\' && i+1 < value.length() && value.charAt(i+1) == 'u') {
                result += "\\u";
                ++i;
            } else if (c == '\\') {
                result += "\\\\"; // TODO reconstruct why this is here and how it can be done properly
            } else if (Character.isISOControl(c)) {
                result += "\\x" + String.format("%02x", (int) c);
            } else {
                result += c;
            }
        }
        return result;
    }

    @Override
    public String toSolidityCode() {
        String result = null;

        //return isHex? "hex\"" + value + "\"": value;
        switch (literalType) {
            case LITERAL_TYPE_INTEGER_DECIMAL:
            case LITERAL_TYPE_INTEGER_HEXADECIMAL:
            case LITERAL_TYPE_BOOL:
                result = value;
                break;
            case LITERAL_TYPE_HEXADECIMAL:
                result = "hex\"" + value + "\"";
                break;
            case LITERAL_TYPE_STRING:
                //result = "\"" + value.replaceAll("\\\"", "\\\\\"") + "\"";

                // Build string while escaping " and outputting all non-printable chars as
                // hexadecimal sequences
                result = "\"";
                if (value == null) {
                    System.out.println("FAILAGE");
                    System.exit(0);
                }
                // TODO remove or re-enable escapeString(). Currently we transform every
                // input string to \u-encoded sequences during data generation or solc input
                // processing
                result += value; //escapeString(value);
                result += "\"";
                break;
            default:
                return null;
        }
        if (subdenomination != null) {
            result += " " + subdenomination;
        }

        return result;
    }
    @Override
    public String toSolidityCodePostfix() { return null; }
}
