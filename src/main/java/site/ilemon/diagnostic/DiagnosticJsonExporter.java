package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

import java.util.List;

/** Exports diagnostics as dependency-free JSON with LSP-compatible ranges. */
public final class DiagnosticJsonExporter {
    private DiagnosticJsonExporter() {
    }

    public static String toJson(Diagnostic diagnostic) {
        return toJson(List.of(diagnostic));
    }

    public static String toJson(List<Diagnostic> diagnostics) {
        var json = new StringBuilder("[");
        for (int i = 0; i < diagnostics.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            appendDiagnostic(json, diagnostics.get(i));
        }
        return json.append(']').toString();
    }

    private static void appendDiagnostic(StringBuilder json, Diagnostic diagnostic) {
        json.append("{\"code\":").append(string(diagnostic.code()))
                .append(",\"severity\":").append(string(diagnostic.severity().name()))
                .append(",\"message\":").append(string(diagnostic.message()))
                .append(",\"file\":");
        if (diagnostic.primarySpan() == null) {
            json.append("null").append(",\"range\":null");
        } else {
            json.append(string(diagnostic.primarySpan().fileName())).append(",\"range\":");
            appendRange(json, diagnostic.primarySpan());
        }
        json.append(",\"primary\":");
        appendLabel(json, diagnostic.primarySpan(), diagnostic.primaryLabel());

        json.append(",\"secondary\":[");
        for (int i = 0; i < diagnostic.secondaryLabels().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            DiagnosticLabel label = diagnostic.secondaryLabels().get(i);
            appendLabel(json, label.span(), label.message());
        }
        json.append("],\"notes\":[");
        for (int i = 0; i < diagnostic.notes().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(string(diagnostic.notes().get(i)));
        }
        json.append("],\"suggestions\":[");
        for (int i = 0; i < diagnostic.suggestions().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            DiagnosticSuggestion suggestion = diagnostic.suggestions().get(i);
            json.append("{\"range\":");
            appendRange(json, suggestion.span());
            json.append(",\"replacement\":").append(string(suggestion.replacement()))
                    .append(",\"message\":").append(string(suggestion.message()))
                    .append(",\"confidence\":").append(suggestion.confidence()).append('}');
        }
        json.append("]}");
    }

    private static void appendLabel(StringBuilder json, SourceSpan span, String label) {
        if (span == null) {
            json.append("null");
            return;
        }
        json.append("{\"file\":").append(string(span.fileName()))
                .append(",\"range\":");
        appendRange(json, span);
        json.append(",\"label\":").append(string(label)).append('}');
    }

    private static void appendRange(StringBuilder json, SourceSpan span) {
        json.append("{\"start\":{\"line\":").append(span.startLine() - 1)
                .append(",\"character\":").append(span.startColumn() - 1)
                .append("},\"end\":{\"line\":").append(span.endLine() - 1)
                .append(",\"character\":").append(span.endColumn() - 1).append("}}");
    }

    private static String string(String value) {
        if (value == null) {
            return "null";
        }
        var escaped = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }
}
