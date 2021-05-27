import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

interface ListenCallback {
  void call();
}

class LGRequest {
  BufferedReader reqStream = null;

  String request;
  String[] parsed;

  String method;
  String pathname;
  String protocol;

  boolean isClosed = false;

  public LGRequest(BufferedReader inputStream, String requestString) {
    request = requestString;
    parsed = request.split("\\s");

    method = parsed[0].toUpperCase();
    pathname = parsed[1];
    protocol = parsed[2].toUpperCase();
    reqStream = inputStream;
  }

  private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}

  public void close() throws IOException {
    reqStream.close();
    isClosed = true;
  }
}

class LGResponse {
  BufferedOutputStream outputStream = null;
  PrintWriter charOut = null;

  String path;
  String method;

  boolean isClosed = false;

  public LGResponse(BufferedOutputStream outStream, PrintWriter cOut, String[] reqArgs) {
    outputStream = outStream;
    charOut = cOut;
    method = reqArgs[0].toUpperCase();
    path = reqArgs[1];
  }

  private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}

  private String getMimeType(String filename) {
    if (filename.endsWith(".html") || filename.endsWith(".html"))
      return "text/html";

    if (filename.endsWith(".json"))
      return "application/json";

    return "text/plain";
  }

  public void sendFile(String filename) { sendFile(filename, new File(".")); }
  public void sendFile(String filename, File root) {
    try {
      File file = new File(root, filename);
      int fileLength = (int)file.length();
      String mimeType = getMimeType(filename);

      byte[] fileData = readFileData(file, fileLength);
      
      charOut.println("HTTP/1.1 200 OK");
      charOut.println("Server: LandingGear HTTP Server :: 1.0");
      charOut.println("Date: " + new Date());
      charOut.println("Content-type: " + mimeType);
      charOut.println("Content-length: " + fileLength);
      charOut.println();
      charOut.flush();
      
      outputStream.write(fileData, 0, fileLength);
      outputStream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void close() throws IOException {
    outputStream.close();
    charOut.close();
    isClosed = true;
  }
}

interface LGHandler {
  void handle(LGRequest req, LGResponse res);
}

class LGMiddleware {
  LGHandler handler;
  String method = "GET";

  public LGMiddleware(String method, LGHandler handler) {
    this.handler = handler;
    this.method = method.toUpperCase();
  };

  void call(LGRequest req, LGResponse res) {
    handler.handle(req, res);
  }
}

class LGConnection implements Runnable {
  Socket socket;
  Map<String, LGMiddleware> middleware;

  File webRoot = new File(".");

  public LGConnection(Socket c, Map<String, LGMiddleware> middleWare) {
    socket = c;
    middleware = middleWare;
  }

  @Override
  public void run() {
    LGRequest req = null;
    LGResponse res = null;

    try {
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
      PrintWriter charOut = new PrintWriter(socket.getOutputStream());

      String reqString = inputStream.readLine();
      String[] reqArgs = reqString.split("\\s");

      String method = reqArgs[0].toUpperCase();
      String path = reqArgs[1];

      boolean exists = middleware.containsKey(method + "[REQ]" + path);
      if (!exists) throw new FileNotFoundException("Cannot " + method + " " + path);

      LGMiddleware middle = middleware.get(method + "[REQ]" + path);
      req = new LGRequest(inputStream, reqString);
      res = new LGResponse(outputStream, charOut, reqArgs);

      middle.call(req, res);

      if (!req.isClosed) req.close();
      if (!res.isClosed) res.close();
    } catch (FileNotFoundException e) {
      System.out.println("Request Error: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IOError: " + e);
    } finally {
      try {
        req.close();
        res.close();
        socket.close();
      } catch (IOException e) {
        System.out.println("Failed closing: " + e.getMessage());
      }
    }
  }
}

public class LandingGear {
  ServerSocket server = null;
  int port;

  Map<String, LGMiddleware> middleware = new HashMap<String, LGMiddleware>();

  File root = new File(".");

  public LandingGear() {};
  public LandingGear(String root) {
    this.root = new File(root);
  }

  public void get(String path, LGHandler handler) {
    LGMiddleware middle = new LGMiddleware("GET", handler);
    middleware.put("GET[REQ]" + path, middle);
  }

  public void listen(int port) { listen(port, () -> {}); }
  public void listen(int port, ListenCallback cb) {
    this.port = port;
    try {
      server = new ServerSocket(port);
      cb.call();

      while (true) {
        LGConnection connection = new LGConnection(server.accept(), middleware);
        connection.webRoot = root;

        Thread clientThread = new Thread(connection);
        clientThread.start();
      }
    } catch (IOException e) {
      System.out.println("Server Connection Error: " + e.getMessage());
    }
  }
}