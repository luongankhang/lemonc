package site.ilemon.util;

/**
 * Immutable source span with file, offsets, and 1-based line/column bounds.
 */
public record SourceSpan(String fileName, int startOffset, int endOffset,
                         int startLine, int startColumn, int endLine, int endColumn) {
    public SourceSpan {
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid offsets: start=" + startOffset + ", end=" + endOffset);
        }
        if (startLine < 1 || startColumn < 1 || endLine < 1 || endColumn < 1) {
            throw new IllegalArgumentException("Invalid line/column values");
        }
    }

    public static SourceSpan of(String fileName, int startOffset, int endOffset,
                                int startLine, int startColumn, int endLine, int endColumn) {
        return new SourceSpan(fileName, startOffset, endOffset, startLine, startColumn, endLine, endColumn);
    }

    public static SourceSpan singlePoint(String fileName, int offset, int line, int column) {
        return new SourceSpan(fileName, offset, offset, line, column, line, column);
    }

    // Keep bean-style accessors for the existing compiler API.
    public String getFileName() { return fileName; }
    public int getStartOffset() { return startOffset; }
    public int getEndOffset() { return endOffset; }
    public int getStartLine() { return startLine; }
    public int getStartColumn() { return startColumn; }
    public int getEndLine() { return endLine; }
    public int getEndColumn() { return endColumn; }
    public int getLength() { return endOffset - startOffset; }

    public SourceSpan withEnd(int newEndOffset, int newEndLine, int newEndColumn) {
        return new SourceSpan(fileName, startOffset, newEndOffset, startLine, startColumn, newEndLine, newEndColumn);
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%d-%d:%d",
            fileName == null ? "<unknown>" : fileName, startLine, startColumn, endLine, endColumn);
    }
}
