package slickmacros.annotations

import scala.reflect.macros.whitebox.Context
import scala.annotation.StaticAnnotation
import scala.language.existentials
import language.experimental.macros
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import scala.slick.model.ForeignKeyAction

class Model(driver: String = "PostgresDriver", timestamps: Boolean = false) extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro ModelMacro.impl
}

class Entity(name: String = null, timestamps: Boolean = false) extends StaticAnnotation

class Col(name: String = null, tpe: String = null, index: Boolean = false, unique: Boolean = false, pk: Boolean = false, onDelete: ForeignKeyAction = ForeignKeyAction.NoAction, onUpdate: ForeignKeyAction = ForeignKeyAction.NoAction, oldName: String = null) extends StaticAnnotation

//class Part extends StaticAnnotation

trait Timestamps

trait Part

object ModelMacro {

  //macro =>

  object FieldIndex extends Enumeration {
    type FieldIndex = Value
    val unique = Value(1)
    val indexed = Value(2)

  }

  implicit def anyToFieldOps(x: Any): FieldOps = null

  implicit def tuple2ToOps(x: (Any, Any)): FieldOps = null

  implicit def tuple3ToOps(x: (Any, Any, Any)): FieldOps = null

  implicit def tuple4ToOps(x: (Any, Any, Any, Any)): FieldOps = null

  implicit def tuple5ToOps(x: (Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple6ToOps(x: (Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple7ToOps(x: (Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple8ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple9ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple10ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple11ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple12ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple13ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple14ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple15ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple16ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple17ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple18ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple19ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple20ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple21ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple22ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  /*
    implicit val ClobTypeMapper =
      MappedColumnType.base[java.sql.Clob, String](
        {
          clob => val len = clob.length(); clob.getSubString(1, len.toInt)
        },
        {
          str => new org.joda.time.DateTime(ts.getTime)
        })
  */

  import FieldIndex._

  trait FieldOps {
    def is(x: FieldIndex): FieldOps = null

    def renamed(x: Any): FieldOps = null

    def are(x: FieldIndex): FieldOps = null

    def withType(x: String): FieldOps = null

    def withName(x: String): FieldOps = null

    def onUpdate(x: ForeignKeyAction): FieldOps = null

    def onDelete(x: ForeignKeyAction): FieldOps = null

    def to(x: Any): FieldOps = null
  }

  def constraints(plural: String = null)(f: => Unit) {}

  object DefType extends Enumeration {
    type DefType = Value
    val CLASSDEF = Value
    val DEFDEF = Value
    val IMPORTDEF = Value
    val ENUMDEF = Value
    val EMBEDDEF = Value
    val OTHERDEF = Value
  }

  import DefType._

  object ClassFlag extends Enumeration {
    type ClassFlag = Value
    val PARTDEF = Value
    val ENTITYDEF = Value
    val TIMESTAMPSDEF = Value
    val OTHER = Value
  }

  import ClassFlag._

  object FieldFlag extends Enumeration {
    type FieldFlag = Value
    val OPTION = Value
    val CASE = Value
    val PART = Value
    val LIST = Value
    val INDEX = Value
    val PK = Value
    val UNIQUE = Value
    val DBTYPE = Value
  }


  import FieldFlag._

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    lazy val mapDateTime = c.parse( """
            implicit val DateTimeTypeMapper =
              MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
              {
                dt => new java.sql.Timestamp(dt.getMillis)
              }, {
                ts => new org.joda.time.DateTime(ts.getTime)
              })
                                    """)

    val reservedNames = List("id", "dateCreated", "lastUpdated")
    val caseAccessor = scala.reflect.internal.Flags.CASEACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramAccessor = scala.reflect.internal.Flags.PARAMACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val prvate = scala.reflect.internal.Flags.PRIVATE.asInstanceOf[Long].asInstanceOf[FlagSet]
    val local = scala.reflect.internal.Flags.LOCAL.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramDefault = scala.reflect.internal.Flags.DEFAULTPARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val param = scala.reflect.internal.Flags.PARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val mutable = scala.reflect.internal.Flags.MUTABLE.asInstanceOf[Long].asInstanceOf[FlagSet]
    val optionalDate = Select(Select(Select(Ident(newTermName("org")), newTermName("joda")), newTermName("time")), newTypeName("DateTime"))
    val caseparam = Modifiers(caseAccessor | paramAccessor)
    val paramparam = Modifiers(param | paramAccessor)
    def idVal(tpeName: TypeName) = q"$caseparam val id:Option[$tpeName]"
    def idValInCtor(tpeName: TypeName) = q"$paramparam val id:Option[$tpeName]"

    //    def dateVal(name: String) = ValDef(Modifiers(mutable | caseAccessor | paramAccessor), newTermName(name), optionalDate, EmptyTree)
    def dateVal(name: String) = ValDef(Modifiers(mutable | caseAccessor | paramAccessor | paramDefault), newTermName(name), optionalDate, Literal(Constant(null)))
    //def dateVal(name: String) = q"var ${newTermName(name)} : org.joda.time.DateTime = null"
    //    def dateValInCtor(name: String) = ValDef(Modifiers(param | paramAccessor | paramDefault), newTermName(name), optionalDate, Literal(Constant(null))) // Ident(newTermName("None")))

    class ClsDesc(val name: String, val flags: Set[ClassFlag], val fields: ListBuffer[FldDesc], val tree: Tree, var plural: String) {
      def parseBody(allClasses: List[ClsDesc]) {
        val constraintsTerm = newTermName("constraints")
        val ClassDef(mod, name, Nil, Template(parents, self, body)) = tree
        body.foreach {
          it =>
            it match {
              case ValDef(_, _, _, _) => fields += FldDesc(it, tree, allClasses)
              case _ =>
            }
        }
      }

      def assoc: Boolean = tree == null

      def part: Boolean = flags.exists(_ == PARTDEF)

      def entity: Boolean = flags.exists(_ == ENTITYDEF)

      def timestamps: Boolean = flags.exists(_ == TIMESTAMPSDEF)

      def dateVals: List[ValDef] = if (timestamps) dateVal("dateCreated") :: dateVal("lastUpdated") :: Nil else Nil

      //def dateValsInCtor: List[ValDef] = if (timestamps) dateValInCtor("dateCreated") :: dateValInCtor("lastUpdated") :: Nil else Nil

      def dateDefs =
        if (timestamps)
          c.parse( """def dateCreated = column[org.joda.time.DateTime]("date_created")""") :: c.parse( """def lastUpdated = column[org.joda.time.DateTime]("last_updated")""") :: Nil
        else
          Nil

      // 0..1 dependency
      def foreignKeys: List[FldDesc] = {
        fields.filter {
          it => it.flags.exists(_ == FieldFlag.CASE) && !it.flags.exists(_ == FieldFlag.LIST)
        } toList
      }

      // 0..* dependency
      def assocs: List[FldDesc] = {
        fields.filter {
          it => it.flags.exists(_ == FieldFlag.CASE) && it.flags.exists(_ == FieldFlag.LIST)
        } toList
      }

      // table cols
      def simpleValDefs: List[FldDesc] = {
        fields.filter {
          it => !it.flags.exists(_ == FieldFlag.LIST)
        } toList
      }

      // PK fields
      def listPKs: List[FldDesc] = {
        fields.filter {
          it => it.flags.exists(_ == FieldFlag.PK)
        } toList
      }

      // all fields including part fields
      def allFields = {
        fields.toList.map {
          it =>
            if (it.part)
              it.cls.get.fields toList
            else
              it :: Nil
        } flatten
      }

      // indexed cols
      def indexes: List[FldDesc] = {
        allFields.filter {
          it =>
            it.flags.exists(_ == FieldFlag.INDEX)
        } toList
      }

      def fieldCount(augment: Boolean = true) = {
        val aug = if (augment) 2 else 0
        simpleValDefs.size + 1 + aug
      }

      def mkShapeClass(augment: Boolean = true): ClassDef = {
        def paramTypes(prefix: String, count: Int) = {
          0.until(count).toList.map(prefix + _).map(t => tq"$t")
        }
        def paramNames(prefix: String, count: Int): List[ValDef] = {
          def tpeName(prefix: String, index: Int) = s"$prefix${index + 1}"
          simpleValDefs.zipWithIndex.map {
            case (it, index) =>
              val tpt = tq"${newTypeName(tpeName(prefix, index))}"
              val ValDef(_, nme, _, _) = it.tree
              val termName = if (it.cse) newTermName(nme.decoded + "Id") else nme
              q"val $termName:$tpt"
          }
        }

        def paramPlaceHolders(count: Int) = {
          0.until(count).toList.map(ignore => "_").mkString(",")
        }
        def mkCaseClass(augment: Boolean = true): ClassDef = {
          def dateVal(name: String, typeName: String) = ValDef(Modifiers(mutable | caseAccessor | paramAccessor | paramDefault), newTermName(name), tq"${newTypeName(typeName)}", Literal(Constant(null)))
          def dateVals(count: Int): List[ValDef] = if (timestamps) dateVal("dateCreated", s"T$count") :: dateVal("lastUpdated", s"T${count + 1}") :: Nil else Nil
          val count = fieldCount(augment)
          val tparams = paramTypes("T", count)
          val valdefs = paramNames("T", count)
          val newAttrs = if (augment) q"val id:T0" :: valdefs ++ dateVals(count) else valdefs
          println(newAttrs)
          println(tparams)
          q"case class ${newTypeName(name)}[..$tparams](..$newAttrs)"
        }
        mkCaseClass(augment)
      }

      def mkCaseClass(augment: Boolean = true)(implicit caseDefs: List[ClsDesc]): ClassDef = {
        println("----------------------------------------------------------------------------")
        println("----------------------------------------------------------------------------")
        println("----------------------------------------------------------------------------")
        println("----------------------------------------------------------------------------")
        mkShapeClass(true)
        println("----------------------------------------------------------------------------")
        println("----------------------------------------------------------------------------")
        println("----------------------------------------------------------------------------")
        println("----------------------------------------------------------------------------")
        def idVal(tpeName: TypeName) = q"val id:Option[$tpeName]"
        if (part) {
          tree.asInstanceOf[ClassDef]
        } else {
          val valdefs = simpleValDefs.map {
            it =>
              if (it.cse) {
                val tpt = if (it.option) {
                  tq"Option[${typeId(it.typeName)}]"
                } else {
                  tq"${typeId(it.typeName)}"
                }
                val ValDef(_, nme, _, _) = it.tree
                val termName = newTermName(nme.decoded + "Id")
                q"val $termName:$tpt"
              } else
                it.tree.asInstanceOf[ValDef]
          }
          val idTypeName = s"${typeId(name)}"
          val newAttrs = if (augment) idVal(idTypeName) :: valdefs ++ dateVals else valdefs

          val xid = q"""def xid = id.getOrElse(throw new Exception("Object has no id yet"))"""
          val defdefs = foreignKeys.map {
            it =>
              if (it.option)
                q"""def ${newTermName("load" + it.name.capitalize)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(NameOps.objectName(it.typeName))}.where(_.id === ${newTermName(NameOps.colIdName(it.name))}).firstOption"""
              else
                q"""def ${newTermName("load" + it.name.capitalize)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(NameOps.objectName(it.typeName))}.where(_.id === ${newTermName(NameOps.colIdName(it.name))}).first"""
          }
          val one2manyDefs = assocs.map {
            it =>
              q"""
            def ${newTermName("load" + it.name.capitalize)} = for {
        	  	x <- self.${newTermName(NameOps.objectName(NameOps.assocTableName(name, it.typeName)))} if x.${newTermName(NameOps.colIdName(name))} === id
        		y <- self.${newTermName(NameOps.objectName(it.typeName))} if x.${newTermName(NameOps.colIdName(it.typeName))} === y.id
        	} yield(y)
            """
          }

          val one2manyDefAdds = assocs.map {
            it =>
              q"""def ${newTermName("add" + it.typeName)}(${newTermName(NameOps.colIdName(it.typeName))} : ${newTypeName("Long")})(implicit session : JdbcBackend#SessionDef) = ${newTermName(NameOps.objectName(NameOps.assocTableName(name, it.typeName)))}.insert(${newTermName(NameOps.assocTableName(name, it.typeName))}(xid, ${newTermName(NameOps.colIdName(it.typeName))}))"""
          }
          val lst = if (augment) List(xid) ++ defdefs ++ one2manyDefs ++ one2manyDefAdds else defdefs ++ one2manyDefs ++ one2manyDefAdds
          q"case class ${newTypeName(name)}(..$newAttrs) { ..$lst }"
        }
      }

      /**
       * create the case class and foreign keys for 1,n relationships and the slick table description and the assoc table for n,m relationships
       * if augment is set to true timestamp & forInsert defs are generated too
       */

      def mkTable(augment: Boolean = true)(implicit caseDefs: List[ClsDesc]): List[Tree] = {
        if (part)
          List(tree.asInstanceOf[ClassDef])
        else {
          val simpleVals = simpleValDefs
          val foreignKeys = this.foreignKeys.map {
            it =>
              //val fkAction = if (colInfo.isDefined && colInfo.get.onDelete != null) colInfo.get.onDelete else "ForeignKeyAction.NoAction"
              c.parse( s"""def ${it.name} = foreignKey("${it.name.toLowerCase}${name}2${it.typeName.toLowerCase}", ${NameOps.colIdName(it.name)}, ${NameOps.objectName(it.typeName)})(_.id, ${it.onUpdateAction}, ${it.onDeleteAction}) """) // onDelete
          }
          val assocs = this.assocs.map {
            it =>
              new ClsDesc(NameOps.assocTableName(name, it.typeName), Set(ENTITYDEF),
                ListBuffer(
                  new FldDesc(NameOps.decapitalize(name), NameOps.decapitalize(name), name, Set(FieldFlag.CASE), None, "NoAction", "NoAction", Some(this), ValDef(caseparam, NameOps.decapitalize(name), null, null)),
                  new FldDesc(NameOps.decapitalize(it.typeName), NameOps.decapitalize(it.typeName), it.typeName, Set(FieldFlag.CASE), None, "NoAction", "NoAction", it.cls, ValDef(caseparam, NameOps.decapitalize(it.typeName), null, null))),
                null, NameOps.plural(NameOps.decapitalize(NameOps.assocTableName(name, it.typeName))))

          }
          val assocTables = assocs.flatMap {
            it => mkTable(false)
          }
          val idCol = q"""def id = column[${typeId(name)}]("id", O.PrimaryKey, O.AutoInc);"""

          val defdefs = simpleVals.flatMap {
            it =>
              if (it.part) {
                it.cls.get.fields.map(_.mkColumn)
              } else {
                List(it.mkColumn)
              }
          }

          val indexdefs: List[c.universe.Tree] = indexes.map {
            it =>
              q"""def ${newTermName(it.name + "Index")} = index(${"idx_" + name.toLowerCase + "_" + it.name.toLowerCase}, ${newTermName(it.name)}, ${it.unique})"""
          }
          val times = mkTimes(augment)
          val forInsert = mkForInsert
          val tagDef = ValDef(Modifiers(prvate | local | paramAccessor), newTermName("tag"), Ident(newTypeName("Tag")), EmptyTree)

          val vparams = q"""${newTermName("tag")}:${newTypeName("Tag")}""" :: Nil
          val body = if (augment) idCol :: times :: dateDefs ++ defdefs ++ indexdefs ++ foreignKeys else times :: indexdefs ++ defdefs ++ foreignKeys
          val plur = NameOps.plural(NameOps.decapitalize(name)).toLowerCase()
          val tableDef = q"""class ${newTypeName(NameOps.tableName(name))}(${newTermName("tag")}:${newTypeName("Tag")}) extends Table[${newTypeName(name)}](${newTermName("tag")}, ${Constant(plur)}){ ..$body }"""
          val tableDef2 =
            ClassDef(Modifiers(),
              newTypeName(NameOps.tableName(name)), Nil,
              Template(
                AppliedTypeTree(Ident(newTypeName("Table")), Ident(newTypeName(name)) :: Nil) :: Nil,
                emptyValDef,
                if (augment) idCol :: times :: forInsert :: dateDefs ++ defdefs ++ indexdefs ++ foreignKeys else times :: indexdefs ++ defdefs ++ foreignKeys))
          List(mkCaseClass(augment), tableDef) ++ mkCompanion ++ assocTables
        }
      }

      private def mkCompanion()(implicit caseDefs: List[ClsDesc]) = {
        //      val xx = new TableQuery(tag => new CompanyTable(tag)) with CrudEx[Company, CompanyTable]
        val ex = if (timestamps) "Ex" else ""
        val crud = if (!assoc) s"with Crud$ex[${name}, ${NameOps.tableName(name)}]" else ""

        val query = c.parse(s"val ${newTermName(NameOps.objectName(name))} = new TableQuery(tag => new ${newTypeName(NameOps.tableName(name))}(tag)) $crud")
        query :: Nil
      }

      /**
       * create the def * = ... from fields names and case class names
       */
      private def mkTimes(augment: Boolean = true): Tree = {
        val expr = {
          if (augment)
            if (timestamps)
              s"""def * = (id.?, ${mkTilde(simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
		        case (id, ${mkCase(simpleValDefs)}, dateCreated, lastUpdated) => ${name}(id, ${mkCaseApply(simpleValDefs)}, dateCreated, lastUpdated)
		      }, { x : ${name} => Some((x.id, ${mkCaseUnapply(simpleValDefs)}, x.dateCreated, x.lastUpdated))
		      })"""
            else
              s"""def * = (id.?, ${mkTilde(simpleValDefs)}).shaped <> ({
		        case (id, ${mkCase(simpleValDefs)}) => ${name}(id, ${mkCaseApply(simpleValDefs)})
		      }, { x : ${name} => Some((x.id, ${mkCaseUnapply(simpleValDefs)}))
		      })"""
          else
          //s"""def * = (${mkTilde(fields toList)}).shaped <> (${name}.tupled, ${name}.unapply _)"""
            s"""def * = (${mkTilde(fields toList)}).shaped <> ({
		        case (${mkCase(simpleValDefs)}) => ${name}( ${mkCaseApply(simpleValDefs)})
		      }, { x : ${name} => Some((${mkCaseUnapply(simpleValDefs)}))
		      })"""

        }
        c.parse(expr)
      }

      private def mkForInsert: Tree = {
        val expr = {
          if (timestamps)
            s"""def forInsert = (${mkTilde(simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
		        case (${mkCase(simpleValDefs)}, dateCreated, lastUpdated) => ${name}(None, ${mkCaseApply(simpleValDefs)}, dateCreated, lastUpdated)
		      }, { x : ${name} => Some((${mkCaseUnapply(simpleValDefs)}, x.dateCreated, x.lastUpdated))
		      })"""
          else
            s"""def forInsert = (${mkTilde(simpleValDefs)}).shaped <> ({
		        case (${mkCase(simpleValDefs)}) => ${name}(None, ${mkCaseApply(simpleValDefs)})
		      }, { x : ${name} => Some((${mkCaseUnapply(simpleValDefs)}))
		      })"""

        }
        c.parse(expr)
      }

      /**
       * create the field1 ~ field2 ~ ... ~ fieldN string from case class column
       * does not handle correctly case classes with a single column (adding a dummy field would probably help)
       */
      private def mkTilde(fields: List[FldDesc]): String = {
        fields match {
          case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
          case field :: Nil =>
            if (field.part)
              "(" + mkTilde(field.cls.get.fields toList) + ")"
            else if (field.cse)
              NameOps.colIdName(field.name)
            else
              field.name
          case head :: tail => s"${mkTilde(head :: Nil)}, ${mkTilde(tail)}"
        }
      }

      private def mkCaseApply(fields: List[FldDesc]): String = {
        fields match {
          case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
          case field :: Nil =>
            if (field.part)
              s"${field.typeName}.tupled.apply(${field.name})"
            else if (field.cse)
              NameOps.colIdName(field.name)
            else
              field.name
          case head :: tail => s"${mkCaseApply(head :: Nil)}, ${mkCaseApply(tail)}"
        }
      }

      private def mkCaseUnapply(fields: List[FldDesc]): String = {
        fields match {
          case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
          case field :: Nil =>
            if (field.part) {
              s"${field.typeName}.unapply(x.${field.name}).get"
            } else if (field.cse)
              "x." + NameOps.colIdName(field.name)
            else
              "x." + field.name
          case head :: tail => s"${mkCaseUnapply(head :: Nil)}, ${mkCaseUnapply(tail)}"
        }
      }

      private def mkCase(fields: List[FldDesc]): String = {
        fields match {
          case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
          case field :: Nil =>
            if (field.part) {
              field.name
            } else if (field.cse)
              NameOps.colIdName(field.name)
            else
              field.name
          case head :: tail => s"${mkCase(head :: Nil)}, ${mkCase(tail)}"
        }
      }


    }

    object ClsDesc {
      def apply(tree: Tree, timestampAll: Boolean) = {
        val ClassDef(mod, name, Nil, Template(parents, _, body)) = tree
        if (!mod.hasFlag(CASE))
          c.abort(c.enclosingPosition, s"Only case classes allowed here ${name.decoded}")

        val annotations = mod.annotations.map(_.children.head.toString)
        val isPart = annotations.exists(_ == "new Part") || parents.exists(_.toString.contains("Part"))
        val flags = Set[ClassFlag]()
        if (isPart)
          flags += PARTDEF
        else
          flags += ENTITYDEF
        val timestamps = mod.annotations.exists(_.toString.contains("true")) || parents.exists(_.toString.contains("Timestamps")) // quick & dirty
        if (timestampAll || timestamps) flags += TIMESTAMPSDEF
        new ClsDesc(name.decoded, flags, ListBuffer(), tree, NameOps.plural(NameOps.decapitalize(name.decoded)))
      }
    }
    class FldDesc(val name: String, val colName: String, val typeName: String, val flags: Set[FieldFlag], val dbType: Option[String], val onDelete: String, val onUpdate: String, val cls: Option[ClsDesc], val tree: Tree) {
      def unique: Boolean = flags.exists(_ == FieldFlag.UNIQUE)

      def part: Boolean = flags.exists(_ == FieldFlag.PART)

      def option: Boolean = flags.exists(_ == FieldFlag.OPTION)

      def cse: Boolean = flags.exists(_ == FieldFlag.CASE)

      def pk: Boolean = flags.exists(_ == FieldFlag.PK)

      def onDeleteAction = s"scala.slick.lifted.ForeignKeyAction.$onDelete"

      def onUpdateAction = s"scala.slick.lifted.ForeignKeyAction.$onUpdate"

      /**
       * given a fieldName and a type tree return "def fieldName = column[tpe]("fieldName")
       */


      def mkColumn: Tree = {
        val q"$mods val $nme:$tpt = $initial" = tree
        if (cse) {
          if (option) {
            q"""def ${newTermName(NameOps.colIdName(name))} = column[Option[${typeId(typeName)}]](${NameOps.asColName(NameOps.colIdName(name))})"""
          } else {
            q"""def ${newTermName(NameOps.colIdName(name))} = column[${typeId(typeName)}](${NameOps.asColName(NameOps.colIdName(name))})"""
          }
        } else {
          val tpe = typeName
          dbType map {
            it =>
              q"""def $nme = column[$tpt](${colName}, O.DBType(${it}))"""
          } getOrElse {
            q"""def $nme = column[$tpt](${colName})"""
          }
        }
      }
    }

    class ScalaAnnotation(val name: String, val fields: Array[String]) {
      def field(i: Int): String = {
        if (fields.size <= i)
          c.abort(c.enclosingPosition, s"field at position $i is required in annotation $name")
        val res = fields(i)
        if (res.startsWith( """"""")) res.substring(1, res.length - 1) else res
      }
    }

    object ScalaAnnotation {
      def apply(expr: Tree) = {
        val sexpr = expr.toString
        if (!sexpr.startsWith("new "))
          c.abort(c.enclosingPosition, s"Invalid annotation $sexpr")
        else {
          val name = sexpr.substring("new ".length, sexpr.indexOf('('))
          val params = sexpr.substring(sexpr.indexOf('(') + 1, sexpr.lastIndexOf(')')).split(',').map {
            case it if (it.contains("=")) =>
              c.abort(c.enclosingPosition, s"Named parameters not uspported on  Slick-macros Annotations")
            case other =>
              other.toString
          }
          new ScalaAnnotation(name, params)
        }
      }
    }

    object FldDesc {
      def apply(fieldTree: Tree, clsTree: Tree, allClasses: List[ClsDesc]) = {
        val ValDef(mod, name, tpt, _) = fieldTree
        if (reservedNames.exists(_ == name.decoded))
          c.abort(c.enclosingPosition, s"Column with name ${name.decoded} not allowed")
        else {
          val flags = Set[FieldFlag]()
          val annotation = mod.annotations.headOption.map(_.children.head.toString)
          var colType: String = null
          var colName: String = NameOps.asColName(name.decoded)
          var onDelete: String = "NoAction"
          var onUpdate: String = "NoAction"

          mod.annotations.map(x => x.toString).foreach {
            annotation =>
              val params = annotation.substring(annotation.indexOf('(') + 1, annotation.lastIndexOf(')')).split(',').map(_.split("="))
              if (annotation startsWith "new Col(") {
                val paramsMap = Map((0 -> "name"), (1 -> "tpe"), (2 -> "index"), (3 -> "unique"), (4 -> "pk"), (4 -> "onDelete"), (5 -> "onUpdate"))
                val named = params.view.zipWithIndex.map {
                  case (param, i) =>
                    if (param.length == 1)
                      (paramsMap(i), param(0).trim)
                    else {
                      (param(0).trim, param(1).trim)
                    }
                }
                named.foreach {
                  param =>

                    param._1 match {
                      case "onDelete" => onDelete = param._2.replaceAll("^.*\\.", "")
                      case "onUpdate" => onUpdate = param._2.replaceAll("^.*\\.", "")
                      case "name" => colName = param._2.replaceAll( """^"|"$""", "")
                      case "tpe" =>
                        flags += FieldFlag.DBTYPE
                        colType = param._2.replaceAll( """^"|"$""", "")
                      case "index" if param._2.contains("true") => flags += FieldFlag.INDEX
                      case "unique" if param._2.contains("true") =>
                        flags += FieldFlag.INDEX
                        flags += FieldFlag.UNIQUE
                      case "pk" if param._2.contains("true") =>
                        flags += FieldFlag.PK
                    }
                }
              } else
                c.abort(c.enclosingPosition, s"Invalid $annotation on column ${name.decoded}")
          }
          def buildTypeName(tree: Tree): String = {
            tree match {
              case Select(subtree, name) =>
                buildTypeName(subtree) + "." + name.decoded
              case AppliedTypeTree(subtree, args) =>
                buildTypeName(subtree) + "[" + args.map(it => buildTypeName(it)).mkString(",") + "]"
              case Ident(x) =>
                x.decoded
              case other => other.toString
            }
          }
          var typeName: String = buildTypeName(tpt)
          val clsDesc: Option[ClsDesc] = tpt match {
            case Ident(tpe) =>
              val clsDesc = allClasses.find(_.name == typeName)
              clsDesc.foreach {
                it =>
                  if (it.entity) {
                    flags += FieldFlag.CASE
                  } else if (it.part)
                    flags += FieldFlag.PART
              }
              clsDesc
            case AppliedTypeTree(Ident(option), tpe :: Nil) if option.decoded == "Option" =>
              typeName = buildTypeName(tpe)
              flags += FieldFlag.OPTION
              val clsDesc = allClasses.find(_.name == typeName)
              clsDesc.foreach {
                it =>
                  if (it.entity)
                    flags += FieldFlag.CASE
              }
              clsDesc
            case AppliedTypeTree(Ident(list), tpe :: Nil) if list.decoded == "List" =>
              typeName = buildTypeName(tpe)
              val clsDesc = allClasses.find(_.name == typeName).getOrElse(c.abort(c.enclosingPosition, s"List not allowed here ${name.decoded} not allowed"))

              if (clsDesc.entity)
                flags ++= Set(FieldFlag.CASE, FieldFlag.LIST)
              else
                c.abort(c.enclosingPosition, s"only entity allowed here ${name.decoded}:${clsDesc.name}")
              Some(clsDesc)
            case _ => None
          }
          val tree = mod.annotations
          tree.foreach {
            case Apply(Select(New(Ident(index)), _), List(Literal(Constant(unique)))) =>
              if (index.decoded == "Index") {
                flags += FieldFlag.INDEX
                if (unique == true) flags += FieldFlag.UNIQUE
              }

            case Apply(Select(New(Ident(pk)), _), _) =>
              if (pk.decoded == "PK") {
                flags += FieldFlag.PK
              }

            case Apply(Select(New(Ident(dbType)), _), List(Literal(Constant(dbTypeValue)))) =>
              if (dbType.decoded == "Type") {
                flags += FieldFlag.DBTYPE
                colType = dbTypeValue.asInstanceOf[String]
              }
          }

          val ClassDef(_, clsName, _, Template(_, _, body)) = clsTree
          body.foreach {
            it =>
              val cns = it match {
                case Apply(Ident(constraintsTerm), List(Block(stats, expr))) =>
                  Some(NameOps.plural(NameOps.decapitalize(clsName.decoded)), stats :+ expr)
                case Apply(Apply(Ident(constraintsTerm), List(Literal(Constant(arg)))), List(Block(stats, expr))) =>
                  Some(arg.toString, stats :+ expr)
                case _ => None
              }
              cns foreach {
                it =>
                  allClasses.find(_.name == clsName.decoded).foreach {
                    x =>
                      x.plural = it._1
                  }
                  (it._2).foreach {
                    s =>
                      val st = s.toString.replace("scala.Tuple", "Tuple").split('.').map(_.trim)
                      if (st.length >= 2) {
                        val fieldNames = {
                          if (st(0).endsWith(")")) {
                            st(0).substring(st(0).indexOf('(') + 1, st(0).lastIndexOf(')')).split(',').map(_.trim)
                          } else {
                            Array(st(0).trim)
                          }
                        }
                        if (fieldNames.contains(name.decoded)) {
                          st.drop(1).foreach {
                            s =>
                              val method = s.substring(0, s.indexOf('(')).trim
                              val arg = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'))
                              method match {
                                case "is" | "are" =>
                                  flags += FieldFlag.INDEX
                                  if (arg == "unique") flags += FieldFlag.UNIQUE
                                case "withName" =>
                                  colName = arg.substring(1, arg.length - 1)
                                case "withType" =>
                                  flags += FieldFlag.DBTYPE; colType = arg.substring(1, arg.length - 1)
                                case "onUpdate" => onUpdate = arg
                                case "onDelete" => onDelete = arg
                              }
                          }
                        }
                      }
                  }
              }
          }
          new FldDesc(name.decoded, colName, typeName, flags, Option(colType), onDelete, onUpdate, clsDesc, fieldTree)
        }
      }
    }

    object NameOps {
      def asColName(name: String): String = {
        name.toCharArray().zipWithIndex map {
          case (ch, i) if Character.isUpperCase(ch) && i > 0 =>
            "_" + Character.toLowerCase(ch)
          case (ch, _) => Character.toLowerCase(ch)
        } mkString
      }

      def colIdName(caseClassName: String) = {
        s"${decapitalize(caseClassName)}Id"
      }

      def tableName(typeName: String) = s"${typeName}Table"

      def decapitalize(name: String): String = {
        if (name == null || name.length == 0) {
          name;
        } else {
          val chars = name.toCharArray()
          var i = 0
          while (i < chars.length && Character.isUpperCase(chars(i))) {
            if (i > 0 && i < chars.length - 1 && Character.isLowerCase(chars(i + 1))) {

            } else {
              chars(i) = Character.toLowerCase(chars(i))
            }
            i = i + 1
          }
          new String(chars)
        }
      }

      def objectName(typeName: String)(implicit caseDefs: List[ClsDesc]) = {
        val plur = caseDefs.find(typeName == _.name).map(_.plural).getOrElse(plural(decapitalize(typeName)))
        s"${plur}"
      }

      //def objectName(typeName: String) = s"${plural(decapitalize(typeName))}"
      //def objectName(typeName: String) = s"${decapitalize(typeName)}Query"

      def assocTableName(table1: String, table2: String) = s"${table1}2${table2}"

      /*
       *
       * Thank you Andromda.org and
       * http://www.csse.monash.edu.au/~damian/papers/HTML/Plurals.html
       */
      def plural(name: String) = {
        val rules = List(
          ("(\\w*)people$", "$1people"),
          ("(\\w*)children$", "$1children"),
          ("(\\w*)feet$", "$1feet"),
          ("(\\w*)teeth$", "$1teeth"),
          ("(\\w*)men$", "$1men"),
          ("(\\w*)equipment$", "$1equipment"),
          ("(\\w*)information$", "$1information"),
          ("(\\w*)rice$", "$1rice"),
          ("(\\w*)money$", "$1money"),
          ("(\\w*)fish$", "$fish"),
          ("(\\w*)sheep$", "$1sheep"),
          ("(\\w+)(es)$", "$1es"),
          // Check exception special case words
          ("(\\w*)person$", "$1people"),
          ("(\\w*)child$", "$1children"),
          ("(\\w*)foot$", "$1feet"),
          ("(\\w*)tooth$", "$1teeth"),
          ("(\\w*)bus$", "$1buses"),
          ("(\\w*)man$", "$1men"),
          ("(\\w*)(ox|oxen)$", "$1$2"),
          ("(\\w*)(buffal|tomat)o$", "$1$2oes"),
          ("(\\w*)quiz$", "$1$2zes"),
          // Greek endings
          ("(\\w+)(matr|vert|ind)ix|ex$", "$1$2ices"),
          ("(\\w+)(sis)$", "$1ses"),
          ("(\\w+)(um)$", "$1a"),
          // Old English. hoof -> hooves, leaf -> leaves
          ("(\\w*)(fe)$", "$1ves"),
          ("(\\w*)(f)$", "$1ves"),
          ("(\\w*)([m|l])ouse$", "$1$2ice"),
          // Y preceded by a consonant changes to ies
          ("(\\w+)([^aeiou]|qu)y$", "$1$2ies"),
          // Voiced consonants add es instead of s
          ("(\\w+)(z|ch|sh|as|ss|us|x)$", "$1$2es"),
          // Check exception special case words
          ("(\\w*)cactus$", "$1cacti"),
          ("(\\w*)focus$", "$1foci"),
          ("(\\w*)fungus$", "$1fungi"),
          ("(\\w*)octopus$", "$1octopi"),
          ("(\\w*)radius$", "$1radii"),
          // If nothing else matches, and word ends in s, assume plural already
          ("(\\w+)(s)$", "$1s"))
        rules.find(it => name.matches(it._1)).map(it => name.replaceFirst(it._1, it._2)).getOrElse(name.replaceFirst("([\\w]+)([^s])$", "$1$2s"))
      }
    }

    /**
     * Create the Enumeration Type Mapper
     */
    def mkEnumMapper(moduleDef: c.universe.Tree): Tree = {
      val ModuleDef(_, name, Template(_, _, defs)) = moduleDef
      val res = defs.flatMap {
        it =>
          it match {
            case ValDef(_, _, _, Apply(Ident(valueKeyword), List(Literal(Constant(value: Int))))) => Some("Int")
            case ValDef(_, _, _, Apply(Ident(valueKeyword), List(Literal(Constant(value: String))))) => Some("String")
            case ValDef(_, _, _, Apply(Ident(valueKeyword), _)) => Some("Int")
            case _ => None
          }
      } toSet

      val valueType = res.headOption.getOrElse("Int")

      val mapper = valueType match {
        case "Int" =>
          s"""implicit val ${name.decoded}TypeMapper = MappedColumnType.base[${name.decoded}.Value, $valueType](
            {
              it => it.id
            },
            {
              id => ${name}(id)
            })"""
        case "String" =>
          s"""implicit val ${name.decoded}TypeMapper = MappedColumnType.base[${name.decoded}.Value, $valueType](
            {
              it => it.toString
            },
            {
              id => ${name.decoded}.withName(id)
            })"""

      }
      c.parse(mapper)
    }

    def typeId(tpeName: String) = newTypeName("Long")

    object ColInfo extends Enumeration {
      type ColInfo = Value
      val DBTYPE = Value
      val TIMESTAMPS = Value
      val INDEX = Value
      val ONDELETE = Value
      val PK = Value
    }

    def defMap(body: List[c.universe.Tree]): Map[DefType, List[(DefType, c.universe.Tree)]] = {
      body.flatMap {
        it =>
          it match {
            case ModuleDef(mod, name, Template(List(Ident(t)), _, _)) if t.isTypeName && t.decoded == "Enumeration" => List((ENUMDEF, it))
            case ClassDef(mod, name, Nil, body) if mod.hasFlag(CASE) => List((CLASSDEF, it))
            case DefDef(_, _, _, _, _, _) => List((DEFDEF, it))
            case Import(_, _) => List((IMPORTDEF, it))
            case _ => List((OTHERDEF, it))
          }
      } groupBy (_._1)
    }
    val result = {
      annottees.map(_.tree).toList match {
        case ModuleDef(mod, moduleName, Template(parents, emptyValDef, body)) :: Nil =>
          val ann = ScalaAnnotation(c.prefix.tree)
          val driverName = ann.field(0)
          val timestampsAll = c.prefix.tree.toString.contains("true") || parents.exists(_.toString.contains("Timestamps")) // Q&D
        val allDefs = defMap(body)
          implicit val caseDefs = allDefs.getOrElse(CLASSDEF, Nil).map(it => ClsDesc(it._2, timestampsAll))
          caseDefs.foreach(_.parseBody(caseDefs))
          val tableDefList = caseDefs.flatMap(_.mkTable())
          println("--> Generated queries :")
          caseDefs.foreach {
            x =>
              println(NameOps.objectName(x.name))
          }
          println("--> End of generated queries.")

          val enumDefList = allDefs.getOrElse(ENUMDEF, Nil).map(_._2)
          val defdefList = allDefs.getOrElse(DEFDEF, Nil).map(_._2)
          val importdefList = allDefs.getOrElse(IMPORTDEF, Nil).map(_._2)
          val otherdefList = allDefs.getOrElse(OTHERDEF, Nil).map(_._2)
          val enumMapperList = (allDefs.get(ENUMDEF).map(_.map {
            it =>
              mkEnumMapper(it._2)
          }) getOrElse (Nil))
          val embedDefList = allDefs.getOrElse(EMBEDDEF, Nil).map(_._2)
          val extraImports = List(
            Import(Select(Select(Select(Ident(newTermName("scala")), newTermName("slick")), newTermName("util")), newTermName("TupleMethods")), List(ImportSelector(nme.WILDCARD, -1, null, -1))),
            Import(Select(Select(Ident(newTermName("scala")), newTermName("slick")), newTermName("jdbc")), List(ImportSelector(newTermName("JdbcBackend"), -1, newTermName("JdbcBackend"), -1))),
            Import(Select(Select(Select(Select(Ident(newTermName("scala")), newTermName("slick")), newTermName("driver")), newTermName(driverName)), newTermName("simple")), List(ImportSelector(nme.WILDCARD, 245, null, -1))),
            Import(Select(Select(Ident(newTermName("slickmacros")), newTermName("dao")), newTermName("Crud")), List(ImportSelector(nme.WILDCARD, 90, null, -1))),
            Import(Select(Ident(newTermName("slickmacros")), newTermName("Implicits")), List(ImportSelector(nme.WILDCARD, 121, null, -1))))
          val objectDefs = extraImports ++ List(mapDateTime) ++ enumDefList ++ importdefList ++ enumMapperList ++ embedDefList ++ tableDefList /* ++ defdefList*/
          ModuleDef(mod, moduleName, Template(parents, ValDef(Modifiers(PRIVATE), newTermName("self"), TypeTree(), EmptyTree), objectDefs))
          q"""object $moduleName extends ..$parents { self => ..$objectDefs }
          """

        case _ =>
          c.abort(c.enclosingPosition, s"Only module defs allowed here")
      }
    }
    println(result)
    val pw = new java.io.PrintWriter(new java.io.File("./slick-macros.log"))
    pw.write(result.toString)
    pw.close
    println("----------------------------------------------------------------------------")
    println("----------------------------------------------------------------------------")
    println("----------------------------------------------------------------------------")
    println("----------------------------------------------------------------------------")

    c.Expr[Any](result)
  }
}

