import model.XDb._
import scala.util.Try
import slickmacros.annotations._
import slickmacros.Implicits._
import scala.slick.driver.PostgresDriver.simple._

object Services {
  implicit val dbConnectionInfo = DBConnectionInfo(url = "jdbc:postgresql:smacros",
    user = "smacros",
    password = "smacros",
    driverClassName = "org.postgresql.Driver")

  val ddls = companies.ddl ++ members.ddl ++ projects.ddl ++ project2Members.ddl

  @DBSession def populate() {
    Try(ddls.drop)
    ddls.create

    val typesafeId = companies.insert(Company(None, "typesafe", "http://www.typesafe.com"))
    val martinId = members.insert(Member(None, "modersky", UserRights.ADMIN, Address(1, "ici", "10001"), typesafeId, None))
    val szeigerId = members.insert(Member(None, "szeiger", UserRights.GUEST, Address(1, "ici", "10001"), typesafeId, Some(martinId)))

    val slickId = projects.insert(Project(None, "Slick", typesafeId))
    project2Members.insert(Project2Member(slickId, martinId))

    val project = projects.where(_.name === "Slick").first
    project.addMember(szeigerId)
  }

  @DBSession def queryDB() {
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
    members.foreach(m => m.loadManager.map(println))
    project.loadMembers.drop(1).take(1).list.foreach(println)
  }
}
