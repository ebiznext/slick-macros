package slickmacros

import scala.reflect.macros.Context
import scala.annotation.StaticAnnotation
import scala.util.parsing.combinator._
import scala.reflect.runtime.universe._
import java.beans.Introspector;
import scala.Option.option2Iterable
import scala.language.experimental.macros

object ModelMacro { macro =>
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    case class FieldDesc(name: String, isOption: Boolean, isCaseClass: Boolean, isList: Boolean, tpe: String)
    type ColDesc = (Modifiers, TermName, Tree, _, Option[FieldDesc])

    val reservedNames = List("id" /*, "dateCreated", "dateUpdated"*/ )
    val caseAccessor = scala.reflect.internal.Flags.CASEACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramAccessor = scala.reflect.internal.Flags.PARAMACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramDefault = scala.reflect.internal.Flags.DEFAULTPARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val param = scala.reflect.internal.Flags.PARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val optionalDate = AppliedTypeTree(Ident(newTypeName("Option")), List(Select(Select(Ident(newTermName("java")), newTermName("sql")), newTypeName("Date"))))
    val caseparam = Modifiers(caseAccessor | paramAccessor)
    val paramparam = Modifiers(param | paramAccessor)
    def idVal(tpeName: TypeName) = q"$caseparam val id:Option[$tpeName]"
    def idValInCtor(tpeName: TypeName) = q"$paramparam val id:Option[$tpeName]"
    /*
    def dateVal(name: String) = ValDef(Modifiers(caseAccessor | paramAccessor), newTermName(name), optionalDate, EmptyTree)
    def dateValInCtor(name: String) = ValDef(Modifiers(param | paramAccessor | paramDefault), newTermName(name), optionalDate, Ident(newTermName("None")))
     */
    def mkCaseClass(typeName: TypeName, columnVals: List[ColDesc], columnDefs: List[ColDesc], parents: List[Tree], self: ValDef, augment: Boolean = true) = {
      val valdefs = columnVals.collect {
        //case (mods, name, tpt, _, Some(_)) => q"$mods val $name:$tpt" //ValDef(mods, name, tpt, EmptyTree)
        case (mods, name, tpt, _, _) => q"$mods val $name:$tpt" // ValDef(mods, name, tpt, EmptyTree)
        //case (mods, name, tpt, _, None) => q"$mods val $name:$tpt" // ValDef(mods, name, tpt, EmptyTree)
      }
      val idTypeName = newTypeName(s"${typeId(typeName.decoded)}")
      val newAttrs = if (augment) idVal(idTypeName) +: valdefs /* :+ dateVal("dateCreated") :+ dateVal("dateUpdated")*/ else valdefs
      val ctorParams = if (augment) idValInCtor(idTypeName) +: valdefs /* :+ dateValInCtor("dateCreated") :+ dateValInCtor("dateUpdated") */ else valdefs
      val newCtor = DefDef(Modifiers(),
        nme.CONSTRUCTOR, List(),
        ctorParams :: Nil,
        TypeTree(),
        Block(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), Nil) :: Nil, Literal(Constant(()))))

      val xid = q"""def xid = id.getOrElse(throw new Exception("Object has no id yet"))"""

      val defdefs = columnVals.collect {
        case (_, _, _, _, Some(FieldDesc(name, false, true, false, tpe))) =>
          q"""def ${newTermName(name)} = Query(${newTermName(objectName(tpe))}).where(_.id === ${newTermName(colIdName(name))}).first"""
        case (_, _, _, _, Some(FieldDesc(name, true, true, false, tpe))) =>
          q"""def ${newTermName(name)} = Query(${newTermName(objectName(tpe))}).where(_.id === ${newTermName(colIdName(name))}).firstOption"""
      }
      println(s"***********DEFDEFS $typeName*******************")
      columnDefs.foreach(println)
      val one2manyDefs = columnDefs.collect {
        case (_, _, _, _, Some(FieldDesc(name, false, true, true, tpe))) =>
          q"""def ${newTermName(name)} = Query(${newTermName(objectName(assocTableName(typeName.decoded, tpe)))}).where(_.${newTermName(colIdName(typeName.decoded))} === id)"""
      }
      //defdefs.foreach(println)
      println("***********-------*******************")
      ClassDef(Modifiers(CASE), typeName, List(), Template(parents, self, if (augment) xid :: newCtor :: newAttrs ++ defdefs ++ one2manyDefs else newCtor :: newAttrs ++ defdefs ++ one2manyDefs))
    }

    /**
     * given a fieldName and a type tree return "def fieldName = column[tpe]("fieldName")
     */
    def mkColumn(name: TermName, tpe: Tree) = {
      q"""def $name = column[$tpe](${name.decoded})"""
    }

    def colIdName(caseClassName: String) = {
      s"${Introspector.decapitalize(caseClassName)}Id"
    }
    /*
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

    def tableName(fieldName: String) = s"${fieldName}Table"

    def objectName(fieldName: String) = plural(fieldName)

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
          s"def * = id.? ~ ${mkTilde(columnNames)} /* ~ dateCreated.? ~ dateUpdated.? */ <> (${typeName.decoded}, ${typeName.decoded}.unapply _)"
        else
          s"def * = ${mkTilde(columnNames)} <> (${typeName.decoded}, ${typeName.decoded}.unapply _)"
      }
      c.parse(expr)
    }

    /**
     * create the def forInsert = ...
     */
    def mkCRUD(typeName: TypeName, columnNames: List[c.universe.TermName]): List[Tree] = {
      val tuple = List.tabulate(columnNames.size /*+ 2*/ )(n => ("t._" + (n + 1).toString)).reduce(_ + ", " + _)
      val apply = s"""{ t => ${typeName.decoded}(None, $tuple) }"""
      val fields = columnNames.map("x." + _.decoded).reduce(_ + "," + _)
      //val unapply = s"""{(x: ${typeName.decoded}) => Some(($fields, x.dateCreated, x.dateUpdated))}"""
      val unapply = s"""{(x: ${typeName.decoded}) => Some(($fields))}"""
      //val expr = s"def forInsert = ${mkTilde(columnNames)} /* ~ dateCreated.? ~ dateUpdated.? */ <> ($apply,$unapply)"
      val expr = s"def forInsert = ${mkTilde(columnNames)} <> ($apply,$unapply)"

      List(
        c.parse(expr),
        q"""def insert(obj: $typeName) = forInsert returning id insert obj""",
        q"""def delete(objId: ${typeId(typeName.decoded)}) = Query(this).where(_.id === objId).delete""",
        q"""def update(obj: $typeName) = (for { row <- this if row.id === obj.xid } yield row) update (obj)""",
        q"""def byId(objId: ${typeId(typeName.decoded)}) = Query(this).where(_.id === objId).firstOption""")

    }

    /**
     * Create the Enumeration Type Mapper
     */
    def mkModules(name: String) = {
      val mapper = s"""implicit val ${name}TypeMapper = MappedTypeMapper.base[${name}.Value, Int](
            {
              it => it.id
            },
            {
              id => ${name}(id)
            })"""
      c.parse(mapper)
    }
    def typeId(tpeName: String) = newTypeName("Long") //newTypeName(s"${tpeName}Id")
    def mkTypeId(tpeName: String): List[Tree] = {
      val tp = typeId(tpeName)
      val obj = newTermName(s"${tpeName}Id")
      val cc = q"""case class $tp(val rowId: Long)"""
      val imp = q"""implicit object $obj extends (Long => $tp)"""
      List(cc, imp)
    }
    /**
     * create the case class and foreign keys for 1,n relationships and the slick table description and the assoc table for n,m relationships
     * if augment is set to true timestamp & forInsert defs are generated too
     */
    def mkTable(caseClassesName: List[String], classdef: Tree, augment: Boolean = true): List[Tree] = {
      val ClassDef(mod, typeName, Nil, Template(parents, self, body)) = classdef
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
      /*
      def dateCVal = c.parse("""def dateCreated = column[java.sql.Date]("dateCreated")""")
      def dateUVal = c.parse("""def dateUpdated = column[java.sql.Date]("dateUpdated")""")
      */
      val defdefs = simpleVals.map(t => mkColumn(t._2, t._3))
      val times = mkTimes(typeName, simpleVals.map(_._2), augment)
      val crud = mkCRUD(typeName, simpleVals.map(_._2))
      val ctor =
        DefDef(
          Modifiers(),
          nme.CONSTRUCTOR,
          Nil,
          Nil :: Nil,
          TypeTree(),
          Block(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), Literal(Constant(typeName.decoded.toLowerCase())) :: Nil) :: Nil, Literal(Constant(()))))

      val tableDef =
        ClassDef(Modifiers(),
          newTypeName(tableName(typeName.decoded)), Nil,
          Template(
            AppliedTypeTree(Ident(newTypeName("Table")), Ident(newTypeName(typeName.decoded)) :: Nil) :: Nil,
            emptyValDef,
            if (augment) ctor :: idCol /* :: dateCVal :: dateUVal */ :: times :: defdefs ++ crud ++ foreignKeys else ctor :: times :: defdefs ++ foreignKeys))
      val objectDef = q"object ${newTermName(objectName(typeName.decoded))} extends ${newTypeName(tableName(typeName.decoded))}"
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
          //val idTypeMapper = q"implicit def IdTypeMapper[T <: { val rowId: Long }](implicit comap: Long => T): scala.slick.lifted.BaseTypeMapper[T] = MappedTypeMapper.base[T, Long](_.rowId, comap)"

          val typeIds = List() //caseClassesName map (mkTypeId(_)) flatten
          val tables = caseClasses.flatMap(mkTable(caseClassesName, _))
          val mods = modules.collect {
            case ModuleDef(modifiers, name, tmpl) => mkModules(name.decoded)
          }
          //List(typeMapper) ++ typeIds ++ modules ++ mods ++ tables

          //val flat = typeIds.flatten
          ModuleDef(Modifiers(), moduleName, Template(parents, self, modules /* ++ List(idTypeMapper) */ ++ typeIds ++ mods ++ tables))
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

