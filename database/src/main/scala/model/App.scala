package model

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.session.Database
import scala.language.existentials
import java.lang.reflect.Method
import scala.slick.SlickException
import scala.reflect.ClassTag
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import Q.interpolation
import scala.language.dynamics
import language.experimental.macros
import Database.threadLocalSession
import slick.lifted.MappedTypeMapper

object XDb2 extends App {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  implicit def UserRightsTypeMapper = MappedTypeMapper.base[UserRights.Value, Int](
    {
      it => it.id
    },
    {
      id => UserRights(id)
    })
  import UserRights._

  case class CompanyId(val slickId: Long)
  implicit object CompanyId extends (Long => CompanyId)

  case class ProjectId(val slickId: Long)
  implicit object ProjectId extends (Long => ProjectId)

  case class MemberId(val slickId: Long)
  implicit object MemberId extends (Long => MemberId)

  implicit def IdTypeMapper[T <: { val slickId: Long }](implicit comap: Long => T) = MappedTypeMapper.base[T, Long](_.slickId, comap)

  case class Company(id: Option[CompanyId], name: String, website: String) {
    def xid = id.getOrElse(throw new Exception("Object has no id yet"))
  }

  class CompanyTable extends Table[Company]("company") {
    def id = column[CompanyId]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def website = column[String]("website")
    def * = id.? ~ name ~ website <> (Company, Company.unapply _)
    def forInsert = name ~ website <> ({ t => Company(None, t._1, t._2) }, { (c: Company) => Some((c.name, c.website)) })
    def members = Query(Members).where(_.companyId === id)
  }
  object Companies extends CompanyTable

  case class Member(id: Option[MemberId], login: String, rights: UserRights, companyId: CompanyId, managerId: Option[MemberId]) {
    def company = Query(Companies).where(_.id === companyId).first
    def xid = id.getOrElse(throw new Exception("Object has no id yet"))
  }

  class MemberTable extends Table[Member]("member") {
    def id = column[MemberId]("id", O.PrimaryKey, O.AutoInc);
    def * = id.? ~ login ~ rights ~ companyId ~ managerId <> (Member, Member.unapply _)
    def login = column[String]("login");
    def rights = column[UserRights]("rights");
    def managerId = column[Option[MemberId]]("managerId");
    def companyId = column[CompanyId]("companyId");
    def company = foreignKey("member2company", companyId, Companies)(_.id)
    def forInsert = login ~ rights ~ companyId ~ managerId <> ({ t => Member(None, t._1, t._2, t._3, t._4) }, { (x: Member) => Some((x.login, x.rights, x.companyId, x.managerId)) });
    def insert(obj: Member) = forInsert returning id insert obj
  }
  object Members extends MemberTable

  case class Project(id: Option[ProjectId], name: String, companyId: CompanyId) {
    def xid = id.getOrElse(throw new Exception("Object has no id yet"))
    def members = Query(Project2MemberTable).where(_.projectId === id)

  }

  class ProjectTable extends Table[Project]("project") {
    def id = column[ProjectId]("id", O.PrimaryKey, O.AutoInc)
    def * = id.? ~ name ~ companyId <> (Project, Project.unapply _)
    def name = column[String]("name");
    def companyId = column[CompanyId]("companyId");
    def forInsert = name ~ companyId <> ({ t => Project(None, t._1, t._2) }, { (x: Project) => Some((x.name, x.companyId)) });
    def insert(obj: Project) = forInsert.returning(id).insert(obj)
    def delete(objId: ProjectId) = this.where(_.id === objId).delete
    def update(obj: Project) = (for { row <- this if row.id === obj.xid } yield row) update (obj)
    def byId(objId: ProjectId) = Query(this).where(_.id === objId).firstOption
    def companyFK = foreignKey("project2company", companyId, Companies)(_.id)
  }
  object Projects extends ProjectTable

  case class Project2Member(projectId: ProjectId, memberId: MemberId) {
    def project = Query(Projects).where(((x$1) => x$1.id.$eq$eq$eq(projectId))).first;
    def member = Query(Members).where(((x$1) => x$1.id.$eq$eq$eq(memberId))).first
  }
  object Project2MemberTable extends Table[Project2Member]("project2member") {
    def projectId = column[ProjectId]("projectId");
    def memberId = column[MemberId]("memberId");
    def * = projectId ~ memberId <> (Project2Member, Project2Member.unapply _)
    def projectFK = foreignKey("project2member2project", projectId, Projects)(((x$1) => x$1.id));
    def memberFK = foreignKey("project2member2member", memberId, Members)(((x$1) => x$1.id))
  }

  Companies.ddl.createStatements.foreach(println)
  Members.ddl.createStatements.foreach(println)

  Database.forURL("jdbc:postgresql:SampleApp", user = "postgres", password = "e-z12B24", driver = "org.postgresql.Driver") withTransaction {
    //Companies.insert(Company(None, "ebiz", "http://www.ebiz.fr"))
    //Members.insert(Member(None, "hayssams", UserRights.ADMIN, CompanyId(1)))
    //val x = Query(Companies).where(_.id === CompanyId(1)).first
    val x = Query(Members).where(_.id === MemberId(1)).first
    val xx = x.xid
    println(x + "," + xx)
    println(xx)
    println(x.company)
    val y = x.company
  }

  val allTables = Seq(Companies, Members, Projects)
}


