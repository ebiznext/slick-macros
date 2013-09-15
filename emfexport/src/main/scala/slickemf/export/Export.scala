package slickemf.export

import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import slickmacros.export.EmfUtils
import slickmacros.reflect.ClassDesc
import slickmacros.reflect.MemberDesc
import slickmacros.reflect.TypeDesc
import scala.collection.JavaConversions._
import java.io.FileOutputStream
import java.io.OutputStream

class Export {
  //create add classes to package

  def getDataType(typeName: String, eCustomTypes: scala.collection.mutable.MutableList[EClassifier]) = {
    mapScalaType2Emf(typeName).getOrElse {
      if (!eCustomTypes.exists(_.getName() == typeName)) {
        eCustomTypes += EmfUtils.createEClass(typeName)
      }
      eCustomTypes.find(_.getName() == typeName).getOrElse(null)
    }
  }
  val eCustomTypes = scala.collection.mutable.MutableList[EClassifier]()

  def export2EMF(classes: List[ClassDesc], ouputFilename: String) {
    val ePackage = EmfUtils.createEPackage("SlickMacrosPackage")
    cases2Emf(classes) foreach { c => EmfUtils.addEClassToEPackage(c, ePackage) }
    tables2Emf(classes) foreach { c => EmfUtils.addEClassToEPackage(c, ePackage) }
    //add custom types
    eCustomTypes foreach (c => EmfUtils.addEClassToEPackage(c.asInstanceOf[EClassifier], ePackage))

    //save ecore file
    EmfUtils.savePackage(ePackage, ouputFilename)
  }

  def cases2Emf(classes: List[ClassDesc]) = {
    val classeNames = classes.filter(!_.isTable).map(_.name)
    val eClasses = classes.filter(!_.isTable).map(it => EmfUtils.createEClass(it.name)) //create all classes

    classes.filter(!_.isTable).foreach { c =>
      val eClass = (eClasses.find(_.asInstanceOf[EClass].getName() == c.name)).getOrElse(null).asInstanceOf[EClass]
      c.members.foreach { m =>
        m match {
          case MemberDesc(mname, TypeDesc("Option", tparams), _) if classeNames.contains(tparams.head) =>
            //option
            val eDataType = eClasses.find(_.getName() == tparams.head).getOrElse(null)
            EmfUtils.addOptionalOne2OneEReferenceToEClass(mname, eDataType, eClass)

          case MemberDesc(mname, TypeDesc("List", tparams), _) if classeNames.contains(tparams.head) =>
            //list one to many
            val eDataType = eClasses.find(_.getName() == tparams.head).getOrElse(null)
            EmfUtils.addOne2ManyEReferenceToEClass(mname, eDataType, eClass)

          case MemberDesc(mname, TypeDesc(tname, Nil), Nil) if classeNames.contains(tname) =>
            //create one to one reference
            val eDataType = eClasses.find(_.getName() == tname).getOrElse(null)
            EmfUtils.addOne2OneEReferenceToEClass(mname, eDataType, eClass)

          case MemberDesc(mname, TypeDesc("Option", tparams), _) =>
            //create optional attribute
            val existingDataType = getDataType("Option[" + tparams.head + "]", eCustomTypes)

            EmfUtils.addOprionalEAttributeToEClass(mname, existingDataType, eClass)

          case MemberDesc(mname, TypeDesc("Query", tparams), mparams) =>
            //query
            val returnType = "Query[" + tparams.head + "," + tparams.last + "]"
            val existingDataType = getDataType(returnType, eCustomTypes)

            val eParameters = mparams.map(it => EmfUtils.createEParameter(it.name, getDataType(it.tpe.name, eCustomTypes)))
            EmfUtils.addEOperationToEClass(mname, existingDataType, eParameters, eClass)

          case MemberDesc(mname, TypeDesc(tname, tparams), Nil) =>
            //create attribute
            val existingDataType = getDataType(tname, eCustomTypes)
            EmfUtils.addEAttributeToEClass(mname, existingDataType, eClass)

          case MemberDesc(mname, TypeDesc("TableElementType",Nil), _) if classeNames.contains(mname.capitalize) =>
            //create one to one reference
            val eDataType = eClasses.find(_.getName() == mname.capitalize).getOrElse(null)
            EmfUtils.addOne2OneEReferenceToEClass(mname, eDataType, eClass)

          case MemberDesc(mname, TypeDesc(tname, tparams), mparams) =>
            //create method
            val existingDataType = getDataType(tname, eCustomTypes)
            val eParameters = mparams.map(it => EmfUtils.createEParameter(it.name, getDataType(it.tpe.name, eCustomTypes)))
            EmfUtils.addEOperationToEClass(mname, existingDataType, eParameters, eClass)
        }
      }
    }
    eClasses
    //eClasses.foreach { c => EmfUtils.addEClassToEPackage(c, ePackage) }
    //add custom types
    //eCustomTypes.foreach(c => EmfUtils.addEClassToEPackage(c.asInstanceOf[EClassifier], ePackage))

  }

  def tables2Emf(classes: List[ClassDesc]) = {
    val classeNames = classes.filter(_.isTable).map(_.name)
    val eClasses = classes.filter(_.isTable).map(it => EmfUtils.createEClass(it.name)) //create all classes

    classes.filter(_.isTable).foreach { c =>

      val eClass = (eClasses.find(_.asInstanceOf[EClass].getName() == c.name)).getOrElse(null).asInstanceOf[EClass]
      c.members.foreach { m =>
        m match {
          case MemberDesc(mname, TypeDesc("ForeignKeyQuery", tparams), Nil) if classeNames.contains(tparams.last + "Table") =>
            //create one to one reference
            val eDataType = eClasses.find(_.asInstanceOf[EClass].getName() == tparams.last + "Table").getOrElse(null).asInstanceOf[EClass]
            EmfUtils.addOptionalOne2OneEReferenceToEClass(mname, eDataType, eClass)

          case MemberDesc(mname, TypeDesc("Column", tparams), Nil) =>
            //create attribute
            val existingDataType = getDataType(tparams.head, eCustomTypes)
            EmfUtils.addEAttributeToEClass(mname, existingDataType, eClass)

          case MemberDesc(mname, TypeDesc("Option", tparams), mparams) =>
            val existingDataType = getDataType("Option[" + tparams.head + "]", eCustomTypes)

            val eParameters = mparams.map(it => EmfUtils.createEParameter(it.name, getDataType(it.tpe.name, eCustomTypes)))
            EmfUtils.addEOperationToEClass(mname, existingDataType, eParameters, eClass)

          case MemberDesc(mname, TypeDesc("MappedProjection", tparams), mparams) =>
            val returnType = "MappedProjection[" + tparams.head + "," + tparams.last + "]"
            val existingDataType = getDataType(returnType, eCustomTypes)
            val eParameters = mparams.map(it => EmfUtils.createEParameter(it.name, getDataType(it.tpe.name, eCustomTypes)))
            EmfUtils.addEOperationToEClass(mname, existingDataType, eParameters, eClass)

          case MemberDesc(mname, TypeDesc(tname, tparams), mparams) =>
            val existingDataType = getDataType(tname, eCustomTypes)
            val eParameters = mparams.map(it => EmfUtils.createEParameter(it.name, getDataType(it.tpe.name, eCustomTypes)))
            EmfUtils.addEOperationToEClass(mname, existingDataType, eParameters, eClass)
        }
      }
    }
    eClasses
  }

  def mapScalaType2Emf(tname: String): Option[EClassifier] = tname match {
    case "String" => Some(EmfUtils.getCorePackage().getEString())
    case "Boolean" => Some(EmfUtils.getCorePackage().getEString())
    case "Int" => Some(EmfUtils.getCorePackage().getEInt())
    case "Integer" => Some(EmfUtils.getCorePackage().getEInt())
    case "Long" => Some(EmfUtils.getCorePackage().getELong())
    case "Double" => Some(EmfUtils.getCorePackage().getEDouble())
    case "Float" => Some(EmfUtils.getCorePackage().getEFloat())
    case "Date" => Some(EmfUtils.getCorePackage().getEDate())
    case "Byte" => Some(EmfUtils.getCorePackage().getEByte())
    case "Char" => Some(EmfUtils.getCorePackage().getEChar())
    case "Short" => Some(EmfUtils.getCorePackage().getEShort())
    case _ => None
  }
}
