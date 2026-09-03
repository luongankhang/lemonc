package site.ilemon.exception;

import site.ilemon.diagnostic.Diagnostic;

public class LexException extends CompilerException {
    public LexException(String message) {
        super(message);
    }

    public LexException(Diagnostic diagnostic) {
        super(diagnostic);
    }
}
