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

class Model extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro ModelMacro.impl
}

class Part extends StaticAnnotation
class Index(unique: Boolean = false) extends StaticAnnotation
class Type(dbType: String) extends StaticAnnotation
class OnDelete(action: ForeignKeyAction) extends StaticAnnotation

object ModelMacro { macro =>

  object DefType extends Enumeration {
    type DefType = Value
    val CLASSDEF = Value
    val DEFDEF = Value
    val IMPORTDEF = Value
    val ENUMDEF = Value
    val EMBEDDEF = Value
    val OTHER = Value
  }
  import DefType._

  object ClassType extends Enumeration {
    type ClassType = Value
    val PARTDEF = Value
    val ENTITYDEF = Value
    val OTHER = Value
  }
  import ClassType._

  object FieldFlag extends Enumeration {
    type FieldFlag = Value
    val OPTION = Value
    val CASE = Value
    val PART = Value
    val LIST = Value
    val INDEX = Value
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

    class ClsDesc(val name: String, val classType: ClassType, val fields: ListBuffer[FldDesc], val timestamps: Boolean, val tree: Tree) {
      def parseBody(allClasses: List[ClsDesc]) {
        val ClassDef(mod, name, Nil, Template(parents, self, body)) = tree
        body.foreach { it =>
          it match {
            case ValDef(_, _, _, _) => fields += FldDesc(it, allClasses)
            //case q"val $mods $name:$tpe = $value" => fields += FldDesc(it, allClasses)
            case _ =>
          }

        }
      }
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
      def apply(tree: Tree) = {
        val ClassDef(mod, name, Nil, _) = tree
        if (!mod.hasFlag(CASE))
          c.abort(c.enclosingPosition, s"Only case classes allwoed here ${name.decoded}")
        val annotation = mod.annotations.headOption.map(_.children.head.toString)
        val isPart = annotation.map(_ == "new Part").getOrElse(false)
        val classType = if (isPart) PARTDEF else ENTITYDEF
        new ClsDesc(name.decoded, classType, ListBuffer(), true, tree)
      }
    }
    class FldDesc(val name: String, val typeName: String, val flags: Set[FieldFlag], val dbType: Option[String], val cls: Option[ClsDesc], val tree: Tree) {
      def unique: Boolean = flags.exists(_ == FieldFlag.UNIQUE)
      def part: Boolean = flags.exists(_ == FieldFlag.PART)
      def option: Boolean = flags.exists(_ == FieldFlag.OPTION)
      def cse: Boolean = flags.exists(_ == FieldFlag.CASE)
    }

    object FldDesc {
      def apply(fieldTree: Tree, allClasses: List[ClsDesc]) = {
        val ValDef(mod, name, tpt, rhs) = fieldTree
        if (reservedNames.exists(_ == name.decoded))
          c.abort(c.enclosingPosition, s"Column with name ${name.decoded} not allowed")
        else {
          val flags = Set[FieldFlag]()
          val annotation = mod.annotations.headOption.map(_.children.head.toString)
          var colType: String = null
          val isIndex = annotation.map(_ == "new Index").getOrElse(false)
          if (isIndex) {
            flags += FieldFlag.INDEX
            mod.annotations.headOption.foreach { it =>
              if (it.children.length >= 2 && it.children(1).toString.equals("true")) flags += FieldFlag.UNIQUE
            }
          }
          val isdbType = annotation.map(_ == "new Type").getOrElse(false)
          if (isdbType) {
            flags += FieldFlag.DBTYPE
            mod.annotations.headOption.foreach { it =>
              if (it.children.length >= 2) colType = it.children(1).toString
            }
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
                if (it.classType == ENTITYDEF) {
                  flags += FieldFlag.CASE
                } else if (it.classType == PARTDEF)
                  flags += FieldFlag.PART
              }
              clsDesc
            case AppliedTypeTree(Ident(option), tpe :: Nil) if option.decoded == "Option" =>
              typeName = buildTypeName(tpe)
              println("^^^^^^^^^^^" + typeName + "%%%%%%%%%%%%%%%%%%" + name.toString)
              flags += FieldFlag.OPTION
              val clsDesc = allClasses.find(_.name == typeName)
              clsDesc.foreach { it =>
                if (it.classType == ENTITYDEF)
                  flags += FieldFlag.CASE
              }
              clsDesc
            case AppliedTypeTree(Ident(list), tpe :: Nil) if list.decoded == "List" =>
              typeName = buildTypeName(tpe)
              val ClsDesc = allClasses.find(_.name == typeName).getOrElse(c.abort(c.enclosingPosition, s"List not allowed here ${name.decoded} not allowed"))
              if (ClsDesc.classType == ENTITYDEF)
                flags ++= Set(FieldFlag.CASE, FieldFlag.LIST)
              else
                c.abort(c.enclosingPosition, s"only entity allowed here ${name.decoded}")
              Some(ClsDesc)
            case _ => None
          }
          val tree = mod.annotations.headOption
          tree.foreach { it =>
            it match {
              case Apply(Select(New(Ident(index)), _), List(Literal(Constant(unique)))) =>
                if (index.decoded == "Index") flags += FieldFlag.INDEX
                if (unique == true) flags += FieldFlag.UNIQUE
              case Apply(Select(New(Ident(dbType)), _), List(Literal(Constant(dbTypeValue)))) =>
                if (dbType.decoded == "Type") flags += FieldFlag.DBTYPE
                colType = dbTypeValue.asInstanceOf[String]
            }
          }
          new FldDesc(name.decoded, typeName, flags, Option(colType), clsDesc, fieldTree)
        }
      }
    }
    case class FieldDesc(name: String, isOption: Boolean, isCaseClass: Boolean, isList: Boolean, isEmbed: Boolean, tpe: String)

    type ColDesc = (Modifiers, TermName, Tree, _, Option[FieldDesc])

    def mkCaseClass(desc: ClsDesc, augment: Boolean = true): ClassDef = {
      if (desc.classType == PARTDEF) {
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
        val newAttrs = if (augment) idVal(idTypeName) +: valdefs :+ dateVal("dateCreated") :+ dateVal("lastUpdated") else valdefs
        val ctorParams1 = if (augment) idValInCtor(idTypeName) +: valdefs :+ dateValInCtor("dateCreated") :+ dateValInCtor("lastUpdated") else valdefs
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

        val newCtor3 = DefDef(Modifiers(),
          nme.CONSTRUCTOR, List(),
          valdefs :: Nil,
          TypeTree(),
          Block(List(Apply(Ident(nme.CONSTRUCTOR), ctor3body)), Literal(Constant(()))))

        val xid = q"""def xid = id.getOrElse(throw new Exception("Object has no id yet"))"""

        val defdefs = desc.foreignKeys.map { it =>
          /*DefDef(
              Modifiers(), 
              newTermName(name),
              Nil,
              List(ValDef(Modifiers(IMPLICIT | PARAM), newTermName("session"), SelectFromTypeTree(Ident(newTypeName("JdbcBackend")), newTypeName("SessionDef")), EmptyTree)::Nil),
              TypeTree(), 

              )*/
          if (it.option)
            q"""def ${newTermName(it.name)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(it.typeName))}.where(_.id === ${newTermName(colIdName(it.name))}).firstOption"""
          else
            q"""def ${newTermName(it.name)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(it.typeName))}.where(_.id === ${newTermName(colIdName(it.name))}).first"""
        }
        val one2manyDefs = desc.assocs.map { it =>
          q"""
            def ${newTermName(it.name)} = for {
        	  	x <- ${newTermName(objectName(assocTableName(desc.name, it.typeName)))} if x.${newTermName(colIdName(desc.name))} === id
        		y <- ${newTermName(objectName(it.typeName))} if x.${newTermName(colIdName(it.typeName))} === y.id
        	} yield(y)
            """
        }
        val one2manyDefAdds = desc.assocs.map { it =>
          q"""def ${newTermName("add" + it.typeName)}(${newTermName(colIdName(it.typeName))} : ${newTypeName("Long")})(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(assocTableName(desc.name, it.typeName)))}.insert(${newTermName(assocTableName(desc.name, it.typeName))}(xid, ${newTermName(colIdName(it.typeName))}))"""
        }
        val lst = if (augment) newCtor1 :: xid :: newAttrs ++ defdefs ++ one2manyDefs ++ one2manyDefAdds else newCtor1 :: newAttrs ++ defdefs ++ one2manyDefs ++ one2manyDefAdds

        //val lst = if (augment) newCtor1 :: xid :: newAttrs ++ defdefs else newCtor1 :: newAttrs ++ defdefs

        ClassDef(Modifiers(CASE), desc.name, List(), Template(List(Select(Ident(newTermName("scala")), newTypeName("Product")), Select(Ident(newTermName("scala")), newTypeName("Serializable"))), emptyValDef, lst))
      }
    }
    /**
     * given a fieldName and a type tree return "def fieldName = column[tpe]("fieldName")
     */
    //        class FldDesc(val name: String, val flags: Set[FieldFlag], val dbType: String, val cls: Option[ClsDesc], val tree: Tree)

    def mkColumn(desc: FldDesc): Tree = {
      val q"$mods val $nme:$tpt = $initial" = desc.tree
      if (desc.cse) {
        if (desc.option) {
          q"""def ${newTermName(colIdName(desc.name))} = column[Option[${typeId(desc.typeName)}]](${colIdName(desc.name)})"""
        } else {
          q"""def ${newTermName(colIdName(desc.name))} = column[${typeId(desc.typeName)}](${colIdName(desc.name)})"""
        }
      } else {
        println(desc.name + ":" + desc.typeName + "//" + desc.option)

        val tpe = desc.typeName
        val expr = desc.dbType map { it =>
          q"""def $nme = column[$tpt](${nme.decoded}, O.DBType(${it}))"""
        } getOrElse {
          q"""def $nme = column[$tpt](${nme.decoded})"""
        }
        expr
        //c.parse(expr)
      }
      /*
      if (desc.cse) {
        if (desc.option) {
          q"""def ${newTermName(colIdName(desc.name))} = column[Option[${typeId(desc.typeName)}]](${colIdName(desc.name)})"""
        } else {
          q"""def ${newTermName(colIdName(desc.name))} = column[${typeId(desc.typeName)}](${colIdName(desc.name)})"""
        }
      } else {
        println(desc.name + ":" + desc.typeName + "//" + desc.option)

        val tpe = desc.typeName
        val expr = desc.dbType map { it =>
          if (desc.option) {
            s"""def ${desc.name} = column[Option[$tpe]]("${desc.name}", O.DBType(${it}))"""
          } else
            s"""def ${desc.name} = column[$tpe]("${desc.name}", O.DBType(${it}))"""
        } getOrElse {
          if (desc.option)
            s"""def ${desc.name} = column[Option[$tpe]]("${desc.name}")"""
          else
            s"""def ${desc.name} = column[$tpe]("${desc.name}")"""
        }
        c.parse(expr)
      }
      */
    }

    def colIdName(caseClassName: String) = {
      s"${Introspector.decapitalize(caseClassName)}Id"
    }

    def tableName(typeName: String) = s"${typeName}Table"

    def objectName(typeName: String) = s"${Introspector.decapitalize(typeName)}Query"

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
          s"""def * = (id.?, ${mkTilde(desc.simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
        case (id, ${mkCase(desc.simpleValDefs)}, dateCreated, lastUpdated) => ${desc.name}(id, ${mkCaseApply(desc.simpleValDefs)}, dateCreated, lastUpdated)
      }, { x : ${desc.name} => Some((x.id, ${mkCaseUnapply(desc.simpleValDefs)}, x.dateCreated, x.lastUpdated))
      })"""
        else
          s"def * = (${mkTilde(desc.fields toList)}).shaped <> (${desc.name}.tupled, ${desc.name}.unapply _)"
      }
      c.parse(expr)
    }

    def mkForInsert(desc: ClsDesc): Tree = {
      val expr = {
        s"""def forInsert = (${mkTilde(desc.simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
        case (${mkCase(desc.simpleValDefs)}, dateCreated, lastUpdated) => ${desc.name}(None, ${mkCaseApply(desc.simpleValDefs)}, dateCreated, lastUpdated)
      }, { x : ${desc.name} => Some((${mkCaseUnapply(desc.simpleValDefs)}, x.dateCreated, x.lastUpdated))
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
    }
    import ColInfo._

    def caseInfos(body: List[Tree]): Map[ColInfo, List[(ColInfo, (String, Tree))]] = {
      body collect {
        case Apply(Ident(func), List(Ident(field), dbType)) if func.decoded == "dbType" => (DBTYPE, (field.decoded, dbType))
        case Apply(Ident(func), List(literal)) if func.decoded == "timestamps" => (TIMESTAMPS, (null, literal))
        //case Apply(Ident(func), Nil) if func.decoded == "timestamps" => (TIMESTAMPS, (null, newTermName("true")))
        case Apply(Ident(func), List(Ident(field), isUnique)) if func.decoded == "index" => (INDEX, (field.decoded, isUnique))
        case Apply(Ident(func), List(Ident(field), action)) if func.decoded == "onDelete" => (ONDELETE, (field.decoded, action))
      } groupBy (_._1)
    }

    def extractBody(caseClassesName: List[String], embeds: List[String], body: List[Tree]) = {
      body.collect {
        case ValDef(mod, name, tpt, rhs) =>
          if (reservedNames.exists(_ == name.decoded))
            c.abort(c.enclosingPosition, s"Column with name ${name.decoded} not allowed")
          else {
            tpt match {
              case Ident(tpe) if embeds.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded), Ident(newTypeName(s"${tpe}")), rhs, Some(FieldDesc(name.decoded, false, true, false, true, tpe.decoded)))

              case Ident(tpe) if caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), Ident(newTypeName(s"${typeId(tpe.decoded)}")), rhs, Some(FieldDesc(name.decoded, false, true, false, false, tpe.decoded)))
              case AppliedTypeTree(Ident(option), List(Ident(tpe))) if option.decoded == "Option" && caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName(s"${typeId(tpe.decoded)}")))), rhs, Some(FieldDesc(name.decoded, true, true, false, false, tpe.decoded)))

              case AppliedTypeTree(Ident(list), List(Ident(tpe))) if list.decoded == "List" && caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName(s"${typeId(tpe.decoded)}")))), rhs, Some(FieldDesc(name.decoded, false, true, true, false, tpe.decoded)))
              case _ =>
                (mod, name, tpt, rhs, None)
            }
          }
      } partition {
        case (mods, name, self, _, Some(FieldDesc(_, _, _, true, _, _))) => true
        case _ => false
      }
    }

    /**
     * create the case class and foreign keys for 1,n relationships and the slick table description and the assoc table for n,m relationships
     * if augment is set to true timestamp & forInsert defs are generated too
     */

    def mkTable(desc: ClsDesc, augment: Boolean = true): List[Tree] = {
      if (desc.classType == PARTDEF)
        List(desc.tree.asInstanceOf[ClassDef])
      else {
        val simpleVals = desc.simpleValDefs
        val listVals = desc.listValDefs
        val indexes = desc.indexes
        val foreignKeys = desc.foreignKeys.map { it =>
          //val fkAction = if (colInfo.isDefined && colInfo.get.onDelete != null) colInfo.get.onDelete else "ForeignKeyAction.NoAction"
          c.parse(s"""def ${it.name}FK = foreignKey("${desc.name.toLowerCase}2${it.typeName.toLowerCase}", ${colIdName(it.name)}, ${objectName(it.typeName)})(_.id) """) // onDelete
        }
        val assocs = desc.assocs.map { it =>
          new ClsDesc(assocTableName(desc.name, it.typeName), ENTITYDEF,
            ListBuffer(
              new FldDesc(Introspector.decapitalize(desc.name), desc.name, Set(FieldFlag.CASE), None, Some(desc), ValDef(caseparam, Introspector.decapitalize(desc.name), null, null)),
              new FldDesc(Introspector.decapitalize(it.typeName), it.typeName, Set(FieldFlag.CASE), None, it.cls, ValDef(caseparam, Introspector.decapitalize(it.typeName), null, null))),
            true, null)

        }
        val assocTables = assocs.flatMap { it => mkTable(it, false) }
        val idCol = q"""def id = column[${typeId(desc.name)}]("id", O.PrimaryKey, O.AutoInc);"""

        def dateCVal = c.parse("""def dateCreated = column[java.sql.Timestamp]("dateCreated")""")
        def dateUVal = c.parse("""def lastUpdated = column[java.sql.Timestamp]("lastUpdated")""")

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
              if (augment) ctor :: idCol :: dateCVal :: dateUVal :: times :: forInsert :: defdefs ++ indexdefs ++ foreignKeys else ctor :: times :: indexdefs ++ defdefs ++ foreignKeys))
        val objectDef = q"val ${newTermName(objectName(desc.name))} = TableQuery[${newTypeName(tableName(desc.name))}]"
        //      List(mkCaseClass(desc, augment), tableDef, objectDef) ++ assocTables
        List(mkCaseClass(desc, augment), tableDef, objectDef) ++ assocTables
      }
    }
    def defMap(body: List[c.universe.Tree]): Map[DefType, List[(DefType, c.universe.Tree)]] = {
      body.flatMap { it =>
        it match {
          case ModuleDef(mod, name, Template(List(Ident(t)), _, _)) if t.isTypeName && t.decoded == "Enumeration" => List((ENUMDEF, it))
          case ClassDef(mod, name, Nil, body) if mod.hasFlag(CASE) => List((CLASSDEF, it))
          case DefDef(_, _, _, _, _, _) => List((DEFDEF, it))
          case Import(_, _) => List((IMPORTDEF, it))
          case _ =>
            println(it)
            c.abort(c.enclosingPosition, "Only moduledef && classdef && defdef allowed here")
        }
      } groupBy (_._1)
    }
    val result = {
      annottees.map(_.tree).toList match {
        case ModuleDef(_, moduleName, Template(parents, self, body)) :: Nil =>
          val allDefs = defMap(body)
          val caseDefs = allDefs.getOrElse(CLASSDEF, Nil).map(it => ClsDesc(it._2))
          caseDefs.foreach(_.parseBody(caseDefs))
          val tableDefList = caseDefs.flatMap(mkTable(_))

          val enumDefList = allDefs.getOrElse(ENUMDEF, Nil).map(_._2)
          val defdefList = allDefs.getOrElse(DEFDEF, Nil).map(_._2)
          val importdefList = allDefs.getOrElse(IMPORTDEF, Nil).map(_._2)
          val enumMapperList = allDefs.get(ENUMDEF).map(_.map { it =>
            val ModuleDef(_, name, _) = it._2
            mkEnumMapper(name.decoded)
          }) getOrElse (Nil)
          val embedDefList = allDefs.getOrElse(EMBEDDEF, Nil).map(_._2)

          ModuleDef(Modifiers(), moduleName, Template(parents, self, enumDefList ++ importdefList ++ enumMapperList ++ embedDefList ++ tableDefList ++ defdefList))
        case _ =>
          c.abort(c.enclosingPosition, s"Only module defs allowed here")
      }
    }
    println(result)
    c.Expr[Any](result)
  }
}

