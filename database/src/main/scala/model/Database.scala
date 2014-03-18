package model

import slickmacros.annotations._
import slickmacros.annotations.ModelMacro._
import slickmacros.annotations.ModelMacro.FieldIndex._
import scala.slick.model.ForeignKeyAction.Cascade

@Model("PostgresDriver")
object XDb extends Timestamps {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value("ADMIN")
    val GUEST = Value("GUEST")
  }
  import UserRights._

  case class Company(name: String, website: String)
  case class Address(num: Int, road: String, zip: String) extends Part
  case class Member(login: String, rights: UserRights, addr: Address, company: Company, manager: Option[Member]) {
    constraints("members") {
      login is unique withType "varchar(100)" //withName ("LOGIN")
      manager onDelete Cascade
    }
  }
  case class Project(name: String, company: Company, members: List[Member])
}

