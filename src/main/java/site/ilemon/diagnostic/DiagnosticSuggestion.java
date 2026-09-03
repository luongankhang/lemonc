package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

/** A machine-applicable replacement suggested by a diagnostic. */
public record DiagnosticSuggestion(SourceSpan span, String replacement, String message, double confidence) {
    public DiagnosticSuggestion {
        if (span == null) {
            throw new IllegalArgumentException("Suggestions require a source span");
        }
        replacement = replacement == null ? "" : replacement;
        message = message == null ? "" : message;
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Suggestion confidence must be between 0 and 1");
        }
    }

    public DiagnosticSuggestion(SourceSpan span, String replacement, String message) {
        this(span, replacement, message, 1.0);
    }
}
