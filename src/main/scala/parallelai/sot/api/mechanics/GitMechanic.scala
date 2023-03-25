package parallelai.sot.api.mechanics

import java.nio.file.NoSuchFileException
import scala.concurrent.{ ExecutionContext, Future, blocking }
import better.files._
import cats.implicits._
import shapeless.datatype.datastore._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.event.Level
import org.slf4j.event.Level._
import parallelai.sot.api.concurrent.GitExcecutionContext
import parallelai.sot.api.config.executor
import parallelai.sot.api.model.{ GitVersion, _ }
import parallelai.sot.api.gcp.datastore.DatastoreConfig

trait GitMechanic extends StatusMechanic with DatastoreMappableType {
  this: DatastoreConfig =>

  private implicit val ec: ExecutionContext = GitExcecutionContext()

  lazy val gitVersionDAO: ApiDatastore[GitVersion] = datastore[GitVersion]

  val directoryExists: File => Boolean = { directory =>
    directory.exists && directory.isDirectory
  }

  val directoryNotExists: File => Boolean = { directory =>
    !directoryExists(directory)
  }

  protected val credentialsProvider = new UsernamePasswordCredentialsProvider(executor.rule.git.user, executor.rule.git.password)

  protected val gitLocalDirectory: File = executor.rule.git.localPath

  def addVersion(version: String): Future[GitVersion] = blocking {
    val statusLog = logPartialStatus(Some(s"Loading GIT $version"), VERSION)

    for {
      file <- codeFromEngineRepo(version)
      gitVersion <- gitVersionDAO put GitVersion(version, file.pathAsString, isActive = true)
      _ <- statusLog(INFO, s"Git code loaded $gitVersion")
    } yield gitVersion
  }

  def activeVersions: Future[List[GitVersion]] = allVersions.map(_.filter(_.isActive))

  def allVersions: Future[List[GitVersion]] = gitVersionDAO.findAll

  def activateVersion(version: String): Future[GitVersion] = changeVersionActiveStatus(version, active = true)

  def deactivateVersion(version: String): Future[GitVersion] = changeVersionActiveStatus(version, active = false)

  def changeVersionActiveStatus(version: String, active: Boolean): Future[GitVersion] =
    gitVersionDAO findOneById version flatMap {
      case None => Future failed new Exception(s"Version $version not found")
      case Some(gv: GitVersion) => gitVersionDAO update gv.copy(isActive = active)
    }

  def deleteVersion(version: String): Future[File] = for {
    file <- deleteRepoCode(version)
    _ <- gitVersionDAO deleteById version
  } yield file

  def refreshVersion(version: String): Future[GitVersion] = blocking {
    val statusLog = logPartialStatus(Some(s"Reloading GIT $version"), VERSION)

    for {
      deletedDirectory <- deleteVersion(version)
      _ <- statusLog(INFO, s"Git code deleted directory $deletedDirectory for reloading")
      gitVersion <- addVersion(version)
    } yield gitVersion
  }

  protected def deleteRepoCode(version: String) = Future {
    executor.git.localFile(version).delete()
  }

  protected def codeFromRepo: Future[File] = Future {
    val statusLog = logPartialStatus(Some(s"GIT: ${executor.rule.git}"), GIT)

    statusLog(DEBUG, s"Checking if code exists at $gitLocalDirectory")

    if (directoryNotExists(gitLocalDirectory)) {
      statusLog(DEBUG, s"Git code not present at $gitLocalDirectory - Getting it...")
      gitClone(statusLog, gitLocalDirectory)
    } else {
      statusLog(DEBUG, s"Git code already present at $gitLocalDirectory")
    }

    gitLocalDirectory
  }

  protected def gitClone(statusLog: (Level, String) => Future[LogEntry], gitLocalDirectory: File): Future[LogEntry] = {
    val git = Git.cloneRepository()
      .setURI(executor.rule.git.repo.toString)
      .setCredentialsProvider(credentialsProvider)
      .setDirectory(gitLocalDirectory.toJava)
      .call()

    statusLog(INFO, s"Git code $gitLocalDirectory - Clone complete")

    git.clean().call()
    statusLog(DEBUG, s"Git code $gitLocalDirectory - Clean complete")

    git.checkout()
      .setCreateBranch(false)
      .setName(executor.rule.git.branch)
      .call()

    statusLog(INFO, s"Git code $gitLocalDirectory - Checkout complete")
  }

  protected def gitPullCommitAndPush(ruleId: String, version: String): Future[LogEntry] = {
    val statusLog = logPartialStatus(Some(s"GIT: ${executor.rule.git}"), GIT)
    val errorLog = logPartialError(Some(s"GIT: ${executor.rule.git}"), GIT)

    def git: Future[LogEntry] = {
      def apply[R](f: => R, statusMessage: String): Future[(R, LogEntry)] = {
        val result = f
        statusLog(DEBUG, s"Git code ${executor.rule.git.localPath} - $statusMessage").map(logEntry => (result, logEntry))
      }

      for {
        (git, _) <- apply(Git.open(File(executor.rule.git.localPath).toJava), s"Git code ${executor.rule.git.localPath} - Repository setup")
        _ <- apply(git.pull().setCredentialsProvider(credentialsProvider).setRebase(true).call(), "Pull with Rebase complete")
        _ <- apply(git.add().addFilepattern(ruleId).call(), "Add all complete")
        _ <- apply(git.commit().setAll(true).setMessage(s"Added Rule $ruleId").call(), "Commit complete")
        _ <- apply(git.tag().setName(ruleId).call(), s"Rule $ruleId complete")
        _ <- apply(git.push().setCredentialsProvider(credentialsProvider).setPushAll().setPushTags().call(), "Push complete")
        successLogEntry <- handleRuleSuccess(ruleId, GIT_DONE, s"Successfully executed pull, add, commit, version and push for rule $ruleId")(statusLog)
      } yield successLogEntry
    }

    if (directoryExists(executor.rule.git.localPath)) for {
      _ <- statusLog(DEBUG, s"Git code found at ${executor.rule.git.localPath} - Proceeding")
      _ <- changeStatus(ruleId, GIT_START)
      gitLogEntry <- git
    } yield gitLogEntry
    else {
      val exception = new Exception(s"Git Folder: ${executor.rule.git.localPath} does not exist - No repository found")

      handleRuleFailure(ruleId, GIT_FAILED, exception)(statusLog)(errorLog).flatMap { _ =>
        Future failed exception // TODO - Nonsense but calling code needs this to fail - a design flaw.
      }
    }
  }

  // TODO - Benchmark as this may have to be marked up as "blocking"
  protected def codeFromEngineRepo(version: String): Future[File] = Future {
    val gitLocalDirectory: File = executor.git.localFile(version)

    val statusLog = logPartialStatus(Some(s"TAG: $version"), VERSION)
    statusLog(DEBUG, s"Checking if code exists at $gitLocalDirectory for version $version")

    if (directoryNotExists(gitLocalDirectory)) {
      statusLog(DEBUG, s"Git code not present at $gitLocalDirectory for version $version - Getting it...")

      val git = Git.cloneRepository()
        .setURI(executor.git.repo.toString)
        .setCredentialsProvider(credentialsProvider)
        .setDirectory(gitLocalDirectory.toJava)
        .call()

      statusLog(INFO, s"Git code $gitLocalDirectory - Clone complete for version $version")

      git.clean().call()
      statusLog(DEBUG, s"Git code $gitLocalDirectory - Clean complete for version $version")

      git.checkout()
        .setCreateBranch(false)
        .setName(version)
        .call()

      statusLog(INFO, s"Git code $gitLocalDirectory - Checkout complete for version $version")
    } else {
      statusLog(DEBUG, s"Git code already present at $gitLocalDirectory")
    }

    gitLocalDirectory
  }

  protected def copyRepositoryCode(ruleId: String, version: String): Future[File] = {
    val gitLocalDirectory: File = executor.git.localFile(version)
    val ruleDirectory: File = executor.rule.localFile(ruleId)

    if (directoryNotExists(gitLocalDirectory)) {
      val errorMessage = s"Cannot copy repository code for required version $version - following directory does not exist: $gitLocalDirectory"

      changeStatus(ruleId, GIT_FAILED, statusDescription = Option(errorMessage)).flatMap { _ =>
        logPartialError(Some(s"GIT: $errorMessage}"), GIT)(ERROR, new NoSuchFileException(errorMessage)).failed.mapTo[Nothing]
      }
    } else for {
      ruleDirectoryExists <- ruleDirectory.parent.exists.pure[Future]
      _ <- if (ruleDirectoryExists) ruleDirectory.parent.pure[Future] else codeFromRepo
    } yield {
      val statusLog = logPartialStatus(Some(s"VERSION FOLDER: $gitLocalDirectory::RULE FOLDER: $ruleDirectory"), VERSION)
      statusLog(DEBUG, s"Copy Git contents to Rule Folder at $ruleDirectory")

      if (ruleDirectory.exists) {
        statusLog(ERROR, "Rule folder exists - Deleting all contents")
        ruleDirectory clear
      } else {
        ruleDirectory.createDirectories
      }

      gitLocalDirectory copyToDirectory ruleDirectory
      (ruleDirectory / ".git") delete true
      (ruleDirectory / ".git*") delete true
      statusLog(DEBUG, s"Copy successful $gitLocalDirectory -> $ruleDirectory")

      ruleDirectory / version
    }
  }
}