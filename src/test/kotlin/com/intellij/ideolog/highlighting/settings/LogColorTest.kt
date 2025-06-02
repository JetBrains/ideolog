package com.intellij.ideolog.highlighting.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import java.awt.Color

class LogColorTest : BasePlatformTestCase() {

  fun testFromColor() {
    val testColor = Color(123, 45, 67)

    val logColor = LogColor.fromColor(testColor)
    assertNotNull("LogColor should not be null", logColor)
    assertEquals("Light RGB value should match the test color", testColor.rgb, logColor!!.lightRgb)
    assertEquals("Dark RGB value should match the test color", testColor.rgb, logColor.darkRgb)

    val nullLogColor = LogColor.fromColor(null)
    assertNull("LogColor should be null when created from null color", nullLogColor)
  }

  fun testToJBColor() {
    val lightRgb = Color(123, 45, 67).rgb
    val darkRgb = Color(89, 101, 112).rgb
    val logColor = LogColor(lightRgb, darkRgb)

    val jbColor = logColor.toJBColor()
    assertNotNull("JBColor should not be null", jbColor)

    val expectedColor = JBColor(lightRgb, darkRgb)
    assertEquals("JBColor should be created with the correct RGB values",
                 expectedColor.toString(), jbColor.toString())
  }

  fun testUpdateWithColor() {
    val lightRgb = Color(123, 45, 67).rgb
    val darkRgb = Color(89, 101, 112).rgb
    val logColor = LogColor(lightRgb, darkRgb)
    val newColor = Color(210, 220, 230)

    val updatedLogColor = logColor.updateWithColor(newColor)
    assertNotNull("Updated LogColor should not be null", updatedLogColor)

    if (JBColor.isBright()) {
      assertEquals("Light RGB should be updated", newColor.rgb, updatedLogColor!!.lightRgb)
      assertEquals("Dark RGB should remain the same", darkRgb, updatedLogColor.darkRgb)
    }
    else {
      assertEquals("Light RGB should remain the same", lightRgb, updatedLogColor!!.lightRgb)
      assertEquals("Dark RGB should be updated", newColor.rgb, updatedLogColor.darkRgb)
    }

    val nullUpdatedLogColor = logColor.updateWithColor(null)
    assertNull("LogColor should be null when updated with null color", nullUpdatedLogColor)
  }
}
