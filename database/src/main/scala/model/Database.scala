package model

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

@Model object XDb {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }

  import UserRights._

  /*
 *  
  implicit Constraints(x:String) {
    def is
    def not
    def Nullable
    validate() 
  }
    def constraints(t: Company) {
        t.name is not Nullable and is not Blank and matches "regexp" and  is in future and is between 1 and 2 and is in ("ABC", "RTE", "", "") orElse "t con ou quoi " and is anticsrf hasLength(220)
    }
* 
*/
  
  
  case class Company(name: String, website: String)
  case class Member(login: String, rights: UserRights, company: Company, manager: Option[Member])
  case class Project(name: String, company: Company, members: List[Member])
}
