package model

import slickmacros.annotations._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.ForeignKeyAction
import scala.slick.lifted.ForeignKeyAction._
//import scala.slick.driver.JdbcDriver.simple._


@Model object XDb extends Timestamps {
  
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  import UserRights._

  case class Company(name: String, website: String) extends Timestamps
  case class Address(num: Int, road: String, zip: String) extends Part
  case class Member(login: String, rights: UserRights, addr: Address, company: Company, manager: Option[Member]) {
    constraints {
      login is unique
      (login, company) withType "varchar(100)"
      manager onDelete Cascade
    }
  }
  case class Project(name: String, company: Company, members: List[Member])
}




