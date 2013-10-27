package slickmacros.annotations

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
import scala.slick.lifted._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

class Model(timestamps: Boolean = false) extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro ModelMacro.impl
}
class Entity(name: String = null, timestamps: Boolean = false) extends StaticAnnotation
class Col(name: String = null, tpe: String = null, index: Boolean = false, unique: Boolean = false, pk: Boolean = false, onDelete: ForeignKeyAction = ForeignKeyAction.NoAction, onUpdate: ForeignKeyAction = ForeignKeyAction.NoAction, oldName: String = null) extends StaticAnnotation
//class Part extends StaticAnnotation

trait Timestamps
trait Part

object ModelMacro { macro =>

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

    val reservedNames = List("id", "dateCreated", "lastUpdated")
    val caseAccessor = scala.reflect.internal.Flags.CASEACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramAccessor = scala.reflect.internal.Flags.PARAMACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val prvate = scala.reflect.internal.Flags.PRIVATE.asInstanceOf[Long].asInstanceOf[FlagSet]
    val local = scala.reflect.internal.Flags.LOCAL.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramDefault = scala.reflect.internal.Flags.DEFAULTPARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val param = scala.reflect.internal.Flags.PARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val mutable = scala.reflect.internal.Flags.MUTABLE.asInstanceOf[Long].asInstanceOf[FlagSet]
    val optionalDate = Select(Select(Ident(newTermName("java")), newTermName("sql")), newTypeName("Timestamp"))
    val caseparam = Modifiers(caseAccessor | paramAccessor)
    val paramparam = Modifiers(param | paramAccessor)
    def idVal(tpeName: TypeName) = q"$caseparam val id:Option[$tpeName]"
    def idValInCtor(tpeName: TypeName) = q"$paramparam val id:Option[$tpeName]"

    def dateVal(name: String) = ValDef(Modifiers(mutable | caseAccessor | paramAccessor), newTermName(name), optionalDate, EmptyTree)
    def dateValInCtor(name: String) = ValDef(Modifiers(param | paramAccessor | paramDefault), newTermName(name), optionalDate, Literal(Constant(null))) // Ident(newTermName("None")))

    class ClsDesc(val name: String, val flags: Set[ClassFlag], val fields: ListBuffer[FldDesc], val tree: Tree, var plural: String) {
      def parseBody(allClasses: List[ClsDesc]) {
        val constraintsTerm = newTermName("constraints")
        val ClassDef(mod, name, Nil, Template(parents, self, body)) = tree
        body.foreach { it =>
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

      def dateValsInCtor: List[ValDef] = if (timestamps) dateValInCtor("dateCreated") :: dateValInCtor("lastUpdated") :: Nil else Nil

      def dateDefs =
        if (timestamps)
          c.parse("""def dateCreated = column[java.sql.Timestamp]("dateCreated")""") :: c.parse("""def lastUpdated = column[java.sql.Timestamp]("lastUpdated")""") :: Nil
        else
          Nil
      def foreignKeys: List[FldDesc] = {
        fields.filter { it => it.flags.exists(_ == FieldFlag.CASE) && !it.flags.exists(_ == FieldFlag.LIST) } toList
      }
      def assocs: List[FldDesc] = {
        fields.filter { it => it.flags.exists(_ == FieldFlag.CASE) && it.flags.exists(_ == FieldFlag.LIST) } toList
      }
      def simpleValDefs: List[FldDesc] = {
        fields.filter { it => !it.flags.exists(_ == FieldFlag.LIST) } toList
      }
      def listValDefs: List[FldDesc] = {
        fields.filter { it => it.flags.exists(_ == FieldFlag.LIST) } toList
      }
      def listPKs: List[FldDesc] = {
        fields.filter { it => it.flags.exists(_ == FieldFlag.PK) } toList
      }
      def allFields = {
        fields.toList.map { it =>
          if (it.part)
            it.cls.get.fields toList
          else
            it :: Nil
        } flatten
      }

      def indexes: List[FldDesc] = {
        allFields.filter { it =>
          it.flags.exists(_ == FieldFlag.INDEX)
        } toList
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
        new ClsDesc(name.decoded, flags, ListBuffer(), tree, plural(Introspector.decapitalize(name.decoded)))
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
    }
    class ScalaAnnotation(val name: String, fields: List[(String, String)])
    object ScalaAnnotation {
      def apply(expr: Tree) = {
        val sexpr = expr.toString
        if (!sexpr.startsWith("new "))
          c.abort(c.enclosingPosition, s"Invalid annotation $sexpr")
        else {
          val name = sexpr.substring("new ".length, sexpr.indexOf('('))
          val params = sexpr.substring(sexpr.indexOf('('), sexpr.lastIndexOf(')')).split(',').map { x =>

          }

        }
        new ScalaAnnotation("", List())
      }
    }
    object FldDesc {
      def apply(fieldTree: Tree, clsTree: Tree, allClasses: List[ClsDesc]) = {
        val ValDef(mod, name, tpt, rhs) = fieldTree
        if (reservedNames.exists(_ == name.decoded))
          c.abort(c.enclosingPosition, s"Column with name ${name.decoded} not allowed")
        else {
          val flags = Set[FieldFlag]()
          val annotation = mod.annotations.headOption.map(_.children.head.toString)
          var colType: String = null
          var colName: String = name.decoded
          var onDelete: String = "NoAction"
          var onUpdate: String = "NoAction"

          mod.annotations.map(x => x.toString).foreach { annotation =>
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
              named.foreach { param =>

                param._1 match {
                  case "onDelete" => onDelete = param._2.replaceAll("^.*\\.", "")
                  case "onUpdate" => onUpdate = param._2.replaceAll("^.*\\.", "")
                  case "name" => colName = param._2.replaceAll("""^"|"$""", "")
                  case "tpe" =>
                    flags += FieldFlag.DBTYPE
                    colType = param._2.replaceAll("""^"|"$""", "")
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
              clsDesc.foreach { it =>
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
              clsDesc.foreach { it =>
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
          tree.foreach { it =>
            it match {
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
          }

          val ClassDef(_, clsName, _, Template(_, _, body)) = clsTree
          body.foreach { it =>
            val cns = it match {
              case Apply(Ident(constraintsTerm), List(Block(stats, expr))) =>
                Some(plural(Introspector.decapitalize(clsName.decoded)), stats :+ expr)
              case Apply(Apply(Ident(constraintsTerm), List(Literal(Constant(arg)))), List(Block(stats, expr))) =>
                Some(arg.toString, stats :+ expr)
              case _ => None
            }
            cns foreach { it =>
              allClasses.find(_.name == clsName.decoded).foreach { x =>
                x.plural = it._1
              }
              (it._2).foreach { s =>
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
                    st.drop(1).foreach { s =>
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

    def mkCaseClass(desc: ClsDesc, augment: Boolean = true): ClassDef = {
      if (desc.part) {
        desc.tree.asInstanceOf[ClassDef]
      } else {
        val valdefs = desc.simpleValDefs.map { it =>
          if (it.cse) {
            val tpt = if (it.option) {
              AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName(s"${typeId(it.typeName)}"))))
            } else {
              Ident(newTypeName(s"${typeId(it.typeName)}"))
            }
            val ValDef(mod, nme, _, _) = it.tree
            val termName = newTermName(nme.decoded + "Id")
            q"$mod val $termName:$tpt"
          } else
            it.tree.asInstanceOf[ValDef]
        }

        val idTypeName = newTypeName(s"${typeId(desc.name)}")
        val newAttrs = if (augment) idVal(idTypeName) :: valdefs ++ desc.dateVals else valdefs
        val ctorParams1 = if (augment) idValInCtor(idTypeName) :: valdefs ++ desc.dateValsInCtor else valdefs
        val newCtor1 = DefDef(Modifiers(),
          nme.CONSTRUCTOR, List(),
          ctorParams1 :: Nil,
          TypeTree(),
          Block(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), Nil) :: Nil, Literal(Constant(()))))

        val terms = desc.simpleValDefs.map { it =>
          if (it.cse) {
            Ident(newTermName(it.name + "Id"))
          } else {
            Ident(newTermName(it.name))
          }

        }
        val ctor3body = Ident(newTermName("None")) :: terms
        val paramsdef = valdefs map { it =>
          val ValDef(mod, nme, tpt, rhs) = it
          ValDef(Modifiers(param), nme, tpt, rhs)
        }
        val newCtor3 = DefDef(Modifiers(),
          nme.CONSTRUCTOR, List(),
          paramsdef :: Nil,
          TypeTree(),
          Block(List(Apply(Ident(nme.CONSTRUCTOR), ctor3body)), Literal(Constant(()))))

        val xid = q"""def xid = id.getOrElse(throw new Exception("Object has no id yet"))"""

        val defdefs = desc.foreignKeys.map { it =>
          if (it.option)
            q"""def ${newTermName("load" + it.name.capitalize)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(it.typeName))}.where(_.id === ${newTermName(colIdName(it.name))}).firstOption"""
          else
            q"""def ${newTermName("load" + it.name.capitalize)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(it.typeName))}.where(_.id === ${newTermName(colIdName(it.name))}).first"""
        }
        val one2manyDefs = desc.assocs.map { it =>
          q"""
            def ${newTermName("load" + it.name.capitalize)} = for {
        	  	x <- self.${newTermName(objectName(assocTableName(desc.name, it.typeName)))} if x.${newTermName(colIdName(desc.name))} === id
        		y <- self.${newTermName(objectName(it.typeName))} if x.${newTermName(colIdName(it.typeName))} === y.id
        	} yield(y)
            """
        }

        val one2manyDefAdds = desc.assocs.map { it =>
          q"""def ${newTermName("add" + it.typeName)}(${newTermName(colIdName(it.typeName))} : ${newTypeName("Long")})(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(assocTableName(desc.name, it.typeName)))}.insert(${newTermName(assocTableName(desc.name, it.typeName))}(xid, ${newTermName(colIdName(it.typeName))}))"""
        }
        val lst = if (augment) newCtor1 :: /* newCtor3 :: */ xid :: newAttrs ++ defdefs ++ one2manyDefs ++ one2manyDefAdds else newCtor1 :: newAttrs ++ defdefs ++ one2manyDefs ++ one2manyDefAdds

        ClassDef(Modifiers(CASE), desc.name, List(), Template(List(
          Select(Ident(newTermName("scala")), newTypeName("Product")),
          Select(Ident(newTermName("scala")), newTypeName("Serializable"))),
          emptyValDef, lst))
      }
    }
    /**
     * given a fieldName and a type tree return "def fieldName = column[tpe]("fieldName")
     */

    def mkColumn(desc: FldDesc): Tree = {
      val q"$mods val $nme:$tpt = $initial" = desc.tree
      if (desc.cse) {
        if (desc.option) {
          q"""def ${newTermName(colIdName(desc.name))} = column[Option[${typeId(desc.typeName)}]](${colIdName(desc.name)})"""
        } else {
          q"""def ${newTermName(colIdName(desc.name))} = column[${typeId(desc.typeName)}](${colIdName(desc.name)})"""
        }
      } else {
        val tpe = desc.typeName
        desc.dbType map { it =>
          q"""def $nme = column[$tpt](${desc.colName}, O.DBType(${it}))"""
        } getOrElse {
          q"""def $nme = column[$tpt](${desc.colName})"""
        }
      }
    }

    def colIdName(caseClassName: String) = {
      s"${Introspector.decapitalize(caseClassName)}Id"
    }

    def tableName(typeName: String) = s"${typeName}Table"

    def objectName(typeName: String) = s"${plural(Introspector.decapitalize(typeName))}"
    
    def assocTableName(table1: String, table2: String) = s"${table1}2${table2}"

    /**
     * create the field1 ~ field2 ~ ... ~ fieldN string from case class column
     * does not handle correctly case classes with a single column (adding a dummy field would probably help)
     */
    def mkTilde(fields: List[FldDesc]): String = {
      fields match {
        case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil =>
          if (field.part)
            "(" + mkTilde(field.cls.get.fields toList) + ")"
          else if (field.cse)
            colIdName(field.name)
          else
            field.name
        case head :: tail => s"${mkTilde(head :: Nil)}, ${mkTilde(tail)}"
      }
    }

    def mkCaseApply(fields: List[FldDesc]): String = {
      fields match {
        case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil =>
          if (field.part)
            s"${field.typeName}.tupled.apply(${field.name})"
          else if (field.cse)
            colIdName(field.name)
          else
            field.name
        case head :: tail => s"${mkCaseApply(head :: Nil)}, ${mkCaseApply(tail)}"
      }
    }

    def mkCaseUnapply(fields: List[FldDesc]): String = {
      fields match {
        case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil =>
          if (field.part) {
            s"${field.typeName}.unapply(x.${field.name}).get"
          } else if (field.cse)
            "x." + colIdName(field.name)
          else
            "x." + field.name
        case head :: tail => s"${mkCaseUnapply(head :: Nil)}, ${mkCaseUnapply(tail)}"
      }
    }

    def mkCase(fields: List[FldDesc]): String = {
      fields match {
        case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil =>
          if (field.part) {
            field.name
          } else if (field.cse)
            colIdName(field.name)
          else
            field.name
        case head :: tail => s"${mkCase(head :: Nil)}, ${mkCase(tail)}"
      }
    }

    /**
     * create the def * = ... from fields names and case class names
     */
    def mkTimes(desc: ClsDesc, augment: Boolean = true): Tree = {
      val expr = {
        if (augment)
          if (desc.timestamps)
            s"""def * = (id.?, ${mkTilde(desc.simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
		        case (id, ${mkCase(desc.simpleValDefs)}, dateCreated, lastUpdated) => ${desc.name}(id, ${mkCaseApply(desc.simpleValDefs)}, dateCreated, lastUpdated)
		      }, { x : ${desc.name} => Some((x.id, ${mkCaseUnapply(desc.simpleValDefs)}, x.dateCreated, x.lastUpdated))
		      })"""
          else
            s"""def * = (id.?, ${mkTilde(desc.simpleValDefs)}).shaped <> ({
		        case (id, ${mkCase(desc.simpleValDefs)}) => ${desc.name}(id, ${mkCaseApply(desc.simpleValDefs)})
		      }, { x : ${desc.name} => Some((x.id, ${mkCaseUnapply(desc.simpleValDefs)}))
		      })"""
        else
          //s"""def * = (${mkTilde(desc.fields toList)}).shaped <> (${desc.name}.tupled, ${desc.name}.unapply _)"""
          s"""def * = (${mkTilde(desc.fields toList)}).shaped <> ({
		        case (${mkCase(desc.simpleValDefs)}) => ${desc.name}( ${mkCaseApply(desc.simpleValDefs)})
		      }, { x : ${desc.name} => Some((${mkCaseUnapply(desc.simpleValDefs)}))
		      })"""

      }
      c.parse(expr)
    }

    def mkForInsert(desc: ClsDesc): Tree = {
      val expr = {
        if (desc.timestamps)
          s"""def forInsert = (${mkTilde(desc.simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
		        case (${mkCase(desc.simpleValDefs)}, dateCreated, lastUpdated) => ${desc.name}(None, ${mkCaseApply(desc.simpleValDefs)}, dateCreated, lastUpdated)
		      }, { x : ${desc.name} => Some((${mkCaseUnapply(desc.simpleValDefs)}, x.dateCreated, x.lastUpdated))
		      })"""
        else
          s"""def forInsert = (${mkTilde(desc.simpleValDefs)}).shaped <> ({
		        case (${mkCase(desc.simpleValDefs)}) => ${desc.name}(None, ${mkCaseApply(desc.simpleValDefs)})
		      }, { x : ${desc.name} => Some((${mkCaseUnapply(desc.simpleValDefs)}))
		      })"""

      }
      c.parse(expr)
    }

    /**
     * Create the Enumeration Type Mapper
     */
    def mkEnumMapper(name: String): Tree = {
      val mapper = s"""implicit val ${name}TypeMapper = MappedColumnType.base[${name}.Value, Int](
            {
              it => it.id
            },
            {
              id => ${name}(id)
            })"""
      c.parse(mapper)
    }

    def typeId(tpeName: String) = newTypeName("Long")

    def mkTypeId(tpeName: String): List[Tree] = {
      val tp = typeId(tpeName)
      val obj = newTermName(s"${tpeName}Id")
      val cc = q"""case class $tp(val rowId: Long)"""
      val imp = q"""implicit object $obj extends (Long => $tp)"""
      List(cc, imp)
    }

    def cleanupBody(body: List[Tree]): List[Tree] = {
      val cleanDefs = List("embed", "onDelete", "index", "timestamps", "dbType")
      body filter { it =>
        it match {
          case Apply(Ident(func), List(Ident(field), Literal(dbType))) if cleanDefs.contains(func.decoded) => false
          case _ => true
        }
      }
    }

    object ColInfo extends Enumeration {
      type ColInfo = Value
      val DBTYPE = Value
      val TIMESTAMPS = Value
      val INDEX = Value
      val ONDELETE = Value
      val PK = Value
    }
    import ColInfo._

    def caseInfos(body: List[Tree]): Map[ColInfo, List[(ColInfo, (String, Tree))]] = {
      body collect {
        case Apply(Ident(func), List(Ident(field), dbType)) if func.decoded == "dbType" => (DBTYPE, (field.decoded, dbType))
        case Apply(Ident(func), List(literal)) if func.decoded == "timestamps" => (TIMESTAMPS, (null, literal))
        case Apply(Ident(func), List(Ident(field), isUnique)) if func.decoded == "index" => (INDEX, (field.decoded, isUnique))
        case Apply(Ident(func), List(Ident(field), action)) if func.decoded == "onDelete" => (ONDELETE, (field.decoded, action))
      } groupBy (_._1)
    }

    /**
     * create the case class and foreign keys for 1,n relationships and the slick table description and the assoc table for n,m relationships
     * if augment is set to true timestamp & forInsert defs are generated too
     */

    def mkTable(desc: ClsDesc, augment: Boolean = true): List[Tree] = {
      if (desc.part)
        List(desc.tree.asInstanceOf[ClassDef])
      else {
        val simpleVals = desc.simpleValDefs
        val listVals = desc.listValDefs
        val indexes = desc.indexes
        val foreignKeys = desc.foreignKeys.map { it =>
          //val fkAction = if (colInfo.isDefined && colInfo.get.onDelete != null) colInfo.get.onDelete else "ForeignKeyAction.NoAction"
          c.parse(s"""def ${it.name}FK = foreignKey("${desc.name.toLowerCase}2${it.typeName.toLowerCase}", ${colIdName(it.name)}, ${objectName(it.typeName)})(_.id, ${it.onUpdateAction}, ${it.onDeleteAction}) """) // onDelete
        }
        val assocs = desc.assocs.map { it =>
          new ClsDesc(assocTableName(desc.name, it.typeName), Set(ENTITYDEF),
            ListBuffer(
              new FldDesc(Introspector.decapitalize(desc.name), Introspector.decapitalize(desc.name), desc.name, Set(FieldFlag.CASE), None, "NoAction", "NoAction", Some(desc), ValDef(caseparam, Introspector.decapitalize(desc.name), null, null)),
              new FldDesc(Introspector.decapitalize(it.typeName), Introspector.decapitalize(it.typeName), it.typeName, Set(FieldFlag.CASE), None, "NoAction", "NoAction", it.cls, ValDef(caseparam, Introspector.decapitalize(it.typeName), null, null))),
            null, plural(Introspector.decapitalize(assocTableName(desc.name, it.typeName))))

        }
        val assocTables = assocs.flatMap { it => mkTable(it, false) }
        val idCol = q"""def id = column[${typeId(desc.name)}]("id", O.PrimaryKey, O.AutoInc);"""

        val defdefs = simpleVals.flatMap { it =>
          if (it.part) {
            it.cls.get.fields.map { fld =>
              mkColumn(fld)
            }
          } else {
            List(mkColumn(it))
          }
        }

        val indexdefs: List[c.universe.Tree] = indexes.map { it =>
          q"""def ${newTermName(it.name + "Index")} = index(${"idx_" + desc.name.toLowerCase + "_" + it.name.toLowerCase}, ${newTermName(it.name)}, ${it.unique})"""
        }
        val times = mkTimes(desc, augment)
        val forInsert = mkForInsert(desc)
        val tagDef = ValDef(Modifiers(prvate | local | paramAccessor), newTermName("tag"), Ident(newTypeName("Tag")), EmptyTree)
        val ctor =
          DefDef(
            Modifiers(),
            nme.CONSTRUCTOR,
            Nil,
            (ValDef(Modifiers(param | paramAccessor), newTermName("tag"), Ident(newTypeName("Tag")), EmptyTree) :: Nil) :: Nil,
            TypeTree(),
            Block(List(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List(Ident(newTermName("tag")), Literal(Constant(desc.name.toLowerCase()))))), Literal(Constant(()))))

        val tableDef =
          ClassDef(Modifiers(),
            newTypeName(tableName(desc.name)), Nil,
            Template(
              AppliedTypeTree(Ident(newTypeName("Table")), Ident(newTypeName(desc.name)) :: Nil) :: Nil,
              emptyValDef,
              if (augment) ctor :: idCol :: times :: forInsert :: desc.dateDefs ++ defdefs ++ indexdefs ++ foreignKeys else ctor :: times :: indexdefs ++ defdefs ++ foreignKeys))
        List(mkCaseClass(desc, augment), tableDef) ++ mkCompanion(desc) ++ assocTables
      }
    }
    def mkCompanion(desc: ClsDesc) = {
      val query = c.parse(s"val ${newTermName(objectName(desc.name))} = TableQuery[${newTypeName(tableName(desc.name))}]")
      val crud = if (desc.timestamps) "CrudEx" else "Crud"
      val crudType = c.parse(s"type ${desc.name}Crud = CrudEx[${desc.name}, ${desc.name}Table]")
      if (desc.assoc)
        query :: Nil
      else
        query :: crudType :: Nil
    }
    def defMap(body: List[c.universe.Tree]): Map[DefType, List[(DefType, c.universe.Tree)]] = {
      body.flatMap { it =>
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
          val annotations = mod.annotations.map(_.children.head.toString)
          val timestampsAll = c.prefix.tree.toString.contains("true") || parents.exists(_.toString.contains("Timestamps")) // Q&D
          val allDefs = defMap(body)
          val caseDefs = allDefs.getOrElse(CLASSDEF, Nil).map(it => ClsDesc(it._2, timestampsAll))
          caseDefs.foreach(_.parseBody(caseDefs))
          val tableDefList = caseDefs.flatMap(mkTable(_))
          println("--> Generated queries :")
          caseDefs.foreach { x =>
            println(plural(Introspector.decapitalize(x.name)))
          }
          println("--> End of generated queries.")
          
          val enumDefList = allDefs.getOrElse(ENUMDEF, Nil).map(_._2)
          val defdefList = allDefs.getOrElse(DEFDEF, Nil).map(_._2)
          val importdefList = allDefs.getOrElse(IMPORTDEF, Nil).map(_._2)
          val otherdefList = allDefs.getOrElse(OTHERDEF, Nil).map(_._2)
          val enumMapperList = allDefs.get(ENUMDEF).map(_.map { it =>
            val ModuleDef(_, name, _) = it._2
            mkEnumMapper(name.decoded)
          }) getOrElse (Nil)
          val embedDefList = allDefs.getOrElse(EMBEDDEF, Nil).map(_._2)

          val extraImports = List(
            Import(Select(Select(Select(Ident(newTermName("scala")), newTermName("slick")), newTermName("util")), newTermName("TupleMethods")), List(ImportSelector(nme.WILDCARD, -1, null, -1))),
            Import(Select(Select(Ident(newTermName("scala")), newTermName("slick")), newTermName("jdbc")), List(ImportSelector(newTermName("JdbcBackend"), -1, newTermName("JdbcBackend"), -1))),
            Import(Select(Select(Ident(newTermName("slickmacros")), newTermName("dao")), newTermName("Crud")), List(ImportSelector(nme.WILDCARD, 90, null, -1))),
            Import(Select(Ident(newTermName("slickmacros")), newTermName("Implicits")), List(ImportSelector(nme.WILDCARD, 121, null, -1))))

          ModuleDef(mod, moduleName, Template(parents, ValDef(Modifiers(PRIVATE), newTermName("self"), TypeTree(), EmptyTree), enumDefList ++ extraImports ++ importdefList ++ enumMapperList ++ embedDefList ++ tableDefList ++ defdefList))

        case _ =>
          c.abort(c.enclosingPosition, s"Only module defs allowed here")
      }
    }
    //println(result)
    c.Expr[Any](result)
  }
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

