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
  // all command line handlers
  private val commandHandlers:
      Map<String, Triple<String, String, suspend (GroupMessageEvent, List<String>) -> Unit>> =
      mapOf(
          "help" to Triple("/help", "查看帮助信息", ::handleHelp),
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
    if (commandHandlers[args.first().removePrefix("/")]?.third?.invoke(
            event,
            args.takeLast(args.size - 1)
        ) == null
    ) {
      event.subject.sendMessage(event.message.quote() + "命令“${args.first()}”无效，输入“/help”查看帮助")
    }
    return true
  }

  @Suppress("UNUSED_PARAMETER")
  private suspend fun handleHelp(event: GroupMessageEvent, args: List<String>) {
    val master = if (event.sender.id == Config.masterId) "恭迎我至高无上的主人${event.senderName}！\n" else ""
    val commandHelp =
        commandHandlers.map { (_, v) -> "${v.first}: ${v.second}" }.joinToString(separator = "\n")
    val msg = "${master}🚩命令说明: \n$commandHelp\n\n💬已启用的消息处理器: \n${MessageHandler.getHelpMessage()}"
    event.subject.sendMessage(event.message.quote() + msg)
  }
}
