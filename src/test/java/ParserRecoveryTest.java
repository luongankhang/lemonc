import org.junit.Test;
import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class ParserRecoveryTest {
    @Test
    public void reportsIndependentStatementErrorsAndKeepsTheirSpans() throws Exception {
        File directory = Files.createTempDirectory("lemonc-recovery").toFile();
        File source = new File(directory, "Recovery.lemon");
        Files.writeString(source.toPath(),
                "class Recovery { void main() { x = ; y = ; } }\n",
                StandardCharsets.UTF_8);
        try {
            Parser parser = new Parser(new Lexer(source));
            try {
                parser.parse();
                fail("Expected ParseException");
            } catch (site.ilemon.exception.ParseException expected) {
                List<Diagnostic> diagnostics = parser.getDiagnostics();
                assertEquals(2, diagnostics.size());
                assertEquals("PARSE003", diagnostics.get(0).code());
                assertEquals("PARSE003", diagnostics.get(1).code());
                assertNotEquals(diagnostics.get(0).primarySpan(), diagnostics.get(1).primarySpan());
                assertEquals(1, diagnostics.get(0).primarySpan().getStartLine());
                assertEquals(1, diagnostics.get(1).primarySpan().getStartLine());
            }
        } finally {
            Files.deleteIfExists(source.toPath());
            Files.deleteIfExists(directory.toPath());
        }
    }
}
