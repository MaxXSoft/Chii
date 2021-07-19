package net.maxxsoft.chii.utils

import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.arrayOf
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/** Configuration of Chii. */
@Serializable
private data class Configuration(
    val account: Long,
    val password: String,
    val watchedGroups: Array<Long>,
    val enableAllHandlers: Boolean,
    val enabledHandlers: Set<String>,
    val masterId: Long,
) {
  companion object {
    /**
     * Load Chii configuration from the specific file.
     *
     * @param file the specific `File` object.
     * @return `Configuration` object.
     */
    fun fromFile(file: File) =
        if (file.exists() && file.length() != 0L) {
          Json.decodeFromString<Configuration>(file.readText())
        } else {
          // generate default configuration
          val default =
              Configuration(
                  getInput("account").toLong(),
                  getInput("password"),
                  arrayOf(),
                  true,
                  setOf(),
                  getInput("id of master").toLong()
              )
          // save changes
          file.writeText(Json.encodeToString(default))
          default
        }

    private fun getInput(prompt: String? = null) =
        prompt?.let { print("$it: ") }.let { readLine() ?: "" }
  }
}

/** Global config. */
object Config {
  private var startTime = LocalDateTime.now()

  /** Configurations from file. */
  var account: Long = 0L
    private set
  var password: String = ""
    private set
  var watchedGroups: Array<Long> = arrayOf()
    private set
  var enableAllHandlers: Boolean = true
    private set
  var enabledHandlers: Set<String> = setOf()
    private set
  var masterId: Long = 0L
    private set

  init {
    reload()
  }

  /** Reload all configurations. */
  fun reload() {
    val conf = Configuration.fromFile(File("config.json"))
    account = conf.account
    password = conf.password
    watchedGroups = conf.watchedGroups
    enableAllHandlers = conf.enableAllHandlers
    enabledHandlers = conf.enabledHandlers
    masterId = conf.masterId
  }

  /** Update start time. */
  fun updateStartTime() {
    startTime = LocalDateTime.now()
  }

  /** Get running time. */
  fun getRunningTime(): String {
    val now = LocalDateTime.now()
    val days = ChronoUnit.DAYS.between(startTime, now)
    val hours = ChronoUnit.HOURS.between(startTime, now) % 24
    val mins = ChronoUnit.MINUTES.between(startTime, now) % 60
    val secs = ChronoUnit.SECONDS.between(startTime, now) % 60
    return "${days}天${hours}小时${mins}分${secs}秒"
  }
}
