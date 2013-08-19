import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.util.parsing.combinator._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox
import scala.reflect.runtime.{ currentMirror => m }
import java.beans.Introspector;

object ModelMacro { macro =>
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._
    val ww = ClassDef(Modifiers(CASE), newTypeName("Y"), List(), Template(List(Select(Ident("scala"), newTypeName("Product")), Select(Ident("scala"), newTypeName("Serializable"))), emptyValDef, List(DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(List()), TypeTree(), Block(List(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())), Literal(Constant(())))))))
    val www = ModuleDef(Modifiers(), newTermName("U"), Template(List(AppliedTypeTree(Ident(newTypeName("Z")), List(Ident(newTypeName("Y"))))), emptyValDef, List(DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(List()), TypeTree(), Block(List(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())), Literal(Constant(())))), EmptyTree)))

    val reservedNames = List("id", "dateCreated", "dateUpdated")
    val caseAccessor = scala.reflect.internal.Flags.CASEACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramAccessor = scala.reflect.internal.Flags.PARAMACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramDefault = scala.reflect.internal.Flags.DEFAULTPARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val param = scala.reflect.internal.Flags.PARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val optionalDate = AppliedTypeTree(Ident(newTypeName("Option")), List(Select(Select(Ident(newTermName("java")), newTermName("sql")), newTypeName("Date"))))
    val idVal = ValDef(Modifiers(caseAccessor | paramAccessor), newTermName("id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName("Int")))), EmptyTree)
    val idValInCtor = ValDef(Modifiers(param | paramAccessor), newTermName("id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName("Int")))), EmptyTree)
    def dateVal(name: String) = ValDef(Modifiers(caseAccessor | paramAccessor), newTermName(name), optionalDate, EmptyTree)
    def dateValInCtor(name: String) = ValDef(Modifiers(param | paramAccessor | paramDefault), newTermName(name), optionalDate, Ident(newTermName("None")))

    def mkCaseClass(typeName: TypeName, columnVals: List[(Modifiers, TermName, Tree, _, _)], parents: List[Tree], self: ValDef, augment: Boolean = true) = {
      val valdefs = columnVals.map(t => ValDef(t._1, t._2, t._3, EmptyTree))
      val newAttrs = if (augment) idVal +: valdefs :+ dateVal("dateCreated") :+ dateVal("dateUpdated") else valdefs
      val ctorParams = if (augment) idValInCtor +: valdefs :+ dateValInCtor("dateCreated") :+ dateValInCtor("dateUpdated") else valdefs
      val newCtor = DefDef(Modifiers(),
        nme.CONSTRUCTOR, List(),
        ctorParams :: Nil,
        TypeTree(),
        Block(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), Nil) :: Nil, Literal(Constant(()))))
      ClassDef(Modifiers(CASE), typeName, List(), Template(parents, self, newAttrs :+ newCtor))
    }

    /**
     * given a fieldName and a type tree return "def fieldName = column[tpe]("fieldName")
     */
    def mkColumn(name: TermName, tpe: Tree) = {
      DefDef(Modifiers(), name, Nil, Nil, TypeTree(), Apply(TypeApply(Ident(newTermName("column")), tpe :: Nil), Literal(Constant(name.decoded)) :: Nil))
    }

    /**
     * a not so clever algorithm to get a table name from a row name aka case class name
     */
    def tableName(fieldName: String) = {
      fieldName.last match {
        case 'y' => fieldName.substring(0, fieldName.length - 1) + "ies"
        case 's' | 'u' => fieldName.substring(0, fieldName.length) + "x"
        case _ => fieldName.substring(0, fieldName.length) + "s"
      }
    }

    /**
     * create the field1 ~ field2 ~ ... ~fieldN string from case class column
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
          s"def * = id.? ~ ${mkTilde(columnNames)} ~ dateCreated.? ~ dateUpdated.? <> (${typeName.decoded}, ${typeName.decoded}.unapply _)"
        else
          s"def * = ${mkTilde(columnNames)} <> (${typeName.decoded}, ${typeName.decoded}.unapply _)"
      }
      c.parse(expr)
    }

    /**
     * create the def forInsert = ...
     */
    def mkForInsert(typeName: TypeName, columnNames: List[c.universe.TermName]): Tree = {
      val tuple = List.tabulate(columnNames.size + 2)(n => ("t._" + (n + 1).toString)).reduce(_ + ", " + _)
      val apply = s"""{ t => ${typeName.decoded}(None, $tuple) }"""
      val fields = columnNames.map("x." + _.decoded).reduce(_ + "," + _)
      val unapply = s"""{(x: ${typeName.decoded}) => Some(($fields, x.dateCreated, x.dateUpdated))}"""
      val expr = s"def forInsert = ${mkTilde(columnNames)} ~ dateCreated.? ~ dateUpdated.? <> ($apply,$unapply)"
      c.parse(expr)
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

    /**
     * create the case class and foreign keys for 1,n relationships and the slick table description and the assoc table for n,m relationships
     * if augment is set to true timestamp & forInsert defs are generated too
     */
    def mkTable(caseClassesName: List[String], classdef: Tree, augment: Boolean = true): List[Tree] = {
      val ClassDef(mod, typeName, Nil, Template(parents, self, body)) = classdef
      val columnVals = body.collect {
        case ValDef(mod, name, tpt, rhs) =>
          if (augment && reservedNames.exists(_ == name.decoded))
            c.abort(c.enclosingPosition, s"Column with name ${name.decoded} not allowed")
          else {
            tpt match {
              case Ident(tpe) if caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), Ident(newTypeName("Int")), rhs, Some(("", tpe)))
              case AppliedTypeTree(Ident(option), List(Ident(tpe))) if option.decoded == "Option" && caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName("Int")))), rhs, Some(("Option", tpe)))

              case AppliedTypeTree(Ident(list), List(Ident(tpe))) if list.decoded == "List" && caseClassesName.exists(_ == tpe.decoded) =>
                (mod, newTermName(name.decoded + "Id"), AppliedTypeTree(Ident(newTypeName("Option")), List(Ident(newTypeName("Int")))), rhs, Some(("List", tpe)))
              case _ =>
                (mod, name, tpt, rhs, None)
            }
          }
      }
      val foreignKeys = columnVals.collect { it =>
        it match {
          case (_, name, _, rhs, Some((option, tpe))) if option == "" || option == "Option" =>
            c.parse(s"""def ${tpe.decoded.toLowerCase} = foreignKey("${typeName.decoded.toLowerCase}2${tpe.decoded.toLowerCase}", $name, ${tableName(tpe.decoded)})(_.id)""")
        }
      }
      val assocs = columnVals.collect { it =>
        it match {
          case (_, name, _, rhs, Some(("List", tpe))) =>
            c.parse(s"""case class ${typeName.decoded}2${tpe.decoded}(${Introspector.decapitalize(typeName.decoded)}:${typeName.decoded}, ${Introspector.decapitalize(tpe.decoded)}:${tpe.decoded})""")
        }
      }
      val assocTables = assocs.flatMap { mkTable(caseClassesName, _, false) }
      val idVal = c.parse("""def id = column[Int]("id", O.PrimaryKey, O.AutoInc);""")
      def dateCVal = c.parse("""def dateCreated = column[java.sql.Date]("dateCreated")""")
      def dateUVal = c.parse("""def dateUpdated = column[java.sql.Date]("dateUpdated")""")
      val defdefs = columnVals.map(t => mkColumn(t._2, t._3))
      val times = mkTimes(typeName, columnVals.map(_._2), augment)
      val forInsert = mkForInsert(typeName, columnVals.map(_._2))
      val ctor =
        DefDef(
          Modifiers(),
          nme.CONSTRUCTOR,
          Nil,
          Nil :: Nil,
          TypeTree(),
          Block(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), Literal(Constant(typeName.decoded.toLowerCase())) :: Nil) :: Nil, Literal(Constant(()))))

      val moduledef =
        ModuleDef(Modifiers(),
          newTermName(tableName(typeName.decoded)),
          Template(
            AppliedTypeTree(Ident(newTypeName("Table")), Ident(newTypeName(typeName.decoded)) :: Nil) :: Nil,
            emptyValDef,
            if (augment) ctor :: idVal :: dateCVal :: dateUVal :: times :: forInsert :: defdefs ++ foreignKeys else ctor :: times :: defdefs ++ foreignKeys))
      List(mkCaseClass(typeName, columnVals, parents, self, augment), moduledef) ++ assocTables
    }
    val result = {
      annottees.map(_.tree).toList match {
        case ModuleDef(_, moduleName, Template(parents, self, body)) :: Nil =>
          val (caseClasses, modules) = body.partition {
            case ModuleDef(x, y, Template(List(Ident(t)), u, v)) if t.isTypeName && t.decoded == "Enumeration" => false
            case ClassDef(mod, typeName, Nil, tmpl) if mod == Modifiers(CASE) => true
            case DefDef(x, y, z, t, u, v) => false
            case Import(_, _) => false
            case _ =>
              c.abort(c.enclosingPosition, "Only moduledef && classdef && defdef allowed here")
          }
          val caseClassesName = caseClasses.flatMap {
            case ClassDef(mod, typeName, Nil, tmpl) => Some(typeName.decoded)
            case _ => None
          }
          val tables = caseClasses.flatMap(mkTable(caseClassesName, _))
          val mods = modules.flatMap {
            case ModuleDef(modifiers, name, tmpl) => Some(mkModules(name.decoded))
            case _ => None
          }
          ModuleDef(Modifiers(), moduleName, Template(parents, self, modules ++ mods ++ tables))
        case _ =>
          c.abort(c.enclosingPosition, s"Only module defs allowed here")
      }
    }
    println("========================")
    println(result)
    println("========================")
    c.Expr[Any](result)
  }
}

class Model extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro ModelMacro.impl
}

