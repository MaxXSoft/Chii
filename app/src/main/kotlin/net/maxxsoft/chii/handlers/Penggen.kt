package net.maxxsoft.chii.handlers

import kotlin.random.Random
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.findIsInstance
import net.maxxsoft.chii.utils.Config
import net.maxxsoft.chii.utils.PenggenData

/** To be a Penggen (捧哏) like Yu Qian (于谦). */
object PenggenHandler :
    MessageHandler(
        "penggen",
        "半自动捧哏，全人工智障，at Chii 并说话时发动。没有任何AI的成分，纯靠统计和概率",
    ) {
  private val DATA =
      Json.decodeFromString<List<PenggenData>>(
          this::class.java.getResource("/penggen.json").readText()
      )

  /** Probability of ignoring the item's probability. */
  private val PROB_IGNORE = 0.5

  override suspend fun handle(event: GroupMessageEvent): Boolean {
    // check if there is someone ated the bot
    val atMessage = event.message.findIsInstance<At>()
    if (atMessage == null || atMessage.contentToString().indexOf("@${Config.account}") != 0) {
      return false
    }
    // select a random Penggen text and reply
    event.subject.sendMessage(event.message.quote() + getPenggenText())
    return true
  }

  private fun getPenggenText(): String {
    val data = pickFromRandomly(DATA) { it.prob }
    val txtObj = pickFromRandomly(data.txts) { it.prob }
    return txtObj.txt.random()
  }

  /**
   * Pick item from list by its probability.
   *
   * @param list the list.
   * @param pred the predicate for getting item's probability.
   * @return an item from list.
   */
  private inline fun <T> pickFrom(list: List<T>, pred: (T) -> Double): T {
    val prob = Random.nextDouble(0.0, 1.0)
    var curProb = 0.0
    for (i in list) {
      curProb += pred(i)
      if (prob <= curProb) return i
    }
    return list.random()
  }

  /**
   * Pick item from list, may ignore item's probability.
   *
   * @param list the list.
   * @param pred the predicate for getting item's probability.
   * @return an item from list.
   */
  private inline fun <T> pickFromRandomly(list: List<T>, pred: (T) -> Double): T =
      if (Random.nextDouble(0.0, 1.0) < PROB_IGNORE) pickFrom(list, pred) else list.random()
}
