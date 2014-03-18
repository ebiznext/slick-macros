import slickmacros.reflect._
import slickmacros.annotations._
import slickemf.export._


object SampleApp extends App {
  Services.populate()
  val descs = new ObjectRef(model.XDb).reflect
  new Export().export2EMF(descs, "database/slickemf.ecore")
}
