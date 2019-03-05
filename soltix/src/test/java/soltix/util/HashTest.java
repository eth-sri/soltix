package soltix.util;

import soltix.ast.ASTNode;
import soltix.interpretation.TypeContainer;
import soltix.interpretation.values.BytesValue;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class HashTest {

    @Test
    public void toChecksumAddress() throws Exception {
        ASTNode type = TypeContainer.getByteType(20);

        // > toChecksumAddress('0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359')
        // '0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359'
        byte [] valueArray = new byte[]{(byte)0xfb, 0x69, 0x16, 0x09, 0x5c, (byte)0xa1, (byte)0xdf, 0x60, (byte)0xbb,
                                        0x79, (byte)0xce, (byte)0x92, (byte)0xce, 0x3e, (byte)0xa7, 0x4c, 0x37, (byte)0xc5,
                                        (byte)0xd3, 0x59};
        BytesValue value = new BytesValue(type, valueArray);

        String valueString = value.toHexConstantString();
        System.out.println("orig " + valueString);

        String checksumCompliantString = Hash.toChecksumAddress(valueString);
        System.out.println("fixed " + checksumCompliantString);

        String indirectlyFixedString = value.toASTNode(false).toSolidityCode();
        System.out.println("indirectly fixed " + indirectlyFixedString);

        // Check correct transformation
        Assert.assertTrue(checksumCompliantString.equals("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"));
        Assert.assertTrue(indirectlyFixedString.equals("bytes20(0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359)"));

        // Check cases with already correct addresses that must be preserved
        String[] correctCases = new String[]{
            // All caps
            "0x52908400098527886E0F7030069857D2E4169EE7",
            "0x8617E340B3D01FA5F11F306F4090FD50E238070D",
            // All Lower
            "0xde709f2102306220921060314715629080e2fb77",
            "0x27b1fdb04752bbc536007a920d24acb045561c26",
            // Normal
            "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
            "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
            "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
        };

        for (int i = 0; i < correctCases.length; ++i) {
            Assert.assertTrue(correctCases[i].equals(Hash.toChecksumAddress(correctCases[i])));
        }
    }
}


//0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359