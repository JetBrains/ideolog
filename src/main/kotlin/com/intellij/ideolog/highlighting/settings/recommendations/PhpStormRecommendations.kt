package com.intellij.ideolog.highlighting.settings.recommendations

import com.intellij.ideolog.IdeologBundle
import com.intellij.util.PlatformUtils

class PhpStormRecommendations : IRecommendations {
  override val productName = "PHP"
  override fun shouldRecommend() = PlatformUtils.isPhpStorm()

  override fun getRecommendations(): List<SpecificRecommendation> {
    return listOf(
      SpecificRecommendation(IdeologBundle.message("laravel"), "https://github.com/JetBrains/ideolog/blob/master/highlightersDirectory/Laravel.xml")
    )
  }
}
