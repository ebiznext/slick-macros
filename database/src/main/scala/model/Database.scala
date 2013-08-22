package model

import scala.slick.driver.PostgresDriver.simple._
import slickmacros._

@Model object XDb {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  import UserRights._
  case class Company(name: String, website: String)
  case class Member(login: String, rights: UserRights, company: Company)
  case class Project(name: String, company: Company, members: List[Member])
}
