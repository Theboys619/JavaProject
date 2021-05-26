import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Looked up stuff for Maps and Sets
// https://stackoverflow.com/questions/16203880/get-array-of-maps-keys (Map keys to array)
// Stack Overflow is my best friend

class Lexer {
  private String input = "";
  private ArrayList<Token> tokens;

  private int index;

  private int line;
  private int column;

  private char curChar = '\0';

  private ArrayList<Map<String, String>> operators;
  private Map<String, String> keywords = new HashMap<String, String>() {{
    put("set", "Keyword");
    put("func", "Keyword");
    put("return", "Keyword");
    put("class", "Keyword");
    put("for", "Keyword");
    put("if", "Keyword");
    put("else", "Keyword");
    put("true", "Keyword");
    put("false", "Keyword");
    put("import", "Keyword");
    put("as", "Keyword");
    put("static", "Keyword");
  }};

  public Lexer() {
    index = 0;
    line = 1;
    column = 1;
    curChar = '\0';
    tokens = new ArrayList<Token>();
    operators = new ArrayList<Map<String, String>>();

    Map<String, String> singleOps = new HashMap<String, String>() {{
      put("=", "Assign");
      put("+", "Add");
      put("-", "Subtract");
      put("*", "Multiply");
      put("/", "Divide");
      put("%", "Modulo");
      put("<", "LessThan");
      put(">", "GreaterThan");
    }};
    Map<String, String> doubleOps = new HashMap<String, String>() {{
      put("+=", "AddAssign");
      put("-=", "SubtractAssign");
      put("==", "Equals");
      put("<=", "LessThanEqual");
      put(">=", "GreaterThanEqual");
    }};

    operators.add(0, singleOps);
    operators.add(1, doubleOps);
  }

  public Lexer(String data) {
    this();
    input = data;
    curChar = input.charAt(0);
  }

  /* Default - advance(int amt = 1) */
  char advance() { return advance(1); }
  char advance(int amt) {
    index += amt;
    column += amt;

    if (index >= input.length()) {
      curChar = '\0';
      return curChar;
    }
    
    curChar = input.charAt(index);
    return curChar;
  }

  /* Default - peek(int amt = 1) */
  char peek() { return peek(1); }
  char peek(int amt) {
    if (index + amt >= input.length()) {
      return '\0';
    }
    
    return input.charAt(index + amt);
  }

  /* Default - grab(int amt = 1) */
  /*
    "Grabs" all characters from current index + the amount specified and returns a string of those characters.
  */
  String grab() { return grab(1); }
  String grab(int amt) {
    String value = "";
    value += curChar;

    for (int i = 1; i < amt; i++) {
      value += peek();
    }

    return value;
  }

  boolean isWhitespace(char c) {
    return c == ' ' || c == '\r' || c == '\t';
  }

  boolean isAlpha(char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
  }

  boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  boolean isNumber(char c) {
    return (
      (c == '-' && isDigit(peek()))
      || isDigit(c)
    );
  }

  boolean isQuote(char c) { return c == '\'' || c == '\"'; };
  boolean isQuote(char c, char quote) {
    if (quote != '"' && quote != '\'') return false;
    if (quote == '"')
      return c == '"';
    else if (quote == '\'')
      return c == '\'';

    return false;
  }

  //https://stackoverflow.com/questions/16203880/get-array-of-maps-keys
  int isOperator(char c) {
    for (int i = operators.size()-1; i >= 0; i--) {
      // Get the map from the operators ArrayList
      Map<String, String> value = operators.get(i);

      // Ge all keys from that map from above
      Set<String> keys = value.keySet();
      String[] keyArray = keys.toArray(new String[keys.size()]); // Get the keys as an Array.

      // Loop through each key and compare
      for (String key : keyArray) {
        if (grab(i + 1).equals(key)) {
          return i + 1; // Returns the length of the operator
        }
      }
    }

    return 0;
  }

  boolean isDelimiter(char c) {
    return (
      (c == '(') ||
      (c == ')') ||
      (c == '{') ||
      (c == '}') ||
      (c == '[') ||
      (c == ']') ||
      (c == ';') ||
      (c == ':') ||
      (c == ',') ||
      (c == '.')
    );
  }

  public ArrayList<Token> tokenize() {
    while (curChar != '\0') {
      int oldIndex = index;
      if (isWhitespace(curChar))
        advance();

      if (curChar == '\n') {
        tokens.add(new Token("Linebreak", "\\n"));
        column = 0;
        line++;

        advance();
      }

      if (curChar == '#') { // Comment
        while (curChar != '\n') {
          advance();
        }
      }

      if (isOperator(curChar) > 0) {
        int opLength = isOperator(curChar);
        String value = grab(opLength);

        Token tok = new Token("Operator", value);
        tok.updatePosition(line, column);
        tokens.add(tok);
        advance(opLength);
      }

      if (isDelimiter(curChar)) {
        Token tok = new Token("Delimiter", curChar);
        tok.updatePosition(line, column);

        advance();

        tokens.add(tok);
      }

      if (isNumber(curChar)) {
        String type = "Int";
        int col = column;
        int ln = line;

        String val = "";

        if (curChar == '-') {
          val += curChar;
          advance();
        }

        while (isNumber(curChar)) {
          val += curChar;
          advance();

          if (curChar == '.') {
            type = "Float";
            val += ".";

            advance();
          }
        }

        Token tok = new Token(type, val);
        tok.updatePosition(ln, col);

        tokens.add(tok);
      }

      if (isQuote(curChar)) {
        char quote = curChar;
        int col = column;
        int ln = line;

        String val = "";
        advance();

        while (curChar != '\0' && curChar != quote) {
          if (curChar == '\n') {
            throw new Error("SyntaxError: Unexpected characater \\n");
          };
          val += curChar;
          advance();
        }

        advance();

        Token tok = new Token("String", val);
        tok.updatePosition(ln, col);

        tokens.add(tok);
      }

      if (isAlpha(curChar)) {
        String val = "";

        int col = column;
        int ln = line;

        while (curChar != '\0' && isAlpha(curChar)) {
          val += curChar;
          advance();
        }

        String type = keywords.containsKey(val)
          ? keywords.get(val)
          : "Identifier";

        // System.out.println(type);

        Token tok = new Token(type, val);
        tok.updatePosition(ln, col);

        tokens.add(tok);
      }

      if (oldIndex == index) {
        System.out.println("Error: Unknown character " + curChar);
        return tokens;
      }
    }

    return tokens;
  }
}