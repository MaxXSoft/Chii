package net.maxxsoft.chii.handlers

import kotlin.random.Random
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.PlainText
import org.ansj.splitWord.analysis.ToAnalysis

/** Random repetition of group members' messages. */
object RandomRepeatHandler :
    MessageHandler(
        "random-repeat",
        "全自动劣质复读机",
    ) {
  // probability of repetition
  private val PROB_REPEAT = 0.1
  // probability of deterioration
  private val PROB_DETER = 0.5
  // Nouns.
  private val NOUNS = setOf("n", "l", "i", "nr", "ns", "nt", "nx", "nz", "en")
  // Threshold of nouns count.
  private val NOUNS_THRESH = 4

  // Last message.
  private var lastMessage = ""

  override suspend fun handle(event: GroupMessageEvent): Boolean {
    // repeat plain text message only
    val texts = event.message.filterIsInstance<PlainText>().filter { !it.content.trim().isEmpty() }
    if (texts.size > 1) return false
    val msg = texts.first().contentToString()
    // check if should repeat
    if (checkLastMsg(msg)) {
      log("followed by other members")
      // always deteriorate if others are also repeating
      event.subject.sendMessage(chaosString(msg))
      return true
    } else if (checkProb(PROB_REPEAT)) {
      log("self-repeating")
      // check if shoud deteriorate
      if (checkProb(PROB_DETER)) {
        event.subject.sendMessage(chaosString(msg))
      } else {
        // just repeat
        event.subject.sendMessage(msg)
      }
      return true
    }
    return false
  }

  /**
   * Roll the dice.
   *
   * @param prob probability.
   */
  private fun checkProb(prob: Double) = Random.nextDouble(0.0, 1.0) < prob

  /**
   * Shuffle word list by nature of word.
   *
   * @param words list of (word, nature).
   * @param natureSet only shuffle words with natures in `natureSet`.
   * @return a new word list.
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

  /**
   * Perform `chaos` operation on a specific string.
   *
   * @param message input string.
   * @return a processed string
   */
  private fun chaosString(message: String): String {
    val words = ToAnalysis.parse(message).map { Pair(it.name, it.natureStr) }
    val nounsCount = words.filter { it.second in NOUNS }.size
    val newWords = if (nounsCount < NOUNS_THRESH) chaos(words) else chaos(words, NOUNS)
    return newWords.map { it.first }.joinToString(separator = "")
  }

  /**
   * Check if the current message is same as the last one, and update `lastMessage`.
   *
   * @param message the current message.
   * @return `true` if same.
   */
  private fun checkLastMsg(message: String): Boolean {
    if (message == lastMessage) {
      // prevent further repetition
      lastMessage = ""
      return true
    } else {
      lastMessage = message
      return false
    }
  }
}
