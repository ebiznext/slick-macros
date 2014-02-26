import model.XDb._
import slickmacros.annotations._
import slickmacros.Implicits._
import scala.slick.driver.PostgresDriver.simple._


object Services {
  implicit val dbConnectionInfo = DbConnectionInfos(url = "jdbc:postgresql:SampleApp", user = "postgres", password = "e-z12B24", driverClassName = "org.postgresql.Driver")


  val ddls = companies.ddl ++ members.ddl ++ projects.ddl ++ project2Members.ddl
  val stmts = ddls.createStatements ++ ddls.dropStatements
  stmts.foreach(println)

  @DBSession def populate(i:Int, c:Company) {
    val csize = companies.list.size
    val id = companies.insert(Company(None, "test", "test"))
    val comp = companies.byId(id)
    println(comp)

    if (csize == 0) {
      val typesafeId = companies returning (companies.map(_.id)) insert (Company(None, "typesafe", "http://www.typesafe.com"))
      val martinId = members returning (members.map(_.id)) insert (Member(None, "modersky", UserRights.ADMIN, Address(1, "ici", "10001"), typesafeId, None))
      val szeigerId = members returning (members.map(_.id)) insert (Member(None, "szeiger", UserRights.GUEST, Address(1, "ici", "10001"), typesafeId, Some(martinId)))
      println(typesafeId + "," + martinId + "," + szeigerId)
      val slickId = projects.insert(Project(None, "Slick", typesafeId))
      project2Members.insert(Project2Member(slickId, martinId))
      val project = projects.where(_.name === "Slick").first
      project.addMember(szeigerId)
    }
    else
      println(csize)
  }

  @DBSession def queryDB() {
    val query = companies.where(_.id === 1L)
    val mappedQuery = companies.map(row => (row.name, row.website))
    mappedQuery.update(("newCompanyName", "http://newWebSite.com"))

    val xx = for {
      m <- members
      c <- m.company if c.name === "newCompanyName"
    } yield (m)
    println(xx.list.size)

    val query2 = companies.where(_.id === 1L)

    query2.doUpdate(name = "typesafe", website = "http://www.typesafe.com")

    //(company, member).doWhere(_1.name === "")
    val company = companies.where(_.name === "typesafe").first
    val project = projects.where(_.name === "Slick").first

    //val members = project.mymembers.list
    members.foreach {
      m =>
        m.loadManager.map(println)
    }
    project.loadMembers.drop(1).take(1).list.foreach(println)
  }


}
