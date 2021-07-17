package net.maxxsoft.chii.handlers

import kotlin.random.Random
import kotlin.reflect.full.isSubclassOf
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.PlainText
import org.ansj.splitWord.analysis.ToAnalysis
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

/** Random repetition of group members' messages. */
object RandomRepeatHandler : MessageHandler("random-repeat") {
  // probability of repetition
  private val PROB_REPEAT = 0.1
  // probability of deterioration
  private val PROB_DETER = 0.5
  // Nouns.
  private val NOUNS = setOf("n", "l", "i", "nr", "ns", "nt", "nx", "nz", "en")
  // Threshold of nouns count.
  private val NOUNS_THRESH = 4

  override suspend fun handle(event: GroupMessageEvent) {
    // repeat plain text message only
    if (event.message.filterIsInstance<PlainText>().size + 1 != event.message.size) return
    val msg = event.message.contentToString()
    // check if should repeat
    if (checkProb(PROB_REPEAT)) {
      // check if shoud deteriorate
      if (checkProb(PROB_DETER)) {
        val words = ToAnalysis.parse(msg).map { Pair(it.name, it.natureStr) }
        val newWords =
            if (words.filter { it.second in NOUNS }.size < NOUNS_THRESH) {
              chaos(words)
            } else {
              chaos(words, NOUNS)
            }
        val newMsg = newWords.map { it.first }.joinToString(separator = "")
        event.subject.sendMessage(newMsg)
      } else {
        // just repeat
        event.subject.sendMessage(msg)
      }
    }
  }

  /**
   * Roll the dice.
   *
   * @param prob probability.
   */
  private fun checkProb(prob: Double): Boolean {
    val p = Random.nextDouble(0.0, 1.0)
    log("$p")
    return p < prob
  }

  /**
   * Shuffle word list by nature of word.
   *
   * @param words list of (word, nature).
   * @param natureSet only shuffle words with natures in `natureSet`.
   */
  private fun chaos(
      words: List<Pair<String, String>>,
      natureSet: Set<String> =
          setOf("v", "n", "l", "i", "ng", "nr", "ns", "nt", "nx", "nz", "en", "vg", "vd", "vn")
  ) =
      words
          .mapIndexed { i, v -> if (v.second in natureSet) i else -1 }
          .filter { it >= 0 }
          .zip(words.filter { it.second in natureSet }.shuffled())
          .toMap()
          .let { m -> words.mapIndexed { i, v -> m[i] ?: v } }
}
