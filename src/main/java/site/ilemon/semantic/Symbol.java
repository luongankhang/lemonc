package site.ilemon.semantic;

import site.ilemon.ast.Ast;

public record Symbol(String name, Ast.Type.T type, Kind kind, int lineNumber) {
    public enum Kind {
        METHOD,
        PARAMETER,
        LOCAL
    }

    public String getName() { return name; }
    public Ast.Type.T getType() { return type; }
    public Kind getKind() { return kind; }
    public int getLineNumber() { return lineNumber; }
}
