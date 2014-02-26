import model.XDb._
import org.joda.time.DateTime
import scala.slick.jdbc.JdbcBackend
import scala.slick.profile.RelationalTableComponent
import scala.Some
import slickmacros.annotations._
import slickmacros.Implicits._
import scala.slick.driver.PostgresDriver.simple._

import scala.reflect.runtime.{universe => u}
import scala.language.experimental.macros
import scala.language.dynamics
import scala.slick.lifted.{Query => LQuery, MappedProjection}


object Services {

//  type TableEx[C] = {
//    def id: Column[Long]
//    def forInsert: MappedProjection[C, _]
//  }
//
//  type RowId = {
//    def id: Option[Long]
//  }
//
//  type RowIdEx = {
//    def id: Option[Long]
//    var dateCreated: org.joda.time.DateTime
//    var lastUpdated: org.joda.time.DateTime
//  }
//
//
//  trait Crud[C <: RowId, +T <: RelationalTableComponent#Table[C] with TableEx[C]] {
//    self: TableQuery[T] =>
//
//    def del(objId: Long)(implicit session: JdbcBackend#SessionDef) = {
//      self.where(_.id === objId)
//    }
//
//    def find(objId: Long)(implicit session: JdbcBackend#SessionDef): Option[C] = self.where(_.id === objId).firstOption
//
//    def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
//      val res = (for {row <- self if row.id === obj.id.get} yield row) update (obj)
//      res
//    }
//
//    def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
//      val res = self returning (self.map(_.id)) insert (obj)
//      res
//    }
//  }
//
//  trait CrudEx[C <: RowIdEx, +T <: RelationalTableComponent#Table[C] with TableEx[C]] extends Crud[C, T] {
//    self: TableQuery[T] =>
//    override def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
//      obj.lastUpdated = DateTime.now
//      super.update(obj)
//    }
//
//    override def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
//      // because x.copy(dateCreated = , lastUpdated = ) is not available :(
//      obj.dateCreated = DateTime.now
//      obj.lastUpdated = obj.dateCreated
//      super.insert(obj)
//    }
//  }

  val ddls = companies.ddl ++ members.ddl ++ projects.ddl ++ project2Members.ddl
  val stmts = ddls.createStatements ++ ddls.dropStatements
  stmts.foreach(println)

  @DBTransaction def populate()(implicit ev: DbConnectionInfos) {
    val csize = companies.list.size
    Company(None, "", "", null, null)
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
