import akka.actor.{Actor, ActorSystem, Props}
import org.apache.logging.log4j.LogManager

// Message classes
case class LogWarn(message: String)
case class LogInfo(message: String)
case class RenameFile(newName: String)

// Logger actor
class LoggerActor extends Actor {
  private val warnLogger = LogManager.getLogger("warnAppender")
  private val infoLogger = LogManager.getLogger("infoAppender")

  override def receive: Receive = {
    case LogWarn(message) =>
      warnLogger.warn(message)

    case LogInfo(message) =>
      infoLogger.info(message)

    case RenameFile(newName) =>
      // Rename log files
      renameLogFile(warnLogger, newName + "_warn.log")
      renameLogFile(infoLogger, newName + "_info.log")
  }

  private def renameLogFile(logger: org.apache.logging.log4j.Logger, newFileName: String): Unit = {
    logger.getAppenders.forEach { appender =>
      appender match {
        case rollingAppender: RollingFileAppender =>
          val triggeringPolicy = rollingAppender.getTriggeringPolicy.asInstanceOf[SizeBasedTriggeringPolicy]
          triggeringPolicy.initialize(rollingAppender)
          triggeringPolicy.rollover()

          val rolloverStrategy = rollingAppender.getManager.asInstanceOf[DefaultRolloverStrategy]
          rolloverStrategy.rollover(rollingAppender)

          rollingAppender.setFileName(newFileName)
          rollingAppender.getManager.updateData(rollingAppender)

        case _ =>
        // Handle other types of appenders if needed
      }
    }
  }
}


object Application extends App {
  // Create an actor system
  val system = ActorSystem("LoggerSystem")

  // Create the logger actor
  val loggerActor = system.actorOf(Props[LoggerActor], "loggerActor")

  // Usage example
  loggerActor ! LogWarn("This is a warning message")
  loggerActor ! LogInfo("This is an info message")

  //loggerActor ! RenameFile("appender1")

  loggerActor ! LogWarn("New warning message after renaming")
  loggerActor ! LogInfo("New info message after renaming")
}
