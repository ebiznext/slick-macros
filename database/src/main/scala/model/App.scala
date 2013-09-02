package model
/*
import scala.slick.driver.PostgresDriver.simple._
import scala.language.existentials
import java.lang.reflect.Method
import scala.slick.SlickException
import scala.reflect.ClassTag
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import Q.interpolation
import scala.language.dynamics
import language.experimental.macros
import scala.slick.lifted.MappedProjection
import TupleMethods._
import scala.slick.profile.BasicDriver

object XDb2 extends App {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  implicit def UserRightsTypeMapper = MappedColumnType.base[UserRights.Value, Int](
    {
      it => it.id
    },
    {
      id => UserRights(id)
    })
  import UserRights._
  case class Company(id: Option[Long], name: String, website: String) {
    def xid = id.getOrElse(throw new Exception("Object has no id yet"))
  }
  
  class CompanyTable(tag: Tag) extends TableEx[Company](tag, "company") with Crud[Company] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def website = column[String]("website")
    def * = id.? ~ name ~ website <> (Company.tupled, Company.unapply _)
    def forInsert = (name ~ website).shaped <> ({ t => Company(None, t._1, t._2) }, { (c: Company) => Some((c.name, c.website)) })
    def members = Members.where(_.companyId === id)
  }
  val Companies = TableQuery[CompanyTable]

  case class Member(id: Option[Long], login: String, rights: UserRights, companyId: Long, managerId: Option[Long]) {
    val companyQuery = Companies.findBy(_.id)
    def company(implicit session:Session) = companyQuery.first(companyId)
    def xid = id.getOrElse(throw new Exception("Object has no id yet"))

  }

  class MemberTable(tag: Tag) extends TableEx[Member](tag, "member") with Crud[Member] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc);
    def * = id.? ~ login ~ rights ~ companyId ~ managerId <> (Member.tupled, Member.unapply _)
    def login = column[String]("login");
    def rights = column[UserRights]("rights");
    def managerId = column[Option[Long]]("managerId");
    def companyId = column[Long]("companyId");
    def company = foreignKey("member2company", companyId, Companies)(_.id)
    def forInsert = (login ~ rights ~ companyId ~ managerId).shaped <> ({ t => Member(None, t._1, t._2, t._3, t._4) }, { (x: Member) => Some((x.login, x.rights, x.companyId, x.managerId)) });
  }
  val Members = TableQuery[MemberTable]

  case class Project(id: Option[Long], name: String, companyId: Long) {
    def xid = id.getOrElse(throw new Exception("Object has no id yet"))
    def members = Project2Members.where(_.projectId === id)
    def company(implicit session:Session) = Companies.where(_.id === companyId).first
  }

  abstract class TableEx[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {
  }
  trait Crud[T <: {
    def id: Column[Long]
    def forInsert: MappedProjection[T, _]
  }] { t: TableQuery[T, _] =>

    def delete(objId: Long)(implicit session:Session): Int = t.where(_.id === objId).delete
    def update(obj: T)(implicit session:Session): Int = (for { row <- this if row.id === obj.id.get } yield row) update (obj)
    def byId(objId: Long)(implicit session:Session): Option[T] = t.where(_.id === objId).firstOption
    def insert(obj: T)(implicit session:Session) = t.map(_.forInsert).returning(t.map(_.id)).insert(obj)

  }
  class ProjectTable(tag: Tag) extends TableEx[Project](tag, "project") with Crud[Project] {
    override def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = id.? ~ name ~ companyId <> (Project.tupled, Project.unapply _)
    def name = column[String]("name");
    def companyId = column[Long]("companyId");
    def forInsert = (name ~ companyId).shaped <> ({ t => Project(None, t._1, t._2) }, { (x: Project) => Some((x.name, x.companyId)) });
    def companyFK = foreignKey("project2company", companyId, Companies)(_.id)
  }
  val Projects = TableQuery[ProjectTable]

  case class Project2Member(projectId: Long, memberId: Long) {
    def project(implicit session:Session) = Projects.where(((x$1) => x$1.id.$eq$eq$eq(projectId))).first;
    def member(implicit session:Session) = Members.where(((x$1) => x$1.id.$eq$eq$eq(memberId))).first
  }

  class Project2MemberTable(tag: Tag) extends Table[Project2Member](tag, "project2member") {
    def projectId = column[Long]("projectId");
    def memberId = column[Long]("memberId");
    def * = (projectId ~ memberId).shaped <> (Project2Member.tupled, Project2Member.unapply _)
    def projectFK = foreignKey("project2member2project", projectId, Projects)(((x$1) => x$1.id));
    def memberFK = foreignKey("project2member2member", memberId, Members)(((x$1) => x$1.id))
  }
  val Project2Members = TableQuery[Project2MemberTable]
  Companies.ddl.createStatements.foreach(println)
  Members.ddl.createStatements.foreach(println)

  case class Company2(id: Option[Long], name: String, website: String) {
    def xid = id.getOrElse(throw new Exception("Object has no id yet"))
  }
  class Company2Table(tag: Tag) extends Table[Company2](tag, "company2") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def website = column[String]("website")
    def * = id.? ~ name ~ website <> (Company2.tupled, Company2.unapply _)
    def forInsert = (name ~ website).shaped <> ({ t => Company(None, t._1, t._2) }, { (c: Company) => Some((c.name, c.website)) })
    def members = Members.where(_.companyId === id)
    def pk = primaryKey("pkaaa", (name, website))
  }
  val Companies2 = TableQuery[Company2Table]

  println("*************")
  Companies2.ddl.createStatements.foreach(println)

  val allTables = Seq(Companies, Members, Projects)
}

*/
