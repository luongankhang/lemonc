package site.ilemon.codegen.ast;

public class Label {
    private final int i;
    private static int count = 0;

    public Label()
    {
        i = count++;
    }

    /**
     * Reset label counter.
     * Must be called before each new compilation task to avoid label numbering conflicts during multiple compilations within the same JVM process.
     */
    public static void resetCounter() {
        count = 0;
    }

    @Override
    public String toString()
    {
        return "Label_" + i;
    }
}