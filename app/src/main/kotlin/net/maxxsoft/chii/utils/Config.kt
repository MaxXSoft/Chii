package net.maxxsoft.chii.utils

import java.io.File
import kotlin.arrayOf
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/** Configuration of Chii. */
@Serializable
public data class Configuration(
    // immutable configurations
    public val account: Long,
    public val password: String,
    public val watchedGroups: Array<Long>,
// mutable configurations
// TODO
) {
  public companion object {
    /**
     * Get a default `Configuration` object.
     *
     * @return `Configuration` object
     */
    public fun default() =
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
    public fun fromFile(file: File) =
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
  public fun saveToFile(file: File) = file.writeText(Json.encodeToString(this))
}
