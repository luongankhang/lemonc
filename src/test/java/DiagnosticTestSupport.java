import org.junit.Assert;
import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.diagnostic.DiagnosticSuggestion;

import java.util.List;

/** Stable assertions shared by diagnostic pipeline tests. */
public final class DiagnosticTestSupport {
    private DiagnosticTestSupport() {
    }

    public static Diagnostic assertSingle(List<Diagnostic> diagnostics, String code, String message,
                                          int line, int column) {
        Assert.assertEquals("diagnostic count", 1, diagnostics.size());
        Diagnostic diagnostic = diagnostics.get(0);
        Assert.assertEquals("error code", code, diagnostic.code());
        Assert.assertEquals("message", message, diagnostic.message());
        Assert.assertNotNull("primary span", diagnostic.primarySpan());
        Assert.assertEquals("start line", line, diagnostic.primarySpan().startLine());
        Assert.assertEquals("start column", column, diagnostic.primarySpan().startColumn());
        return diagnostic;
    }

    public static void assertSuggestion(Diagnostic diagnostic, String replacement) {
        Assert.assertTrue("expected a suggestion", diagnostic.suggestions().stream()
                .map(DiagnosticSuggestion::replacement)
                .anyMatch(replacement::equals));
    }
}
