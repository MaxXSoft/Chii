package net.maxxsoft.chii.handlers

import kotlin.reflect.full.isSubclassOf
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.slf4j.LoggerFactory

sealed class MessageHandler(val id: String) {
  companion object {
    // logger
    @JvmStatic
    @Suppress("JAVA_CLASS_ON_COMPANION")
    private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

    // all instances of `MessageHandler`
    private val INSTANCES =
        MessageHandler::class
            .sealedSubclasses
            .filter { it.isSubclassOf(MessageHandler::class) }
            .map { it.objectInstance }
            .filterIsInstance<MessageHandler>()
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
  }

  /**
   * Log some information.
   *
   * @param message message of log.
   */
  protected fun log(message: String) {
    logger.info("$id: $message")
  }

  /**
   * Handle group message event.
   *
   * @param event group message event.
   */
  abstract suspend fun handle(event: GroupMessageEvent)
}
