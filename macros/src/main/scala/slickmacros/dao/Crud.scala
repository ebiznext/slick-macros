package slickmacros.dao

import scala.reflect.macros.Context
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.universe._
import java.beans.Introspector;
import scala.language.existentials
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import Q.interpolation
import language.experimental.macros

import scala.slick.driver.JdbcDriver.simple._
import scala.slick.profile.BasicDriver
import scala.slick.lifted.MappedProjection
import scala.slick.jdbc.JdbcBackend
import scala.slick.profile._

object Crud {

  type TableEx[C] = {
    def id: Column[Long]
    def forInsert: MappedProjection[C, _]
  }

  type RowId = {
    def id: Option[Long]
  }

  type RowIdEx = {
    def id: Option[Long]
    var dateCreated: java.sql.Timestamp
    var lastUpdated: java.sql.Timestamp
  }

  def rowValidate[T <: RowId](obj: T) {}

  class Crud[C <: RowId, +T <: RelationalTableComponent#Table[C] with TableEx[C]](val query: TableQuery[T, T#TableElementType]) {

    def del(objId: Long)(implicit session: JdbcBackend#SessionDef) = query.where(_.id === objId)

    def find(objId: Long)(implicit session: JdbcBackend#SessionDef): Option[C] = query.where(_.id === objId).firstOption

    def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
      rowValidate(obj)
      (for { row <- query if row.id === obj.id.get } yield row) update (obj)
    }

    def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
      rowValidate(obj)
      query map (_.forInsert) returning (query.map(_.id)) insert (obj)
    }
  }

  class CrudEx[C <: RowIdEx, +T <: RelationalTableComponent#Table[C] with TableEx[C]](query: TableQuery[T, T#TableElementType]) extends Crud[C, T](query) {
    override def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
      obj.lastUpdated = new java.sql.Timestamp(new java.util.Date().getTime)
      super.update(obj)
    }

    override def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
      // because x.copy(dateCreated = , lastUpdated = ) is not available :(
      obj.dateCreated = new java.sql.Timestamp(new java.util.Date().getTime)
      obj.lastUpdated = obj.dateCreated
      super.insert(obj)
    }
  }
  implicit def crudToQuery[C <: RowId, T <: RelationalTableComponent#Table[C] with TableEx[C]] (crud: Crud[C, T]) : TableQuery[T, T#TableElementType] = crud.query
}


