import slickmacros.reflect._
import slickmacros.annotations._
import slickmacros.dao.Crud._
import scala.slick.driver.PostgresDriver.simple._

import  slickemf.export._

object SampleApp extends App {
  import model.XDb._
  implicit val dbConnectionInfo = DbConnectionInfos(url = "jdbc:postgresql:SampleApp", user = "postgres", password = "e-z12B24", driverClassName = "org.postgresql.Driver")
  
  val ddls = companyQuery.ddl ++ memberQuery.ddl ++ projectQuery.ddl ++ project2MemberQuery.ddl
  val stmts = ddls.createStatements ++ ddls.dropStatements
  stmts.foreach(println)
  

  object companyDAO extends Crud[Company, CompanyTable](companyQuery) {}
  object memberDAO extends Crud[Member, MemberTable](memberQuery) {}
  object projectDAO extends Crud[Project, ProjectTable](projectQuery) {}

  @DBTransaction def populate() {
    val csize = companyQuery.list.size
    if (csize == 0) {
      val typesafeId = companyDAO.insert(Company(None, "typesafe", "http://www.typesafe.com"))
      val martinId = memberDAO.insert(Member(None, "modersky", UserRights.ADMIN, Address(1,"ici", "10001"), typesafeId, None))
      val szeigerId = memberDAO.insert(Member(None, "szeiger", UserRights.GUEST, Address(1,"ici", "10001"),typesafeId, Some(martinId)))

      val slickId = projectDAO.insert(Project(None, "Slick", typesafeId))
      project2MemberQuery.insert(Project2Member(slickId, martinId))
      val project = projectQuery.where(_.name === "Slick").first
      project.addMember(szeigerId)
    }
  }

  @DBSession def queryDB() {
    val company = companyQuery.where(_.name === "typesafe").first
    val project = projectQuery.where(_.name === "Slick").first

    val members = project.members.list
    members.foreach { m =>
      m.manager.map(println)
    }
    project.members.drop(1).take(1).list.foreach(println)
  }
  populate
  queryDB
  val descs = new ObjectRef(model.XDb).reflect
  new Export().export2EMF(descs, "database/slickemf.ecore")

}
