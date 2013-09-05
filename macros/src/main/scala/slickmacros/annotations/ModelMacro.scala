package slickmacros.annotations

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

object ModelMacro { macro =>

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    case class FieldDesc(name: String, isOption: Boolean, isCaseClass: Boolean, isList: Boolean, tpe: String)
    type ColDesc = (Modifiers, TermName, Tree, _, Option[FieldDesc])

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

    def mkCaseClass(typeName: TypeName, columnVals: List[ColDesc], columnDefs: List[ColDesc], parents: List[Tree], self: ValDef, augment: Boolean = true) = {
      val valdefs = columnVals.collect {
        case (mods, name, tpt, _, _) => q"$mods val $name:$tpt"
      }

      val idTypeName = newTypeName(s"${typeId(typeName.decoded)}")
      val newAttrs = if (augment) idVal(idTypeName) +: valdefs :+ dateVal("dateCreated") :+ dateVal("lastUpdated") else valdefs
      val ctorParams1 = if (augment) idValInCtor(idTypeName) +: valdefs :+ dateValInCtor("dateCreated") :+ dateValInCtor("lastUpdated") else valdefs
      val newCtor1 = DefDef(Modifiers(),
        nme.CONSTRUCTOR, List(),
        ctorParams1 :: Nil,
        TypeTree(),
        Block(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), Nil) :: Nil, Literal(Constant(()))))

      val terms = columnVals.collect {
        case (mods, name, tpt, _, _) => Ident(name)
      }
      val ctor3body = Ident(newTermName("None")) :: terms

      val newCtor3 = DefDef(Modifiers(),
        nme.CONSTRUCTOR, List(),
        valdefs :: Nil,
        TypeTree(),
        Block(List(Apply(Ident(nme.CONSTRUCTOR), ctor3body)), Literal(Constant(()))))

      val xid = q"""def xid = id.getOrElse(throw new Exception("Object has no id yet"))"""

      val defdefs = columnVals.collect {
        case (_, _, _, _, Some(FieldDesc(name, false, true, false, tpe))) =>
          /*DefDef(
              Modifiers(), 
              newTermName(name),
              Nil,
              List(ValDef(Modifiers(IMPLICIT | PARAM), newTermName("session"), SelectFromTypeTree(Ident(newTypeName("JdbcBackend")), newTypeName("SessionDef")), EmptyTree)::Nil),
              TypeTree(), 

              )*/
          q"""def ${newTermName(name)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(tpe))}.where(_.id === ${newTermName(colIdName(name))}).first"""
        case (_, _, _, _, Some(FieldDesc(name, true, true, false, tpe))) =>
          q"""def ${newTermName(name)}(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(tpe))}.where(_.id === ${newTermName(colIdName(name))}).firstOption"""
      }
      val one2manyDefs = columnDefs.collect {
        case (_, _, _, _, Some(FieldDesc(name, false, true, true, tpe))) =>
          q"""
            def ${newTermName(name)} = for {
        	  	x <- ${newTermName(objectName(assocTableName(typeName.decoded, tpe)))} if x.${newTermName(colIdName(typeName.decoded))} === id
        		y <- ${newTermName(objectName(tpe))} if x.${newTermName(colIdName(tpe))} === y.id
        	} yield(y)
            """
      }
      val one2manyDefAdds = columnDefs.collect {
        case (_, _, _, _, Some(FieldDesc(name, false, true, true, tpe))) =>
          q"""def ${newTermName("add" + tpe)}(${newTermName(colIdName(tpe))} : ${newTypeName("Long")})(implicit session : JdbcBackend#SessionDef) = ${newTermName(objectName(assocTableName(typeName.decoded, tpe)))}.insert(${newTermName(assocTableName(typeName.decoded, tpe))}(xid, ${newTermName(colIdName(tpe))}))"""
      }
      val lst = if (augment) newCtor1 :: xid :: newAttrs ++ defdefs ++ one2manyDefs ++ one2manyDefAdds else newCtor1 :: newAttrs ++ defdefs ++ one2manyDefs ++ one2manyDefAdds

      ClassDef(Modifiers(CASE), typeName, List(), Template(parents, self, lst))
    }

    /**
     * given a fieldName and a type tree return "def fieldName = column[tpe]("fieldName")
     */
    def mkColumn(name: TermName, tpe: Tree, customType: Option[Constant]) = {
      customType map { it =>
        q"""def $name = column[$tpe](${name.decoded}, O.DBType(${it}))"""
      } getOrElse (q"""def $name = column[$tpe](${name.decoded})""")
    }

    def colIdName(caseClassName: String) = {
      s"${Introspector.decapitalize(caseClassName)}Id"
    }

    def tableName(fieldName: String) = s"${fieldName}Table"

    def objectName(fieldName: String) = s"${Introspector.decapitalize(fieldName)}Query"

    def assocTableName(table1: String, table2: String) = s"${table1}2${table2}"

    /**
     * create the field1 ~ field2 ~ ... ~ fieldN string from case class column
     * does not handle correctly case classes with a single column (adding a dummy field would probably help)
     */
    def mkTilde(columnNames: List[c.universe.TermName]): String = {
      columnNames match {
        case Nil => c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case term :: Nil => term.decoded
        case term :: tail => s"${term.decoded} ~ ${mkTilde(tail)}"
      }
    }

    /**
     * create the def * = ... from fields names and case class names
     */
    def mkTimes(typeName: TypeName, columnNames: List[c.universe.TermName], augment: Boolean): Tree = {
      val expr = {
        if (augment)
          s"def * = id.? ~ ${mkTilde(columnNames)} ~ dateCreated ~ lastUpdated <> (${typeName.decoded}.tupled, ${typeName.decoded}.unapply _)"
        else
          s"def * = ${mkTilde(columnNames)} <> (${typeName.decoded}.tupled, ${typeName.decoded}.unapply _)"
      }
      c.parse(expr)
    }

    /**
     * create the def forInsert = ...
     */
    def mkCRUD(typeName: TypeName, columnNames: List[c.universe.TermName]): List[Tree] = {
      val tuple = List.tabulate(columnNames.size + 2)(n => ("t._" + (n + 1).toString)).reduce(_ + ", " + _)
      val apply = s"""{ t => ${typeName.decoded}(None, $tuple) }"""
      val fields = columnNames.map("x." + _.decoded).reduce(_ + "," + _)
      val unapply = s"""{(x: ${typeName.decoded}) => Some(($fields, x.dateCreated, x.lastUpdated))}"""
      val expr = s"def forInsert = (${mkTilde(columnNames)} ~ dateCreated ~ lastUpdated).shaped <> ($apply,$unapply)"

      List(
        c.parse(expr))

    }

    /**
     * Create the Enumeration Type Mapper
     */
    def mkModules(name: String) = {
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

    def cleanupBody(body: List[Tree]) = {
      body filter { it =>
        it match {
          case Apply(Ident(func), List(Ident(field), Literal(dbType))) if func.decoded == "colType" => false
          case Apply(Ident(func), List(Ident(field), Literal(isUnique))) if func.decoded == "tableIndex" => false
          case _ => true
        }
      }
    }

    def customTypes(body: List[Tree]) = {
      body collect {
        case Apply(Ident(func), List(Ident(field), Literal(dbType))) if func.decoded == "colType" => (field.decoded, dbType)
      }
    }
    def indexes(body: List[Tree]) = {
      body collect {
        case Apply(Ident(func), List(Ident(field), Literal(isUnique))) if func.decoded == "tableIndex" => (field.decoded, isUnique)
      }
    }
    /**
     * create the case class and foreign keys for 1,n relationships and the slick table description and the assoc table for n,m relationships
     * if augment is set to true timestamp & forInsert defs are generated too
     */
    def mkTable(caseClassesName: List[String], classdef: Tree, augment: Boolean = true): List[Tree] = {
      val ClassDef(mod, typeName, Nil, Template(parents, self, body)) = classdef
      val customDBTypes = customTypes(body) toMap
      val dbIndexes = indexes(body)
      val (listVals, simpleVals) = body.collect {
        case ValDef(mod, name, tpt, rhs) =>
          if (augment && reservedNames.exists(_ == name.decoded))
            c.abort(c.enclosingPosition, s"Column with name ${name.decoded} not allowed")
          else {
            tpt match {
              case Ident(tpe) if caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), Ident(newTypeName(s"${typeId(tpe.decoded)}")), rhs, Some(FieldDesc(name.decoded, false, true, false, tpe.decoded)))
              case AppliedTypeTree(Ident(option), List(Ident(tpe))) if option.decoded == "Option" && caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName(s"${typeId(tpe.decoded)}")))), rhs, Some(FieldDesc(name.decoded, true, true, false, tpe.decoded)))

              case AppliedTypeTree(Ident(list), List(Ident(tpe))) if list.decoded == "List" && caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName(s"${typeId(tpe.decoded)}")))), rhs, Some(FieldDesc(name.decoded, false, true, true, tpe.decoded)))
              case _ =>
                (mod, name, tpt, rhs, None)
            }
          }
      } partition {
        case (mods, name, self, _, Some(FieldDesc(_, _, _, true, _))) => true
        case _ => false
      }

      val foreignKeys = simpleVals.collect { it =>
        it match {
          case (_, name, _, rhs, Some(FieldDesc(_, _, true, _, tpe))) =>
            c.parse(s"""def ${tpe.toLowerCase}FK = foreignKey("${typeName.decoded.toLowerCase}2${tpe.toLowerCase}", $name, ${objectName(tpe)})(_.id)""")
        }
      }
      val assocs = listVals.map { it =>
        val (_, name, _, rhs, Some(FieldDesc(_, _, true, true, tpe))) = it
        val assocType = newTypeName(assocTableName(typeName.decoded, tpe))
        q"""case class ${assocType}(${newTermName(Introspector.decapitalize(typeName.decoded))}:$typeName, ${newTermName(Introspector.decapitalize(tpe))}:${newTypeName(tpe)})"""
      }
      val assocTables = assocs.flatMap { mkTable(caseClassesName, _, false) }
      val idCol = q"""def id = column[${typeId(typeName.decoded)}]("id", O.PrimaryKey, O.AutoInc);"""

      def dateCVal = c.parse("""def dateCreated = column[java.sql.Timestamp]("dateCreated")""")
      def dateUVal = c.parse("""def lastUpdated = column[java.sql.Timestamp]("lastUpdated")""")

      val defdefs = simpleVals.map(t => mkColumn(t._2, t._3, customDBTypes.get(t._2.decoded)))

      val indexdefs: List[c.universe.Tree] = dbIndexes.map(it => q"""def ${newTermName(it._1 + "Index")} = index(${"IDX_" + typeName.decoded.toUpperCase + "_" + it._1.toUpperCase}, ${newTermName(it._1)}, ${it._2})""")
      val times = mkTimes(typeName, simpleVals.map(_._2), augment)
      val crud = mkCRUD(typeName, simpleVals.map(_._2))
      val tagDef = ValDef(Modifiers(prvate | local | paramAccessor), newTermName("tag"), Ident(newTypeName("Tag")), EmptyTree)
      val ctor =
        DefDef(
          Modifiers(),
          nme.CONSTRUCTOR,
          Nil,
          (ValDef(Modifiers(param | paramAccessor), newTermName("tag"), Ident(newTypeName("Tag")), EmptyTree) :: Nil) :: Nil,
          TypeTree(),
          Block(List(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List(Ident(newTermName("tag")), Literal(Constant(typeName.decoded.toLowerCase()))))), Literal(Constant(()))))

      val tableDef =
        ClassDef(Modifiers(),
          newTypeName(tableName(typeName.decoded)), Nil,
          Template(
            AppliedTypeTree(Ident(newTypeName("Table")), Ident(newTypeName(typeName.decoded)) :: Nil) :: Nil,
            emptyValDef,
            if (augment) ctor :: idCol :: dateCVal :: dateUVal :: times :: defdefs ++ indexdefs ++ crud ++ foreignKeys else ctor :: times :: indexdefs ++ defdefs ++ foreignKeys))
      val objectDef = q"val ${newTermName(objectName(typeName.decoded))} = TableQuery[${newTypeName(tableName(typeName.decoded))}]"
      List(mkCaseClass(typeName, simpleVals, listVals, parents, self, augment), tableDef, objectDef) ++ assocTables
    }
    val result = {
      annottees.map(_.tree).toList match {
        case ModuleDef(_, moduleName, Template(parents, self, body)) :: Nil =>
          val (caseClasses, modules) = body.partition {
            case ModuleDef(_, _, Template(List(Ident(t)), _, _)) if t.isTypeName && t.decoded == "Enumeration" => false
            case ClassDef(mod, _, Nil, _) if mod == Modifiers(CASE) => true
            case DefDef(_, _, _, _, _, _) => false
            case Import(_, _) => false
            case _ =>
              c.abort(c.enclosingPosition, "Only moduledef && classdef && defdef allowed here")
          }
          val caseClassesName = caseClasses.collect {
            case ClassDef(mod, typeName, Nil, tmpl) => typeName.decoded
          }

          val tables = caseClasses.flatMap(mkTable(caseClassesName, _))
          val mods = modules.collect {
            case ModuleDef(modifiers, name, tmpl) => mkModules(name.decoded)
          }

          ModuleDef(Modifiers(), moduleName, Template(parents, self, modules ++ mods ++ tables))
        case _ =>
          c.abort(c.enclosingPosition, s"Only module defs allowed here")
      }
    }
    println(result)
    c.Expr[Any](result)
  }
}

class Model extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro ModelMacro.impl
}

