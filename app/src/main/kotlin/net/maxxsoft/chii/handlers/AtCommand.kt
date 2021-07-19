package net.maxxsoft.chii.handlers

import kotlin.text.trim
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.findIsInstance
import net.maxxsoft.chii.utils.Config

/** At-command parser. */
object AtCommandHandler :
    MessageHandler(
        "at-command",
        "处理斜杠命令。你可以 at Chii，然后发送斜杠开头的命令。输入“/help”获取帮助",
    ) {

  /** Information of command. */
  private data class CommandInfo(
      val abbr: String,
      val help: String,
      val privileged: Boolean,
      val handler: suspend (GroupMessageEvent, List<String>) -> Unit,
  )

  // all command line handlers
  private val commandHandlers =
      mapOf(
          "help" to CommandInfo("/help", "查看帮助信息", false, ::handleHelp),
          "reload" to CommandInfo("/reload", "重新载入设置", true, ::handleReload),
      )

  override suspend fun handle(event: GroupMessageEvent): Boolean {
    // check if there is someone ated the bot
    val atMessage = event.message.findIsInstance<At>()
    if (atMessage == null || atMessage.contentToString().indexOf("@${Config.account}") != 0) {
      return false
    }
    // try to parse the command line
    val command = event.message.findIsInstance<PlainText>()?.content?.trim()
    if (command == null || !command.startsWith("/")) return false
    // call command handler
    val args = command.split("\\s+")
    val cmd = commandHandlers[args.first().removePrefix("/")]
    if (cmd == null) {
      event.subject.sendMessage(event.message.quote() + "命令“${args.first()}”无效，输入“/help”查看帮助")
    } else if (cmd.privileged && event.sender.id != Config.masterId) {
      event.subject.sendMessage(event.message.quote() + "你不是我的主人，无权执行这条指令，爪巴😅（")
    } else {
      cmd.handler(event, args.takeLast(args.size - 1))
    }
    return true
  }

  @Suppress("UNUSED_PARAMETER")
  private suspend fun handleHelp(event: GroupMessageEvent, args: List<String>) {
    val master = if (event.sender.id == Config.masterId) "恭迎我至高无上的主人${event.senderName}！\n" else ""
    val status = "已运行${Config.getRunningTime()}"
    val linePrefix = "  🔘"
    val commandHelp =
        commandHandlers
            .map { (_, v) -> "$linePrefix${v.abbr}: ${v.help}" }
            .joinToString(separator = "\n")
    val msgHelp = MessageHandler.getHelpMessage(Config.enabledHandlers, linePrefix)
    val msg = "${master}${status}\n\n🚩命令说明: \n$commandHelp\n\n💬已启用的消息处理器: \n$msgHelp"
    event.subject.sendMessage(event.message.quote() + msg)
  }

  @Suppress("UNUSED_PARAMETER")
  private suspend fun handleReload(event: GroupMessageEvent, args: List<String>) {
    Config.reload()
    event.subject.sendMessage(event.message.quote() + "设置已重新载入")
  }
}
