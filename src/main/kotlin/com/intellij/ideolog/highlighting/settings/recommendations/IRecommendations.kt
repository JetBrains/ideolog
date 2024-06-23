package com.intellij.ideolog.highlighting.settings.recommendations

import org.jetbrains.annotations.Nls

data class SpecificRecommendation(
  @Nls val name: String,
  val url: String
)

interface IRecommendations {
  val productName: String
  fun shouldRecommend(): Boolean
  fun getRecommendations(): List<SpecificRecommendation>
}
