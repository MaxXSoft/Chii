package net.maxxsoft.chii

import java.io.File
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.maxxsoft.chii.utils.Configuration

private interface App {
  fun run()
  fun quit() {}
}

private object Main : App {
  // configuration file
  private val CONF_FILE = "config.json"
  // read configuration from file
  private val CONF = Configuration.fromFile(File(CONF_FILE))

  override fun run() =
      runBlocking<Unit> {
        // create bot & login
        val bot = BotFactory.newBot(CONF.account, CONF.password) { fileBasedDeviceInfo() }
        bot.login()
        // listen on group message event
        bot.eventChannel.subscribeAlways<GroupMessageEvent> {
          // check if is watched group
          if (this.group.id in CONF.watchedGroups) {
            println(sender.id)
          }
        }
      }

  override fun quit() {
    // save configuration
    CONF.saveToFile(File(CONF_FILE))
    println("configuration saved")
  }
}

fun main() {
  val app = Main
  Runtime.getRuntime().addShutdownHook(Thread { app.quit() })
  app.run()
}
