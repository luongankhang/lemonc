package site.ilemon.diagnostic;

/** Structured type information attached to a type-checking diagnostic. */
public record TypeDiagnosticContext(String expectedType, String actualType,
                                    String expression, String context) {
    public TypeDiagnosticContext {
        expectedType = expectedType == null ? "unknown" : expectedType;
        actualType = actualType == null ? "unknown" : actualType;
        expression = expression == null ? "expression" : expression;
        context = context == null ? "type checking" : context;
    }
}
