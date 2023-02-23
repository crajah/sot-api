package parallelai.sot.api.config

import java.net.URI
import java.nio.file.Path
import better.files._
import parallelai.sot.api.file

case class Executor(directory: File, configuration: Configuration, dao: Dao, sbt: Sbt, git: Git, google: Google, launch: Launch, rule: Rule, folder: Folder)

case class Configuration(resourcePath: Path, configFileName: String)

case class Dao(prefix: String)

case class Sbt(command: String, opts: String)

case class Git(repo: URI, user: String, password: String, branch: String, tag: Option[String], localPath: Path) {
  val localFile: String => File = { version =>
    File(localPath) / version
  }
}

case class Google(bucket: String, dataflowRegion: String, projectId: String)

case class Launch(className: String, opts: String)

case class Rule(localPath: Path, localStagePath: Path, launchPath: Path, jar: Jar, git: Git) extends file.FileOps {
  val localFile: String => File = { name =>
    File(localPath) / name
  }

  val jarFile: (String, String) => File = { (ruleId, version) =>
    localFile(ruleId) / version / jar.path.toString / jar.fileName // TODO
  }

  val jarStageFile: (String, String) => File = { (ruleId, version) =>
    File(localStagePath) / version / jarName(ruleId) // TODO
  }

  val jarLaunchFile: String => File = { ruleId =>
    File(launchPath) / jarName(ruleId)
  }
}

case class Jar(path: Path, fileName: String)

case class Folder(root: Root)

case class Root(id: String)