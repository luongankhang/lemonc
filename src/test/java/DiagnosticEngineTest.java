import org.junit.Test;
import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.diagnostic.DiagnosticEngine;
import site.ilemon.diagnostic.Severity;
import site.ilemon.util.SourceSpan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiagnosticEngineTest {
    @Test
    public void reportsStructuredDiagnosticWithLabelsAndNotes() {
        var engine = new DiagnosticEngine();
        var primary = SourceSpan.of("Example.lemon", 2, 5, 1, 3, 1, 6);
        var secondary = SourceSpan.singlePoint("Example.lemon", 12, 2, 1);

        Diagnostic diagnostic = engine.error("E100")
                .message("invalid assignment")
                .primary(primary, "assignment")
                .type("int", "bool", "True", "assignment")
                .secondary(secondary, "declared here")
                .note("the target types must match")
                .suggestion(primary, "x", "use the declared target")
                .report();

        assertEquals(1, engine.diagnostics().size());
        Diagnostic stored = engine.diagnostics().get(0);
        assertEquals(Severity.ERROR, stored.severity());
        assertEquals("E100", stored.code());
        assertEquals(primary, stored.primarySpan());
        assertEquals(1, stored.secondaryLabels().size());
        assertEquals(1, stored.notes().size());
        assertEquals(1, stored.suggestions().size());
        assertEquals("int", stored.typeContext().expectedType());
        assertEquals("bool", stored.typeContext().actualType());
        assertTrue(engine.hasErrors());
    }
}
