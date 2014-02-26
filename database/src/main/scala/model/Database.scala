package model

import slickmacros.annotations._
import slickmacros.annotations.ModelMacro._
import slickmacros.annotations.ModelMacro.FieldIndex._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.model.ForeignKeyAction.Cascade

object Implicits {
  implicit val DateTimeTypeMapper =
    MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
    {
      dt => new java.sql.Timestamp(dt.getMillis)
    }, {
      ts => new org.joda.time.DateTime(ts.getTime)
    })
}

@Model
object XDb  extends Timestamps {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value("ADMIN")
    val GUEST = Value("GUEST")
  }
  import UserRights._
  import Implicits._

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

