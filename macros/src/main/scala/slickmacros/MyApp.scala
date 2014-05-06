package slickmacros

/**
 * Created by hayssams on 23/04/14.
 */
object MyApp extends App {
  def paramTypes(prefix: String, count: Int) = {
    0.until(count).toList.map(prefix+_).mkString(",")
  }

  def paramPlaceHolders(count: Int) = {
    0.until(count).toList.map(ignore => "_").mkString(",")
  }

  println(paramTypes("T", 10))
  println(paramPlaceHolders(10))

}
