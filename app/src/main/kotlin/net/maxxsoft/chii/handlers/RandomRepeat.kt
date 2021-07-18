package net.maxxsoft.chii.handlers

import kotlin.random.Random
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.PlainText
import org.ansj.splitWord.analysis.ToAnalysis

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
    val texts = event.message.filterIsInstance<PlainText>().filter { !it.content.trim().isEmpty() }
    if (texts.size > 1) return
    val msg = texts.first().contentToString()
    // check if should repeat
    if (checkProb(PROB_REPEAT)) {
      // check if shoud deteriorate
      if (checkProb(PROB_DETER)) {
        val words = ToAnalysis.parse(msg).map { Pair(it.name, it.natureStr) }
        val nounsCount = words.filter { it.second in NOUNS }.size
        val newWords = if (nounsCount < NOUNS_THRESH) chaos(words) else chaos(words, NOUNS)
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
