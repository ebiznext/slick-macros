
/*
object XDb2 {
  case class Company(id: Option[Int], name: String, website: String, website2: Option[String], website3: String,
    dateCreated: Option[java.sql.Date], dateUpdated: Option[java.sql.Date], users: List[XUser])
  case class XUser(id: Option[Int], login: String, companyId: Option[Int], company: Option[Company], company2: Company, dateCreated: Option[java.sql.Date], dateUpdated: Option[java.sql.Date], companies:List[Company])
}
object XDb {
  //@Entity case class Company(name: String, website: String, website2: Option[String], website3: String)
  //@Entity  case class XUser(login: String, rights: UserRights.Value, companyId: Option[Int])
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  case class Company(id: Option[Int], name: String, website: String, website2: Option[String], website3: String, dateCreated: Option[java.sql.Date], dateUpdated: Option[java.sql.Date])
  object Companies extends Table[Company]("company") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def website = column[String]("website")
    def * = id.? ~ name ~ website ~ website2 ~ website3 ~ dateCreated.? ~ dateUpdated.? <> (Company, Company.unapply _)
    def website2 = column[Option[String]]("website2")
    def website3 = column[String]("website3")
    def dateCreated = column[java.sql.Date]("dateCreated")
    def dateUpdated = column[java.sql.Date]("dateUpdated")
    def forInsert = name ~ website ~ website2 ~ website3 ~ dateCreated.? ~ dateUpdated.? <> ({
      t => Company(None, t._1, t._2, t._3, t._4, t._5, t._6)
    }, {
      (u: Company) => Some((u.name, u.website, u.website2, u.website3, u.dateCreated, u.dateUpdated))
    })
  }
  case class XUser(id: Option[Int], login: String, rights: UserRights.Value, companyId: Option[Int], company: Option[Company], company2: Company, dateCreated: Option[java.sql.Date], dateUpdated: Option[java.sql.Date])
   implicit val UserRightsTypeMapper = MappedTypeMapper.base[UserRights.Value, Int](
    {
      it => it.id
    },
    {
      id => UserRights(id)
    })
  import UserRights._

  case class Company(name: String, website: String, website2: Option[String], website3: String)
  case class Y

  object U extends Z[Y] {
  }
  object Companies extends Table[Company]("company") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def website = column[String]("website")
    def website2 = column[Option[String]]("website")
    def website3 = column[String]("website")
    def * = id.? ~ name ~ website ~ website2 ~ website3 <> (Company, Company.unapply _)
    def forInsert = name ~ website ~ website2 ~ website3 <> ({
      t => Company(None, t._1, t._2, t._3, t._4)
    }, {
      (u: Company) => Some((u.name, u.website, u.website2, u.website3))
    })
  }
  object X 
  object Y {
    val x = 0
  }
  import scala.reflect.runtime.universe._
  implicit val UserRightsTypeMapper = MappedTypeMapper.base[UserRights.Value, Int](
    {
      it => it.id
    },
    {
      id => UserRights(id)
    })

  import UserRights._
  
    object Companies extends Table[Company]("company") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def website = column[String]("website")
    def website2 = column[Option[String]]("website")
    def website3 = column[String]("website")
    def * = id.? ~ name ~ website ~ website2 ~ website3 <> (Company, Company.unapply _)
    def forInsert = name ~ website ~ website2 ~ website3 <> ({
      t => Company(None, t._1, t._2, t._3, t._4)
    }, {
      (u: Company) => Some((u.name, u.website, u.website2, u.website3))
    })
  }

  object XUsers extends Table[XUser]("xuser") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def login = column[String]("name")
    def rights = column[UserRights.Value]("name")
    def companyId = column[Option[Int]]("company_fk")
    def * = id.? ~ login ~ rights ~ companyId <> (XUser, XUser.unapply _)
    def forInsert = login ~ rights ~ companyId <> ({
      t => XUser(None, t._1, t._2, t._3)
    }, {
      (u: XUser) => Some((u.login, u.rights, u.companyId))
    })
  }
}
*/


/*
@Entity case class UserData(login: String, pwd: String)
object Test extends App {
  val myData = UserData(None, "a", "b")
  println("Should print None =>" + myData.dateCreated)
  
  
  @Transactional def myService = {
    DefMacroTable.insert(DefMacroData(None, "login", "secret"))
  }
}

*/
/*
 import scala.slick.driver.PostgresDriver.simple._
import scala.slick.session.Database
import scala.slick.session.Database.forDataSource
import scala.slick.SlickException
import Database.threadLocalSession
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import Q.interpolation

case class DefMacroData(id: Option[Int], login: String, pwd: String)

object DefMacroTable extends Table[DefMacroData]("defmacrodata") with DynamicAccessors[DefMacroData] {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def login = column[String]("login")
  def pwd = column[String]("pwd")

  def * = id.? ~ login ~ pwd <> (DefMacroData, DefMacroData.unapply _)

  def forInsert = login ~ pwd <> ({
    t => DefMacroData(None, t._1, t._2)
  }, {
    (u: DefMacroData) => Some((u.login, u.pwd))
  })

    def doInsert(r: DefMacroData) = DefMacroTable.forInsert returning DefMacroTable.id insert r

}
 
*/
/*
    // This conversion only works for fully packed types
    implicit def productQueryToUpdateInvoker[T](q: Query[_ <: ColumnBase[T], T]): UpdateInvoker[T] =
      createUpdateInvoker(updateCompiler.run(Node(q)).tree)
*/