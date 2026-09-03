package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/** Fluent, extensible builder for diagnostics emitted by a compiler phase. */
public final class DiagnosticBuilder {
    private final DiagnosticEngine engine;
    private final Severity severity;
    private final String code;
    private String message = "";
    private SourceSpan primarySpan;
    private String primaryLabel = "";
    private final List<DiagnosticLabel> secondaryLabels = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final List<DiagnosticSuggestion> suggestions = new ArrayList<>();
    private TypeDiagnosticContext typeContext;

    DiagnosticBuilder(DiagnosticEngine engine, Severity severity, String code) {
        this.engine = engine;
        this.severity = severity;
        this.code = code;
    }

    public DiagnosticBuilder message(String message) {
        this.message = message;
        return this;
    }

    public DiagnosticBuilder primary(SourceSpan span, String label) {
        this.primarySpan = span;
        this.primaryLabel = label;
        return this;
    }

    public DiagnosticBuilder secondary(SourceSpan span, String label) {
        this.secondaryLabels.add(new DiagnosticLabel(span, label));
        return this;
    }

    public DiagnosticBuilder note(String note) {
        this.notes.add(note);
        return this;
    }

    public DiagnosticBuilder suggestion(SourceSpan span, String replacement, String message) {
        return suggestion(span, replacement, message, 1.0);
    }

    public DiagnosticBuilder suggestion(DiagnosticSuggestion suggestion) {
        if (suggestion != null && suggestion.confidence() >= 0.75) {
            this.suggestions.add(suggestion);
        }
        return this;
    }

    public DiagnosticBuilder suggestion(SourceSpan span, String replacement, String message, double confidence) {
        if (confidence >= 0.75) {
            this.suggestions.add(new DiagnosticSuggestion(span, replacement, message, confidence));
        }
        return this;
    }

    public DiagnosticBuilder suggestion(SourceSpan span, String replacement) {
        return suggestion(span, replacement, "replace with '" + replacement + "'");
    }

    public DiagnosticBuilder type(String expected, String actual, String expression, String context) {
        this.typeContext = new TypeDiagnosticContext(expected, actual, expression, context);
        return this;
    }

    public Diagnostic build() {
        return new Diagnostic(severity, code, DiagnosticEngine.english(message), primarySpan,
                primaryLabel, secondaryLabels, notes, suggestions, typeContext);
    }

    public Diagnostic report() {
        return engine.report(build());
    }
}
