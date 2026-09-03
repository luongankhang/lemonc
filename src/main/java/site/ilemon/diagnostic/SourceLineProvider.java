package site.ilemon.diagnostic;

/** Supplies source text for a diagnostic renderer. */
@FunctionalInterface
public interface SourceLineProvider {
    String line(String fileName, int lineNumber);
}
