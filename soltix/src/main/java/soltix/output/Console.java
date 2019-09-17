package soltix.output;

import soltix.ast.ASTNode;

public class Console {
    static public void error(ASTNode node, String message) {
        System.out.println("Error on line " + node.getInputCodeLineNumber() + ": " + message);
    }
}
