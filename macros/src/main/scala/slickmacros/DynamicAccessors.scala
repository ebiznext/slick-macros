package slickmacros

import scala.reflect.macros.Context
import scala.reflect.runtime.{ universe => u }
import scala.language.experimental.macros

/**
 * Work in progress
 * will update timestamp on insert & updates
 * provide friendly update like update(name = "me", age=10)
 * provide friendly finder find((name, age))
 * where clause will generate autojoin
 */
object DynamicAccessors {
  def getTypeTag[T: u.TypeTag](obj: T) = u.typeTag[T]

  def insertImpl[T: c.WeakTypeTag](c: Context)(obj: c.Expr[T]): c.Expr[Int] = {
    import c.universe._
    def getTypeTag[T: u.TypeTag](obj: T) = u.typeTag[T]
    val prefix = c.typeCheck(c.prefix.tree)
    val args = c.typeCheck(obj.tree)
    val instanceT = implicitly[c.WeakTypeTag[T]].tpe
    val field = prefix.tpe.members filter (member => member.name.decoded == "myType") head
    val traitType = field.typeSignatureIn(prefix.tpe)
    if (traitType.typeSymbol == args.tpe.typeSymbol) {
      val result = Apply(Select(prefix, newTermName("doInsert")), List(args))
      println(result)
      c.Expr[Int](result)
    } else
      c.abort(c.enclosingPosition, s"${args.tpe} does not conform to $traitType")
  }
  //  def doInsert(r: DefMacroData) = DefMacroTable.forInsert returning DefMacroTable.id insert r

}

trait DynamicAccessors[T] {
  implicit val myType: T = implicitly
  def doInsert[T <: AnyRef](obj: T): Int = macro DynamicAccessors.insertImpl[T]
  /*
   * 	def doWhere = macro whereImpl
	    def doDelete = macro deleteImpl
	    def doUpdate = macro deleteImpl
	    def doFind = macro findImpl
	*/
}
