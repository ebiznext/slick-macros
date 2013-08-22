package slickmacros

import java.sql.Driver

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.runtime.universe._

import javax.sql.DataSource
import java.util.Properties

case class DbConnectionInfos(
  jndiName: String = null,
  dataSource: DataSource = null,
  url: String = null,
  user: String = null,
  password: String = null,
  driverClassName: String = null,
  driver: Driver = null,
  properties: Properties = null)

/**
 * Work in progress
 * Just reference it in front of any method and the Database context will be injected
 */
object TransactionMacro {
  def implTransaction(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    impl(c, "withTransaction")(annottees: _*)
  }
  def implSession(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    impl(c, "withSession")(annottees: _*)
  }
  def impl(c: Context, sessionType: String)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._
    val param = scala.reflect.internal.Flags.PARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val implict = scala.reflect.internal.Flags.IMPLICIT.asInstanceOf[Long].asInstanceOf[FlagSet]
    // Quasiquotes would be more than welcome here :(
    val result = {
      annottees.map(_.tree).toList match {
        case DefDef(mods: Modifiers, name: Name, tparams: List[TypeDef], vparamss: List[List[ValDef]], tpt: Tree, rhs: Tree) :: Nil =>
          // case DefDef(mods: Modifiers, name: Name, tparams: List[TypeDef], vparamss: List[List[ValDef]], tpt: Tree, rhs: Tree) :: Nil =>
          val implictParam = vparamss.find { it =>
            it match {
              case ValDef(mods, name, Ident(tpe), _) :: Nil if mods.hasFlag(implict) && tpe.decoded == "DbConnectionInfos" => true
              case _ => false
            }
          } map { it =>
            val ValDef(_, name, _, _) :: Nil = it
            name.decoded
          } map { it =>
            (it, Nil :: Nil)
          } getOrElse {
            ("_dbOptions", List(ValDef(Modifiers(implict | param), newTermName("_dbOptions"), Ident(newTypeName("DbConnectionInfos")), EmptyTree) :: Nil))
          }
          val implicitValName = implictParam._1

          /*
          s"""
		    val db =
		      if ($implicitValName.jndiName ne null)
		        Database.forName($implicitValName.jndiName)
		      else if ($implicitValName.dataSource ne null)
		        Database.forDataSource($implicitValName.dataSource)
		      else if ($implicitValName.driver ne null)
		        Database.forDriver($implicitValName.driver, $implicitValName.url, $implicitValName.user, $implicitValName.password, $implicitValName.properties)
		      else if (_dbOptions.driverClassName ne null)
		        Database.forURL($implicitValName.url, $implicitValName.user, $implicitValName.password, $implicitValName.properties, $implicitValName.driverClassName)
		        else
		          throw new SlickException("One of jndiName / dataSource / driver / driverClassName must be set")
		    db withTransaction {
		
		    }
              """
              */
          DefDef(
            mods,
            name,
            tparams,
            vparamss ++ implictParam._2,
            tpt,
            Block(
              List(
                ValDef(
                  Modifiers(),
                  newTermName("_db"),
                  TypeTree(),
                  If(Apply(Select(Select(Ident(newTermName(implicitValName)), newTermName("jndiName")), newTermName("ne")), Literal(Constant(null)) :: Nil),
                    Apply(Select(Ident(newTermName("Database")), newTermName("forName")), Select(Ident(newTermName(implicitValName)), newTermName("jndiName")) :: Nil),
                    If(Apply(Select(Select(Ident(newTermName(implicitValName)), newTermName("dataSource")), newTermName("ne")), Literal(Constant(null)) :: Nil),
                      Apply(Select(Ident(newTermName("Database")), newTermName("forDataSource")), Select(Ident(newTermName(implicitValName)), newTermName("dataSource")) :: Nil),
                      If(Apply(Select(Select(Ident(newTermName(implicitValName)), newTermName("driver")), newTermName("ne")), Literal(Constant(null)) :: Nil),
                        Apply(Select(Ident(newTermName("Database")), newTermName("forDriver")), List(Select(Ident(newTermName(implicitValName)), newTermName("driver")), Select(Ident(newTermName(implicitValName)), newTermName("url")), Select(Ident(newTermName(implicitValName)), newTermName("user")), Select(Ident(newTermName(implicitValName)), newTermName("password")), Select(Ident(newTermName(implicitValName)), newTermName("properties")))),
                        If(Apply(Select(Select(Ident(newTermName(implicitValName)), newTermName("driverClassName")), newTermName("ne")), Literal(Constant(null)) :: Nil),
                          Apply(Select(Ident(newTermName("Database")), newTermName("forURL")), List(Select(Ident(newTermName(implicitValName)), newTermName("url")), Select(Ident(newTermName(implicitValName)), newTermName("user")), Select(Ident(newTermName(implicitValName)), newTermName("password")), Select(Ident(newTermName(implicitValName)), newTermName("properties")), Select(Ident(newTermName(implicitValName)), newTermName("driverClassName")))),
                          Throw(Apply(Select(New(Ident(newTypeName("SlickException"))), nme.CONSTRUCTOR), Literal(Constant("One of jndiName / dataSource / driver / driverClassName must be set")) :: Nil)))))))),
              Apply(Select(Ident(newTermName("_db")), newTermName(sessionType)), rhs :: Nil)))
        case _ => c.abort(c.enclosingPosition, "Transaction may be attached to a method definition only")
      }
    }
    println(result)
    c.Expr[Any](result)
  }
}

class Transactional extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro TransactionMacro.implTransaction
}

class SessionOnly extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro TransactionMacro.implSession
}

