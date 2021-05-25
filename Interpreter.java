import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

import java.util.Scanner;

// Yes it would be nicer to split stuff into other files but too bad

// wrapper around HashMap / Map
class PropMap {
  Map<String, ImpObject> props;

  public PropMap() {
    props = new HashMap<String, ImpObject>();
  }

  public PropMap(Map<String, ImpObject> newProps) {
    props = newProps;
  }

  public PropMap(PropMap propMap) {
    props = new HashMap<String, ImpObject>();

    for (Entry<String, ImpObject> entry : propMap.props.entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
  }

  public boolean HasProp(String name) {
    return props.containsKey(name);
  }

  public ImpObject Get(String name) { return Get(name, true); }
  public ImpObject Get(String name, boolean throwing) {
    boolean exists = HasProp(name);
    if (!exists) {
      if (throwing) throw new Error("Property " + name + " is not defined");
      return new ImpObject();
    }

    return props.get(name);
  }

  public ImpObject Set(String name, ImpObject val) {
    return props.put(name, val);
  }

  public ImpObject Define(String name, ImpObject val) {
    if (HasProp(name)) throw new Error("Property " + name + " already exists");

    return props.put(name, val);
  }
}

enum ImpTypes {
  None,
  Int,
  Double,
  String,
  Boolean,
  Function,
  ClassObject,
  Object,
  Class,
  ClassString
};

// Every value in Imp is an ImpObject or is a child of it
class ImpObject {
  ImpTypes type;
  PropMap props = new PropMap();

  Any value;

  public ImpObject() {
    type = ImpTypes.None;
    value = new Any();
  }

  public ImpObject(ImpTypes t) {
    type = t;
    value = new Any();
  }

  public ImpObject(String val) {
    type = ImpTypes.String;
    value = new Any(val);
  }

  public ImpObject(int val) {
    type = ImpTypes.Int;
    value = new Any(val);
  }

  public ImpObject(double val) {
    type = ImpTypes.Double;
    value = new Any(val);
  }

  public ImpObject(boolean val) {
    type = ImpTypes.Boolean;
    value = new Any(val);
  }

  public ImpObject(ImpTypes t, Any val) {
    type = t;
    value = val;
  }

  public String getString() {
    if (type == ImpTypes.ClassString) {
      ImpObject length = props.Get("length", false);
      String str = "";

      if (length.isNull()) length = new ImpObject(0);

      for (int i = 0; i < length.getInt(); i++) {
        str += props.Get(Integer.toString(i));
      }

      return str;
    }
    
    if (type == ImpTypes.Function) {
      return "[ImpFunction " + value.getString() + "]";
    } else if (type == ImpTypes.ClassObject || type == ImpTypes.Object) {
      return "[Object " + value.getString() + "]";
    }

    return value.getString();
  }

  public int getInt() {
    return value.getInt();
  }

  public double getDouble() {
    return value.getDouble();
  }

  public boolean getBoolean() {
    return value.getBoolean();
  }

  public boolean isNull() {
    return type == ImpTypes.None;
  }

  public String toString() {
    if (type == ImpTypes.ClassString) {
      ImpObject length = props.Get("length", false);
      String str = "";

      if (length.isNull()) length = new ImpObject(0);

      for (int i = 0; i < length.getInt(); i++) {
        str += props.Get(Integer.toString(i));
      }

      return str;
    }
    
    if (type == ImpTypes.Function) {
      return "[ImpFunction " + value.getString() + "]";
    } else if (type == ImpTypes.ClassObject || type == ImpTypes.Object) {
      return "[Object " + getString() + "]";
    } else if (type == ImpTypes.String) {
      return value.getString();
    }

    return "[" + type + " " + getString() + "]";
  }
}

// Yes I did indeed need to look up how this worked for implementation of native methods / functions
interface NativeFunction {
  public ImpObject call(ArrayList<ImpObject> args, Interpreter interp);
}

class ImpFunction extends ImpObject {
  Interpreter interp;

  NativeFunction nativeFunc;
  boolean isNative = false;

  String name = "";

  AST exp;

  ImpObject objThis;

  public ImpFunction(Interpreter i, AST expr) {
    super(ImpTypes.Function);
    isNative = false;
    interp = i;
    exp = expr;
    name = exp.value.getString();
    objThis = new ImpObject();
  }
  public ImpFunction(Interpreter i, NativeFunction func, String funcName) {
    super(ImpTypes.Function);
    isNative = true;
    nativeFunc = func;
    interp = i;
    name = funcName;
    objThis = new ImpObject();
  }

  public ImpObject Call(ArrayList<ImpObject> args, ImpObject thisObj) {
    return Call(args, thisObj, true);
  }
  public ImpObject Call(ArrayList<ImpObject> args, ImpObject thisObj, boolean extend) {
    if (thisObj.isNull() && objThis.isNull())
      objThis = thisObj;
    else if (!thisObj.isNull())
      objThis = thisObj;
    
    if (extend)
      interp.scope = new ImpScope(interp, interp.scope);
    interp.scope.Set("this", objThis);

    if (isNative) {
      ImpObject returnValue = nativeFunc.call(args, interp);

      if (extend)
        interp.scope = interp.scope.parent;

      return returnValue;
    }

    ArrayList<AST> argDefs = exp.args;

    for (int i = 0; i < argDefs.size(); i++) {
      interp.scope.Set(argDefs.get(i), new ImpObject());

      if (argDefs.get(i).type == ExprTypes.Assign) {
        AST expr = argDefs.get(i);
        ImpObject right = interp.Evaluate(expr.right);

        interp.scope.Set(expr.left, right);

        if (i >= args.size() || args.get(i).isNull())
          continue;
      }

      if (i >= args.size())
        continue;
      
      if (argDefs.get(i).type == ExprTypes.Assign)
        interp.scope.Set(argDefs.get(i).left, args.get(i));
      else
        interp.scope.Set(argDefs.get(i), args.get(i));
    }

    ImpObject returnValue = interp.iScope(exp.scope, false);

    if (extend)
      interp.scope = interp.scope.parent;

    return returnValue;
  }
  public ImpObject ExpCall(ArrayList<AST> expArgs, ImpObject thisObj, boolean extend) {
    ArrayList<ImpObject> args = new ArrayList<ImpObject>();

    for (AST arg : expArgs) {
      args.add(interp.Evaluate(arg));
    }

    return Call(args, thisObj, extend);
  }
  public ImpObject ExpCall(ArrayList<AST> expArgs, ImpObject thisObj) {
    return ExpCall(expArgs, thisObj, true);
  }
}

class ImpClass extends ImpObject {
  Interpreter interp;
  AST classExp;
  String className;
  ImpObject objThis;

  public ImpClass(){
    super(ImpTypes.Class);
  }
  public ImpClass(Interpreter i, AST exp) {
    super(ImpTypes.Class);
    interp = i;
    classExp = exp;
    className = exp.value.getString();
    objThis = new ImpObject();
  }

  public ImpObject NewInstance(ArrayList<ImpObject> args, ImpObject thisObj) {
    if (thisObj.isNull() && objThis.isNull())
      objThis = thisObj;
    else if (!thisObj.isNull())
      objThis = thisObj;

    ArrayList<AST> definitions = classExp.scope.block;

    ImpObject obj = new ImpObject();
    obj.type = ImpTypes.ClassObject;
    obj.value = new Any(className);

    interp.scope = new ImpScope(interp, interp.scope);

    for (AST def : definitions) {
      if (
        def.type != ExprTypes.Function &&
        def.type != ExprTypes.Variable &&
        def.type != ExprTypes.Assign
      ) throw new Error("Can only have functions and variables in classes");

      if (def.type == ExprTypes.Function) {
        ImpObject func = new ImpFunction(interp, def); // Creates a new function with the interpreter

        // Check if it is already defined, if so throw an error.
        if (obj.props.Get(def.value.getString(), false).type != ImpTypes.None) {
          throw new Error("Method already defined");
        }

        obj.props.Set(def.value.getString(), func); // Define it
      }

      if (def.type == ExprTypes.Assign) {
        ImpObject left = interp.Evaluate(def.left); // Visit / interpret left branch
        ImpObject right = interp.Evaluate(def.right); // Visit / interpret right branch
        ImpObject val = interp.iIdentifier(def.left); // Just checks to see if it exists

        obj.props.Set(def.left.value.getString(), right); // Define the property
      }

      if (def.type == ExprTypes.Variable) {
        ImpObject value = new ImpObject();

        obj.props.Set(
          def.value.getString(),
          value
        );
      }
    }

    ImpFunction constructor = (ImpFunction)obj.props.Get(className);
    constructor.Call(args, obj, false);

    interp.scope = interp.scope.parent;

    return obj;
  }
  public ImpObject ExpNewInstance(ArrayList<AST> expArgs, ImpObject thisObj) {
    ArrayList<ImpObject> args = new ArrayList<ImpObject>();

    for (AST arg : expArgs) {
      args.add(interp.Evaluate(arg));
    }

    return NewInstance(args, thisObj);
  }

  public String toString() {
    return "[Class " + className + "]";
  }
}

class ImpScope {
  Interpreter interp;

  ImpScope parent;
  PropMap props;

  boolean returned = false;

  public ImpScope(Interpreter i) {
    interp = i;
    props = new PropMap();
  }
  public ImpScope(Interpreter i, ImpScope p) {
    interp = i;
    parent = p;
    props = new PropMap();
  }

  public boolean HasProp(String name) {
    return props.HasProp(name);
  }

  public ImpObject Get(String name) { return Get(name, true); }
  public ImpObject Get(String name, boolean throwing) {
    boolean exists = HasProp(name);

    if (!exists) {
      if (!Objects.isNull(parent))
        return parent.Get(name, throwing);
      else if (throwing)
        throw new Error("Property " + name + " is not defined.");

      return new ImpObject();
    }

    return props.Get(name);
  }

  public ImpObject Set(String name, ImpObject val) {
    return props.Set(name, val);
  }

  public ImpObject Set(AST exp, ImpObject val) {
    return props.Set(exp.value.getString(), val);
  }

  public ImpObject Define(String name, ImpObject val) {
    return props.Define(name, val);
  }

  public ImpObject Define(AST exp, ImpObject val) {
    return props.Define(exp.value.getString(), val);
  }
}

public class Interpreter {
  ImpScope scope;
  AST ast;

  public Interpreter() {
    scope = new ImpScope(this);
  }
  public Interpreter(ImpScope s) {
    scope = s;
  }

  ImpObject iScope(AST exp) { return iScope(exp, true); }
  ImpObject iScope(AST exp, boolean extend) {
    ImpObject returnValue = new ImpObject();

    if (extend)
      scope = new ImpScope(this, scope);

    for (AST expr : exp.block) {
      if (expr.type != ExprTypes.Function) continue;
      returnValue = Evaluate(expr);
    }

    for (AST expr : exp.block) {
      if (expr.type == ExprTypes.Function) continue;
      returnValue = Evaluate(expr); // Evaluate each statement or expression

      if (scope.returned) {
        return returnValue; // Check if it is a returnValue or has been returned and return (only for functions)
      }
    }

    if (returnValue.isNull()) return new ImpObject(); // return nothing;

    return returnValue; // return back the return value from Impala
  }

  ImpObject ConstructObject(String name, ArrayList<ImpObject> args) {
    ImpClass cls = (ImpClass)scope.Get(name);

    return cls.NewInstance(args, new ImpObject());
  }

  ImpObject ConstructString(String data) {
    ImpClass cls = (ImpClass)scope.Get("String");

    ArrayList<ImpObject> args = new ArrayList<ImpObject>();
    args.add(new ImpObject(data));

    ImpObject str = cls.NewInstance(args, new ImpObject()); 

    for (int i = 0; i < data.length(); i++) {
      ImpObject c = new ImpObject(data.substring(i, i + 1));
      str.props.Set(Integer.toString(i), c);
    }

    str.props.Set("length", new ImpObject(data.length()));
    str.value = new Any(data);
    str.type = ImpTypes.ClassString;

    return str;
  }

  ImpObject iVariable(AST exp) {
    return scope.Define(exp.value.getString(), new ImpObject());
  }

  ImpObject iIdentifier(AST exp) {
    return scope.Get(exp.value.getString());
  }

  ImpObject iAssign(AST exp) {
    ImpObject identifier = Evaluate(exp.left);
    ImpObject val = Evaluate(exp.right);
    String op = exp.op.getString();

    if (op.equals("+=") || op.equals("-=")) {
      val = iOperation(identifier, val, op);
    }

    scope.Set(exp.left.value.getString(), val);

    return val;
  }

  // To be honest I wonder how fast / slow it is since the JVM isn't the fastest thing in the world compared to C++, C, and other compiled languages

  ImpObject iFuncCall(AST exp) {
    String name = exp.value.getString();
    ImpObject func = scope.Get(name);

    ArrayList<ImpObject> args = new ArrayList<ImpObject>();

    for (AST arg : exp.args) {
      ImpObject evaledArg = Evaluate(arg);

      args.add(evaledArg);
    }

    if (func.type == ImpTypes.Class) {
      return ((ImpClass)func).NewInstance(args, new ImpObject());
    }

    return ((ImpFunction)func).Call(args, new ImpObject());
  }

  ImpObject iString(AST exp) {
    return ConstructString(exp.value.getString());
  }

  ImpObject iInteger(AST exp) {
    return new ImpObject(Integer.parseInt(exp.value.getString()));
  }

  ImpObject iDouble(AST exp) {
    return new ImpObject(Double.parseDouble(exp.value.getString()));
  }

  ImpObject iBoolean(AST exp) {
    return new ImpObject(exp.value.getString().equals("true"));
  }

  ImpObject iFunction(AST exp) {
    Token identifier = exp.value;
    String name = identifier.getString();

    ImpObject func = new ImpFunction(this, exp);
    scope.Define(name, func);

    return func;
  }

  ImpObject iReturn(AST exp) {
    ImpObject returnValue = Evaluate(exp.scope);
    scope.returned = true;

    return returnValue;
  }

  ImpObject iClass(AST exp) {
    ImpObject cls = new ImpClass(this, exp);
    // Polymorphism actually hurts the brain it is proven.

    scope.Define(exp.value.getString(), cls);

    return cls;
  }

  // God this stuff looks so ugly.
  // Remember that thing I called a monstrosity in the parser?
  // Yea well this takes the cake this is way uglier to look at
  // tbh I don't even know what's going on here; I just wrote it.
  ImpObject iAccess(AST exp) {
    AST mainIdentifier = new AST(ExprTypes.Identifier, exp.value);

    ArrayList<ImpObject> propPath = new ArrayList<ImpObject>();
    ArrayList<String> propNames = new ArrayList<String>();

    ImpObject lastEvaluated = Evaluate(mainIdentifier);
    // Evauluates the first property

    propPath.add(lastEvaluated);
    propNames.add(exp.value.getString());
    // Add that evaluated value into the propPath list
    // and add the prop's name into a list too

    ArrayList<AST> props = exp.Access;
    // Get the props

    for (int i = 0; i < props.size(); i++) {
      AST propExp = props.get(i);
      String propName = (propExp.type == ExprTypes.Assign || propExp.type == ExprTypes.Binary)
        ? propExp.left.value.getString()
        : propExp.value.getString();
      // get the AST Expression and name of the property

      lastEvaluated = lastEvaluated.props.Get(propName, false);
      // Evauluates for regular identifier prop accessing      
      
      propPath.add(i + 1, lastEvaluated);
      propNames.add(i + 1, propName);
      // Adds the evaluated value and name into a list from i + 1 since the first one is already added into index 0.
      

      if (propExp.type == ExprTypes.Binary) {
        // Get the property from the last prop and evauluate the right expression
        ImpObject a = propPath.get(i).props.Get(propName);
        ImpObject b = Evaluate(propExp.right);
        lastEvaluated = iOperation(a, b, propExp.op.getString());

        propPath.set(i + 1, lastEvaluated);
        // Set the evaluated response into the propPath at its number of access (i + 1)
      }

      if (propExp.type == ExprTypes.FunctionCall) {
        if (lastEvaluated.isNull()) throw new Error("Cannot call a undefined function / property");

        ImpFunction func = (ImpFunction)lastEvaluated;
        func.interp = this;
        lastEvaluated = func.ExpCall(propExp.args, propPath.get(i));

        propPath.set(i + 1, lastEvaluated);
        // Call prop function and set lastEvaluated. Set the index in propPath to the lastEvaluated.
      } else if (propExp.type == ExprTypes.Assign) {
        if (propPath.get(i).isNull()) throw new Error("Cannot assign a property on an undefined property.");

        if (propExp.isBracketOp) {
          propName = Evaluate(propExp.left).getString();
          // Evaluate the left expression and get the String name of the property.

          propNames.set(i + 1, propName);
          // Set the name into propNames list
        }

        ImpObject val = Evaluate(propExp.right);
        ImpObject lastProp = propExp.isBracketOp
          ? Evaluate(propExp.left)
          : propPath.get(i);

        String op = propExp.op.getString();

        if (op.equals("+=") || op.equals("-=")) {
          ImpObject identifier = lastProp.props.Get(propName);
          val = iOperation(identifier, val, op);
        }

        lastProp.props.Set(propName, val);

        propPath.set(i + 1, val);
        propNames.set(i + 1, propName);

        ImpObject oldProp = propPath.get(0);

        for (int j = 1; j < propPath.size(); j++) {
          // Sets all properties from the prop list to make sure all references and copied versions of the objects have the same value.

          String name = propNames.get(j);
          oldProp = oldProp.props.Set(name, propPath.get(j));
        }

      } else if (propExp.type == ExprTypes.Access) {
        ImpObject val = iAccess(propExp);
        
        ImpObject prop = propPath.get(i).props.Get(val.getString());
        propPath.set(i + 1, prop);
        propNames.set(i + 1, val.getString());
        
        lastEvaluated = prop;
      } else if (propExp.isBracketOp) {
        ImpObject val = Evaluate(propExp);
        ImpObject prop = propPath.get(i).props.Get(val.getString());

        propPath.set(i + 1, prop);
        propNames.set(i + 1, val.getString());
        
        lastEvaluated = prop;
      }
    }
    scope.Set(exp.value.getString(), propPath.get(0));

    return lastEvaluated;
  }

  ImpObject iIf(AST exp) { return iIf(exp, true); }
  ImpObject iIf(AST exp, boolean extend) {
    if (extend)
      scope = new ImpScope(this, scope);
    
    ImpObject condition = Evaluate(exp.condition);

    ImpObject returnValue = new ImpObject();

    if (condition.getBoolean()) {
      returnValue = Evaluate(exp.then);

      if (scope.returned && extend) scope.parent.returned = true;
      if (extend)
        scope = scope.parent;
      
      return returnValue;
    }

    if (!exp.els.isNull()) {
      returnValue = Evaluate(exp.els);

      if (scope.returned && extend) scope.parent.returned = true;
      if (extend)
        scope = scope.parent;
      
      return returnValue;
    }
    
    return new ImpObject();
  }

  ImpObject iOperation(ImpObject a, ImpObject b, String op) {
    if (op.equals("==")) {

      if (a.type == ImpTypes.Int && a.type == ImpTypes.Int) {
        return new ImpObject(a.getInt() == b.getInt());
      } else if (a.type == ImpTypes.String && b.type == ImpTypes.String) {
        return new ImpObject(a.getString().equals(b.getString()));
      } else if (a.type == ImpTypes.Double && b.type == ImpTypes.Double) {
        return new ImpObject(a.getDouble() + b.getDouble());
      } else if (a.type == ImpTypes.Double && b.type == ImpTypes.Int) {
        return new ImpObject(a.getDouble() + b.getDouble());
      }

      return new ImpObject(false);

    } else if (op.equals("<")) {

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Int)
        return new ImpObject(a.getInt() < b.getInt());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Double)
        return new ImpObject(a.getDouble() < b.getDouble());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Int)
        return new ImpObject(a.getDouble() < b.getDouble());

      return new ImpObject(false);
      
    } else if (op.equals(">")) {

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Int)
        return new ImpObject(a.getInt() > b.getInt());

      return new ImpObject(false);

    } else if (op.equals("<=")) {

      return new ImpObject(iOperation(a, b, "<").getBoolean() || iOperation(a, b, "==").getBoolean());

    } else if (op.equals(">=")) {

      return new ImpObject(iOperation(a, b, ">").getBoolean() || iOperation(a, b, "==").getBoolean());

    } else if (op.equals("+") || op.equals("+=")) {

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Int)
        return new ImpObject(a.getInt() + b.getInt());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Double)
        return new ImpObject(a.getDouble() + b.getDouble());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Int)
        return new ImpObject(a.getDouble() + b.getDouble());

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Double)
        return new ImpObject(a.getInt() + b.getInt());

      if (a.type == ImpTypes.ClassString && b.type == ImpTypes.ClassString) {
        ImpClass cls = (ImpClass)scope.Get("String");

        ArrayList<ImpObject> newStr = new ArrayList<ImpObject>();
        newStr.add(a);

        ImpObject newInstance = cls.NewInstance(new ArrayList<ImpObject>(), new ImpObject());
        newInstance.type = ImpTypes.ClassString;
        ImpFunction method = (ImpFunction)newInstance.props.Get("concat");
        method.Call(newStr, newInstance);
        newStr.set(0, b);
        method.Call(newStr, newInstance);

        return newInstance;
      }

      if (a.type == ImpTypes.ClassString && b.type == ImpTypes.Int)
        return iOperation(a, ConstructString(b.getString()), "+");

      if (a.type == ImpTypes.ClassString && b.type == ImpTypes.Double)
        return iOperation(a, ConstructString(b.getString()), "+");

    } else if (op.equals("-") || op.equals("-=")) {

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Int)
        return new ImpObject(a.getInt() - b.getInt());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Double)
        return new ImpObject(a.getDouble() - b.getDouble());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Int)
        return new ImpObject(a.getDouble() - b.getDouble());

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Double)
        return new ImpObject(a.getInt() - b.getInt());

    } else if (op.equals("/")) {

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Int)
        return new ImpObject(a.getInt() / b.getInt());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Double)
        return new ImpObject(a.getDouble() / b.getDouble());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Int)
        return new ImpObject(a.getDouble() / b.getDouble());

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Double)
        return new ImpObject(a.getInt() / b.getInt());

    } else if (op.equals("*")) {

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Int)
        return new ImpObject(a.getInt() * b.getInt());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Double)
        return new ImpObject(a.getDouble() * b.getDouble());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Int)
        return new ImpObject(a.getDouble() * b.getDouble());

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Double)
        return new ImpObject(a.getInt() * b.getInt());

    } else if (op.equals("%")) {

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Int)
        return new ImpObject(a.getInt() % b.getInt());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Double)
        return new ImpObject(a.getDouble() % b.getDouble());

      if (a.type == ImpTypes.Double && b.type == ImpTypes.Int)
        return new ImpObject(a.getDouble() % b.getDouble());

      if (a.type == ImpTypes.Int && b.type == ImpTypes.Double)
        return new ImpObject(a.getInt() % b.getInt());

    }

    throw new Error("Invalid operator " + op + ".");
  }

  ImpObject iBinary(AST exp) {
    ImpObject a = Evaluate(exp.left);
    ImpObject b = Evaluate(exp.right);
    String op = exp.op.getString();

    return iOperation(a, b, op);
  }

  ImpObject iFor(AST exp) { return iFor(exp, true); }
  ImpObject iFor(AST exp, boolean extend) {
    ImpObject variable = new ImpObject();
    ImpObject condition = new ImpObject(true);
    ImpObject reassign = new ImpObject();

    if (extend)
      scope = new ImpScope(this, scope);

    if (!exp.assign.isNull()) {
      variable = Evaluate(exp.assign);
      if (exp.assign.type == ExprTypes.Variable) {
        variable = scope.Set(exp.assign.value.getString(), new ImpObject(0));
      }
    }
    if (!exp.condition.isNull()) {
      condition = Evaluate(exp.condition);
    }

    AST forScope = exp.scope;

    ImpObject returnValue = new ImpObject();

    ArrayList<AST> block = forScope.block;

    while (condition.getBoolean()) {
      if (!exp.condition.isNull()) {
        condition = Evaluate(exp.condition);
        if (!(condition.getBoolean())) break;
      }

      for (AST expr : block) {
        returnValue = Evaluate(expr);

        if (extend && scope.returned) {
          if (extend)
            scope = scope.parent;
          return returnValue;
        }
      }

      if (!exp.reassign.isNull()) {
        reassign = Evaluate(exp.reassign);
      }
    }

    if (extend)
      scope = scope.parent;

    if (!returnValue.isNull()) return new ImpObject();

    return returnValue;
  }

  ImpObject iArray(AST exp) {
    ArrayList<AST> args = exp.args;

    ImpClass arr = (ImpClass)scope.Get("Array");
    ImpObject arrObject = arr.NewInstance(new ArrayList<ImpObject>(), new ImpObject());

    for (int i = 0; i < args.size(); i++) {
      arrObject.props.Set(Integer.toString(i), Evaluate(args.get(i)));
    }

    arrObject.props.Set("length", new ImpObject(args.size()));

    return arrObject;
  }

  ImpObject iImport(AST exp) {
    String importPath = exp.value.getString();

    ImpObject exports = new Interpreter().Interpret(importPath);
    return scope.Set(exp.assign.value.getString(), exports);
  }

  ImpObject Evaluate(AST exp) {
    switch (exp.type) {
      case Scope: return iScope(exp);

      case String: return iString(exp);
      case Integer: return iInteger(exp);
      case Double: return iDouble(exp);
      case Boolean: return iBoolean(exp);

      case Array: return iArray(exp);

      case Binary: return iBinary(exp);

      case Variable: return iVariable(exp);
      case Assign: return iAssign(exp);
      case Identifier: return iIdentifier(exp);

      case Access: return iAccess(exp);

      case FunctionCall: return iFuncCall(exp);
      case Function: return iFunction(exp);
      case Class: return iClass(exp);

      case Return: return iReturn(exp);

      case If: return iIf(exp);
      case For: return iFor(exp);

      case Import: return iImport(exp);

      default: {
        System.out.println(exp);
        throw new Error("Invalid item");
      }
    }
  }

  public ImpScope InterpretFile(String file) {
    Lexer lexer = new Lexer(MFileReader.readFile(file));
    ArrayList<Token> tokens = lexer.tokenize();

    Parser parser = new Parser(tokens);

    AST mainAst = parser.parse();
    Interpreter interp = new Interpreter();

    ast = mainAst;
    interp.iScope(ast);
    return interp.scope;
  }

  public void loadAsGlobal(String file) {
    ImpScope globalScope = InterpretFile(file);

    // if (exports.type == ImpTypes.Class)
    //   scope.Set(((ImpClass)exports).className, exports);

    for (Entry<String, ImpObject> entry : globalScope.props.props.entrySet()) {
      scope.Set(entry.getKey(), entry.getValue());
    }
  }

  public ImpObject Interpret(String filename) {
    Lexer lexer = new Lexer(MFileReader.readFile(filename));
    Parser parser = new Parser(lexer.tokenize());

    AST mainAst = parser.parse();

    scope.Define("print", new ImpFunction(this, (ArrayList<ImpObject> funcArgs, Interpreter i) -> {
      for (ImpObject arg : funcArgs) {
        System.out.print(arg.getString() + " ");
      }

      System.out.println();

      return new ImpObject();
    }, "print"));

    scope.Define("input", new ImpFunction(this, (ArrayList<ImpObject> funcArgs, Interpreter i) -> {
      boolean isFirst = true;
      for (ImpObject arg : funcArgs) {
        if (isFirst) isFirst = false;
        else System.out.print(" ");

        System.out.print(arg.getString());
      }

      ImpObject str = new ImpObject(new Scanner(System.in).nextLine());

      return str;
    }, "input"));

    return Interpret(mainAst);
  }

  public ImpObject Interpret(AST mainAst) {
    ast = mainAst;

    // loadAsGlobal("./builtins/String.imp");
    // loadAsGlobal("./builtins/Array.imp");
    loadAsGlobal("./builtins/globals.imp");

    return iScope(mainAst, false);
  }
}