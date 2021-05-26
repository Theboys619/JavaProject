import java.util.ArrayList;

enum ExprTypes {
  None,
  While,
	For,
	If,
	Integer,
  Double,
	Float,
	String,
	Boolean,
	Class,
	Access,
  Index,
	Array,
	Variable,
	Import,
	Identifier,
	Assign,
	Binary,
	Scope,
	FunctionCall,
	Function,
	FunctionDecl,
	Return,
	Datatype
}

// The main AST class
// Used for creating the Abstract Syntax Tree
public class AST {
  ExprTypes type;
  Token value;

  // Binary
  AST left;
  Token op;
  AST right;

  boolean returned = false;
  boolean isBracketOp = false;
  boolean isCall = false;

  // Scopes and Functinos

  ArrayList<AST> block = new ArrayList<AST>();
  ArrayList<AST> args = new ArrayList<AST>();
  AST scope;
  boolean isstatic = false;

  // If
  AST condition;
  AST then;
  AST els;

  // For
  AST reassign;
  AST assign;

  // Identifiers
  ArrayList<AST> Access = new ArrayList<AST>();

  AST() {
    type = ExprTypes.None;
    value = new Token("NULL", "NULL");
  }
  AST(Token val) {
    type = ExprTypes.None;
    value = val;
  }
  AST(ExprTypes exprtype, Token val) {
    type = exprtype;
    value = val;
  }

  public void setType(ExprTypes exprType) {
    type = exprType;
  }

  public void setValue(Token val) {
    value = val;
  }

  public String toString() {
    return "TYPE#: " + type + ", Value: " + value.getString();
  }

  public boolean isNull() {
    return type == ExprTypes.None;
  }
}