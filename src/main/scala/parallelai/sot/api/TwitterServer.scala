package parallelai.sot.api

import java.util.logging.{ Level, LogManager }
import org.slf4j.bridge.SLF4JBridgeHandler
import com.twitter.app.App
import com.twitter.server._
import com.twitter.util.logging.Slf4jBridgeUtility

trait TwitterServer extends App
  with Linters with Hooks with AdminHttpServer with Admin with Lifecycle with Stats {

  /** Attempt Slf4jBridgeHandler installation */
  Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation

  init {
    // Turn off Java util logging so that slf4j can configure it
    LogManager.getLogManager.getLogger("").getHandlers.toList.foreach(_.setLevel(Level.OFF))
    SLF4JBridgeHandler.install()
  }

  onExit {
    SLF4JBridgeHandler.uninstall()
  }
}