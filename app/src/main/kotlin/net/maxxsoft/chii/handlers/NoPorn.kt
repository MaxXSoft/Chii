package net.maxxsoft.chii.handlers

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import java.io.InputStream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.findIsInstance
import net.maxxsoft.chii.utils.Config
import net.maxxsoft.chii.utils.LoliconResponse

/** No pornography, but allowed to send SeTu. */
object NoPornHandler :
    MessageHandler(
        "no-porn",
        "戒色小助手，帮你记录戒色点滴",
    ) {
  override suspend fun handle(event: GroupMessageEvent): Boolean {
    // check if there is someone ated the bot
    val atMessage = event.message.findIsInstance<At>()
    if (atMessage == null || atMessage.contentToString().indexOf("@${Config.account}") != 0) {
      return false
    }
    // try find '戒色' in the current message
    val command = event.message.findIsInstance<PlainText>()?.content?.trim()
    if (command == null || "戒色" !in command) return false
    // fetch setu from lolicon API
    runCatching { event.subject.sendImage(getRandom()!!) }.onFailure {
      event.subject.sendMessage("本来想发图的但没发出去...")
    }
    return true
  }

  suspend fun getRandom() =
      HttpClient().use {
        runCatching {
              val resp = it.get<String>("https://api.lolicon.app/setu/v2?r18=0&num=1&size=regular")
              val obj = Json.decodeFromString<LoliconResponse>(resp)
              it.get<InputStream>(obj.data?.first()?.urls?.regular!!)
            }
            .getOrNull()
      }
}
