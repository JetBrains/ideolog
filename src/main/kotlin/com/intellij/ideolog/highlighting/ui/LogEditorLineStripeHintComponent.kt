package com.intellij.ideolog.highlighting.ui

import com.intellij.codeInsight.editorLineStripeHint.EditorLineStripeHintComponent
import com.intellij.codeInsight.editorLineStripeHint.RepositionableJPanel
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.ui.JBColor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D

private const val ROUND_PRESENTATION_WIDTH_APPENDER = 5

class LogEditorLineStripeHintComponent(
  editor: Editor,
  panelRenderer: () -> List<List<EditorCustomElementRenderer>>,
  stripeColor: JBColor,
  private val offset: Int,
) : EditorLineStripeHintComponent(editor, panelRenderer, stripeColor) {
  override fun createPhantomInlayComponent(renderer: EditorCustomElementRenderer): RepositionableJPanel? {
    return if (renderer is LogEditorLineStripePresentationRenderer) {
      LogPhantomInlayComponent(editor, renderer)
    } else {
      null
    }
  }

  override fun getLineStripeY(): Int = editor.visualPositionToXY(editor.offsetToVisualPosition(offset - 1)).y

  override fun getLineEndOffset(): Int = editor.document.getLineEndOffset(editor.offsetToVisualPosition(offset).line)

  override fun getInlaysStartOffset(): Int = offset

  override fun getMeaningfulTextEnd(lineEndOffset: Int, inlays: List<Inlay<*>>): Int = -1

  class LogPhantomInlayComponent(
    private val editor: Editor,
    private val presentationRenderer: LogEditorLineStripePresentationRenderer
  ) : RepositionableJPanel() {
    init {
      addMouseListener(presentationRenderer)
      val pixelWidth = presentationRenderer.presentation.width + ROUND_PRESENTATION_WIDTH_APPENDER
      maximumSize = Dimension(pixelWidth, editor.lineHeight)
    }

    override fun reposition() { }

    override fun paint(g: Graphics) {
      presentationRenderer.presentation.paint(
        g as Graphics2D,
        editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_HIGHLIGHTED)
      )
    }
  }
}
