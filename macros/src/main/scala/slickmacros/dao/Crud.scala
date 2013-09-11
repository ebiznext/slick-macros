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

  type RowEx = {
    def id: Option[Long]
    var dateCreated: java.sql.Timestamp
    var lastUpdated: java.sql.Timestamp
  }
  
  def rowValidate[T <: RowEx](obj : T) { }
  
  abstract class Crud[C <: RowEx, +T <: RelationalTableComponent#Table[C] with TableEx[C]](val query: TableQuery[T, T#TableElementType]) {

    def delById(objId: Long)(implicit session: JdbcBackend#SessionDef) = query.where(_.id === objId)

    def findById(objId: Long)(implicit session: JdbcBackend#SessionDef): Option[C] = query.where(_.id === objId).firstOption
    
    def load(objId: Long)(implicit session: JdbcBackend#SessionDef): C = query.where(_.id === objId).first

    def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
      rowValidate(obj)
      obj.lastUpdated = new java.sql.Timestamp(new java.util.Date().getTime)
      (for { row <- query if row.id === obj.id.get } yield row) update (obj)
    }

    def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
      rowValidate(obj)
      obj.dateCreated = new java.sql.Timestamp(new java.util.Date().getTime)
      obj.lastUpdated = obj.dateCreated
      query map (_.forInsert) returning (query.map(_.id)) insert (obj)
    }
  }
}