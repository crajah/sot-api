package parallelai.sot.api.mechanics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files._
import grizzled.slf4j.Logging
import spray.json._
import parallelai.sot.api.config.executor
import parallelai.sot.api.entities.Files.{ ApplicationConfigFile, RuleFile }
import parallelai.sot.api.file

trait ConfigMechanic extends file.FileOps with Logging {
  def createConfiguration(json: JsValue, rulePath: File, ruleName: String): Future[(ApplicationConfigFile, RuleFile)] = Future {
    val ruleResourcePath = rulePath / executor.configuration.resourcePath.toString

    val ruleFile = ruleResourcePath / s"$ruleName.json"
    info(s"Creating Rulefile $ruleFile")
    ruleFile.createIfNotExists(createParents = true)
    ruleFile write json.prettyPrint

    val applicationConfigFile = ruleResourcePath / executor.configuration.configFileName
    applicationConfigFile delete true
    info(s"Creating App Conf $applicationConfigFile")

    applicationConfigFile write s"""
    {
      "json": {
        "file": {
          "name": "$ruleName.json"
        }
      }
    }
    """.parseJson.prettyPrint

    (new ApplicationConfigFile(applicationConfigFile), new RuleFile(ruleFile))
  }
}