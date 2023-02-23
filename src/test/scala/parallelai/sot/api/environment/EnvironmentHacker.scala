package parallelai.sot.api.environment

import java.util.{ Collections, Properties }
import scala.collection.JavaConverters._
import org.scalatest.{ BeforeAndAfterEach, Suite }

trait EnvironmentHacker extends BeforeAndAfterEach {
  this: Suite =>

  private var env: Map[String, String] = _
  private var sysProperties: Properties = _

  override protected def beforeEach(): Unit = {
    env = sys.env
    sysProperties = System.getProperties
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    setEnv(env.asJava)
    System.setProperties(sysProperties)
    super.afterEach()
  }

  def setEnv(newEnv: java.util.Map[String, String]): Unit =
    try {
      val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
      val theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment")
      theEnvironmentField.setAccessible(true)
      val env = theEnvironmentField.get(null).asInstanceOf[java.util.Map[String, String]]
      env.putAll(newEnv)
      val theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
      theCaseInsensitiveEnvironmentField.setAccessible(true)
      val cienv = theCaseInsensitiveEnvironmentField.get(null).asInstanceOf[java.util.Map[String, String]]
      cienv.putAll(newEnv)
    } catch {
      case _: NoSuchFieldException =>
        try {
          val classes = classOf[Collections].getDeclaredClasses
          val env = System.getenv

          for (cl <- classes) {
            if (cl.getName == "java.util.Collections$UnmodifiableMap") {
              val field = cl.getDeclaredField("m")
              field.setAccessible(true)
              val obj = field.get(env)
              val map = obj.asInstanceOf[java.util.Map[String, String]]
              map.clear()
              map.putAll(newEnv)
            }
          }
        } catch {
          case e: Exception => e.printStackTrace()
        }

      case e: Exception => e.printStackTrace()
    }
}