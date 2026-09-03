package site.ilemon.exception;

import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.diagnostic.DiagnosticEngine;
import site.ilemon.diagnostic.Severity;
import site.ilemon.diagnostic.DiagnosticCodes;

public class CompilerException extends RuntimeException {
    private final Diagnostic diagnostic;

    public CompilerException(String message) {
        this(Diagnostic.of(Severity.ERROR, DiagnosticCodes.INTERNAL_COMPILER_ERROR,
                DiagnosticEngine.english(message), null, "compiler error"));
    }

    public CompilerException(Diagnostic diagnostic) {
        this(diagnostic, diagnostic == null ? "" : diagnostic.message());
    }

    private CompilerException(Diagnostic diagnostic, String message) {
        super(DiagnosticEngine.english(message));
        this.diagnostic = diagnostic;
    }

    public Diagnostic getDiagnostic() {
        return diagnostic;
    }
}
