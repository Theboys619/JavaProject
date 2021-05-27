import java.util.ArrayList;

class Main {
  public static void main(String[] args) {
    Interpreter interpreter = new Interpreter();

    ImpOSModule.defineModule(interpreter);
    interpreter.addModule("Filesystem", "./builtins/Filesystem.imp");
    interpreter.addModule("LandingGear", "./builtins/LandingGear.imp");

    ImpObject exports = interpreter.Interpret("./tests/webserver.imp"); // webserver example
    // ImpObject exports = interpreter.Interpret("./tests/imports.imp"); // importing example
    // ImpObject exports = interpreter.Interpret("./tests/arrays.imp"); // example of arrays
    // ImpObject exports = interpreter.Interpret("./tests/strings.imp"); // strings example
    // ImpObject exports = interpreter.Interpret("./tests/test.imp"); // just a giant test
  }
}