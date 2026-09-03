package site.ilemon.semantic;

import site.ilemon.ast.Ast;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import site.ilemon.exception.SemanticException;
import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.diagnostic.DiagnosticEngine;
import site.ilemon.util.SourceSpan;

/**
 * Method-level variable table.
 */
public class MethodVarTable {
    private final HashMap<String, Symbol> table;
    private final DiagnosticEngine diagnosticEngine;

    public MethodVarTable() {
        this(new DiagnosticEngine());
    }

    public MethodVarTable(DiagnosticEngine diagnosticEngine) {
        this.table = new HashMap<>();
        this.diagnosticEngine = diagnosticEngine;
    }

    public void put(List<Ast.Declare.T> formals, List<Ast.Declare.T> locals) {
        for (Ast.Declare.T dec : formals) {
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null) {
                throw duplicate("duplicate parameter " + declareSingle.getId()
                        + " at line " + dec.getLineNum(), dec);
            }
            this.table.put(declareSingle.getId(), new Symbol(
                    declareSingle.getId(), declareSingle.getType(), Symbol.Kind.PARAMETER, dec.getLineNum()));
        }

        for (Ast.Declare.T dec : locals) {
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null) {
                throw duplicate("duplicate variable " + declareSingle.getId()
                        + " at line " + dec.getLineNum(), dec);
            }
            this.table.put(declareSingle.getId(), new Symbol(
                    declareSingle.getId(), declareSingle.getType(), Symbol.Kind.LOCAL, dec.getLineNum()));
        }
    }

    private SemanticException duplicate(String message, Ast.Declare.T declaration) {
        Diagnostic diagnostic = diagnosticEngine.error("SEM-DUPLICATE-DECLARATION")
                .message(message)
                .primary(declaration.getSpan() == null
                        ? SourceSpan.singlePoint(null, 0, Math.max(1, declaration.getLineNum()), 1)
                        : declaration.getSpan(), "duplicate declaration")
                .report();
        return new SemanticException(diagnostic);
    }

    public Ast.Type.T get(String id) {
        Symbol symbol = this.table.get(id);
        return symbol == null ? null : symbol.getType();
    }

    public Symbol resolve(String id) {
        return this.table.get(id);
    }

    public Set<String> names() {
        return Set.copyOf(table.keySet());
    }

    public Ast.Type.T put(String key, Ast.Type.T value) {
        Symbol previous = this.table.put(key, new Symbol(key, value, Symbol.Kind.LOCAL, -1));
        return previous == null ? null : previous.getType();
    }
}
