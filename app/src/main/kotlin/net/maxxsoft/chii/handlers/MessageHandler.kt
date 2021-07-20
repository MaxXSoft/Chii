package net.maxxsoft.chii.handlers

import kotlinx.coroutines.channels.Channel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.maxxsoft.chii.utils.Config
import org.slf4j.LoggerFactory

abstract class MessageHandler(val id: String, val description: String) {
  companion object {
    // logger
    @JvmStatic
    @Suppress("JAVA_CLASS_ON_COMPANION")
    private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

    // channel of logger
    private val loggerChannel = Channel<String>()

    /**
     * Handle the group message event.
     *
     * @param event group message event.
     */
    suspend fun handle(event: GroupMessageEvent) {
      var resetFlag = false
      Config.enabledHandlers.forEach {
        if (!resetFlag) {
          if (it.handle(event)) resetFlag = true
        } else {
          it.reset()
        }
      }
    }

    /** Poll message from logger. */
    @Suppress("EXPERIMENTAL_API_USAGE")
    suspend fun pollLogger() {
      while (!loggerChannel.isClosedForReceive) {
        logger.info(loggerChannel.receive())
      }
    }

    /** Close the channel for logger. */
    fun closeLoggerChannel() {
      loggerChannel.close()
    }

    /**
     * Get help message of all message handlers.
     *
     * @return help message (`String`)
     */
    fun getHelpMessage(linePrefix: String = "") =
        Config.enabledHandlers.joinToString(separator = "\n") {
          "$linePrefix${it.id}: ${it.description}"
        }
  }

  /**
   * Log some information.
   *
   * @param message message of log.
   */
  protected suspend fun log(message: String) {
    loggerChannel.send("$id: $message")
  }

  /**
   * Handle group message event.
   *
   * @param event group message event.
   * @return returns `true` if the message has already been handled
   */
  abstract suspend fun handle(event: GroupMessageEvent): Boolean

  /** Reset internal state. */
  open fun reset() {}
}
