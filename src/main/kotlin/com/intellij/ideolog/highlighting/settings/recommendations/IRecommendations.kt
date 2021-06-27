package com.intellij.ideolog.highlighting.settings.recommendations

data class SpecificRecommendation(
  val name: String,
  val url: String
)

interface IRecommendations {
  val productName: String
  fun shouldRecommend(): Boolean
  fun getRecommendations(): List<SpecificRecommendation>
}
