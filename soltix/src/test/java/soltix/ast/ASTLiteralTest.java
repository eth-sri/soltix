package soltix.ast;

import org.junit.Assert;
import org.junit.Test;

public class ASTLiteralTest {

    @Test
    public void testConstructor() throws Exception {
        final ASTLiteral astLiteral = new ASTLiteral(0L, "",
                "foo", ASTLiteral.LiteralType.LITERAL_TYPE_STRING, "Bar");

        Assert.assertEquals(ASTLiteral.LiteralType.LITERAL_TYPE_STRING,
                astLiteral.getType());
        Assert.assertEquals("foo", astLiteral.getValueString());
        Assert.assertEquals("Bar", astLiteral.getSubdenomination());
    }

    @Test
    public void testEscapeString() {
        Assert.assertEquals("\\\"", ASTLiteral.escapeString("\""));
        Assert.assertEquals("\\\\\\u", ASTLiteral.escapeString("\\\\u"));
        Assert.assertEquals("\\x13", ASTLiteral.escapeString("\u0013"));
    }

    @Test
    public void testToSolidityCodeTypeIntegerDecimal() throws Exception {
        Assert.assertEquals("foo Bar", new ASTLiteral(0L, "",
                "foo", ASTLiteral.LiteralType.LITERAL_TYPE_INTEGER_DECIMAL,
                "Bar").toSolidityCode());
    }

    @Test
    public void testToSolidityCodeTypeIntegerHexadecimal() throws Exception {
        Assert.assertEquals("foo Bar", new ASTLiteral(0L, "",
                "foo", ASTLiteral.LiteralType.LITERAL_TYPE_INTEGER_HEXADECIMAL,
                "Bar").toSolidityCode());
    }

    @Test
    public void testToSolidityCodeTypeBool() throws Exception {
        Assert.assertEquals("foo Bar", new ASTLiteral(0L, "",
                "foo", ASTLiteral.LiteralType.LITERAL_TYPE_BOOL,
                "Bar").toSolidityCode());
    }

    @Test
    public void testToSolidityCodeTypeString() throws Exception {
        Assert.assertEquals("\"foo\" Bar", new ASTLiteral(0L, "",
                "foo", ASTLiteral.LiteralType.LITERAL_TYPE_STRING,
                "Bar").toSolidityCode());
    }

    @Test
    public void testToSolidityCodeTypeHexadecimal() throws Exception {
        Assert.assertEquals("hex\"foo\" Bar", new ASTLiteral(0L, "",
                "foo", ASTLiteral.LiteralType.LITERAL_TYPE_HEXADECIMAL,
                "Bar").toSolidityCode());
    }

    @Test
    public void testToSolidityCodePostfix() throws Exception {
        Assert.assertNull(new ASTLiteral(0L, "",
                "", ASTLiteral.LiteralType.LITERAL_TYPE_HEXADECIMAL,
                "").toSolidityCodePostfix());
    }
}
