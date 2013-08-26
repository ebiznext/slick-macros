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
  // dynamic session & dynamic transaction would be more than welcome here
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
        case q"$mods def $name[..$tparams](...$vparamss): $tpt = $body" :: Nil =>
          val implictParam = vparamss.find { it =>
            it match {
              // "$IMPLICIT ..." did not work here
              case q"$mods val $name:$tpt = $rhs" :: Nil if mods.hasFlag(implict) && tpt.toString == "DbConnectionInfos" => true
              case _ => false
            }
          } map { it =>
            val q"$mods val $name:$tpt = $rhs" :: Nil = it
            name.decoded
          } map { it =>
            (it, None)
          } getOrElse {
            ("_dbOptions", Some(List(q"implicit val _dbOptions:DbConnectionInfos") :: Nil))
          }
          val implicitValName = newTermName(implictParam._1)

          val newvparams = implictParam._2 map(vparamss ++ _) getOrElse(vparamss)

          val defdef = q"""$mods def $name[..$tparams](...$newvparams): $tpt = { 
		    val _db =
		      if ($implicitValName.jndiName ne null)
		        Database.forName($implicitValName.jndiName)
		      else if ($implicitValName.dataSource ne null)
		        Database.forDataSource($implicitValName.dataSource)
		      else if ($implicitValName.driver ne null)
		        Database.forDriver($implicitValName.driver, $implicitValName.url, $implicitValName.user, $implicitValName.password, $implicitValName.properties)
		      else if ($implicitValName.driverClassName ne null)
		        Database.forURL($implicitValName.url, $implicitValName.user, $implicitValName.password, $implicitValName.properties, $implicitValName.driverClassName)
		        else
		          throw new SlickException("One of jndiName / dataSource / driver / driverClassName must be set")
              	_db withTransaction { 
              		$body 
              	}
              }"""
          defdef
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

