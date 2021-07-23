package net.maxxsoft.chii.utils

import kotlinx.serialization.Serializable

/** Response object for lolicon API's request. */
@Serializable
data class LoliconResponse(
  val error: String,
  val data: List<SetuImageInfo>? = null,
) {
  @Serializable
  data class SetuImageInfo(
    val pid: Int,
    val p: Int,
    val uid: Int,
    val title: String,
    val author: String,
    val r18: Boolean,
    val width: Int,
    val height: Int,
    val tags: List<String>,
    val ext: String,
    val uploadDate: Long,
    val urls: SetuUrl,
  ) {
    @Serializable
    data class SetuUrl(
      val original: String? = null,
      val regular: String? = null,
      val small: String? = null,
      val thumb: String? = null,
      val mini: String? = null,
    )
  }
}
