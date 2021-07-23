package net.maxxsoft.chii.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.maxxsoft.chii.handlers.*
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Configuration of Chii. */
@Serializable
private data class Configuration(
  val account: Long,
  val password: String,
  val watchedGroups: Set<Long>,
  val enableAllHandlers: Boolean,
  val enabledHandlers: List<String>,
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
        Json.decodeFromString(file.readText())
      } else {
        // generate default configuration
        val default =
          Configuration(
            getInput("account").toLong(),
            getInput("password"),
            setOf(),
            true,
            listOf(),
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
  // all instances of `MessageHandler`
  private val INSTANCES: List<MessageHandler> =
    listOf(
      AtCommandHandler,
      MuteGameHandler,
      NoPornHandler,
      PenggenHandler,
      RandomRepeatHandler,
    )

  private var startTime = LocalDateTime.now()

  /** Configurations from file. */
  var account: Long = 0L
    private set
  var password: String = ""
    private set
  var watchedGroups: Set<Long> = setOf()
    private set
  var enabledHandlers: List<MessageHandler> = listOf()
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
    enabledHandlers =
      if (conf.enableAllHandlers) INSTANCES
      else INSTANCES.associateBy { it.id }.let { m -> conf.enabledHandlers.map { m[it]!! } }
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
    val minutes = ChronoUnit.MINUTES.between(startTime, now) % 60
    val secs = ChronoUnit.SECONDS.between(startTime, now) % 60
    return "${days}天${hours}小时${minutes}分${secs}秒"
  }
}
