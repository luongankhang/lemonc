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
                .replace("[语法分析]", "[parser]")
                .replace("[语义分析]", "[semantic analysis]")
                .replace("未知", "unknown")
                .replace("语法错误", "syntax error")
                .replace("期望", "expected")
                .replace("实际得到", "but found")
                .replace("当前 token 为", "current token is")
                .replace("类名", "class name")
                .replace("与文件名", "does not match file name")
                .replace("不一致", "")
                .replace("数组大小必须是整数，但得到", "array size must be an integer, but found")
                .replace("数组大小必须为正整数，但得到", "array size must be positive, but found")
                .replace("不支持的数组基础类型", "unsupported array element type")
                .replace("不支持的数组参数基础类型", "unsupported array parameter element type")
                .replace("声明语句格式错误，变量", "invalid declaration: variable")
                .replace("后期望", "must be followed by")
                .replace("声明语句格式错误，类型后必须跟变量名", "invalid declaration: a type must be followed by a variable name")
                .replace("printf 的第一个参数必须是字符串格式", "the first printf argument must be a format string")
                .replace("期望赋值语句或方法调用", "expected an assignment or method call")
                .replace("无法解析简单语句", "could not parse simple statement")
                .replace("数组属性只支持 length", "array property must be length")
                .replace("运算符", "operator")
                .replace("不支持数组操作数", "does not support array operands")
                .replace("左右操作数类型不匹配", "left and right operand types do not match")
                .replace("要求左右操作数都是 bool", "requires both operands to be bool")
                .replace("只支持 int 操作数", "only supports int operands")
                .replace("不能将", "cannot assign")
                .replace("类型的表达式赋值给", "expression to")
                .replace("类型的变量", "typed variable")
                .replace("void 方法", "void method")
                .replace("不能作为表达式使用", "cannot be used as an expression")
                .replace("内部错误", "internal error")
                .replace("变量表未找到", "variable table was not found")
                .replace("未定义的变量", "undefined variable")
                .replace("在使用前未赋值", "may be used before assignment")
                .replace("条件必须是 bool，实际为", "condition must be bool, but is")
                .replace("重复定义的方法", "duplicate method definition")
                .replace("程序必须定义 void main()", "program must define void main()")
                .replace("返回类型必须是 void，实际声明为", "return type must be void, but is declared as")
                .replace("不能声明参数，必须是 void main()", "cannot declare parameters; it must be void main()")
                .replace("不是所有路径都有 return", "does not return on all paths")
                .replace("不支持的数字类型", "unsupported numeric type")
                .replace("参数个数不匹配", "argument count mismatch")
                .replace("格式串需要", "format string requires")
                .replace("实际", "actual")
                .replace("占位符", "placeholder")
                .replace("需要", "requires")
                .replace("! 运算符要求操作数是 bool，实际为", "! operator requires a bool operand, but is")
                .replace("不允许 return 语句", "does not allow return statements")
                .replace("不能返回值", "cannot return a value")
                .replace("返回值类型不匹配", "return value type mismatch")
                .replace("break语句必须包含在循环体中。", "break statement must be inside a loop")
                .replace("continue语句必须包含在循环体中。", "continue statement must be inside a loop")
                .replace("格式串中的 % 缺少占位符", "format string contains % without a placeholder")
                .replace("不支持占位符", "does not support placeholder")
                .replace("未定义的方法", "undefined method")
                .replace("的参数个数不正确", "has an incorrect argument count")
                .replace("的第", "argument")
                .replace("个参数类型不匹配", "has a mismatched type")
                .replace("未定义的数组", "undefined array")
                .replace("不是数组，实际类型为", "is not an array; actual type is")
                .replace("数组下标必须是 int，实际为", "array index must be int, but is")
                .replace("数组元素", "array element")
                .replace("重复的参数", "duplicate parameter")
                .replace("重复的变量", "duplicate variable")
                .replace("在行", "at line")
                .replaceAll("[\\p{IsHan}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
