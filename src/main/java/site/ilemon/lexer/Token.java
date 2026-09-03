package site.ilemon.lexer;

import site.ilemon.util.SourceSpan;

public class Token {
    public String lexeme;
    public TokenKind kind;
    public SourceSpan span;

    // Backward-compatible fields used throughout the current codebase and tests.
    public int lineNumber;
    public int columnNumber;
    public int endLineNumber;
    public int endColumnNumber;

    public Token(TokenKind kind, String lexeme, SourceSpan span) {
        this.kind = kind;
        this.lexeme = lexeme;
        this.span = span;
        if (span == null) {
            this.lineNumber = -1;
            this.columnNumber = -1;
            this.endLineNumber = -1;
            this.endColumnNumber = -1;
        } else {
            this.lineNumber = span.getStartLine();
            this.columnNumber = span.getStartColumn();
            this.endLineNumber = span.getEndLine();
            this.endColumnNumber = span.getEndColumn();
        }
    }

    public Token(String lexeme, SourceSpan span, TokenKind kind) {
        this(kind, lexeme, span);
    }

    /** Compatibility constructor for clients that still provide line/column data. */
    public Token(TokenKind kind, String lexeme, int lineNumber, int columnNumber,
                 String fileName, String sourceContent) {
        this(kind, lexeme, spanFromLocation(lexeme, lineNumber, columnNumber, fileName, sourceContent));
    }

    public Token(TokenKind kind) {
        this(kind, "", null);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public SourceSpan getSpan() {
        return span;
    }

    private static SourceSpan spanFromLocation(String lexeme, int line, int column,
                                               String fileName, String source) {
        int start = 0;
        int currentLine = 1;
        if (source != null) {
            while (start < source.length() && currentLine < line) {
                if (source.charAt(start++) == '\n') {
                    currentLine++;
                }
            }
        }
        start = Math.max(0, start + Math.max(0, column - 1));
        int length = lexeme == null ? 0 : lexeme.length();
        return SourceSpan.singlePoint(fileName, start, Math.max(1, line), Math.max(1, column))
                .withEnd(start + length, Math.max(1, line), Math.max(1, column + length));
    }

    @Override
    public String toString() {
        return "<lexeme=" + lexeme
                + ", span=" + span
                + ", lineNumber=" + lineNumber
                + ", columnNumber=" + columnNumber
                + ", kind=" + kind + ">";
    }
}
