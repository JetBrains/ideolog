package com.intellij.ideolog.highlighting.settings

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.ui.JBColor
import java.awt.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

@TestApplication
class LogHighlightingPatternTest {
  @Test
  fun testSetLogColorWithNullLogColor() {
    val pattern = LogHighlightingPattern()
    val testColor = Color(123, 45, 67)

    pattern.foregroundColor = testColor
    assertNotNull(pattern.foregroundColor, "Foreground color should not be null")
    assertEquals(testColor.rgb, (pattern.foregroundColor as JBColor).rgb, "Light RGB value should match the test color")

    pattern.backgroundColor = testColor
    assertNotNull(pattern.backgroundColor, "Background color should not be null")
    assertEquals(testColor.rgb, (pattern.backgroundColor as JBColor).rgb, "Light RGB value should match the test color")
  }

  @Test
  fun testSetLogColorWithExistingLogColor() {
    val pattern = LogHighlightingPattern()
    val initialColor = Color(123, 45, 67)
    val newColor = Color(89, 101, 112)

    pattern.foregroundColor = initialColor
    pattern.backgroundColor = initialColor

    pattern.foregroundColor = newColor
    assertNotNull(pattern.foregroundColor, "Foreground color should not be null")

    assertNotSame(initialColor.rgb, (pattern.foregroundColor as JBColor).rgb, "Foreground color should be updated")

    pattern.backgroundColor = newColor
    assertNotNull(pattern.backgroundColor, "Background color should not be null")
    assertNotSame(initialColor.rgb, (pattern.backgroundColor as JBColor).rgb, "Background color should be updated")
  }

  @Test
  fun testSetLogColorWithNullColor() {
    val pattern = LogHighlightingPattern()
    val initialColor = Color(123, 45, 67)

    pattern.foregroundColor = initialColor
    pattern.backgroundColor = initialColor

    pattern.foregroundColor = null
    assertNull(pattern.foregroundColor, "Foreground color should be null")

    pattern.backgroundColor = null
    assertNull(pattern.backgroundColor, "Background color should be null")
  }
}
