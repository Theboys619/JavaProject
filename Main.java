import java.util.ArrayList;
import java.util.concurrent.Callable;

// class PrintFunc implements NativeFunction {
//   public Value call(ArrayList<Value> argls, Context mainctx, Context thisCtx) {

//   }
// }

class Main {
  public static void main(String[] args) {
    // System.out.println("Hello world!");
    Lexer lexer = new Lexer(MFileReader.readFile("tests/test.imp"));
    ArrayList<Token> tokens = lexer.tokenize();

    // Debugging purposes
    // for (Token tok : tokens) {
    //   System.out.println(tok);
    // }

    Parser parser = new Parser(tokens);

    try {
      AST ast = parser.parse();

      Interpreter interp = new Interpreter();
      interp.scope.Define("print", new ImpFunction(interp, (ArrayList<ImpObject> funcArgs, Interpreter i) -> {
        for (ImpObject arg : funcArgs) {
          System.out.print(arg.getString() + " ");
        }

        System.out.println();

        return new ImpObject();
      }, "print"));

      ImpObject lastVal = interp.Interpret(ast);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}