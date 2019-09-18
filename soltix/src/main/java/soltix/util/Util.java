package soltix.util;

public class Util {
    static public void unimpl() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();


        for (int i = 1; i < elements.length; ++i) {
            StackTraceElement s = elements[i];
            System.out.println("\tat " + s.getClassName() + "." + (s.getMethodName() != null? s.getMethodName(): "null")
                + "(" + (s.getFileName() != null? s.getFileName(): "null") + ":"
                +  s.getLineNumber() + ")");
        }
        System.out.println("");
        System.out.println("UNIMPLEMENTED PATH - see stack trace above");
        System.exit(1);
    }
}
