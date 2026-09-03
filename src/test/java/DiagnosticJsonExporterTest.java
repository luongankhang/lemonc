import org.junit.Test;
import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.diagnostic.DiagnosticEngine;
import site.ilemon.diagnostic.DiagnosticJsonExporter;
import site.ilemon.util.SourceSpan;

import static org.junit.Assert.assertTrue;

public class DiagnosticJsonExporterTest {
    @Test
    public void exportsLspCompatibleStructuredDiagnostic() {
        var engine = new DiagnosticEngine();
        SourceSpan span = SourceSpan.of("main.lemon", 10, 15, 12, 15, 12, 20);
        Diagnostic diagnostic = engine.error("E3001")
                .message("type mismatch")
                .primary(span, "expression")
                .note("use an integer expression")
                .suggestion(span, "0", "replace with an integer", 0.95)
                .report();

        String json = DiagnosticJsonExporter.toJson(diagnostic);
        assertTrue(json.startsWith("[{\"code\":\"E3001\""));
        assertTrue(json.contains("\"severity\":\"ERROR\""));
        assertTrue(json.contains("\"file\":\"main.lemon\""));
        assertTrue(json.contains("\"range\":{\"start\":{\"line\":11,\"character\":14}"));
        assertTrue(json.contains("\"line\":11,\"character\":14"));
        assertTrue(json.contains("\"notes\":[\"use an integer expression\"]"));
        assertTrue(json.contains("\"replacement\":\"0\""));
    }
}
