package model

import scala.slick.driver.PostgresDriver.simple._
import scala.language.existentials
import java.lang.reflect.Method
import scala.slick.SlickException
import scala.reflect.ClassTag
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import Q.interpolation
import scala.language.dynamics
import language.experimental.macros
import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.NameTransformer
import scala.reflect.macros.Context
import scala.reflect.runtime.{ universe => u }
import slickmacros.annotations._
import slickmacros.Utils._
import TupleMethods._

import scala.slick.lifted.MappedProjection
import TupleMethods._
import scala.slick.profile.BasicDriver
import scala.slick.jdbc.JdbcBackend
import scala.slick.profile._
import slickmacros.annotations.ModelMacro._

@Model object XDb {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  import UserRights._

  case class Company(name: String, website: String) {
    //colType(website, "CLOB")
    colIndex(name, true)
  }
  case class Member(login: String, rights: UserRights, company: Company, manager: Option[Member])
  case class Project(name: String, company: Company, members: List[Member])

}



