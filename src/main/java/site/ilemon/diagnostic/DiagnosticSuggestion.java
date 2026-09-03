package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

/** A machine-applicable replacement suggested by a diagnostic. */
public record DiagnosticSuggestion(SourceSpan span, String replacement, String message) {
    public DiagnosticSuggestion {
        if (span == null) {
            throw new IllegalArgumentException("Suggestions require a source span");
        }
        replacement = replacement == null ? "" : replacement;
        message = message == null ? "" : message;
    }
}
