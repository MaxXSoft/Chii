package net.maxxsoft.chii.utils

import java.io.File
import kotlin.arrayOf
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/** Configuration of Chii. */
@Serializable
data class Configuration(
    // immutable configurations
    val account: Long,
    val password: String,
    val watchedGroups: Array<Long>,
// mutable configurations
// TODO
) {
  companion object {
    /**
     * Get a default `Configuration` object.
     *
     * @return `Configuration` object
     */
    fun default() =
        Configuration(
            getInput("account").toLong(),
            getInput("password"),
            arrayOf(),
        )

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
          default()
        }

    private fun getInput(prompt: String? = null) =
        prompt?.let { print("$it: ") }.let { readLine() ?: "" }
  }

  /**
   * Save the current Chii configuration to the specific file.
   *
   * @param file the specific `File` object.
   */
  fun saveToFile(file: File) = file.writeText(Json.encodeToString(this))
}
