package net.maxxsoft.chii.handlers

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.at
import net.maxxsoft.chii.utils.Config

/**
 * Muting other group members just like gaming.
 *
 * Reference: https://www.zhihu.com/question/41782502/answer/153323172
 */
object MuteGameHandler :
    MessageHandler(
        "mute-game",
        "禁言游戏，带技能系统，“/rule”查看规则和玩法",
    ) {
  /**
   * Id of all muted group members.
   *
   * (groupId, memberId) -> (muteSecs, muteTime)
   */
  private val mutedIds = HashMap<Pair<Long, Long>, Pair<Int, LocalDateTime>>()

  /** Result of method `muteMember` */
  private enum class MuteResult {
    SUCCEEDED, // operation succeeded
    MUTED, //     member has already been muted
    FAILED, //    failed (argument illegal, etc.)
  }

  /** Mute command. */
  private data class MuteCommand(
      val member: Member,
      val keyword: String,
      val durationMins: Int,
  )

  override suspend fun handle(event: GroupMessageEvent): Boolean {
    // try to parse mute command
    val command = parseMuteCommand(event.group, event.message)
    if (command == null) return false
    // check if the sender has already been muted
    val mutedSecs = checkMuted(event.sender)
    if (mutedSecs != null) {
      sendMuteMsg(event.sender, mutedSecs)
      return true
    }
    // check if the target member is bot
    if (command.member.id == Config.account) {
      if (muteMember(event.sender, command.durationMins) == MuteResult.SUCCEEDED) {
        event.subject.sendMessage(event.message.quote() + "谁给谁${command.keyword}上？😅")
      } else {
        event.subject.sendMessage(event.message.quote() + "爬😅")
      }
      return true
    }
    // roll a dice
    val prob = 55 - command.durationMins * 5
    val dice = Random.nextInt(0, 10000)
    val succ = dice >= (100 - prob) * 100
    val member = if (succ) command.member else event.sender
    // mute the specific member
    when (muteMember(member, command.durationMins)) {
      MuteResult.SUCCEEDED -> {
        val probStr = "$prob%"
        val diceFrac = (dice % 100).toString().padStart(4, '0')
        val diceStr = "${dice / 100}.$diceFrac"
        val result = if (succ) "${command.keyword}上了" else "你自己${command.keyword}上吧"
        event.subject.sendMessage(
            event.sender.at() +
                "试图${command.keyword}" +
                command.member.at() +
                "${command.durationMins}分钟，成功率${probStr}，roll出${diceStr}，${result}"
        )
      }
      MuteResult.MUTED -> {
        event.subject.sendMessage(event.message.quote() + "别鞭尸了，丫禁着呢")
      }
      MuteResult.FAILED -> event.subject.sendMessage(event.message.quote() + "禁言操作失败😨")
    }
    return true
  }

  /** Parse mute command (with parameter checking). */
  private suspend fun parseMuteCommand(group: Group, message: MessageChain): MuteCommand? {
    // state, 0 -> idle, 1 -> get member, 2 -> get the rest message
    var state = 0
    var member: Member? = null
    var keyword: String? = null
    var durationMins = 1
    // iterate through message chain
    for (msg in message) {
      state =
          when (state) {
            0 -> if (msg.contentToString().trim() == "给") 1 else 0
            1 -> {
              // try to get member
              member = if (msg is At) group.get(msg.target) ?: return null else return null
              2
            }
            else -> {
              // get keyword
              val txt = msg.contentToString().trim()
              val index = txt.indexOf("上")
              if (index <= 0) return null
              keyword = txt.substring(0, index).trim()
              // try to get duration
              val rest = txt.substring(index + 1).trim()
              val indexMin = rest.indexOf("分钟")
              if (indexMin > 0) {
                durationMins = parseInt(rest.substring(0, indexMin).trim()) ?: return null
                if (durationMins < 1 || durationMins > 10) return null
              } else if (!rest.isEmpty()) {
                return null
              }
              break
            }
          }
    }
    if (member == null || keyword == null) return null
    return MuteCommand(member, keyword, durationMins)
  }

  /**
   * Check if the specific member has already been muted by bot.
   *
   * @return remaining time in seconds if the member has already been muted, otherwise `null`.
   */
  private fun checkMuted(member: Member): Int? {
    // anonymous member can not be muted
    if (member !is NormalMember) return null
    // member has already been muted, get remaining time in seconds
    if (member.isMuted) return member.muteTimeRemaining
    // return if member is not in the map
    val info = mutedIds[Pair(member.group.id, member.id)]
    if (info == null) return null
    // check if regject time exceeded
    val lastingSecs = ChronoUnit.SECONDS.between(info.second, LocalDateTime.now())
    if (lastingSecs >= info.first) {
      mutedIds.remove(Pair(member.group.id, member.id))
      return null
    }
    // member are still be muted
    return info.first - lastingSecs.toInt()
  }

  /**
   * Mute the specific member.
   *
   * @param durationMins duration in minutes, up to 10 minutes.
   * @return `MuteResult`.
   */
  private suspend fun muteMember(member: Member, durationMins: Int): MuteResult {
    // check if duration is illegal
    if (durationMins < 0 || durationMins > 10) return MuteResult.FAILED
    // anonymous member can not be muted
    if (member !is NormalMember) return MuteResult.FAILED
    // check if member has already been muted
    if (member.isMuted) return MuteResult.MUTED
    // try to mute the current member
    val durationSecs = durationMins * 60
    try {
      member.mute(durationSecs)
    } catch (_: PermissionDeniedException) {
      // permission denied, try to mute manually
      val curPair = Pair(member.group.id, member.id)
      val curInfo = Pair(durationSecs, LocalDateTime.now())
      val prev = mutedIds[curPair]
      // check if the mute record not found
      if (prev == null) {
        mutedIds[curPair] = curInfo
        return MuteResult.SUCCEEDED
      }
      // mute record found, get lasting time in seconds
      val lastingSecs = ChronoUnit.SECONDS.between(prev.second, curInfo.second)
      // check if time exceeded
      if (lastingSecs >= prev.first) {
        mutedIds[curPair] = curInfo
        return MuteResult.SUCCEEDED
      }
      // the member has still been muted
      return MuteResult.MUTED
    } catch (e: Exception) {
      // failed
      log("failed to mute, ${e.message}")
      log("${e.stackTraceToString()}")
      return MuteResult.FAILED
    }
    // successfully muted
    return MuteResult.SUCCEEDED
  }

  /** Send the message "`member` has already been muted" */
  private suspend fun sendMuteMsg(member: Member, remainingSecs: Int) {
    val remainingMsg = if (remainingSecs > 60) "${remainingSecs / 60}分钟" else "${remainingSecs}秒"
    member.group.sendMessage(member.at() + "你已被👴禁言（剩余$remainingMsg）")
  }

  /** Parse string to integer (supports chinese characters). */
  private suspend fun parseInt(str: String): Int? {
    val CHAR_MAP = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十").zip(1..10).toMap()
    return str.toIntOrNull() ?: CHAR_MAP[str]
  }

  /** Get help message for the game rule. */
  fun getRuleMessage() =
      """
        🚭禁言游戏，版本 v0.1。
        💡目前只支持最基础的欧气测试模式，即：你可以禁言别人n(1-10)分钟，但该操作成功率为(55-5n)%，失败时会禁言自己。
        🆚使用“给 @某人 xxx上[n分钟]”来禁言某人，例如“给 @wgm 禁上”会试图禁言“wgm”1分钟，“给 @wgm 来上5分钟”会试图禁言“wgm”5分钟。
      """.trimIndent()
}
