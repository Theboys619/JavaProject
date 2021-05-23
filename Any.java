public class Any {
  private int integer = 0;
  private double decimal = 0.0;
  private String string = "";
  private boolean bool = false;

  public String type = "None";

  public Any() {
    type = "Null";
    string = "Null";
  };
  public Any(int x) {
    type = "Int";
    integer = x;
  }
  public Any(double x) {
    type = "Double";
    decimal = x;
  }
  public Any(String x) {
    type = "String";
    string = x;
  }
  public Any(char x) {
    type = "String";
    string = String.valueOf(x); // Looked String.valueOf up
  }
  public Any(boolean x) {
    type = "Boolean";
    bool = x;
  }

  // Methods to get property of the class
  public String getString() {
    if (type.equals("Int") || type.equals("Double"))
      return String.valueOf(integer);

    if (type.equals("Boolean"))
      return bool ? "true" : "false";

    return string;
  }

  public int getInt() {
    if (type.equals("Boolean")) {
      return bool ? 1 : 0;
    }

    if (type.equals("Double")) {
      return (int)Math.floor(decimal);
    }

    return integer;
  }

  public double getDouble() {
    if (type.equals("Int")) {
      return (double)integer;
    }

    if (type.equals("Boolean"))
      return bool ? 1 : 0;

    return decimal;
  }

  public boolean getBoolean() {
    if (type.equals("Int"))
      return integer >= 1;

    if (type.equals("Double"))
      return decimal >= 1.0;

    return bool;
  }
}