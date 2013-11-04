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

  object OpType extends Enumeration {
    type OpType = Value
    val INSERT = Value(0)
    val UPDATE = Value(1)
  }
  import OpType._
  trait RowInterceptor[T] {
    def before(opType: OpType, obj: T): Unit
    def after(opType: OpType, obj: T): Unit
    def beforeDelete(obj: Long): Unit
    def afterDelete(obj: Long): Unit
  }

  object RowInterceptor {
    implicit object AnyRowInterceptor extends RowInterceptor[Any] {
      def before(opType: OpType, obj: Any) {}
      def after(opType: OpType, obj: Any) {}
      def beforeDelete(obj: Long) {}
      def afterDelete(obj: Long) {}
    }
  }

  class Crud[C <: RowId, +T <: RelationalTableComponent#Table[C] with TableEx[C]](val query: TableQuery[T, T#TableElementType]) {

    def del[C: RowInterceptor](objId: Long)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]) = {
      ev.beforeDelete(objId)
      query.where(_.id === objId)
      ev.afterDelete(objId)
    }

    def find(objId: Long)(implicit session: JdbcBackend#SessionDef): Option[C] = query.where(_.id === objId).firstOption

    def update(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]): Int = {
      ev.before(OpType.UPDATE, obj)
      val res = (for { row <- query if row.id === obj.id.get } yield row) update (obj)
      ev.after(OpType.UPDATE, obj)
      res
    }

    def insert(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]) = {
      ev.before(OpType.INSERT, obj)
      val res = query map (_.forInsert) returning (query.map(_.id)) insert (obj)
      ev.after(OpType.INSERT, obj)
      res
    }
  }

  class CrudEx[C <: RowIdEx, +T <: RelationalTableComponent#Table[C] with TableEx[C]](query: TableQuery[T, T#TableElementType]) extends Crud[C, T](query) {
    override def update(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]): Int = {
      obj.lastUpdated = new java.sql.Timestamp(new java.util.Date().getTime)
      super.update(obj)
    }

    override def insert(obj: C)(implicit session: JdbcBackend#SessionDef, ev: RowInterceptor[C]) = {
      // because x.copy(dateCreated = , lastUpdated = ) is not available :(
      obj.dateCreated = new java.sql.Timestamp(new java.util.Date().getTime)
      obj.lastUpdated = obj.dateCreated
      super.insert(obj)
    }
  }
  implicit def crudToQuery[C <: RowId, T <: RelationalTableComponent#Table[C] with TableEx[C]](crud: Crud[C, T]): TableQuery[T, T#TableElementType] = crud.query
}


