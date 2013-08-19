import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.util.parsing.combinator._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{ currentMirror => rm }
import scala.reflect.runtime.{ universe => ru }


/**
 * Work in progress 
 * Just reference it in front of any method and the Database context will be injected
 */
object TransactionalMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    val result = {
      annottees.map(_.tree).toList match {
        case DefDef(mods: Modifiers, name: Name, tparams: List[TypeDef], vparamss: List[List[ValDef]], tpt: Tree, rhs: Tree) :: Nil =>
          DefDef(mods, name, tparams, vparamss, tpt, Apply(Select(Apply(Select(Ident(newTermName("Database")), newTermName("forURL")), List(Literal(Constant("jdbc:postgresql:tetra")), AssignOrNamedArg(Ident(newTermName("user")), Literal(Constant("tetra"))), AssignOrNamedArg(Ident(newTermName("password")), Literal(Constant("e-z12B24"))), AssignOrNamedArg(Ident(newTermName("driver")), Literal(Constant("org.postgresql.Driver"))))), newTermName("withSession")), List(rhs)))
        case _ => c.abort(c.enclosingPosition, "Transactional may be attached to a method definition only")
      }
    }
    println(result)
    c.Expr[Any](result)
  }
}

class Transactional extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro TransactionalMacro.impl
}