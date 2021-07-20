package net.maxxsoft.chii

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.maxxsoft.chii.handlers.MessageHandler
import net.maxxsoft.chii.utils.Config

private interface App {
  fun run()
  fun quit() {}
}

private object Main : App {
  override fun run() =
      runBlocking<Unit> {
        // update start time
        Config.updateStartTime()
        // create bot & login
        val bot = BotFactory.newBot(Config.account, Config.password) { fileBasedDeviceInfo() }
        bot.login()
        // listen on group message event
        bot.eventChannel.subscribeAlways<GroupMessageEvent> {
          // check if is watched group
          if (group.id in Config.watchedGroups) {
            // handle with message handlers
            MessageHandler.handle(this)
          }
        }
        // poll logs
        launch(Dispatchers.IO) { MessageHandler.pollLogger() }
      }

  override fun quit() {
    // close logger channel
    MessageHandler.closeLoggerChannel()
  }
}

private object Misc : App {
  override fun run() {}
}

fun main() {
  val app = Main
  Runtime.getRuntime().addShutdownHook(Thread { app.quit() })
  app.run()
}
