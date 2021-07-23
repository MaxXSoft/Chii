package net.maxxsoft.chii.utils

import kotlinx.serialization.Serializable

@Serializable
data class PenggenData(
  val prob: Double,
  val txts: List<PenggenTextData>,
) {
  @Serializable
  data class PenggenTextData(
    val prob: Double,
    val txt: List<String>,
  )
}
