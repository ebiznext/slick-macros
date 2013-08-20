import scala.slick.driver.PostgresDriver.simple._
import scala.slick.session.Database
import scala.slick.session.Database.forDataSource
import scala.language.existentials
import java.lang.reflect.Method
import scala.slick.SlickException
import scala.reflect.ClassTag
import Database.threadLocalSession
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import Q.interpolation
import scala.language.dynamics
import language.experimental.macros
import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.NameTransformer
import scala.reflect.macros.Context
import scala.reflect.runtime.{ universe => u }
import scala.slick.lifted.MappedTypeMapper

object SampleApp extends App {
  import XDb._
  val ddls = Companies.ddl ++ Members.ddl ++ Project2Members.ddl
  val stmts = ddls.createStatements ++ ddls.dropStatements
  stmts.foreach(println(_))

}
