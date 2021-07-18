package net.maxxsoft.chii.handlers

import kotlinx.coroutines.channels.Channel
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.slf4j.LoggerFactory

abstract class MessageHandler(val id: String) {
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
            RandomRepeatHandler,
        )
            .associateBy { it.id }

    /**
     * Handle by using all instances.
     *
     * @param event group message event.
     */
    suspend fun handleAll(event: GroupMessageEvent) {
      INSTANCES.forEach { (_, v) -> v.handle(event) }
    }

    /**
     * Handle by using some specific instances.
     *
     * @param event group message event.
     */
    suspend fun handleSome(event: GroupMessageEvent, ids: Array<String>) {
      val idSet = ids.toSet()
      INSTANCES.forEach { (k, v) -> if (k in idSet) v.handle(event) }
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
   */
  abstract suspend fun handle(event: GroupMessageEvent)
}
