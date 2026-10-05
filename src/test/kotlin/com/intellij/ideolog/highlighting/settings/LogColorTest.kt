package com.intellij.ideolog.highlighting.settings

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.XmlSerializer
import java.awt.Color
import org.jdom.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@TestApplication
class LogColorTest {
  @Test
  fun testFromColor() {
    val testColor = Color(123, 45, 67)

    val logColor = LogColor.fromColor(testColor)
    assertNotNull(logColor, "LogColor should not be null")
    assertEquals(testColor.rgb, logColor!!.lightRgb, "Light RGB value should match the test color")
    assertEquals(testColor.rgb, logColor.darkRgb, "Dark RGB value should match the test color")

    val nullLogColor = LogColor.fromColor(null)
    assertNull(nullLogColor, "LogColor should be null when created from null color")
  }

  @Test
  fun testToJBColor() {
    val lightRgb = Color(123, 45, 67).rgb
    val darkRgb = Color(89, 101, 112).rgb
    val logColor = LogColor(lightRgb, darkRgb)

    val jbColor = logColor.toJBColor()
    assertNotNull(jbColor, "JBColor should not be null")

    val expectedColor = JBColor(lightRgb, darkRgb)
    assertEquals(
      expectedColor.toString(), jbColor.toString(),
      "JBColor should be created with the correct RGB values"
    )
  }

  @Test
  fun testUpdateWithColor() {
    val lightRgb = Color(123, 45, 67).rgb
    val darkRgb = Color(89, 101, 112).rgb
    val logColor = LogColor(lightRgb, darkRgb)
    val newColor = Color(210, 220, 230)

    val updatedLogColor = logColor.updateWithColor(newColor)
    assertNotNull(updatedLogColor, "Updated LogColor should not be null")

    if (JBColor.isBright()) {
      assertEquals(newColor.rgb, updatedLogColor!!.lightRgb, "Light RGB should be updated")
      assertEquals(darkRgb, updatedLogColor.darkRgb, "Dark RGB should remain the same")
    } else {
      assertEquals(lightRgb, updatedLogColor!!.lightRgb, "Light RGB should remain the same")
      assertEquals(newColor.rgb, updatedLogColor.darkRgb, "Dark RGB should be updated")
    }

    val nullUpdatedLogColor = logColor.updateWithColor(null)
    assertNull(nullUpdatedLogColor, "LogColor should be null when updated with null color")
  }

  @Test
  fun testDeserializeCurrentJsonColorFormat() {
    val element = Element("LogHighlightingPattern").apply {
      setAttribute("fg", """{"lightRgb":-65536,"darkRgb":-65536}""")
      setAttribute("bg", """{"lightRgb":-39836,"darkRgb":-39836}""")
    }

    val pattern = XmlSerializer.deserialize(element, LogHighlightingPattern::class.java)

    assertEquals(LogColor(-65536, -65536), pattern.fgRgb)
    assertEquals(LogColor(-39836, -39836), pattern.bgRgb)
  }

  @Test
  fun testDeserializeLegacyIntColorFormat() {
    val element = Element("LogHighlightingPattern").apply {
      setAttribute("fg", "-65536")
      setAttribute("bg", "-39836")
    }

    val pattern = XmlSerializer.deserialize(element, LogHighlightingPattern::class.java)

    assertEquals(LogColor(-65536, -65536), pattern.fgRgb)
    assertEquals(LogColor(-39836, -39836), pattern.bgRgb)
  }

  @Test
  fun testDeserializeMalformedColorDoesNotThrow() {
    val element = Element("LogHighlightingPattern").apply {
      setAttribute("fg", "not-a-color")
    }

    val pattern = XmlSerializer.deserialize(element, LogHighlightingPattern::class.java)

    assertNull(pattern.fgRgb)
  }

  @Test
  fun testColorSerializationRoundTrip() {
    val pattern = LogHighlightingPattern().apply {
      fgRgb = LogColor(lightRgb = -65536, darkRgb = -12345)
      bgRgb = LogColor(lightRgb = 100, darkRgb = 200)
    }

    val restored = XmlSerializer.deserialize(XmlSerializer.serialize(pattern), LogHighlightingPattern::class.java)

    assertEquals(pattern.fgRgb, restored.fgRgb)
    assertEquals(pattern.bgRgb, restored.bgRgb)
  }
}
