package net.maxxsoft.chii.handlers

import kotlinx.coroutines.channels.Channel
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.slf4j.LoggerFactory

abstract class MessageHandler(val id: String, val description: String) {
  companion object {
    // logger
    @JvmStatic
    @Suppress("JAVA_CLASS_ON_COMPANION")
    private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

    // Channel of logger.
    private val loggerChannel = Channel<String>()

    // all instances of `MessageHandler`
    private val INSTANCES =
        listOf(
            AtCommandHandler,
            NoPornHandler,
            PenggenHandler,
            RandomRepeatHandler,
        )
            .map { Pair(it.id, it) }

    /**
     * Handle by using all instances.
     *
     * @param event group message event.
     */
    suspend fun handleAll(event: GroupMessageEvent) {
      var resetFlag = false
      INSTANCES.forEach { (_, v) ->
        if (!resetFlag) {
          if (v.handle(event)) resetFlag = true
        } else {
          v.reset()
        }
      }
    }

    /**
     * Handle by using some specific instances.
     *
     * @param event group message event.
     */
    suspend fun handleSome(event: GroupMessageEvent, ids: Set<String>) {
      var resetFlag = false
      INSTANCES.forEach { (k, v) ->
        if (!resetFlag) {
          if (k in ids && v.handle(event)) resetFlag = true
        } else {
          v.reset()
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
    fun getHelpMessage(enabledHandlers: Set<String>, linePrefix: String = "") =
        INSTANCES.filter { (_, v) -> v.id in enabledHandlers }.joinToString(separator = "\n") {
            (_, v) ->
          "$linePrefix${v.id}: ${v.description}"
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
