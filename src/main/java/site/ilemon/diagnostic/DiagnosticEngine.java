package site.ilemon.diagnostic;

import site.ilemon.util.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/** Collects diagnostics and provides the common reporting API for compiler phases. */
public final class DiagnosticEngine {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public Diagnostic report(Severity severity, String code, String message,
                             SourceSpan span, String primaryLabel) {
        var diagnostic = Diagnostic.of(severity, code, english(message), span, primaryLabel);
        return report(diagnostic);
    }

    public DiagnosticBuilder error(String code) {
        return new DiagnosticBuilder(this, Severity.ERROR, code);
    }

    public DiagnosticBuilder warning(String code) {
        return new DiagnosticBuilder(this, Severity.WARNING, code);
    }

    public DiagnosticBuilder note(String code) {
        return new DiagnosticBuilder(this, Severity.NOTE, code);
    }

    public Diagnostic report(Diagnostic diagnostic) {
        var normalized = diagnostic;
        String englishMessage = english(diagnostic.message());
        if (!englishMessage.equals(diagnostic.message())) {
            normalized = new Diagnostic(diagnostic.severity(), diagnostic.code(), englishMessage,
                    diagnostic.primarySpan(), diagnostic.primaryLabel(),
                    diagnostic.secondaryLabels(), diagnostic.notes(), diagnostic.suggestions(), diagnostic.typeContext());
        }
        diagnostics.add(normalized);
        return normalized;
    }

    public Diagnostic error(String code, String message, SourceSpan span, String label) {
        return report(Severity.ERROR, code, message, span, label);
    }

    public Diagnostic warning(String code, String message, SourceSpan span, String label) {
        return report(Severity.WARNING, code, message, span, label);
    }

    public Diagnostic note(String code, String message, SourceSpan span, String label) {
        return report(Severity.NOTE, code, message, span, label);
    }

    public List<Diagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.ERROR);
    }

    /** Converts legacy non-English messages while phases are being migrated incrementally. */
    public static String english(String message) {
        if (message == null) return "";
        return message
                .replace("[parser]", "[parser]")
                .replace("[semantic analysis]", "[semantic analysis]")
                .replace("unknown", "unknown")
                .replace("syntax error", "syntax error")
                .replace("expected", "expected")
                .replace("but found", "but found")
                .replace("current token is", "current token is")
                .replace("class name", "class name")
                .replace("does not match file name", "does not match file name")
                .replace("array size must be an integer, but found", "array size must be an integer, but found")
                .replace("array size must be positive, but found", "array size must be positive, but found")
                .replace("unsupported array element type", "unsupported array element type")
                .replace("unsupported array parameter element type", "unsupported array parameter element type")
                .replace("invalid declaration: variable", "invalid declaration: variable")
                .replace("must be followed by", "must be followed by")
                .replace("invalid declaration: a type must be followed by a variable name", "invalid declaration: a type must be followed by a variable name")
                .replace("the first printf argument must be a format string", "the first printf argument must be a format string")
                .replace("expected an assignment or method call", "expected an assignment or method call")
                .replace("could not parse simple statement", "could not parse simple statement")
                .replace("array property must be length", "array property must be length")
                .replace("operator", "operator")
                .replace("does not support array operands", "does not support array operands")
                .replace("left and right operand types do not match", "left and right operand types do not match")
                .replace("requires both operands to be bool", "requires both operands to be bool")
                .replace("only supports int operands", "only supports int operands")
                .replace("cannot assign", "cannot assign")
                .replace("expression to", "expression to")
                .replace("typed variable", "typed variable")
                .replace("void method", "void method")
                .replace("cannot be used as an expression", "cannot be used as an expression")
                .replace("internal error", "internal error")
                .replace("variable table was not found", "variable table was not found")
                .replace("undefined variable", "undefined variable")
                .replace("may be used before assignment", "may be used before assignment")
                .replace("condition must be bool, but is", "condition must be bool, but is")
                .replace("duplicate method definition", "duplicate method definition")
                .replace("program must define void main()", "program must define void main()")
                .replace("return type must be void, but is declared as", "return type must be void, but is declared as")
                .replace("cannot declare parameters; it must be void main()", "cannot declare parameters; it must be void main()")
                .replace("does not return on all paths", "does not return on all paths")
                .replace("unsupported numeric type", "unsupported numeric type")
                .replace("argument count mismatch", "argument count mismatch")
                .replace("format string requires", "format string requires")
                .replace("actual", "actual")
                .replace("placeholder", "placeholder")
                .replace("requires", "requires")
                .replace("! operator requires a bool operand, but is", "! operator requires a bool operand, but is")
                .replace("does not allow return statements", "does not allow return statements")
                .replace("cannot return a value", "cannot return a value")
                .replace("return value type mismatch", "return value type mismatch")
                .replace("break statement must be inside a loop", "break statement must be inside a loop")
                .replace("continue statement must be inside a loop", "continue statement must be inside a loop")
                .replace("format string contains % without a placeholder", "format string contains % without a placeholder")
                .replace("does not support placeholder", "does not support placeholder")
                .replace("undefined method", "undefined method")
                .replace("has an incorrect argument count", "has an incorrect argument count")
                .replace("argument", "argument")
                .replace("has a mismatched type", "has a mismatched type")
                .replace("undefined array", "undefined array")
                .replace("is not an array; actual type is", "is not an array; actual type is")
                .replace("array index must be int, but is", "array index must be int, but is")
                .replace("array element", "array element")
                .replace("duplicate parameter", "duplicate parameter")
                .replace("duplicate variable", "duplicate variable")
                .replace("at line", "at line")
                .replaceAll("[\\p{IsHan}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
