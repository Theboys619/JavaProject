import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Parser {
  private ArrayList<Token> tokens = new ArrayList<Token>();
  private Token curTok;

  private int index = 0;

  public static Map<String, Integer> PRECEDENCE = new HashMap<String, Integer>() {{
    put("=", 1);
    put("+=", 2);
    put("-=", 2);
    put("&&", 4);
    put("||", 5);
    put("<",  7);
    put(">",  7);
    put(">=", 7);
    put("<=", 7);
    put("==", 7);
    put("!=", 7);
    put("+",  10);
    put("-",  10);
    put("*",  10);
    put("/",  10);
    put("%",  10);
  }};

  public Parser(ArrayList<Token> toks) {
    tokens = toks;
    curTok = toks.get(0);
  }

  Token advance() { return advance(1); };
  Token advance(int amt) {
    index += amt;
    if (index > tokens.size() - 1) return curTok = new Token();

    return curTok = tokens.get(index);
  }

  Token peek() { return peek(1); };
  Token peek(int amt) {
    if (index + amt > tokens.size() - 1) return new Token();
    return tokens.get(index + amt);
  }

  boolean isType(String type, String value, Token peeked) {
    return peeked.getType().equals(type) && peeked.getString().equals(value);
  }
  boolean isType(String type, Token peeked) {
    return peeked.getType().equals(type);
  }
  boolean isType(String type, String value) {
    return curTok.getType().equals(type) && curTok.getString().equals(value);
  }
  boolean isType(String type) {
    return curTok.getType().equals(type);
  }

  boolean isIgnore(Token tok) {
    return (tok.getType().equals("Delimiter") && tok.getString().equals(";")) || (tok.getType().equals("Linebreak"));
  }

  boolean isEOF() {
    if (curTok.isNull()) return true;

    return curTok.getType().equals("EOF");
  }

  // Skips over ';' and '\n's (Linebreaks).
  void skipIgnore() {
    while (isIgnore(curTok) && !curTok.isNull() && !isEOF()) {
      advance();
    }
  }

  void skipOverVal(String val, Token tok) {
    if (!tok.getString().equals(val)) throw new Error("Unexpected Token with value '" + val + "'");

    advance();
  }

  void skipOver(String type, Token tok) {
    if (!tok.getType().equals(type)) throw new Error("Unexpected Token '" + type + "'");

    advance();
  }

  void skipOver(String type, String val, Token tok) {
    if (type.length() == 0 && val.length() > 0) {
      if (tok.getString().equals(val)) {
        advance();
        return;
      }

      throw new Error("Unexpected token " + tok.getString());
    }

    if (!tok.getType().equals(type)) throw new Error("Unexpected Token '" + type + "'");
    if (!tok.getString().equals(val)) throw new Error("Unexpected Token with value '" + val + "'");

    advance();
  }

  ArrayList<AST> pDelimiters(String start, String end, String separator) {
    ArrayList<AST> values = new ArrayList<AST>();
    boolean isFirst = true;

    skipOver("", start, curTok);

    while (!isEOF()) {
      if (isType("Delimiter", end, curTok)) {
        break;
      } else if (isFirst) {
        isFirst = false;
      } else {
        if (separator == "" && isIgnore(curTok)) {
          skipIgnore();
        } else {
          skipOver("", separator, curTok);
        }
      }

      if (isType("Delimiter", end, curTok)) {
        break;
      }

      AST val = pExpression();
      values.add(val);
    }

    skipOverVal(end, curTok);

    return values;
  }

  boolean isCallable(AST callStmt) {
    return callStmt.type != ExprTypes.Function && callStmt.type != ExprTypes.If && callStmt.type != ExprTypes.Return;
  }

  AST checkCall(AST expr) {
    if (isType("Delimiter", "(", peek()) && isCallable(expr) && isType("Identifier")) {
      return pCall(expr);
    }

    return expr;
  }

  AST pCall(AST expression) {
    Token varName = expression.value;
    advance();

    AST funcCall = new AST(ExprTypes.FunctionCall, varName);
    funcCall.args = pDelimiters("(", ")", ",");

    return funcCall;
  }

  AST pBinary(AST left, int prec) {
    Token op = curTok;

    if (isType("Operator")) {
      String opval = op.getString();
      int newPrec = PRECEDENCE.get(opval);

      if (newPrec > prec) {
        advance();

        ArrayList<String> assignments = new ArrayList<String>();
        assignments.add("=");
        assignments.add("+=");
        assignments.add("-=");

        ExprTypes type = assignments.contains(opval) ? ExprTypes.Assign : ExprTypes.Binary;
        
        AST expr = new AST(type, op);
        expr.left = left;
        expr.op = op;
        expr.right = pBinary(pAll(), newPrec);

        return pBinary(expr, prec);
      }
    }

    return left;
  }

  // What is this monstrosity?
  ArrayList<AST> pDotOp(AST exp) {
    ArrayList<AST> access = new ArrayList<AST>();

    while(isType("Delimiter", ".") || isType("Delimiter", "[")) {
      boolean isDot = isType("Delimiter", ".");

      if (exp.type != ExprTypes.Access)
        exp.type = ExprTypes.Access;
        
      advance();

      if (!isType("Identifier") && isDot) throw new Error("Invalid token after dot operator");

      boolean isFuncCall = isType("Delimiter", "(", peek());
      AST currentIdent = new AST(ExprTypes.Identifier, curTok);
      if (!isFuncCall && isDot)
        advance();

      AST dotAst = !isDot
        ? pExpression()
        : (
          isFuncCall
            ? pCall(currentIdent)
            : pBinary(currentIdent, 0)
        );
      if (!isDot) {
        skipOverVal("]", curTok);
        dotAst = pBinary(dotAst, 0);
        dotAst.isBracketOp = true;
      }

      access.add(dotAst);
      // advance();
    }

    exp.Access = access;
    return access;
  }

  AST pIdentifier(AST exp) {
    exp.type = ExprTypes.Identifier;

    if (!isType("Delimiter", "(", peek())) {
      advance();
    }

    // pIndexAccess(expr);

    // Could / should change
    // if (!pDotOp(expr)) {
    // 	expr.dataType = pDatatype();
    // }

    pDotOp(exp);

    return exp;
  }

  AST pSet() {
    advance(); // skip over set Keyword

    Token identifierTok = curTok;
    AST stmt = new AST(ExprTypes.Variable, identifierTok);

    advance();

    return stmt;
  }

  AST pFunction() {
    advance(); // skip over func Keyword

    if (!isType("Identifier")) throw new Error("Invalid function name " + curTok.getString());

    AST func = new AST(ExprTypes.Function, curTok);
    advance();

    func.args = pDelimiters("(", ")", ",");
    func.scope = new AST(ExprTypes.Scope, curTok);

    func.scope.block = pDelimiters("{", "}", "");

    return func;
  }

  AST pReturn() {
    advance(); // skip over return Keyword

    AST returnStmt = new AST(ExprTypes.Return, curTok);
    returnStmt.scope = pExpression();

    return returnStmt;
  }

  AST pClass() {
    advance(); // skip over class Keyword

    AST classAst = new AST(ExprTypes.Class, curTok);
    advance(); // skip over class name

    AST classDefs = new AST(ExprTypes.Scope, classAst.value);
    classDefs.block = pDelimiters("{", "}", "");
    classAst.scope = classDefs;

    return classAst;
  }

  AST pSmallIf(AST condition) {
    AST ifStmt = new AST(ExprTypes.If, curTok);

    AST then = pExpression();
    AST els = new AST();

    if (curTok.getString() == ";" && isType("Keyword", "else", peek())) advance();

    // Check for else keyword
    if (isType("Keyword", "else")) {
      advance();

      // Colon means it is a small if statement and is kind of like a ternary.
      if (isType("Delimiter", ":")) {
        advance();
        els = pExpression(); // parse a single expression after colon.
      } else if (isType("Keyword", "if"))
        els = pIf(); // parse if-else body after else and if keywords
      else {
        if (!isType("Delimiter", "{"))
          els = pExpression(); // Parse single expression if not a scoped body
        else {
          els = new AST(ExprTypes.Scope, curTok);
          els.block = pDelimiters("{", "}", ";"); // parse else body
        }
      }
    }

    ifStmt.then = then;
    ifStmt.condition = condition;
    ifStmt.els = els;

    return ifStmt;
  }

  AST pIf() {
    advance(); // skip over if Keyword

    AST ifStmt = new AST(ExprTypes.If, curTok);

    AST condition = pExpression();

    // Checks if it is a small-if meaning it has a colon and only single expressions
    if (isType("Delimiter", ":")) {
      advance();
      return pSmallIf(condition);
    }

    AST then = new AST();
    AST els = new AST();

    if (!isType("Delimiter", "{"))
      then = pExpression(); // parse single expression if not a scoped body
    else {
      then = new AST(ExprTypes.Scope, curTok);
      then.block = pDelimiters("{", "}", ""); // parse the if body
    }

    if (isType("Keyword", "else")) {
      advance(); // advance over else Keyword

      if (isType("Delimiter", ":")) {
        advance();
        els = pExpression(); // parse a single expression since colon.
      } else if (isType("Keyword", "if"))
        els = pIf(); // parse the else-if body
      else {
        if (!isType("Delimiter", "{"))
          els = pExpression();
        else {
          els = new AST(ExprTypes.Scope, curTok);
          els.block = pDelimiters("{", "}", "");
        }
      }
    }

    ifStmt.then = then;
    ifStmt.condition = condition;
    ifStmt.els = els;

    return ifStmt;
  }

  AST pFor() {
    advance(); // skip over for Keyword

    AST loop = new AST(ExprTypes.For, curTok);

    AST initial = new AST();
    AST condition = new AST();
    AST reassign = new AST();
    
    skipOverVal("(", curTok);

    if (isType("Delimiter", ";"))
      advance();
    else {
      initial = pExpression();
      if (isType("Delimiter", ";")) advance();
    }

    if (isType("Delimiter", ";"))
      advance();
    else {
      condition = pExpression();
      if (isType("Delimiter", ";")) advance();
    }

    if (isType("Delimiter", ";"))
      advance();
    else {
      reassign = pExpression();
    }

    skipOverVal(")", curTok);
    
    loop.scope = new AST(ExprTypes.Scope, curTok);
    loop.scope.block = pDelimiters("{", "}", "");
    loop.assign = initial;
    loop.condition = condition;
    loop.reassign = reassign;

    return loop;
  }

  AST pArray() {
    AST arrayStmt = new AST(ExprTypes.Array, curTok);
    arrayStmt.args = pDelimiters("[", "]", ",");

    return arrayStmt;
  }

  AST pImport() {
    // import "Test" as express;

    advance(); // skip over import Keyword

    Token importStr = curTok;
    advance();

    AST importAst = new AST(ExprTypes.Import, importStr);
    skipOverVal("as", curTok);
    importAst.assign = pIdentifier(new AST(ExprTypes.Identifier, curTok));

    return importAst;
  }

  AST $pAll() {
    if (isType("Delimiter", "(")) {
      skipOver("Delimiter", "(", curTok); 
      AST expr = pExpression();
      skipOver("Delimiter", ")", curTok);

      return expr;
    }

    if (isType("Delimiter", "["))
      return pArray();

    AST oldTok = new AST(curTok);

    if (isType("Keyword", "set")) {
      return pSet();
    }

    if (isType("Keyword", "func")) {
      return pFunction();
    }

    if (isType("Keyword", "return"))
      return pReturn();

    if (isType("Keyword", "class"))
      return pClass();

    if (isType("Keyword", "if"))
      return pIf();

    if (isType("Keyword", "for"))
      return pFor();

    if (isType("Keyword", "import"))
      return pImport();

    if (isType("Keyword", "true") || isType("Keyword", "false")) {
      oldTok.setType(ExprTypes.Boolean);
      advance();

      return oldTok;
    } else if (isType("String", curTok)) {
      oldTok.setType(ExprTypes.String);

      advance();

      return oldTok;
    } else if (isType("Int", curTok)) {
      oldTok.setType(ExprTypes.Integer);

      advance();

      return oldTok;
    } else if (isType("Float", curTok)) {
      oldTok.setType(ExprTypes.Double);

      advance();

      return oldTok;
    }

    if (isType("Identifier", curTok)) {
      return pIdentifier(oldTok);
    }

    if (isType("Linebreak", "\\n", curTok)) {
      while (isType("Linebreak", "\\n", curTok)) {
        advance();
      }

      return pAll();
    }

    if (isIgnore(curTok)) {
      skipIgnore();
    }

    return oldTok; 
  }

  AST pAll() {
    return checkCall($pAll());
  }

  AST pExpression() {
    return checkCall(pBinary(pAll(), 0));
  }

  public AST parse() {
    AST ast = new AST(ExprTypes.Scope, new Token("Identifier", "_TOP_"));

    while (!curTok.isNull() && !isEOF()) {
      AST exp = pExpression();
      ast.block.add(exp);

      if (isIgnore(curTok)) {
        skipIgnore();
      }
    }

    return ast;
  }
}