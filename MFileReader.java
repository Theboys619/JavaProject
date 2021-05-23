import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.IOException;;

public class MFileReader {
  public static String readFile(String file) {
    String code = "";
    try {
      File f = new File(file);
      Scanner reader = new Scanner(f);

      boolean isFirst = true;

      while (reader.hasNextLine()) {
        if (isFirst)
          isFirst = false;
        else
          code += "\n";

        code += reader.nextLine();
      }

      reader.close();

      return code;
    } catch (FileNotFoundException e) {
      System.out.println("Could not find the file.");
      e.printStackTrace();
    }

    return code;
  }
}