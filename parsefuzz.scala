import scala.util.Random
import scala.language.implicitConversions

val maxDepth = 30

var r = new Random()

def choose[A](from: Seq[A]): A = from(r.nextInt(from.length))

abstract class Node {
  def produce(depth: Int): Unit

  def ? = new Opt(this)
  def *(min: Int = 0) = new Star(this, min)
  def ++(other: Node): Node = new Concat(this, other)
  def | : Node => Node = new Or(this, _)
}

case object IDENT extends Node {
  override def produce(depth: Int): Unit = {
    System.out.print(choose('a' to 'z'))
  }
}

case object INTEGER_LITERAL extends Node {
  override def produce(depth: Int): Unit = {
    System.out.print(choose('0' to '9'))
  }
}

case class Terminal(s: String) extends Node {
  override def produce(depth: Int): Unit = {
    System.out.print(s)
  }
}

implicit def String2Terminal(s: String): Node = new Terminal(s)

case class Or(n1: Node, n2: Node) extends Node {
  private def flatten(n: Node): Seq[Node] = n match {
    case or: Or => or.flattened
    case _ => Seq(n)
  }

  val flattened: Seq[Node] = flatten(n1) ++ flatten(n2)

  override def produce(depth: Int): Unit = {
    choose(flattened).produce(depth+1)
  }
}

case class Concat(n1: Node, n2: Node) extends Node {
  override def produce(depth: Int): Unit = {
    n1.produce(depth+1)
    n2.produce(depth+1)
  }
}

case class Opt(n: Node) extends Node {
  override def produce(depth: Int): Unit = {
    if (depth < maxDepth && r.nextBoolean()) {
      n.produce(depth+1)
    }
  }
}

case class Star(n: Node, min: Int = 0) extends Node {
  override def produce(depth: Int): Unit = {
    if (depth >= maxDepth) return
    1 to min foreach (_ => n.produce(depth+1))
    while (r.nextBoolean()) {
      n.produce(depth+1)
    }
  }
}

class Ind(n: => Node) extends Node {
  override def produce(depth: Int): Unit = n.produce(depth)
}

val Ind = new Ind(_)
def OneOf(ss: Seq[String]): Node = ss map Terminal reduce Or

val Program = ClassDeclaration.*(2)
lazy val ClassDeclaration: Node = "class " ++ IDENT ++ " {\n" ++ ("  " ++ ClassMember).*(5) ++ "\n}\n\n"
lazy val ClassMember: Node = Field | Method | MainMethod
lazy val Field: Node = "public " ++ Type ++ " " ++ IDENT ++ ";\n"
lazy val MainMethod: Node = "public static void " ++ IDENT ++ "(String[] " ++ IDENT ++ ") " ++ Block
lazy val Method: Node = "public " ++ Type ++ " " ++ IDENT ++ "(" ++ Parameters.? ++ ") " ++ Block
lazy val Parameters: Node = Parameter | Parameter ++ ", " ++ Ind(Parameters)
lazy val Parameter: Node = Type ++ " " ++ IDENT
lazy val Type: Node = Ind(Type) ++ "[]" | BasicType
lazy val BasicType: Node = "int" | "boolean" | "void" | IDENT
lazy val Statement: Node = Block | EmptyStatement | IfStatement | ExpressionStatement | WhileStatement | ReturnStatement
lazy val Block: Node = "{\n    " ++ BlockStatement.*() ++ "  \n  }\n"
lazy val BlockStatement: Node = Ind(Statement) | LocalVariableDeclarationStatement
lazy val LocalVariableDeclarationStatement: Node = Type ++ " " ++ IDENT ++ (" = " ++ Expression).? ++ ";\n    "
lazy val EmptyStatement: Node = ";\n   "
lazy val WhileStatement: Node = "while (" ++ Expression ++ ")\n    " ++ Ind(Statement)
lazy val IfStatement: Node = "if (" ++ Expression ++ ")\n    " ++ Ind(Statement) ++ ("else\n    " ++ Ind(Statement)).?
lazy val ExpressionStatement: Node = Expression ++ ";\n    "
lazy val ReturnStatement: Node = "return " ++ Expression.? ++ ";\n    "
lazy val Expression: Node =
  Ind(Expression) ++ " " ++ OneOf(List("!=", "*", "+", "-", "/", "<=", "<", "==", "=", ">=", ">", "%", "&&", "||")) ++ " " ++ Ind(Expression) |
  UnaryExpression
lazy val UnaryExpression: Node = PostfixExpression | ("!" | "-") ++ Ind(UnaryExpression)
lazy val PostfixExpression: Node = PrimaryExpression ++ PostfixOp.*()
lazy val PostfixOp: Node = MethodInvocation | FieldAccess | ArrayAccess
lazy val MethodInvocation: Node = "." ++ IDENT ++ "(" ++ Arguments ++ ")"
lazy val FieldAccess: Node = "." ++ IDENT
lazy val ArrayAccess: Node = "[" ++ Ind(Expression) ++ "]"
lazy val Arguments: Node = (Ind(Expression) ++ (", " ++ Ind(Expression)).*()).?
lazy val PrimaryExpression: Node = "null" | "false" | "true" | INTEGER_LITERAL | IDENT | IDENT ++ "(" ++ Arguments ++ ")" | "this" | "(" ++ Ind(Expression) ++ ")" | NewObjectExpression | NewArrayExpression
lazy val NewObjectExpression: Node = "new " ++ IDENT ++ "()"
lazy val NewArrayExpression: Node = "new " ++ BasicType ++ "[" ++ Ind(Expression) ++ "]" ++ Terminal("[]").*()

Program.produce(0)