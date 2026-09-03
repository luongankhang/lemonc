package site.ilemon.util;

/**
 * Represents a span of source code, identified by a file, a start offset, and an end offset.
 * It also provides convenience methods to calculate line and column numbers if the source content is available.
 */
public class SourceSpan {
    private final String fileName;
    private final int startOffset;
    private final int endOffset;
    private final String sourceContent;

    /**
     * Creates a new SourceSpan.
     *
     * @param fileName       The name of the source file.
     * @param startOffset    The 0-based character offset where the span starts.
     * @param endOffset      The 0-based character offset where the span ends (exclusive).
     * @param sourceContent  The full source content of the file. Can be null if line/column calculation is not needed.
     */
    public SourceSpan(String fileName, int startOffset, int endOffset, String sourceContent) {
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid offsets: start=" + startOffset + ", end=" + endOffset);
        }
        this.fileName = fileName;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.sourceContent = sourceContent;
    }

    public String getFileName() {
        return fileName;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLength() {
        return endOffset - startOffset;
    }

    /**
     * Calculates the 1-based line number for the start of the span.
     * Requires sourceContent to be provided in the constructor.
     *
     * @return The 1-based line number, or -1 if sourceContent is null.
     */
    public int getStartLine() {
        if (sourceContent == null || startOffset >= sourceContent.length()) {
            return -1;
        }
        return calculateLine(startOffset, sourceContent) + 1; // Convert 0-based to 1-based
    }

    /**
     * Calculates the 1-based column number for the start of the span.
     * Requires sourceContent to be provided in the constructor.
     *
     * @return The 1-based column number, or -1 if sourceContent is null.
     */
    public int getStartColumn() {
        if (sourceContent == null || startOffset >= sourceContent.length()) {
            return -1;
        }
        return calculateColumn(startOffset, sourceContent) + 1; // Convert 0-based to 1-based
    }

    /**
     * Calculates the 1-based line number for the end of the span.
     * Requires sourceContent to be provided in the constructor.
     *
     * @return The 1-based line number, or -1 if sourceContent is null.
     */
    public int getEndLine() {
        if (sourceContent == null || endOffset > sourceContent.length()) {
            return -1;
        }
        return calculateLine(endOffset - 1, sourceContent) + 1; // Use endOffset - 1 for the last character of the span
    }

    /**
     * Calculates the 1-based column number for the end of the span.
     * Requires sourceContent to be provided in the constructor.
     *
     * @return The 1-based column number, or -1 if sourceContent is null.
     */
    public int getEndColumn() {
        if (sourceContent == null || endOffset > sourceContent.length()) {
            return -1;
        }
        return calculateColumn(endOffset - 1, sourceContent) + 1; // Use endOffset - 1 for the last character of the span
    }

    private static int calculateLine(int offset, String source) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static int calculateColumn(int offset, String source) {
        int col = 0;
        for (int i = offset - 1; i >= 0 && source.charAt(i) != '\n'; i--) {
            col++;
        }
        // Count characters from the start of the line up to (but not including) the character at offset
        // This gives a 0-based column number, which getStartColumn will convert to 1-based
        return col;
    }

    @Override
    public String toString() {
        return String.format("[%s:%d:%d-%d:%d]", fileName, getStartLine(), getStartColumn(), getEndLine(), getEndColumn());
    }
}