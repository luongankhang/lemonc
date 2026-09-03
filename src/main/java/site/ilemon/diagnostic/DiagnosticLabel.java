package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

public record DiagnosticLabel(SourceSpan span, String message) {
    public DiagnosticLabel {
        if (span == null) {
            throw new IllegalArgumentException("Diagnostic labels require a source span");
        }
        message = message == null ? "" : message;
    }
}
