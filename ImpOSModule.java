import java.util.ArrayList;

public class ImpOSModule {
  static void defineModule(Interpreter interpreter) {
    ImpFunction readFile = new ImpFunction(interpreter, (ArrayList<ImpObject> funcArgs, Interpreter i) -> {
      // os.readFile(STRING)

      if (funcArgs.size() < 1) throw new Error("Filepath not specified. Filepath required.");
      String filePath = funcArgs.get(0).getString();

      String val = MFileReader.readFile(filePath);
      return i.ConstructString(val);

    }, "readFile");

    ImpFunction landingGear = new ImpFunction(interpreter, (ArrayList<ImpObject> funcArgs, Interpreter i) -> {
      // os.LandingGear();

      LandingGear app = new LandingGear();

      ImpFunction listenFunc = new ImpFunction(i, (ArrayList<ImpObject> listenArgs, Interpreter interp) -> {
        // app.listen(PORT)

        if (listenArgs.size() < 1) throw new Error("Listen requires one argument.");

        if (listenArgs.size() == 2)
          app.listen(listenArgs.get(0).getInt(), () -> {
            ((ImpFunction)listenArgs.get(1)).Call(new ArrayList<ImpObject>(), new ImpObject());
          });
        else
          app.listen(listenArgs.get(0).getInt());

        return new ImpObject();
      }, "listen");

      ImpFunction getFunc = new ImpFunction(i, (ArrayList<ImpObject> getFuncArgs, Interpreter interp) -> {
        // app.get(METHOD, MIDDLEWARE)

        if (getFuncArgs.size() < 2) throw new Error("Method 'get' requires two arguments.");

        ImpFunction cb = (ImpFunction)getFuncArgs.get(1);

        app.get(getFuncArgs.get(0).getString(), (LGRequest req, LGResponse res) -> {
          ImpObject reqObj = new ImpObject(ImpTypes.Object, new Any("LGRequest"));
          ImpObject resObj = new ImpObject(ImpTypes.Object, new Any("LGResponse"));

          ImpFunction sendFileFunc = new ImpFunction(interp, (ArrayList<ImpObject> sendFileArgs, Interpreter interp2) -> {
            // res.sendFile(STRING)

            if (sendFileArgs.size() < 1) throw new Error("Method 'sendFile' requires one argument.");

            String filename = sendFileArgs.get(0).getString();
            res.sendFile(filename);

            return new ImpObject();
          }, "sendFile");

          resObj.props.Define("sendFile", (ImpObject)sendFileFunc);

          ArrayList<ImpObject> middleArgs = new ArrayList<ImpObject>();
          middleArgs.add(reqObj);
          middleArgs.add(resObj);
          cb.Call(middleArgs, new ImpObject());
        });

        return new ImpObject();
      }, "get");

      ImpObject lgApp = new ImpObject(ImpTypes.Object, new Any("LandingGear"));
      lgApp.props.Define("listen", (ImpObject)listenFunc);
      lgApp.props.Define("get", (ImpObject)getFunc);

      return lgApp;
    }, "LandingGear");

    ImpObject os = new ImpObject(ImpTypes.Object, new Any("os"));
    os.props.Define("readFile", (ImpObject)readFile);
    os.props.Define("LandingGear", (ImpObject)landingGear);

    interpreter.addBuiltin("os", os);
  }
}