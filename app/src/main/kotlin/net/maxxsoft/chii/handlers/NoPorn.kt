package net.maxxsoft.chii.handlers

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import java.io.InputStream
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.findIsInstance
import net.maxxsoft.chii.utils.Config
import net.maxxsoft.chii.utils.Database
import net.maxxsoft.chii.utils.LoliconResponse

/** No pornography, but allowed to send SeTu. */
object NoPornHandler :
    MessageHandler(
        "no-porn",
        "戒色小助手，帮你记录戒色点滴。打卡请 at Chii 然后说点带“戒色”的话",
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
    // check in
    if (!checkIn(event)) return true
    // fetch setu from lolicon API
    runCatching { event.subject.sendImage(getRandom()!!) }.onFailure {
      event.subject.sendMessage("本来想发图的但没发出去🤦‍♂️算了（")
      log(it.message ?: "")
      log(it.stackTraceToString())
    }
    return true
  }

  // returns false if has already checked in
  suspend fun checkIn(event: GroupMessageEvent): Boolean {
    val msg =
        Database().use { db ->
          val tableName = "noporn_records"
          val where = "WHERE id = ${event.sender.id} AND group_id = ${event.group.id}"
          // query for last record
          db.executeQuery("SELECT last_checkin_time, lasting_days FROM $tableName $where;") {
            if (it.next()) {
              // record found, check last check-in time
              val lastCheckinTime =
                  LocalDate.ofInstant(
                      Instant.ofEpochMilli(it.getLong("last_checkin_time")),
                      ZoneOffset.systemDefault()
                  )
              val now = LocalDate.now()
              val diffDays = ChronoUnit.DAYS.between(lastCheckinTime, now)
              if (diffDays == 0L) {
                Pair("今天已经打过卡了，好好戒色（摸头", false)
              } else if (diffDays == 1L) {
                // update check-in time and lasting days
                val curTime = Timestamp.valueOf(LocalDate.now().atStartOfDay()).time
                val lastingDays = it.getLong("lasting_days") + 1
                db.executeUpdate(
                    """UPDATE $tableName
                       SET last_checkin_time = $curTime, lasting_days = $lastingDays
                       $where;"""
                )
                Pair("芜湖！戒色day${lastingDays}，继续保持💪", true)
              } else {
                // reset check-in time and lasting days
                val curTime = Timestamp.valueOf(LocalDate.now().atStartOfDay()).time
                db.executeUpdate(
                    """UPDATE $tableName
                       SET last_checkin_time = $curTime, lasting_days = 1
                       $where;"""
                )
                Pair("你已经有${diffDays - 1}天没戒色了，重来吧😅今天day1", true)
              }
            } else {
              // record not found, insert new record
              val curTime = Timestamp.valueOf(LocalDate.now().atStartOfDay()).time
              db.executeUpdate(
                  """INSERT INTO $tableName VALUES (
                       ${event.sender.id}, ${event.group.id}, $curTime, 1
                     );"""
              )
              Pair("戒色day1，加油💪", true)
            }
          }
        }
    event.subject.sendMessage(event.message.quote() + msg.first)
    return msg.second
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
