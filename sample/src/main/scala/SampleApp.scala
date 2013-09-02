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
import slickmacros._
import slickmacros.Utils._
import slickmacros.reflect._
import scala.reflect.runtime.universe._
import slickemf.export._

object SampleApp extends App {
  import model.XDb._
  def allTableObjects(any: Any): List[Symbol] = {
    val typeMirror = runtimeMirror(any.getClass.getClassLoader)
    val instanceMirror = typeMirror.reflect(any)
    val members = instanceMirror.symbol.typeSignature.members
    members.filter(_.typeSignature <:< typeOf[Table[_]]) toList
  }

  val ddls = Companies.ddl ++ Members.ddl ++ Projects.ddl ++ Project2Members.ddl
  val stmts = ddls.createStatements ++ ddls.dropStatements
  //stmts.foreach(println)
  implicit val dbConnectionInfo = DbConnectionInfos(url = "jdbc:postgresql:SampleApp", user = "postgres", password = "e-z12B24", driverClassName = "org.postgresql.Driver")

  @Transactional def allCompanies = Query(Companies).list

  @SessionOnly def allCompaniesExplicit(i: Int)(implicit x: DbConnectionInfos) = Query(Companies).list

  @Transactional def populate() {
    val csize = Query(Companies).list.size
    if (csize == 0) {
      val typesafeId = Companies.insert(Company(None, "typesafe", "http://www.typesafe.com"))
      val martinId = Members.insert(Member(None, "modersky", UserRights.ADMIN, typesafeId, None))
      val szeigerId = Members.insert(Member(None, "szeiger", UserRights.GUEST, typesafeId, Some(martinId)))

      val slickId = Projects.insert(Project(None, "Slick", typesafeId))
      Project2Members.insert(Project2Member(slickId, martinId))
      val project = Query(Projects).where(_.name === "Slick").first
      project.addMember(szeigerId)
    }
  }

  @SessionOnly def queryDB() {
    val company = Query(Companies).where(_.name === "typesafe").first
    val project = Query(Projects).where(_.name === "Slick").first

    val members = project.members.list
    members.foreach { m =>
      m.manager.map(println)
    }
    project.members.drop(1).take(1).list.foreach(println)
  }
  val descs = new ObjectRef(model.XDb).reflect
  Export.cases2Emf(descs, "database/slickcases.ecore")
  Export.tables2Emf(descs, "database/slicktables.ecore")

}