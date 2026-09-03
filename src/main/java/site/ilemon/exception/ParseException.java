package site.ilemon.exception;

import site.ilemon.diagnostic.Diagnostic;

public class ParseException extends CompilerException {
    public ParseException(String message) {
        super(message);
    }

    public ParseException(Diagnostic diagnostic) {
        super(diagnostic);
    }
}
