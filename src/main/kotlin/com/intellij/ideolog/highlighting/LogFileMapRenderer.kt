package com.intellij.ideolog.highlighting

import com.intellij.ideolog.file.LogFileEditor
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import java.awt.Color
import java.awt.Font
import java.awt.Point
import java.util.*
import java.util.regex.Pattern
import kotlin.concurrent.scheduleAtFixedRate

class LogFileMapRenderer(private val myLogFileEditor: LogFileEditor) {
  val NumBuckets = 1024
  val HighlighterBuckets: Array<Color?> = arrayOfNulls(NumBuckets)
  val HeatMapBuckets: DoubleArray = DoubleArray(NumBuckets)
  val BreadcrumbBuckets: DoubleArray = DoubleArray(NumBuckets)

  val ComposedBuckets: Array<Color?> = arrayOfNulls(NumBuckets)
  var myHighlighters: Array<RangeHighlighter>? = null

  val settingsStore = LogHighlightingSettingsStore.getInstance()

  private val myMarkupModel: MarkupModelEx = (myLogFileEditor.editor as EditorEx).markupModel

  init {
    ComposeBuckets()

    initBreadcrumbs()
    initEventMaps()
  }

  private var myIsEnabledBreadcrumbs = true
  private fun initBreadcrumbs() {
    val growRate = .01
    val interval = .333
    val glowSize = 4
    val minSize = 4

    var timer: TimerTask? = null
    timer = Timer().scheduleAtFixedRate(1000.toLong(), (interval * 1000).toLong(),
      {
        if (myLogFileEditor.editor.isDisposed)
          timer!!.cancel()
        else {
          ApplicationManager.getApplication().invokeLater({
            // Compiler bug
            @Suppress("RemoveEmptyParenthesesFromLambdaCall")
            ApplicationManager.getApplication().runReadAction()
            {
              if ((myIsEnabledBreadcrumbs) && (!myLogFileEditor.editor.isDisposed) && (myLogFileEditor.editor.component.isVisible) && (myLogFileEditor.editor.caretModel.isUpToDate)) {
                // Get buckets hit by visible range
                val area = myLogFileEditor.editor.scrollingModel.visibleAreaOnScrollingFinished
                val offsStart = myLogFileEditor.editor.logicalPositionToOffset(myLogFileEditor.editor.xyToLogicalPosition(area.location))
                val offsEnd = myLogFileEditor.editor.logicalPositionToOffset(myLogFileEditor.editor.xyToLogicalPosition(Point(area.location.x + area.width, area.location.y + area.height)))

                var nBucketStart = GetBucketForOffset(offsStart)
                var nBucketEnd = GetBucketForOffset(offsEnd)

                // Ensure minSize
                if (nBucketEnd - nBucketStart < minSize) {
                  val deficit = minSize - (nBucketEnd - nBucketStart)
                  nBucketStart = Math.max(nBucketStart - Math.floor(deficit / 2.0).toInt(), 0)
                  nBucketEnd = Math.min(nBucketEnd + Math.ceil(deficit / 2.0).toInt(), NumBuckets - 1)
                }

                // Paint fully hit buckets
                for (nBucket in nBucketStart until nBucketEnd) {
                  val bc = BreadcrumbBuckets[nBucket]
                  if (bc < 1)
                    BreadcrumbBuckets[nBucket] = bc + (1 - bc) * growRate
                }

                // Paint buckets partly hit by glow, before
                for (nBucket in Math.max(nBucketStart - glowSize, 0) until nBucketStart) {
                  val opacity = (1 - (nBucketStart - nBucket + 1).toDouble() / (glowSize + 1))
                  val bc = BreadcrumbBuckets[nBucket]
                  if (bc < opacity)
                    BreadcrumbBuckets[nBucket] = bc + (opacity - bc) * growRate
                }
                // Paint buckets partly hit by glow, after
                for (nBucket in nBucketEnd until Math.min(nBucketEnd + glowSize, NumBuckets - 1)) {
                  val opacity = (1 - (nBucket - nBucketEnd + 1).toDouble() / (glowSize + 1))
                  val bc = BreadcrumbBuckets[nBucket]
                  if (bc < opacity)
                    BreadcrumbBuckets[nBucket] = bc + (opacity - bc) * growRate
                }

                ComposeBuckets()
              }
            }
          })
        }
      })
  }

  private fun GetBucketForOffset(offsStart: Int) = (offsStart.toDouble() / (myLogFileEditor.editor.document.textLength + 1) * NumBuckets).toInt()

  private var myIsPendingEventMap = true
  private var myIsRunningEventMap = false
  private var myIsRenderingTimeHighlighting = false
  private val mySync = Object()
  private fun initEventMaps() {
    val interval = 1.0
    var timer: TimerTask? = null
    timer = Timer().scheduleAtFixedRate(1000.toLong(), (interval * 1000).toLong(),
      {
        if (myLogFileEditor.editor.isDisposed)
          timer!!.cancel()
        else {
          try {
            synchronized(mySync)
            {
              if (!myIsPendingEventMap)
                return@scheduleAtFixedRate
              if (myIsRunningEventMap)
                return@scheduleAtFixedRate
              myIsRunningEventMap = true
              myIsPendingEventMap = false
            }

            val offsetLimit = myLogFileEditor.editor.document.textLength
            val fileFormat = detectLogFileFormat(myLogFileEditor.editor)
            var offs = 0
            val isRenderingTimeHighlighting = myIsRenderingTimeHighlighting
            val customPatterns = LogHighlightingSettingsStore.getInstance().myState.patterns.filter { it.enabled && it.showOnStripe }.map { Pattern.compile(it.pattern, Pattern.CASE_INSENSITIVE) to it }.toTypedArray()
            val customHighlightings = myLogFileEditor.editor.getUserData(highlightingSetUserKey) ?: emptySet<String>()
            println(customHighlightings.joinToString { ", " })
            val colorDefaultBackground = myLogFileEditor.editor.colorsScheme.defaultBackground

            var nBucketCur = 0
            var nCurBucketMaxTimeDelta = 0L
            var isCurBucketError = false
            var colorCurCustomHighlighter: Color? = null
            var timestampPrev: Long? = null

            fun CommitBucket() {
              if (nBucketCur >= NumBuckets)
                return
              if (isCurBucketError) {
                isCurBucketError = false
                HighlighterBuckets[nBucketCur] = Color.red
                if (HighlighterBuckets[Math.max(nBucketCur - 1, 0)] != Color.red) // Min size of two buckets
                  HighlighterBuckets[Math.max(nBucketCur - 1, 0)] = Color.red
              } else {
                HighlighterBuckets[nBucketCur] = colorCurCustomHighlighter
                if (HighlighterBuckets[Math.max(nBucketCur - 1, 0)] != colorCurCustomHighlighter) // Min size of two buckets
                  HighlighterBuckets[Math.max(nBucketCur - 1, 0)] = colorCurCustomHighlighter
              }
              colorCurCustomHighlighter = null

              val maxSec = timeDifferenceToRed.toDouble()
              val heatValue = Math.max(0.0, Math.min(nCurBucketMaxTimeDelta, 10) / maxSec) // Scale up to maxSec
              nCurBucketMaxTimeDelta = 0
              HeatMapBuckets[nBucketCur] = heatValue
              if (HeatMapBuckets[Math.max(nBucketCur - 1, 0)] < heatValue)
                HeatMapBuckets[Math.max(nBucketCur - 1, 0)] = heatValue

              nBucketCur++
            }

            while (offs < offsetLimit) {
              val logEvent = LogEvent.fromEditor(myLogFileEditor.editor, offs)
              if (logEvent.rawText.isEmpty()) // At EOF
                break
              offs += logEvent.rawText.length

              // Attrs of this entry
              val timestamp = if (isRenderingTimeHighlighting) (logEvent.date.let { fileFormat.parseLogEventTimeSeconds(it) } ?: timestampPrev) else 0
              val timeDelta = if ((timestamp != null) && (timestampPrev != null)) timestamp - timestampPrev else 0
              timestampPrev = timestamp
              val isError = logEvent.level == "ERROR"
              var colorCustomHighlighter: Color? = null
              if (!isError) {
                colorCustomHighlighter = customPatterns.filter { it.first.matcher(logEvent.rawText).find() }.firstOrNull()?.second?.let { it.foregroundColor ?: it.backgroundColor }
                @Suppress("LoopToCallChain")
                if (colorCustomHighlighter == null)
                  for (word in customHighlightings) {
                    if (logEvent.rawText.contains(word)) {
                      colorCustomHighlighter = LogHighlightingIterator.getLineBackground(word, colorDefaultBackground)
                    }
                  }
              }

              // Roll to affected buckets
              val nBucketEnd = GetBucketForOffset(logEvent.startOffset + logEvent.rawText.length - 1)
              for (nBucket in nBucketCur..nBucketEnd) {
                // Apply attrs to bucket
                nCurBucketMaxTimeDelta = Math.max(nCurBucketMaxTimeDelta, timeDelta)
                isCurBucketError = isCurBucketError || isError
                colorCurCustomHighlighter = colorCurCustomHighlighter ?: colorCustomHighlighter

                // Commit unless the last one (or the only one)
                if (nBucketCur < nBucketEnd)
                  CommitBucket()
              }
              assert(nBucketCur == nBucketEnd, { "miscounted" })

            }
            CommitBucket() // Last one
            BeginInvokeComposeBuckets()
          } finally {
            synchronized(mySync)
            {
              myIsRunningEventMap = false
            }
          }
        }
      }
    )
  }

  companion object {
    val LogFileMapRendererKey = Key.create<LogFileMapRenderer>("LogFileMapRenderer")
    fun GetOrCreateLogFileMapRenderer(editor: LogFileEditor) = GetLogFileMapRenderer(editor.editor) ?: LogFileMapRenderer(editor).let { editor.editor.putUserData(LogFileMapRendererKey, it) }
    fun GetLogFileMapRenderer(editor: Editor) = editor.getUserData(LogFileMapRendererKey)
  }

  fun ComposeBuckets() {
    val highlighters = myHighlighters ?: RecreateHighlighters()

    for (nBucket in 0 until NumBuckets) {
      val newColor: Color?
      
      newColor = if (HighlighterBuckets[nBucket] != null) {
        // User highlighter immediately wins
        HighlighterBuckets[nBucket]!!
      } else {
        if(settingsStore.myState.errorStripeMode == "heatmap") {
          val hue = HeatMapBuckets[nBucket] * -1.0 / 3.0 + 1.0 / 3.0 // Should go between green and red
          val bri = BreadcrumbBuckets[nBucket] * .35 + .60 // Not too dark, up to almost full
          val sat = .5

          Color.getHSBColor(hue.toFloat(), sat.toFloat(), bri.toFloat())
        } else {
          null
        }
      }

      if (ComposedBuckets[nBucket] != newColor) {
        ComposedBuckets[nBucket] = newColor
        myMarkupModel.setRangeHighlighterAttributes(highlighters[nBucket], TextAttributes().apply { setAttributes(null, null, null, newColor, EffectType.BOXED, Font.PLAIN) })
      }
    }
  }

  fun BeginInvokeComposeBuckets() {
    // Compiler bug
    @Suppress("RemoveEmptyParenthesesFromLambdaCall")
    ApplicationManager.getApplication().invokeLater()
    {
      ApplicationManager.getApplication().runReadAction()
      {
        if ((!myLogFileEditor.editor.isDisposed) && (myLogFileEditor.editor.component.isVisible) && (myLogFileEditor.editor.caretModel.isUpToDate))
          ComposeBuckets()
      }
    }
  }

  fun RecreateHighlighters(): Array<RangeHighlighter> {
    if (myHighlighters != null)
      for (h in myHighlighters!!)
        myMarkupModel.removeHighlighter(h)

    val docLen = myLogFileEditor.editor.document.textLength.toDouble()
    myHighlighters = Array(NumBuckets, { nBucket -> myMarkupModel.addRangeHighlighter((docLen / NumBuckets * nBucket).toInt(), (docLen / NumBuckets * (nBucket + 1)).toInt(), 7, TextAttributes().apply { setAttributes(null, null, null, ComposedBuckets[nBucket], EffectType.BOXED, Font.PLAIN) }, HighlighterTargetArea.EXACT_RANGE) })

    return myHighlighters!!
  }

  fun setIsRenderingTimeHighlighting(value: Boolean) {
    if (myIsRenderingTimeHighlighting == value)
      return
    myIsRenderingTimeHighlighting = value
    synchronized(mySync)
    {
      myIsPendingEventMap = true
    }
  }

  fun invalidateHighlighters() {
    synchronized(mySync) { myIsPendingEventMap = true }
  }
}
