import slickmacros.reflect._
import slickmacros.annotations._
import slickemf.export._


object SampleApp extends App {
  implicit val dbConnectionInfo = DbConnectionInfos(url = "jdbc:postgresql:SampleApp", user = "postgres", password = "e-z12B24", driverClassName = "org.postgresql.Driver")
  Services.populate
  //queryDB
  val descs = new ObjectRef(model.XDb).reflect
  new Export().export2EMF(descs, "database/slickemf.ecore")

}
