package net.maxxsoft.chii.utils

import java.io.File
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
    val enabledHandlers: Array<String>,
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
                  arrayOf(),
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
  var account: Long = 0
    private set
  var password: String = ""
    private set
  var watchedGroups: Array<Long> = arrayOf()
    private set
  var enableAllHandlers: Boolean = true
    private set
  var enabledHandlers: Array<String> = arrayOf()
    private set
  var masterId: Long = 0
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
}
