@file:Suppress("UseJBColor")

package com.intellij.ideolog.highlighting.settings

import com.intellij.ui.DarculaColors
import com.intellij.ui.Gray
import java.awt.Color

object LogColors {
  val GREEN: LogColor = LogColor(
    Color(0, 128, 0).rgb,
    Color(98, 150, 85).rgb,
  )

  val ORANGE: LogColor = LogColor(
    Color(200, 140, 0).rgb,
    Color(159, 107, 0).rgb,
  )

  val RED: LogColor = LogColor(
    Color(224, 32, 32).rgb,
    DarculaColors.RED.rgb,
  )

  val GRAY: LogColor = LogColor(
    Gray._128.rgb,
    Gray._128.rgb,
  )
}
