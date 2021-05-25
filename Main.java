import java.util.ArrayList;

class Main {
  public static void main(String[] args) {
    Interpreter interpreter = new Interpreter();
    ImpObject exports = interpreter.Interpret("tests/test.imp");
  }
}