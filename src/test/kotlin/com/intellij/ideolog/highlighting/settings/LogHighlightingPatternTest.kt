package com.intellij.ideolog.highlighting.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import java.awt.Color

class LogHighlightingPatternTest : BasePlatformTestCase() {

  fun testSetLogColorWithNullLogColor() {
    val pattern = LogHighlightingPattern()
    val testColor = Color(123, 45, 67)

    pattern.foregroundColor = testColor
    assertNotNull("Foreground color should not be null", pattern.foregroundColor)
    assertEquals("Light RGB value should match the test color", testColor.rgb, (pattern.foregroundColor as JBColor).rgb)

    pattern.backgroundColor = testColor
    assertNotNull("Background color should not be null", pattern.backgroundColor)
    assertEquals("Light RGB value should match the test color", testColor.rgb, (pattern.backgroundColor as JBColor).rgb)
  }

  fun testSetLogColorWithExistingLogColor() {
    val pattern = LogHighlightingPattern()
    val initialColor = Color(123, 45, 67)
    val newColor = Color(89, 101, 112)

    pattern.foregroundColor = initialColor
    pattern.backgroundColor = initialColor

    pattern.foregroundColor = newColor
    assertNotNull("Foreground color should not be null", pattern.foregroundColor)

    assertNotSame("Foreground color should be updated", initialColor.rgb, (pattern.foregroundColor as JBColor).rgb)

    pattern.backgroundColor = newColor
    assertNotNull("Background color should not be null", pattern.backgroundColor)
    assertNotSame("Background color should be updated", initialColor.rgb, (pattern.backgroundColor as JBColor).rgb)
  }

  fun testSetLogColorWithNullColor() {
    val pattern = LogHighlightingPattern()
    val initialColor = Color(123, 45, 67)

    pattern.foregroundColor = initialColor
    pattern.backgroundColor = initialColor

    pattern.foregroundColor = null
    assertNull("Foreground color should be null", pattern.foregroundColor)

    pattern.backgroundColor = null
    assertNull("Background color should be null", pattern.backgroundColor)
  }
}
