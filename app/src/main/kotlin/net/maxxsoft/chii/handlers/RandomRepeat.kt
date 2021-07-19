package net.maxxsoft.chii.handlers

import kotlin.random.Random
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.PlainText
import org.ansj.splitWord.analysis.ToAnalysis

/** Random repetition of group members' messages. */
object RandomRepeatHandler :
    MessageHandler(
        "random-repeat",
        "全自动劣质复读机，5%概率自动复读，100%被动复读。自动复读时劣化概率50%，被动时80%",
    ) {
  // probability of repetition
  private val PROB_REPEAT = 0.05
  // probability of deterioration (self-repeating)
  private val PROB_DETER = 0.5
  // probability of deterioration (follow)
  private val PROB_DETER_FOLLOW = 0.8
  // Nouns.
  private val NOUNS = setOf("n", "l", "i", "nr", "ns", "nt", "nx", "nz", "en")
  // Verbs.
  private val VERBS = setOf("v", "vg", "vd", "vn")
  // Nouns count threshold of nouns-only mode.
  private val NOUNS_THRESH = 4

  // Last message.
  private var lastMessage = HashMap<Long, String>()

  override suspend fun handle(event: GroupMessageEvent): Boolean {
    // repeat plain text message only
    val texts = event.message.filterIsInstance<PlainText>().filter { !it.content.trim().isEmpty() }
    if (texts.isEmpty() || texts.size > 1) return false
    val msg = texts.first().contentToString()
    // check if should repeat
    if (checkLastMsg(event.group.id, msg)) {
      log("followed by other members")
      event.subject.sendMessage(if (checkProb(PROB_DETER_FOLLOW)) chaosString(msg) else msg)
      return true
    } else if (checkProb(PROB_REPEAT)) {
      log("self-repeating")
      event.subject.sendMessage(if (checkProb(PROB_DETER)) chaosString(msg) else msg)
      return true
    }
    return false
  }

  override fun reset() {
    lastMessage.clear()
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
  private fun chaos(words: List<Pair<String, String>>, natureSet: Set<String>) =
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
    val nounsOnly = chaos(words, NOUNS)
    val newWords = if (nounsCount < NOUNS_THRESH) chaos(nounsOnly, VERBS) else nounsOnly
    return newWords.map { it.first }.joinToString(separator = "")
  }

  /**
   * Check if the current message is same as the last one, and update `lastMessage`.
   *
   * @param message the current message.
   * @return `true` if same.
   */
  private fun checkLastMsg(groupId: Long, message: String): Boolean {
    if (lastMessage[groupId]?.let { message == it } ?: false) {
      // prevent further repetition
      lastMessage.remove(groupId)
      return true
    } else {
      lastMessage[groupId] = message
      return false
    }
  }
}
