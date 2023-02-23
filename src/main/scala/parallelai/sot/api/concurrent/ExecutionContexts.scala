package parallelai.sot.api.concurrent

import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ Executors, ForkJoinPool, ThreadFactory }
import scala.concurrent.ExecutionContext
import grizzled.slf4j.Logging

object ExecutionContexts {
  implicit val webServiceExecutionContext: WebServiceExecutionContext = WebServiceExecutionContext()
}

sealed trait CustomExecutionContext extends ExecutionContext with Logging {
  def threadPoolName: String

  def ec: ExecutionContext

  def threadFactory: ThreadFactory = new ThreadFactory {
    private val counter = new AtomicLong(0L)

    def newThread(r: Runnable): Thread = new Thread(r) {
      setName(s"$threadPoolName-${counter.getAndIncrement}")
      setDaemon(true)

      override def run(): Unit = {
        trace(Thread.currentThread)
        r.run()
      }
    }
  }

  def execute(runnable: Runnable): Unit =
    ec execute runnable

  def reportFailure(cause: Throwable): Unit =
    ec reportFailure cause
}

class FixedSizeExecutionContext(val threadPoolName: String, threadPoolSize: Int = Runtime.getRuntime.availableProcessors) extends CustomExecutionContext {
  val ec: ExecutionContext = ExecutionContext fromExecutorService Executors.newFixedThreadPool(threadPoolSize, threadFactory)
}

class ForkJoinExecutionContext(val threadPoolName: String, threadPoolSize: Int = Runtime.getRuntime.availableProcessors) extends CustomExecutionContext {
  private val forkPoolThreadFactory: ForkJoinWorkerThreadFactory = (forkJoinPool: ForkJoinPool) => {
    val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(forkJoinPool)
    worker.setName(s"$threadPoolName-${worker.getPoolIndex}")
    worker
  }

  val ec: ExecutionContext = ExecutionContext fromExecutorService new ForkJoinPool(threadPoolSize, forkPoolThreadFactory, Thread.getDefaultUncaughtExceptionHandler, false)
}

object DatastoreExcecutionContext {
  private val ec = new FixedSizeExecutionContext("datastore-thread")

  def apply(): FixedSizeExecutionContext = ec
}

object GitExcecutionContext {
  private val ec = new FixedSizeExecutionContext("git-thread")

  def apply(): FixedSizeExecutionContext = ec
}

object SbtExcecutionContext {
  // private val ec = new ForkJoinExecutionContext("sbt-thread") // TODO
  private val ec = new FixedSizeExecutionContext("sbt-thread")

  def apply(): FixedSizeExecutionContext = ec
}

object FileExcecutionContext {
  private val ec = new FixedSizeExecutionContext("file-thread")

  def apply(): FixedSizeExecutionContext = ec
}

sealed abstract class WebServiceExecutionContext extends FixedSizeExecutionContext("web-service-thread")

object WebServiceExecutionContext {
  private val ec = new WebServiceExecutionContext {}

  def apply(): WebServiceExecutionContext = ec
}