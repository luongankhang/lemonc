package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.ast.Ast.Type.TypeKind;
import site.ilemon.exception.SemanticException;
import site.ilemon.diagnostic.Diagnostic;
import site.ilemon.diagnostic.DiagnosticCodes;
import site.ilemon.diagnostic.DiagnosticEngine;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


/**
 * 语义分析 Visitor。
 *
 * <p>基于 Visitor 模式遍历 AST，执行以下静态语义检查：</p>
 * <ul>
 *   <li><b>类型检查</b>：赋值、运算、方法调用的类型一致性</li>
 *   <li><b>变量检查</b>：未声明变量、使用前未赋值</li>
 *   <li><b>方法检查</b>：未定义方法、重复定义、参数个数/类型匹配</li>
 *   <li><b>控制流检查</b>：if/while 条件必须是 bool 类型</li>
 *   <li><b>返回值检查</b>：返回类型与方法声明一致、main 方法必须是 void</li>
 * </ul>
 *
 * <p>发现错误时抛出 {@link SemanticException}，错误信息格式为
 * {@code [语义分析] 行 N: 错误描述}。</p>
 *
 * @author andy
 * @see site.ilemon.visitor.ISemanticVisitor
 * @see SemanticException
 */
public class SemanticVisitor implements ISemanticVisitor {

    private boolean pass = true;

    private final boolean collectErrors;
    private final DiagnosticEngine diagnosticEngine = new DiagnosticEngine();

    private final ArrayList<String> errors = new ArrayList<>();

    private final ArrayList<Integer> errorLineNumbers = new ArrayList<>();

    private Ast.Type.T currType;

    private String currMethodName;

    private HashMap<String,MethodVarTable> methodVarTable;

    private HashMap<String,Ast.Type.T> methodNameRetTypeMap;

    private HashSet<String> currMethodLocalVar;

    private int loopDepth = 0;

    private HashMap<String,Ast.Method.MethodSingle> methodMap;

    private Ast.Type.T typeOfMethodDeclared;

    public SemanticVisitor(){
        this(false);
    }

    private SemanticVisitor(boolean collectErrors){

        this.collectErrors = collectErrors;
        this.methodVarTable = new HashMap<>();
        this.methodMap = new HashMap<>();
        this.methodNameRetTypeMap = new HashMap<>();
    }

    public static SemanticVisitor collecting() {
        return new SemanticVisitor(true);
    }

    public DiagnosticEngine getDiagnosticEngine() {
        return diagnosticEngine;
    }

    public java.util.List<Diagnostic> getDiagnostics() {
        return diagnosticEngine.diagnostics();
    }

    public ArrayList<String> getErrors() {
        return new ArrayList<>(this.errors);
    }

    public ArrayList<Integer> getErrorLineNumbers() {
        return new ArrayList<>(this.errorLineNumbers);
    }

    public boolean passOrNot(){
        return pass;
    }

    private Ast.Type.T unknownType() {
        return new Ast.Type.Int();
    }

    private String typeName(Ast.Type.T type) {
        if (type == null) {
            return "未知";
        }
        String name = type.toString();
        return name.startsWith("@") ? name.substring(1) : name;
    }

    private void checkSameOperandTypes(int lineNum, String operator, Ast.Type.T leftType, Ast.Type.T rightType) {
        if (isArrayType(leftType) || isArrayType(rightType)) {
            error(lineNum, String.format(
                    "运算符 '%s' 不支持数组操作数：左侧为 %s，右侧为 %s",
                    operator, typeName(leftType), typeName(rightType)));
        }
        Ast.Type.T promoted = promoteNumeric(leftType, rightType);
        if (promoted != null) {
            this.currType = promoted;
            return;
        }
        if (!isMatch(leftType, rightType)) {
            typeError(DiagnosticCodes.TYPE_OPERATOR, typeName(leftType), typeName(rightType), "binary expression",
                    lineNum, null, "operator '" + operator + "'", "operand types must match or use a supported numeric conversion");
        }
        this.currType = leftType;
    }

    private void checkBooleanOperandTypes(int lineNum, String operator, Ast.Type.T leftType, Ast.Type.T rightType) {
        if (leftType == null || rightType == null ||
                leftType.getKind() != TypeKind.BOOL || rightType.getKind() != TypeKind.BOOL) {
            typeError(DiagnosticCodes.TYPE_OPERATOR, "bool", typeName(leftType) + " and " + typeName(rightType),
                    "binary expression", lineNum, null, "operator '" + operator + "'", "use boolean operands");
        }
    }

    @Override
    public void visit(Ast.Expr.Add obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "+", leftType, this.currType);
    }

    @Override
    public void visit(Ast.Expr.And obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        checkBooleanOperandTypes(obj.getLineNum(), "&&", leftType, this.currType);
    }

    @Override
    public void visit(Ast.Type.Bool obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.Byte obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.Long obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Stmt.Assign obj) {
        if(obj.getExpr() instanceof Ast.Expr.T){
            this.visit((Ast.Expr.T)obj.getExpr());
            Ast.Type.T exprType = null;
            if( obj.getExpr() instanceof Ast.Expr.Call)
                exprType = ((Ast.Expr.Call) obj.getExpr()).getReturnType();
            else
                exprType = this.currType;
            if( this.currMethodLocalVar.contains(obj.getId().getId()))
                this.currMethodLocalVar.remove(obj.getId().getId());
            this.visit(obj.getId());
            Ast.Type.T targetType = this.currType;
            if (isArrayType(this.currType) || isArrayType(exprType)) {
                typeError(DiagnosticCodes.TYPE_ASSIGNMENT, typeName(targetType), typeName(exprType),
                        expressionName(obj.getExpr()), obj.getLineNum(), obj.getSpan(),
                        "array assignment", "arrays cannot be assigned as whole values");
                return;
            }
            if (!isAssignable(targetType, exprType, obj.getExpr())) {
                if (!byteRangeErrorIfNeeded(targetType, exprType, obj.getExpr(), obj.getLineNum(), obj.getSpan(),
                        "assignment to '" + obj.getId().getId() + "'")) {
                    typeError(DiagnosticCodes.TYPE_ASSIGNMENT, typeName(targetType), typeName(exprType),
                            expressionName(obj.getExpr()), obj.getLineNum(), obj.getSpan(),
                            "assignment to '" + obj.getId().getId() + "'", null);
                }
            }
        }

    }

    @Override
    public void visit(Ast.Stmt.Block obj) {
        for( Ast.Stmt.T stmt : obj.getStmts()){
            this.visit(stmt);
        }
    }

    @Override
    public void visit(Ast.Expr.Call obj) {
        Ast.Type.T returnType = validateMethodCall(obj.getName(), obj.getInputParams(), obj.getLineNum(), obj.getSpan());
        if (returnType.getKind() == TypeKind.VOID) {
            semanticError(DiagnosticCodes.SEM_INVALID_SYMBOL_USAGE, "void method '" + obj.getName()
                    + "' cannot be used as an expression", obj.getLineNum(), obj.getSpan(),
                    "void function used as value", "call a non-void function instead", null);
        }
        obj.setReturnType(returnType);
        this.currType = returnType;
    }

    @Override
    public void visit(Ast.Declare.T obj) {
        if (obj instanceof Ast.Declare.DeclareSingle) {
            this.currType = ((Ast.Declare.DeclareSingle) obj).getType();
        }
    }

    @Override
    public void visit(Ast.Expr.Div obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "/", leftType, this.currType);
    }

    @Override
    public void visit(Ast.Expr.Mod obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        Ast.Type.T rightType = this.currType;
        if (leftType == null || rightType == null
                || !isIntegerLike(leftType) || !isIntegerLike(rightType)) {
            typeError(DiagnosticCodes.TYPE_OPERATOR, "int or byte", typeName(leftType) + " and " + typeName(rightType),
                    expressionName(obj), obj.getLineNum(), obj.getSpan(), "operator '%'", "the remainder operator requires int operands");
        }
        this.currType = new Ast.Type.Int();
    }

    @Override
    public void visit(Ast.Type.Float obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.Double obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Expr obj) {

    }

    @Override
    public void visit(Ast.Expr.GT obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), ">", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.Id obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if( mTable == null )
            internalError(obj.getLineNum(), "internal error: variable table for method '" + currMethodName + "' was not found");
        if (mTable == null) {
            this.currType = unknownType();
            obj.setType(this.currType);
            return;
        }
        if( mTable.get(obj.getId()) == null )
            semanticError(DiagnosticCodes.SEM_UNKNOWN_VARIABLE, "undefined variable: " + obj.getId(),
                    obj.getLineNum(), obj.getSpan(), "unknown variable",
                    "the name is not declared in the current method scope",
                    nearestName(obj.getId(), mTable.names()));
        if (mTable.get(obj.getId()) == null) {
            this.currType = unknownType();
            obj.setType(this.currType);
            return;
        }
        if( currMethodLocalVar.contains(obj.getId()))
            error(obj.getLineNum(),String.format("变量 '%s' 在使用前未赋值",obj.getId()));
        if( obj.getType() == null ) {
            // 类型可能在 Parser 阶段未成功解析（变量未在 varTable 中注册）
            obj.setType(mTable.get(obj.getId()));
        }
        this.currType = obj.getType();
    }

    @Override
    public void visit(Ast.Stmt.If obj) {
        this.visit(obj.getCondition());
        if (this.currType.getKind() != TypeKind.BOOL)
            typeError(DiagnosticCodes.TYPE_CONDITION, "bool", typeName(this.currType), expressionName(obj.getCondition()),
                    obj.getCondition().getLineNum(), obj.getCondition().getSpan(), "if condition", null);

        HashSet<String> before = new HashSet<>(this.currMethodLocalVar);
        this.currMethodLocalVar = new HashSet<>(before);
        this.visit(obj.getThenStmt());
        HashSet<String> thenUnassigned = new HashSet<>(this.currMethodLocalVar);

        if (obj.getElseStmt() != null) {
            this.currMethodLocalVar = new HashSet<>(before);
            this.visit(obj.getElseStmt());
            HashSet<String> elseUnassigned = new HashSet<>(this.currMethodLocalVar);
            thenUnassigned.addAll(elseUnassigned);
            this.currMethodLocalVar = thenUnassigned;
        } else {
            this.currMethodLocalVar = before;
        }
    }

    @Override
    public void visit(Ast.Type.Int obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Program.T programSingle) {
        this.visit(((Ast.Program.ProgramSingle)programSingle).getMainClass());
    }

    @Override
    public void visit(Ast.Expr.LT obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), "<", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.LTE obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), "<=", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.GTE obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), ">=", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.EQ obj) {
        checkComparison(obj.getLeft(), obj.getRight(), "==", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.NEQ obj) {
        checkComparison(obj.getLeft(), obj.getRight(), "!=", obj.getLineNum());
    }

    @Override
    public void visit(Ast.MainClass.T obj) {
        Ast.MainClass.MainClassSingle mainClassSingle = (Ast.MainClass.MainClassSingle) obj;
        for(int i = 0; i < mainClassSingle.getMethods().size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.getMethods().get(i);
            if( methodMap.containsKey(method.getId())){
                semanticError(DiagnosticCodes.SEM_DUPLICATE_DECLARATION, "duplicate method declaration: " + method.getId(),
                        method.getLineNum(), method.getSpan(), "duplicate method", "the method was declared earlier", null);
            }else{
                methodMap.put(method.getId(),method);
                methodNameRetTypeMap.put(method.getId(),method.getRetType());
            }
        }
        validateMainMethod();
        for(int i = 0; i < mainClassSingle.getMethods().size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.getMethods().get(i);
            this.visit(method);
        }
    }

    private void validateMainMethod() {
        Ast.Method.MethodSingle main = this.methodMap.get("main");
        if (main == null) {
            error(1, "program must define void main()");
        }
        if (main == null) {
            return;
        }
        if (main.getRetType().getKind() != TypeKind.VOID) {
            error(main.getLineNum(), "main 方法的返回类型必须是 void，实际声明为: "
                    + typeName(main.getRetType()));
        }
        if (main.getFormals() != null && !main.getFormals().isEmpty()) {
            error(main.getLineNum(), "main 方法不能声明参数，必须是 void main()");
        }
    }

    @Override
    public void visit(Ast.Method.MethodSingle obj) {
        MethodVarTable mTable = new MethodVarTable(diagnosticEngine);
        this.currMethodLocalVar = new HashSet<>();
        for( Ast.Declare.T dec : obj.getLocals()){
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (!isArrayType(declareSingle.getType())) {
                this.currMethodLocalVar.add(declareSingle.getId());
            }
        }

        mTable.put(obj.getFormals(),obj.getLocals());
        this.methodVarTable.put(obj.getId(),mTable);
        this.currMethodName = obj.getId();
        this.typeOfMethodDeclared = obj.getRetType();

        if( obj.getId().equals("main")){
            if( obj.getRetType().getKind() != TypeKind.VOID)
                error(obj.getLineNum(), "main 方法的返回类型必须是 void，实际声明为: " + typeName(obj.getRetType()));
        }
        for( int i = 0; i < obj.getStms().size(); i++){
            Ast.Stmt.T stmt = obj.getStms().get(i);
            this.visit(stmt);
        }
        if( !obj.getId().equals("main")
                && obj.getRetType().getKind() != TypeKind.VOID
                && !statementsMustReturn(obj.getStms()) ){
            error(obj.getLineNum(), "非 void 方法 '" + obj.getId() + "' 不是所有路径都有 return");
        }
    }

    @Override
    public void visit(Ast.Expr.Mul obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "*", leftType, this.currType);


    }

    @Override
    public void visit(Ast.Expr.Number obj) {
        if(obj.getType() instanceof Ast.Type.Int){
            this.currType = new Ast.Type.Int();
        }else if(obj.getType() instanceof Ast.Type.Float){
            this.currType = new Ast.Type.Float();
        }else if(obj.getType() instanceof Ast.Type.Long){
            this.currType = new Ast.Type.Long();
        }else if(obj.getType() instanceof Ast.Type.Double){
            this.currType = new Ast.Type.Double();
        }else{
            // 不支持的数字类型
            error(obj.getLineNum(),"不支持的数字类型: " + typeName(obj.getType()));
        }
    }

    @Override
    public void visit(Ast.Expr.Or obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        checkBooleanOperandTypes(obj.getLineNum(), "||", leftType, this.currType);
    }



    @Override
    public void visit(Ast.Type.Str obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Expr.Sub obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "-", leftType, this.currType);
    }

    @Override
    public void visit(Ast.Type obj) {

    }

    @Override
    public void visit(Ast.Type.Void obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Stmt.T obj) {
        obj.accept(this);
    }

    @Override
    public void visit(Ast.Stmt.Printf obj) {
        ArrayList<Character> placeholders = printfPlaceholders(obj.getFormat(), obj.getLineNum());
        int argCount = obj.getExprs() == null ? 0 : obj.getExprs().size();
        if (placeholders.size() != argCount) {
            error(obj.getLineNum(), String.format(
                    "printf 参数个数不匹配：格式串需要 %d 个，实际 %d 个",
                    placeholders.size(), argCount));
        }
        for (int i = 0; i < argCount; i++) {
            Ast.Expr.T expr = obj.getExprs().get(i);
            this.visit(expr);
            char placeholder = placeholders.get(i);
            if (placeholder == 'd' && !isIntegerLike(this.currType)) {
                typeError(DiagnosticCodes.TYPE_FORMAT, "int or byte", typeName(this.currType), expressionName(expr),
                        expr.getLineNum(), expr.getSpan(), "printf %d argument", null);
            }
            if (placeholder == 'f'
                    && this.currType.getKind() != TypeKind.FLOAT
                    && this.currType.getKind() != TypeKind.DOUBLE) {
                typeError(DiagnosticCodes.TYPE_FORMAT, "float or double", typeName(this.currType), expressionName(expr),
                        expr.getLineNum(), expr.getSpan(), "printf %f argument", null);
            }
        }
    }

    @Override
    public void visit(Ast.Stmt.PrintLine obj) {

    }

    @Override
    public void visit(Ast.Expr.T obj) {
        obj.accept(this);
    }

    @Override
    public void visit(Ast.Expr.True obj) {
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.False obj) {
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.Not obj) {
        this.visit(obj.getExpr());
        if( this.currType.getKind() != TypeKind.BOOL)
            typeError(DiagnosticCodes.TYPE_OPERATOR, "bool", typeName(this.currType), expressionName(obj.getExpr()),
                    obj.getLineNum(), obj.getSpan(), "operator '!'", "use a boolean expression");
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.Str obj) {
        this.currType = new Ast.Type.Str();
    }

    @Override
    public void visit(Ast.Type.T obj) {

    }

    @Override
    public void visit(Ast.Stmt.Return obj) {
        if( "main".equals(this.currMethodName) ){
            error(obj.getLineNum(), "main 方法不允许 return 语句");
        }
        if( this.typeOfMethodDeclared != null
                && this.typeOfMethodDeclared.getKind() == TypeKind.VOID ){
            error(obj.getLineNum(), "void 方法不能返回值");
        }
        this.visit(obj.getExpr());
        if (!isAssignable(typeOfMethodDeclared, this.currType, obj.getExpr())) {
            if (!byteRangeErrorIfNeeded(typeOfMethodDeclared, this.currType, obj.getExpr(), obj.getLineNum(), obj.getSpan(),
                    "return statement")) {
                typeError(DiagnosticCodes.TYPE_RETURN, typeName(typeOfMethodDeclared), typeName(this.currType),
                        expressionName(obj.getExpr()), obj.getLineNum(), obj.getSpan(), "return statement", null);
            }
        }
    }


    @Override
    public void visit(Ast.Stmt.While obj) {
        this.visit(obj.getCondition());
        if( this.currType.getKind() != TypeKind.BOOL )
            typeError(DiagnosticCodes.TYPE_CONDITION, "bool", typeName(this.currType), expressionName(obj.getCondition()),
                    obj.getCondition().getLineNum(), obj.getCondition().getSpan(), "while condition", null);
        HashSet<String> before = new HashSet<>(this.currMethodLocalVar);
        loopDepth++;
        this.currMethodLocalVar = new HashSet<>(before);
        this.visit(obj.getBody());
        loopDepth--;
        this.currMethodLocalVar = before;
    }

    @Override
    public void visit(Ast.Stmt.For obj) {
        if (obj.getInit() != null) {
            this.visit(obj.getInit());
        }
        this.visit(obj.getCondition());
        if( this.currType.getKind() != TypeKind.BOOL )
            typeError(DiagnosticCodes.TYPE_CONDITION, "bool", typeName(this.currType), expressionName(obj.getCondition()),
                    obj.getCondition().getLineNum(), obj.getCondition().getSpan(), "for condition", null);
        HashSet<String> before = new HashSet<>(this.currMethodLocalVar);
        loopDepth++;
        this.currMethodLocalVar = new HashSet<>(before);
        this.visit(obj.getBody());
        if (obj.getUpdate() != null) {
            this.visit(obj.getUpdate());
        }
        loopDepth--;
        this.currMethodLocalVar = before;
    }

    @Override
    public void visit(Ast.Stmt.Break obj) {
        if (loopDepth <= 0)
            error(obj.getLineNum(), "break语句必须包含在循环体中。");
    }

    @Override
    public void visit(Ast.Stmt.Continue obj) {
        if (loopDepth <= 0)
            error(obj.getLineNum(), "continue语句必须包含在循环体中。");
    }

    @Override
    public void visit(Ast.Stmt.Call obj) {
        Ast.Type.T returnType = validateMethodCall(obj.getName(), obj.getInputParams(), obj.getLineNum(), obj.getSpan());
        obj.setReturnType(returnType);
        this.currType = returnType;
    }

    private static class FlowResult {
        final boolean canCompleteNormally;
        final boolean mustReturn;

        FlowResult(boolean canCompleteNormally, boolean mustReturn) {
            this.canCompleteNormally = canCompleteNormally;
            this.mustReturn = mustReturn;
        }
    }

    private boolean statementsMustReturn(ArrayList<Ast.Stmt.T> statements) {
        return flowOfStatements(statements).mustReturn;
    }

    private FlowResult flowOfStatements(ArrayList<Ast.Stmt.T> statements) {
        if (statements == null || statements.isEmpty()) {
            return new FlowResult(true, false);
        }
        boolean canCompleteNormally = true;
        for (Ast.Stmt.T statement : statements) {
            if (!canCompleteNormally) {
                break;
            }
            FlowResult result = flowOfStatement(statement);
            canCompleteNormally = result.canCompleteNormally;
            if (result.mustReturn) {
                return new FlowResult(false, true);
            }
        }
        return new FlowResult(canCompleteNormally, false);
    }

    private FlowResult flowOfStatement(Ast.Stmt.T statement) {
        if (statement instanceof Ast.Stmt.Return) {
            return new FlowResult(false, true);
        }
        if (statement instanceof Ast.Stmt.Block) {
            return flowOfStatements(((Ast.Stmt.Block) statement).getStmts());
        }
        if (statement instanceof Ast.Stmt.If) {
            Ast.Stmt.If ifStmt = (Ast.Stmt.If) statement;
            if (ifStmt.getElseStmt() == null) {
                return new FlowResult(true, false);
            }
            FlowResult thenFlow = flowOfStatement(ifStmt.getThenStmt());
            FlowResult elseFlow = flowOfStatement(ifStmt.getElseStmt());
            return new FlowResult(
                    thenFlow.canCompleteNormally || elseFlow.canCompleteNormally,
                    thenFlow.mustReturn && elseFlow.mustReturn);
        }
        return new FlowResult(true, false);
    }

    private ArrayList<Character> printfPlaceholders(String format, int lineNum) {
        ArrayList<Character> placeholders = new ArrayList<>();
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) != '%') {
                continue;
            }
            if (i + 1 >= format.length()) {
                error(lineNum, "printf 格式串中的 % 缺少占位符");
            }
            char placeholder = format.charAt(++i);
            if (placeholder == 'd' || placeholder == 'f') {
                placeholders.add(placeholder);
            } else {
                error(lineNum, "printf 不支持占位符 %" + placeholder);
            }
        }
        return placeholders;
    }

    private void error(int lineNum, String msg){
        this.pass = false;
        Diagnostic diagnostic = diagnosticEngine.error(DiagnosticCodes.SEM_GENERAL)
                .message(msg)
                .primary(site.ilemon.util.SourceSpan.singlePoint(null, 0, Math.max(1, lineNum), 1), "here")
                .report();
        if (this.collectErrors) {
            this.errors.add(diagnostic.message());
            this.errorLineNumbers.add(lineNum);
            return;
        }
        throw new SemanticException(diagnostic);
    }

    private void internalError(int lineNum, String message) {
        this.pass = false;
        Diagnostic diagnostic = diagnosticEngine.error(DiagnosticCodes.INTERNAL_COMPILER_ERROR)
                .message(message)
                .primary(site.ilemon.util.SourceSpan.singlePoint(null, 0, Math.max(1, lineNum), 1), "internal compiler error")
                .report();
        if (this.collectErrors) {
            this.errors.add(diagnostic.message());
            this.errorLineNumbers.add(lineNum);
            return;
        }
        throw new SemanticException(diagnostic);
    }

    private void typeError(String code, String expected, String actual, String expression,
                           int lineNum, site.ilemon.util.SourceSpan span, String context, String note) {
        this.pass = false;
        var builder = diagnosticEngine.error(code)
                .message("type mismatch: expected " + expected + ", but found " + actual)
                .type(expected, actual, expression, context)
                .primary(span == null
                        ? site.ilemon.util.SourceSpan.singlePoint(null, 0, Math.max(1, lineNum), 1)
                        : span, context);
        if (note != null) {
            builder.note(note);
        }
        Diagnostic diagnostic = builder.report();
        if (this.collectErrors) {
            this.errors.add(diagnostic.message());
            this.errorLineNumbers.add(lineNum);
            return;
        }
        throw new SemanticException(diagnostic);
    }

    private String expressionName(Ast.Expr.T expression) {
        return expression == null ? "expression" : expression.getClass().getSimpleName();
    }

    /** Returns a spelling suggestion only when the candidate is unambiguously close. */
    private String nearestName(String unknown, java.util.Set<String> candidates) {
        if (unknown == null || unknown.isEmpty() || candidates == null || candidates.isEmpty()) {
            return null;
        }
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        boolean tied = false;
        for (String candidate : candidates) {
            if (candidate == null || candidate.equals(unknown)) {
                continue;
            }
            int distance = editDistance(unknown, candidate);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
                tied = false;
            } else if (distance == bestDistance) {
                tied = true;
            }
        }
        int threshold = Math.max(1, unknown.length() / 3);
        return best != null && !tied && bestDistance <= threshold ? best : null;
    }

    private int editDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitution = previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private void semanticError(String code, String message, int lineNum,
                               site.ilemon.util.SourceSpan span, String primaryLabel,
                               String note, String suggestion) {
        this.pass = false;
        var builder = diagnosticEngine.error(code)
                .message(message)
                .primary(span == null
                        ? site.ilemon.util.SourceSpan.singlePoint(null, 0, Math.max(1, lineNum), 1)
                        : span, primaryLabel);
        if (note != null) {
            builder.note(note);
        }
        if (suggestion != null && span != null) {
            builder.suggestion(span, suggestion);
        }
        Diagnostic diagnostic = builder.report();
        if (this.collectErrors) {
            this.errors.add(diagnostic.message());
            this.errorLineNumbers.add(lineNum);
            return;
        }
        throw new SemanticException(diagnostic);
    }

    private boolean isMatch(Ast.Type.T target,Ast.Type.T curr){
        if( target == null || curr == null )
            return false;
        if(target.getKind() == curr.getKind())
            return true;
        // 允许float隐式转换为double
        if(target.getKind() == TypeKind.DOUBLE && curr.getKind() == TypeKind.FLOAT)
            return true;
        if(target.getKind() == TypeKind.FLOAT && curr.getKind() == TypeKind.INT)
            return true;
        if(target.getKind() == TypeKind.DOUBLE && curr.getKind() == TypeKind.INT)
            return true;
        if(target.getKind() == TypeKind.INT && curr.getKind() == TypeKind.BYTE)
            return true;
        if(target.getKind() == TypeKind.FLOAT && curr.getKind() == TypeKind.BYTE)
            return true;
        if(target.getKind() == TypeKind.DOUBLE && curr.getKind() == TypeKind.BYTE)
            return true;
        if(target.getKind() == TypeKind.LONG
                && (curr.getKind() == TypeKind.INT || curr.getKind() == TypeKind.BYTE))
            return true;
        if(target.getKind() == TypeKind.FLOAT && curr.getKind() == TypeKind.LONG)
            return true;
        if(target.getKind() == TypeKind.DOUBLE && curr.getKind() == TypeKind.LONG)
            return true;
        return false;
    }

    private boolean isAssignable(Ast.Type.T target, Ast.Type.T actual, Ast.Expr.T expression) {
        if (target != null && target.getKind() == TypeKind.BYTE
                && actual != null && actual.getKind() == TypeKind.INT) {
            Long value = byteLiteralValue(expression);
            return value != null && value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
        }
        return isMatch(target, actual);
    }

    private boolean byteRangeErrorIfNeeded(Ast.Type.T target, Ast.Type.T actual, Ast.Expr.T expression,
                                        int lineNum, site.ilemon.util.SourceSpan span, String context) {
        if (target == null || target.getKind() != TypeKind.BYTE
                || actual == null || actual.getKind() != TypeKind.INT) {
            return false;
        }
        Long value = byteLiteralValue(expression);
        if (value != null && (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)) {
            site.ilemon.util.SourceSpan primarySpan = span != null ? span : expression.getSpan();
            semanticError(DiagnosticCodes.TYPE_BYTE_RANGE,
                    "byte literal is out of range: expected -128..127, but found " + value,
                    lineNum, primarySpan, context, "byte is a signed 8-bit type",
                    "use a value between -128 and 127");
            return true;
        }
        return false;
    }

    private Long byteLiteralValue(Ast.Expr.T expression) {
        if (expression instanceof Ast.Expr.Number number
                && number.getType().getKind() == TypeKind.INT) {
            try {
                return Long.parseLong(number.getValue().toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (expression instanceof Ast.Expr.Sub sub
                && sub.getLeft() instanceof Ast.Expr.Number zero
                && sub.getRight() instanceof Ast.Expr.Number number
                && zero.getValue().toString().equals("0")) {
            try {
                return -Long.parseLong(number.getValue().toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Ast.Type.T promoteNumeric(Ast.Type.T left, Ast.Type.T right) {
        if (!isNumberType(left) || !isNumberType(right)) {
            return null;
        }
        if (left.getKind() == TypeKind.DOUBLE || right.getKind() == TypeKind.DOUBLE) {
            return new Ast.Type.Double();
        }
        if (left.getKind() == TypeKind.FLOAT || right.getKind() == TypeKind.FLOAT) {
            return new Ast.Type.Float();
        }
        if (left.getKind() == TypeKind.LONG || right.getKind() == TypeKind.LONG) {
            return new Ast.Type.Long();
        }
        return new Ast.Type.Int();
    }

    private boolean isNumberType(Ast.Type.T type) {
        if (type == null) {
            return false;
        }
        TypeKind kind = type.getKind();
        return kind == TypeKind.INT || kind == TypeKind.BYTE || kind == TypeKind.LONG
                || kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE;
    }

    private boolean isIntegerLike(Ast.Type.T type) {
        return type != null && (type.getKind() == TypeKind.INT || type.getKind() == TypeKind.BYTE
                || type.getKind() == TypeKind.LONG);
    }

    private boolean isArrayType(Ast.Type.T type) {
        if (type == null) {
            return false;
        }
        TypeKind kind = type.getKind();
        return kind == TypeKind.INT_ARRAY || kind == TypeKind.FLOAT_ARRAY
                || kind == TypeKind.DOUBLE_ARRAY || kind == TypeKind.BOOL_ARRAY
                || kind == TypeKind.STRING_ARRAY || kind == TypeKind.BYTE_ARRAY
                || kind == TypeKind.LONG_ARRAY;
    }

    /**
     * 校验比较运算符（== / !=）：只要左右类型匹配即可
     */
    private void checkComparison(Ast.Expr.T left, Ast.Expr.T right, String op, int lineNum) {
        this.visit(left);
        Ast.Type.T leftType = this.currType;
        this.visit(right);
        if (isArrayType(leftType) || isArrayType(this.currType)) {
            error(lineNum, String.format("比较运算符 '%s' 不支持数组操作数：左侧为 %s，右侧为 %s",
                    op, typeName(leftType), typeName(this.currType)));
        }
        if (promoteNumeric(leftType, this.currType) == null && !isMatch(leftType, this.currType)) {
            typeError(DiagnosticCodes.TYPE_OPERATOR, typeName(leftType), typeName(this.currType), "comparison expression",
                    lineNum, left.getSpan(), "comparison operator '" + op + "'", null);
        }
        this.currType = new Ast.Type.Bool();
    }

    /**
     * 校验序比较运算符（> / < / >= / <=）：除了类型匹配，还要求是数值类型
     */
    private void checkOrderComparison(Ast.Expr.T left, Ast.Expr.T right, String op, int lineNum) {
        this.visit(left);
        Ast.Type.T leftType = this.currType;
        this.visit(right);
        if (promoteNumeric(leftType, this.currType) == null) {
            typeError(DiagnosticCodes.TYPE_OPERATOR, "numeric operands", typeName(leftType) + " and " + typeName(this.currType),
                    "comparison expression", lineNum, left.getSpan(), "comparison operator '" + op + "'", null);
        }
        this.currType = new Ast.Type.Bool();
    }

    /**
     * 公共方法调用校验逻辑，被 Expr.Call 和 Stmt.Call 共用。
     * 校验方法是否存在、参数个数是否匹配、参数类型是否匹配。
     * @return 方法的返回类型
     */
    private Ast.Type.T validateMethodCall(String methodName, ArrayList<Ast.Expr.T> inputParams,
                                          int lineNum, site.ilemon.util.SourceSpan span) {
        Ast.Method.MethodSingle method = this.methodMap.get(methodName);
        if (method == null) {
            semanticError(DiagnosticCodes.SEM_UNKNOWN_FUNCTION, "undefined function: " + methodName,
                    lineNum, span, "unknown function",
                    "no function with this name is declared in the current program",
                    nearestName(methodName, methodMap.keySet()));
            return unknownType();
        }
        if (inputParams.size() != method.getFormals().size()) {
            error(lineNum, String.format("方法 '%s' 的参数个数不正确：期望 %d 个，实际 %d 个",
                    methodName, method.getFormals().size(), inputParams.size()));
        }
        for (int i = 0; i < inputParams.size(); i++) {
            this.visit(inputParams.get(i));
            Ast.Type.T actualType = this.currType;
            this.visit(method.getFormals().get(i));
            Ast.Type.T expectedType = this.currType;
            if (!isAssignable(expectedType, actualType, inputParams.get(i))) {
                Ast.Expr.T argument = inputParams.get(i);
                if (!byteRangeErrorIfNeeded(expectedType, actualType, argument, argument.getLineNum(), argument.getSpan(),
                        "argument " + (i + 1) + " of '" + methodName + "'")) {
                    typeError(DiagnosticCodes.TYPE_ARGUMENT, typeName(expectedType), typeName(actualType), expressionName(argument),
                            argument.getLineNum(), argument.getSpan(), "argument " + (i + 1) + " of '" + methodName + "'", null);
                }
            }
        }
        return this.methodNameRetTypeMap.get(methodName);
    }

    // ========== 数组相关的 visit 方法 ==========

    @Override
    public void visit(Ast.Type.IntArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.ByteArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.LongArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.FloatArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.DoubleArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.BoolArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.StringArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Expr.ArrayAccess obj) {
        // 检查数组是否已声明
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if (mTable == null) {
            internalError(obj.getLineNum(), "internal error: variable table for method '" + currMethodName + "' was not found");
        }
        if (mTable == null) {
            this.currType = unknownType();
            return;
        }
        Ast.Type.T arrayType = mTable.get(obj.getArrayName());
        if (arrayType == null) {
            semanticError(DiagnosticCodes.SEM_UNKNOWN_VARIABLE, "undefined array: " + obj.getArrayName(),
                    obj.getLineNum(), obj.getSpan(), "unknown array",
                    "the name is not declared in the current method scope", null);
        }
        if (arrayType == null) {
            this.currType = unknownType();
            return;
        }
        Ast.Type.T elementType = getElementType(arrayType);
        if (elementType == null) {
            error(obj.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    obj.getArrayName(), typeName(arrayType)));
        }
        // 检查下标类型必须是int
        if (elementType == null) {
            this.currType = unknownType();
            return;
        }
        this.visit(obj.getIndex());
        if (this.currType.getKind() != TypeKind.INT) {
            typeError(DiagnosticCodes.TYPE_INDEX, "int", typeName(this.currType), expressionName(obj.getIndex()),
                    obj.getIndex().getLineNum(), obj.getIndex().getSpan(), "array index", null);
        }
        // 设置元素类型
        obj.setElementType(elementType);
        this.currType = obj.getElementType();
    }

    @Override
    public void visit(Ast.Expr.ArrayLength obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if (mTable == null) {
            internalError(obj.getLineNum(), "internal error: variable table for method '" + currMethodName + "' was not found");
            this.currType = unknownType();
            return;
        }
        Ast.Type.T arrayType = mTable.get(obj.getArrayName());
        if (arrayType == null) {
            semanticError(DiagnosticCodes.SEM_UNKNOWN_VARIABLE, "undefined array: " + obj.getArrayName(),
                    obj.getLineNum(), obj.getSpan(), "unknown array",
                    "the name is not declared in the current method scope", null);
        }
        if (arrayType == null) {
            this.currType = unknownType();
            return;
        }
        if (getElementType(arrayType) == null) {
            error(obj.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    obj.getArrayName(), typeName(arrayType)));
            this.currType = unknownType();
            return;
        }
        this.currType = new Ast.Type.Int();
    }

    @Override
    public void visit(Ast.Stmt.ArrayAssign obj) {
        // 检查数组是否已声明
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if (mTable == null) {
            internalError(obj.getLineNum(), "internal error: variable table for method '" + currMethodName + "' was not found");
            this.currType = unknownType();
            return;
        }
        Ast.Type.T arrayType = mTable.get(obj.getArrayName());
        if (arrayType == null) {
            semanticError(DiagnosticCodes.SEM_UNKNOWN_VARIABLE, "undefined array: " + obj.getArrayName(),
                    obj.getLineNum(), obj.getSpan(), "unknown array",
                    "the name is not declared in the current method scope", null);
            this.currType = unknownType();
            return;
        }
        // 设置元素类型
        Ast.Type.T elementType = getElementType(arrayType);
        if (elementType == null) {
            error(obj.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    obj.getArrayName(), typeName(arrayType)));
            this.currType = unknownType();
            return;
        }
        obj.setElementType(elementType);
        // 检查下标类型
        this.visit(obj.getIndex());
        if (this.currType.getKind() != TypeKind.INT) {
            typeError(DiagnosticCodes.TYPE_INDEX, "int", typeName(this.currType), expressionName(obj.getIndex()),
                    obj.getIndex().getLineNum(), obj.getIndex().getSpan(), "array index", null);
        }
        // 检查赋值类型
        this.visit(obj.getExpr());
        if (!isAssignable(elementType, this.currType, obj.getExpr())) {
            if (!byteRangeErrorIfNeeded(elementType, this.currType, obj.getExpr(), obj.getLineNum(), obj.getSpan(),
                    "array element assignment")) {
                typeError(DiagnosticCodes.TYPE_ASSIGNMENT, typeName(elementType), typeName(this.currType),
                        expressionName(obj.getExpr()), obj.getLineNum(), obj.getSpan(), "array element assignment", null);
            }
        }
    }

    // 获取数组元素类型
    private Ast.Type.T getElementType(Ast.Type.T arrayType) {
        if (arrayType instanceof Ast.Type.IntArray) {
            return new Ast.Type.Int();
        } else if (arrayType instanceof Ast.Type.ByteArray) {
            return new Ast.Type.Byte();
        } else if (arrayType instanceof Ast.Type.LongArray) {
            return new Ast.Type.Long();
        } else if (arrayType instanceof Ast.Type.FloatArray) {
            return new Ast.Type.Float();
        } else if (arrayType instanceof Ast.Type.DoubleArray) {
            return new Ast.Type.Double();
        } else if (arrayType instanceof Ast.Type.BoolArray) {
            return new Ast.Type.Bool();
        } else if (arrayType instanceof Ast.Type.StringArray) {
            return new Ast.Type.Str();
        }
        return null;
    }
}
