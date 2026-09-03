package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

import java.util.List;

/** Renders structured diagnostics in a compiler-style terminal format. */
public final class DiagnosticRenderer {
    private final SourceLineProvider sourceLines;

    public DiagnosticRenderer(SourceLineProvider sourceLines) {
        this.sourceLines = sourceLines;
    }

    public String render(Diagnostic diagnostic) {
        return render(List.of(diagnostic));
    }

    public String render(List<Diagnostic> diagnostics) {
        var output = new StringBuilder();
        for (int i = 0; i < diagnostics.size(); i++) {
            if (i > 0) {
                output.append(System.lineSeparator()).append(System.lineSeparator());
            }
            renderDiagnostic(diagnostics.get(i), output);
        }
        return output.toString();
    }

    private void renderDiagnostic(Diagnostic diagnostic, StringBuilder output) {
        output.append(diagnostic.severity().name().toLowerCase())
                .append('[').append(diagnostic.code()).append("]: ")
                .append(diagnostic.message()).append(System.lineSeparator());

        renderSpan(diagnostic.primarySpan(), diagnostic.primaryLabel(), diagnostic, output, true);
        for (DiagnosticLabel label : diagnostic.secondaryLabels()) {
            renderSpan(label.span(), label.message(), null, output, false);
        }
        for (String note : diagnostic.notes()) {
            output.append("= note: ").append(note).append(System.lineSeparator());
        }
        for (DiagnosticSuggestion suggestion : diagnostic.suggestions()) {
            output.append("= help: ").append(suggestion.message())
                    .append(" (replace with '").append(suggestion.replacement()).append("')")
                    .append(System.lineSeparator());
        }
        if (output.length() > 0 && output.charAt(output.length() - 1) == '\n') {
            output.setLength(output.length() - System.lineSeparator().length());
        }
    }

    private void renderSpan(SourceSpan span, String label, Diagnostic diagnostic,
                            StringBuilder output, boolean primary) {
        if (span == null) {
            return;
        }
        output.append(primary ? "--> " : "--> ")
                .append(span.getFileName() == null ? "<unknown>" : span.getFileName())
                .append(':').append(span.getStartLine()).append(':').append(span.getStartColumn())
                .append(System.lineSeparator());
        output.append('|').append(System.lineSeparator());
        String source = sourceLines == null ? null : sourceLines.line(span.getFileName(), span.getStartLine());
        if (source == null) {
            source = "";
        }
        output.append(span.getStartLine()).append(" | ").append(source).append(System.lineSeparator());
        output.append("| ").append(" ".repeat(Math.max(0, span.getStartColumn() - 1)));
        int width = span.getStartLine() == span.getEndLine()
                ? Math.max(1, span.getEndColumn() - span.getStartColumn())
                : Math.max(1, source.length() - span.getStartColumn() + 1);
        output.append("^".repeat(width));
        String effectiveLabel = label == null ? "" : label;
        if (diagnostic != null && diagnostic.typeContext() != null) {
            var type = diagnostic.typeContext();
            effectiveLabel += (effectiveLabel.isEmpty() ? "" : "; ")
                    + "expected `" + type.expectedType() + "`, found `" + type.actualType() + "`";
        }
        if (!effectiveLabel.isEmpty()) {
            output.append(' ').append(effectiveLabel);
        }
        output.append(System.lineSeparator());
    }
}
