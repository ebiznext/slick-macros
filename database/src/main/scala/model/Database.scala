package model

import slickmacros.annotations._
import scala.slick.driver.PostgresDriver.simple._
//import scala.slick.driver.JdbcDriver.simple._

@Model object XDb {

  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  import UserRights._

 @Entity(timestamps=true) case class Company(name: String, website: String)

  @Part case class Address(num: Int, @Type("varchar(1024)") road: String, zip: String)
  case class Member(@Index(true) login: String, rights: UserRights, add: Address, company: Company, manager: Option[Member])
  case class Project(name: String, company: Company, members: List[Member])

}

