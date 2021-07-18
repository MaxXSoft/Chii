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
          // default configuration
          Configuration(
              getInput("account").toLong(),
              getInput("password"),
              arrayOf(),
              true,
              arrayOf(),
              getInput("id of master").toLong()
          )
        }

    private fun getInput(prompt: String? = null) =
        prompt?.let { print("$it: ") }.let { readLine() ?: "" }
  }
}

/** Global config. */
object Config {
  val account: Long
  val password: String
  val watchedGroups: Array<Long>
  val enableAllHandlers: Boolean
  val enabledHandlers: Array<String>
  val masterId: Long

  init {
    val conf = Configuration.fromFile(File("config.json"))
    account = conf.account
    password = conf.password
    watchedGroups = conf.watchedGroups
    enableAllHandlers = conf.enableAllHandlers
    enabledHandlers = conf.enabledHandlers
    masterId = conf.masterId
  }
}
