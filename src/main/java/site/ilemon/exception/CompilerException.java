package site.ilemon.exception;

import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.diagnostic.DiagnosticEngine;
import site.ilemon.diagnostic.Severity;
import site.ilemon.diagnostic.DiagnosticCodes;

public class CompilerException extends RuntimeException {
    private final Diagnostic diagnostic;

    public CompilerException(String message) {
        this(internalDiagnostic(message));
    }

    private static Diagnostic internalDiagnostic(String message) {
        var engine = new DiagnosticEngine();
        return engine.error(DiagnosticCodes.INTERNAL_COMPILER_ERROR)
                .message(message)
                .primary(null, "compiler error")
                .report();
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
