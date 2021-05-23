public class Token {
  private String value;
  private String type;

  public int line = 0;
  public int column = 0;

  public Token(String t, String val) {
    type = t;
    value = val;
  }

  public Token(String t, char val) {
    type = t;
    value = Character.toString(val);
  }

  public Token(String t) {
    value = "NULL";
    type = t;
  }

  public Token() {
    value = "NULL";
    type = "NULL";
  }

  public void updatePosition(int ln, int col) {
    line = ln;
    column = col;
  }

  public boolean isNull() {
    return value.equals("NULL") || type.equals("NULL");
  }

  public String getType() {
    return type;
  }

  public String getString() {
    return value;
  }

  public String toString() {
    return "Token<" + type + ", " + value + ">";
  }
}