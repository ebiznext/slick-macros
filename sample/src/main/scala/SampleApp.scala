import slickmacros.reflect._
import slickmacros.annotations._
import slickmacros.dao.Crud._
import slickmacros.Implicits._
import scala.slick.driver.PostgresDriver.simple._
import slickemf.export._
import slickmacros._
import scala.reflect.macros.Context
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.universe._

import scala.reflect.runtime.{ universe => u }
import scala.language.experimental.macros
import scala.language.dynamics
import scala.slick.lifted.{ Query => LQuery }
import scala.slick.lifted.ColumnBase


object SampleApp extends App {
  import model.XDb._
  implicit val dbConnectionInfo = DbConnectionInfos(url = "jdbc:postgresql:SampleApp", user = "postgres", password = "e-z12B24", driverClassName = "org.postgresql.Driver")

  val ddls = companies.ddl ++ members.ddl ++ projects.ddl ++ project2Members.ddl
  val stmts = ddls.createStatements ++ ddls.dropStatements
  stmts.foreach(println)

  @DBTransaction def populate() {
    object CompanyDAO extends CompanyCrud(companies) {
      
    }
    val csize = companies.list.size
    if (csize == 0) {
      val typesafeId = companies.insert(Company(None, "typesafe", "http://www.typesafe.com"))
      val martinId = members.insert(Member(None, "modersky", UserRights.ADMIN, Address(1, "ici", "10001"), typesafeId, None))
      val szeigerId = members.insert(Member(None, "szeiger", UserRights.GUEST, Address(1, "ici", "10001"), typesafeId, Some(martinId)))

      val slickId = projects.insert(Project(None, "Slick", typesafeId))
      project2Members.insert(Project2Member(slickId, martinId))
      val project = projects.where(_.name === "Slick").first
      project.addMember(szeigerId)
    }
  }

  @DBSession def queryDB() {
    val query = companies.where(_.id === 1L)
    val mappedQuery = companies.map(row => (row.name, row.website))
    mappedQuery.update(("newCompanyName", "http://newWebSite.com")) 
    
    for {
      c <- companies if c.name === "myCompanyName"
      m <- members if m.companyId === 0L
    } yield(m)
    
    //(Company(), Member()).doWhere(_1.id === 1).doYield(_2)
    
    val query2 = companies.where(_.id === 1L)
    
    query2.doUpdate(name = "typesafe", website = "http://www.typesafe.com")
    
    //(company, member).doWhere(_1.name === "")
    val company = companies.where(_.name === "typesafe").first
    val project = projects	.where(_.name === "Slick").first

    //val members = project.mymembers.list
    members.foreach { m =>
      m.loadManager.map(println)
    }
    project.loadMembers.drop(1).take(1).list.foreach(println)
  }

  populate
  queryDB
  val descs = new ObjectRef(model.XDb).reflect
  new Export().export2EMF(descs, "database/slickemf.ecore")

}
