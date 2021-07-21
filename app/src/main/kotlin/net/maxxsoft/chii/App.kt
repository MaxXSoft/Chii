package net.maxxsoft.chii

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.maxxsoft.chii.handlers.AtCommandHandler
import net.maxxsoft.chii.handlers.MessageHandler
import net.maxxsoft.chii.utils.Config

private interface App {
  fun run()
  fun quit() {}
}

private object Main : App {
  // all bot instances
  private val bots = ArrayList<Bot>()

  // all coroutines
  private val jobs = ArrayList<Job>()

  override fun run() =
      runBlocking<Unit> {
        // update start time
        Config.updateStartTime()
        // create bot & login
        val bot = BotFactory.newBot(Config.account, Config.password) { fileBasedDeviceInfo() }
        bots.add(bot)
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
        jobs.add(launch(Dispatchers.IO) { MessageHandler.pollLogger() })
        // detect shutdown command
        jobs.add(launch { detectShutdown() })
      }

  override fun quit() {
    // cancel all coroutines
    jobs.forEach { it.cancel() }
    // close logger channel
    MessageHandler.closeLoggerChannel()
    // close bot
    bots.forEach { it.close() }
  }

  private suspend fun detectShutdown() {
    while (!AtCommandHandler.shutdown) {
      yield()
    }
    quit()
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
