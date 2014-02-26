package slickmacros.dao

import scala.language.existentials
import language.experimental.macros
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend
import scala.slick.profile._
import org.joda.time.DateTime

object Crud {
  type TableEx[C] = {
    def id: Column[Long]
  }

  type RowId = {
    def id: Option[Long]
  }

  type RowIdEx = {
    def id: Option[Long]
    var dateCreated: org.joda.time.DateTime
    var lastUpdated: org.joda.time.DateTime
  }

  /**
   *
   */
  trait Crud[C <: RowId, T <: RelationalTableComponent#Table[C] with TableEx[C]] {
    self: TableQuery[T] =>

    def del(objId: Long)(implicit session: JdbcBackend#SessionDef) = {
      self.where(_.id === objId)
    }

    def byId(objId: Long)(implicit session: JdbcBackend#SessionDef): Option[C] = self.where(_.id === objId).firstOption

    def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
      val res = (for {row <- self if row.id === obj.id.get} yield row) update (obj)
      res
    }

    def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
      val res = self returning (self.map(_.id)) insert (obj)
      res
    }
  }

  trait CrudEx[C <: RowIdEx, T <: RelationalTableComponent#Table[C] with TableEx[C]] extends Crud[C, T] {
    self: TableQuery[T] =>
    override def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
      obj.lastUpdated = DateTime.now
      super.update(obj)
    }

    override def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
      // because x.copy(dateCreated = , lastUpdated = ) is not available :(
      obj.dateCreated = DateTime.now
      obj.lastUpdated = obj.dateCreated
      super.insert(obj)
    }
  }
}


