package site.ilemon.exception;

import site.ilemon.diagnostic.Diagnostic;

public class SemanticException extends CompilerException {
    public SemanticException(String message) {
        super(message);
    }

    public SemanticException(Diagnostic diagnostic) {
        super(diagnostic);
    }
}
