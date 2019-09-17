package soltix.util;

public class Util {
    static public void unimpl() {
        System.out.println(Thread.currentThread().getStackTrace());
        System.out.println("");
        System.out.println("UNIMPLEMENTED PATH - see stack trace above");
        System.exit(1);
    }
}
