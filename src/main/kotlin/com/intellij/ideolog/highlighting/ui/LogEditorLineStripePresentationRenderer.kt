package com.intellij.ideolog.highlighting.ui

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseListener

class LogEditorLineStripePresentationRenderer(
  private val renderer: PresentationRenderer,
  mouseListener: MouseListener,
) : EditorCustomElementRenderer, MouseListener by mouseListener {
  val presentation: InlayPresentation = renderer.presentation

  override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) =
    renderer.paint(inlay, g, targetRegion, textAttributes)

  override fun calcWidthInPixels(inlay: Inlay<*>): Int = renderer.presentation.width

  override fun toString(): String = renderer.presentation.toString()
}
