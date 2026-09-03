package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

import java.util.List;

/** Immutable compiler diagnostic shared by every compiler phase. */
public record Diagnostic(Severity severity, String code, String message,
                         SourceSpan primarySpan, String primaryLabel,
                         List<DiagnosticLabel> secondaryLabels,
                         List<String> notes,
                         List<DiagnosticSuggestion> suggestions,
                         TypeDiagnosticContext typeContext) {
    public Diagnostic {
        if (severity == null || !DiagnosticCodes.isValid(code) || message == null) {
            throw new IllegalArgumentException("Diagnostic severity, code (E####), and message are required");
        }
        primaryLabel = primaryLabel == null ? "" : primaryLabel;
        secondaryLabels = List.copyOf(secondaryLabels == null ? List.of() : secondaryLabels);
        notes = List.copyOf(notes == null ? List.of() : notes);
        suggestions = List.copyOf(suggestions == null ? List.of() : suggestions);
    }

    public static Diagnostic of(Severity severity, String code, String message,
                                SourceSpan span, String primaryLabel) {
        return new Diagnostic(severity, code, message, span, primaryLabel, List.of(), List.of(), List.of(), null);
    }

    public Diagnostic withSecondaryLabel(SourceSpan span, String label) {
        var labels = new java.util.ArrayList<>(secondaryLabels);
        labels.add(new DiagnosticLabel(span, label));
        return new Diagnostic(severity, code, message, primarySpan, primaryLabel, labels, notes, suggestions, typeContext);
    }

    public Diagnostic withNote(String note) {
        var updatedNotes = new java.util.ArrayList<>(notes);
        updatedNotes.add(note);
        return new Diagnostic(severity, code, message, primarySpan, primaryLabel, secondaryLabels, updatedNotes, suggestions, typeContext);
    }
}
