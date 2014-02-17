package slickmacros.dao

import scala.reflect.macros.Context
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.universe._
import java.beans.Introspector
import scala.language.existentials
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import Q.interpolation
import language.experimental.macros
import scala.slick.driver.JdbcDriver.simple._
import scala.slick.profile.BasicDriver
import scala.slick.lifted.MappedProjection
import scala.slick.jdbc.JdbcBackend
import scala.slick.profile._
import org.joda.time.DateTime

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
    var dateCreated: org.joda.time.DateTime
    var lastUpdated: org.joda.time.DateTime
  }

  trait RowInterceptor[T] {
    def beforeUpdate(obj: T) {}
    def afterUpdate(obj: T) {}
    def beforeInsert(obj: T) {}
    def afterInsert(obj: T) {}
    def beforeDelete(obj: Long) {}
    def afterDelete(obj: Long) {}
  }

  object RowInterceptor {  }

  class Crud[C <: RowId, +T <: RelationalTableComponent#Table[C] with TableEx[C]](val query: TableQuery[T, T#TableElementType]) {

    def del(objId: Long)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]) = {
      ev.beforeDelete(objId)
      query.where(_.id === objId)
      ev.afterDelete(objId)
    }

    def find(objId: Long)(implicit session: JdbcBackend#SessionDef): Option[C] = query.where(_.id === objId).firstOption

    def update(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]): Int = {
      ev.beforeUpdate(obj)
      val res = (for { row <- query if row.id === obj.id.get } yield row) update (obj)
      ev.afterUpdate(obj)
      res
    }

    def insert(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]) = {
      ev.beforeInsert(obj)
      val res = query map (_.forInsert) returning (query.map(_.id)) insert (obj)
      ev.afterInsert(obj)
      res
    }
  }

  class CrudEx[C <: RowIdEx, +T <: RelationalTableComponent#Table[C] with TableEx[C]](query: TableQuery[T, T#TableElementType]) extends Crud[C, T](query) {
    override def update(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]): Int = {
      obj.lastUpdated = DateTime.now
      super.update(obj)
    }

    override def insert(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]) = {
      // because x.copy(dateCreated = , lastUpdated = ) is not available :(
      obj.dateCreated = DateTime.now
      obj.lastUpdated = obj.dateCreated
      super.insert(obj)
    }
  }
  implicit def crudToQuery[C <: RowId, T <: RelationalTableComponent#Table[C] with TableEx[C]](crud: Crud[C, T]): TableQuery[T, T#TableElementType] = crud.query
}


