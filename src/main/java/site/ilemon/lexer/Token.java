package site.ilemon.lexer;
import site.ilemon.util.SourceSpan;

public class Token {

    public String lexeme;
    public SourceSpan span;
    public TokenKind kind;
    // Public fields for backward compatibility with parser
    public int lineNumber;
    public int columnNumber;

    // Private cached fields derived from span
    private int lineNumberCached = -1;
    private int columnNumberCached = -1;

    public Token(String lexeme, SourceSpan span, TokenKind kind) {
        this.lexeme = lexeme;
        this.span = span;
        this.kind = kind;
        // Initialize public fields from span
        if (span != null) {
             this.lineNumber = span.getStartLine();
             this.columnNumber = span.getStartColumn();
             this.lineNumberCached = this.lineNumber; // Also cache for getters if needed
             this.columnNumberCached = this.columnNumber;
        } else {
             this.lineNumber = -1;
             this.columnNumber = -1;
        }
    }

    public Token(TokenKind kind, String lexeme, SourceSpan span) {
        this.lexeme = lexeme;
        this.kind = kind;
        this.span = span;
        if (span != null) {
             this.lineNumber = span.getStartLine();
             this.columnNumber = span.getStartColumn();
             this.lineNumberCached = this.lineNumber;
             this.columnNumberCached = this.columnNumber;
        } else {
             this.lineNumber = -1;
             this.columnNumber = -1;
        }
    }

    public Token(TokenKind kind, String lexeme, int lineNumber, int columnNumber, String fileName, String sourceContent) {
        this.lexeme = lexeme;
        this.kind = kind;
        // Create a span for backward compatibility, assuming the token length is the lexeme length
        int startOffset = calculateOffsetFromLineAndColumn(lineNumber, columnNumber, sourceContent);
        this.span = new SourceSpan(fileName, startOffset, startOffset + lexeme.length(), sourceContent);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.lineNumberCached = lineNumber;
        this.columnNumberCached = columnNumber;
    }

    public Token(TokenKind kind) {
        this.kind = kind;
        this.span = null;
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    // Getter cho lineNumber và columnNumber, sử dụng giá trị cached từ span
    // Giữ lại để đảm bảo tương thích nếu cần, nhưng ưu tiên trường public
    public int getLineNumber() {
        if (lineNumberCached == -1 && span != null) {
            lineNumberCached = span.getStartLine();
        }
        return lineNumberCached != -1 ? lineNumberCached : this.lineNumber;
    }

    public int getColumnNumber() {
        if (columnNumberCached == -1 && span != null) {
            columnNumberCached = span.getStartColumn();
        }
        return columnNumberCached != -1 ? columnNumberCached : this.columnNumber;
    }

    // Helper method to calculate offset from line and column for backward compatibility constructors
    private static int calculateOffsetFromLineAndColumn(int lineNumber, int columnNumber, String source) {
        if (source == null || lineNumber < 1 || columnNumber < 1) {
            return -1;
        }
        int currentLine = 1;
        int currentOffset = 0;
        while (currentOffset < source.length() && currentLine < lineNumber) {
            if (source.charAt(currentOffset) == '\n') {
                currentLine++;
            }
            currentOffset++;
        }
        if (currentLine == lineNumber) {
            // Now advance 'columnNumber - 1' positions, skipping tabs and spaces potentially
            // For simplicity here, we assume columnNumber is 0-based index within the line
            // Adjust if the input columnNumber is 1-based
            int startOfLineOffset = currentOffset;
            // Move to the calculated column (assuming 1-based column number)
            for (int i = 1; i < columnNumber && currentOffset < source.length() && source.charAt(currentOffset) != '\n'; i++) {
                currentOffset++;
            }
            if (currentOffset < source.length() && currentLine == lineNumber) {
                return currentOffset; // This is the 0-based offset
            }
        }
        return -1; // Not found
    }

    @Override
    public String toString() {
        return "<lexeme=" + lexeme
                + ", span=" + span
                + ", lineNumber=" + lineNumber
                + ", columnNumber=" + columnNumber
                + ", kind=" + kind + ">\n";
    }
}