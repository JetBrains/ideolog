package com.intellij.ideolog.highlighting

import com.intellij.ideolog.file.LogFileEditor
import com.intellij.ideolog.highlighting.settings.LogHighlightingSettingsStore
import com.intellij.ideolog.lex.detectLogFileFormat
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class LogFileMapRenderer(private val myLogFileEditor: LogFileEditor) {
  private val numBuckets = 1024
  private val highlighterBuckets: Array<Color?> = arrayOfNulls(numBuckets)
  private val heatMapBuckets: DoubleArray = DoubleArray(numBuckets)
  private val breadcrumbBuckets: DoubleArray = DoubleArray(numBuckets)

  private val composedBuckets: Array<Color?> = arrayOfNulls(numBuckets)
  private var myHighlighters: Array<RangeHighlighter>? = null

  private val settingsStore = LogHighlightingSettingsStore.getInstance()

  private var detachedFromEditor = false

  private val myMarkupModel: MarkupModelEx = myLogFileEditor.editor.markupModel

  init {
    composeBuckets()

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
    timer = Timer().scheduleAtFixedRate(1000.toLong(), (interval * 1000).toLong()
    ) {
      if (myLogFileEditor.editor.isDisposed || detachedFromEditor)
        timer!!.cancel()
      else {
        ApplicationManager.getApplication().invokeLater {
          ApplicationManager.getApplication().runReadAction {
            if ((myIsEnabledBreadcrumbs) && (!myLogFileEditor.editor.isDisposed && !detachedFromEditor) && (myLogFileEditor.editor.component.isVisible) && (myLogFileEditor.editor.caretModel.isUpToDate)) {
              // Get buckets hit by visible range
              val area = myLogFileEditor.editor.scrollingModel.visibleAreaOnScrollingFinished
              val offsStart = myLogFileEditor.editor.logicalPositionToOffset(myLogFileEditor.editor.xyToLogicalPosition(area.location))
              val offsEnd = myLogFileEditor.editor.logicalPositionToOffset(myLogFileEditor.editor.xyToLogicalPosition(Point(area.location.x + area.width, area.location.y + area.height)))

              var nBucketStart = getBucketForOffset(offsStart)
              var nBucketEnd = getBucketForOffset(offsEnd)

              // Ensure minSize
              if (nBucketEnd - nBucketStart < minSize) {
                val deficit = minSize - (nBucketEnd - nBucketStart)
                nBucketStart = max(nBucketStart - floor(deficit / 2.0).toInt(), 0)
                nBucketEnd = min(nBucketEnd + ceil(deficit / 2.0).toInt(), numBuckets - 1)
              }

              // Paint fully hit buckets
              for (nBucket in nBucketStart until nBucketEnd) {
                val bc = breadcrumbBuckets[nBucket]
                if (bc < 1)
                  breadcrumbBuckets[nBucket] = bc + (1 - bc) * growRate
              }

              // Paint buckets partly hit by the glow before
              for (nBucket in max(nBucketStart - glowSize, 0) until nBucketStart) {
                val opacity = (1 - (nBucketStart - nBucket + 1).toDouble() / (glowSize + 1))
                val bc = breadcrumbBuckets[nBucket]
                if (bc < opacity)
                  breadcrumbBuckets[nBucket] = bc + (opacity - bc) * growRate
              }
              // Paint buckets partly hit by the glow after
              for (nBucket in nBucketEnd until min(nBucketEnd + glowSize, numBuckets - 1)) {
                val opacity = (1 - (nBucket - nBucketEnd + 1).toDouble() / (glowSize + 1))
                val bc = breadcrumbBuckets[nBucket]
                if (bc < opacity)
                  breadcrumbBuckets[nBucket] = bc + (opacity - bc) * growRate
              }

              composeBuckets()
            }
          }
        }
      }
    }
  }

  private fun getBucketForOffset(offsStart: Int) = (offsStart.toDouble() / (myLogFileEditor.editor.document.textLength) * (numBuckets - 1)).toInt()

  private var myIsPendingEventMap = true
  private var myIsRunningEventMap = false
  private var myIsRenderingTimeHighlighting = false
  private val mySync = Object()
  private fun initEventMaps() {
    val interval = 1.0
    var timer: TimerTask? = null
    timer = Timer().scheduleAtFixedRate(1000.toLong(), (interval * 1000).toLong()
    ) {
      if (myLogFileEditor.editor.isDisposed || detachedFromEditor)
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
          val customPatterns = LogHighlightingSettingsStore.getInstance().myState.patterns
            .filter { it.enabled && it.showOnStripe && fileFormat.validateFormatUUID(it.formatId) }
            .map { Pattern.compile(it.pattern, Pattern.CASE_INSENSITIVE) to it }
            .toTypedArray()
          val customHighlightings = myLogFileEditor.editor.getUserData(highlightingSetUserKey) ?: emptySet()
          val colorDefaultBackground = myLogFileEditor.editor.colorsScheme.defaultBackground

          var nBucketCur = 0
          var nCurBucketMaxTimeDelta = 0L
          var colorCurCustomHighlighter: Color? = null
          var timestampPrev: Long? = null

          fun commitBucket() {
            if (nBucketCur >= numBuckets)
              return

            highlighterBuckets[nBucketCur] = colorCurCustomHighlighter
            if (highlighterBuckets[max(nBucketCur - 1, 0)] != colorCurCustomHighlighter) // Min size of two buckets
              highlighterBuckets[max(nBucketCur - 1, 0)] = colorCurCustomHighlighter
            colorCurCustomHighlighter = null

            val maxSec = timeDifferenceToRed.toDouble()
            val heatValue = max(0.0, min(nCurBucketMaxTimeDelta, 10) / maxSec) // Scale up to maxSec
            nCurBucketMaxTimeDelta = 0
            heatMapBuckets[nBucketCur] = heatValue
            if (heatMapBuckets[max(nBucketCur - 1, 0)] < heatValue)
              heatMapBuckets[max(nBucketCur - 1, 0)] = heatValue

            nBucketCur++
          }

          while (offs < offsetLimit) {
            val logEvent = LogEvent.fromEditor(myLogFileEditor.editor, offs)
            if (logEvent.rawText.isEmpty()) // At EOF
              break
            offs += logEvent.rawText.length + 1

            // Attrs of this entry
            val timestamp = if (isRenderingTimeHighlighting) (logEvent.date.let { fileFormat.parseLogEventTimeSeconds(it) }
              ?: timestampPrev) else 0
            val timeDelta = if ((timestamp != null) && (timestampPrev != null)) timestamp - timestampPrev else 0
            timestampPrev = timestamp
            var colorCustomHighlighter: Color? = customPatterns.firstOrNull { it.first.matcher(logEvent.rawLevel).find() }?.second?.let {
              it.foregroundColor ?: it.backgroundColor
            }
            @Suppress("LoopToCallChain")
            if (colorCustomHighlighter == null)
              for (word in customHighlightings) {
                if (logEvent.rawText.contains(word)) {
                  colorCustomHighlighter = LogHighlightingIterator.getLineBackground(word, colorDefaultBackground)
                }
              }

            // Roll to affected buckets
            val nBucketEnd = getBucketForOffset(logEvent.startOffset + logEvent.rawText.length)
            (nBucketCur..nBucketEnd).forEach {
                // Apply attrs to bucket
              nCurBucketMaxTimeDelta = max(nCurBucketMaxTimeDelta, timeDelta)
              colorCurCustomHighlighter = colorCurCustomHighlighter ?: colorCustomHighlighter

              // Commit unless the last one (or the only one)
              if (nBucketCur < nBucketEnd)
                commitBucket()
            }
            assert(nBucketCur == nBucketEnd) { "miscounted" }

          }
          commitBucket() // Last one
          beginInvokeComposeBuckets()
        } finally {
          synchronized(mySync)
          {
            myIsRunningEventMap = false
          }
        }
      }
    }
  }

  companion object {
    val LogFileMapRendererKey = Key.create<LogFileMapRenderer>("LogFileMapRenderer")
    fun getOrCreateLogFileMapRenderer(editor: LogFileEditor) = getLogFileMapRenderer(editor.editor)
      ?: LogFileMapRenderer(editor).let { editor.editor.putUserData(LogFileMapRendererKey, it) }

    fun getLogFileMapRenderer(editor: Editor) = editor.getUserData(LogFileMapRendererKey)
  }

  private fun composeBuckets() {
    val highlighters = if (myHighlighters.isNullOrEmpty()) recreateHighlighters() else myHighlighters
    if (highlighters.isNullOrEmpty()) return
    for (nBucket in 0 until numBuckets) {
      val newColor: Color? = if (highlighterBuckets[nBucket] != null) {
        // User highlighter immediately wins
        highlighterBuckets[nBucket]!!
      } else {
        if (settingsStore.myState.errorStripeMode == "heatmap") {
          val hue = heatMapBuckets[nBucket] * -1.0 / 3.0 + 1.0 / 3.0 // Should go between green and red
          val bri = breadcrumbBuckets[nBucket] * .35 + .60 // Not too dark, up to almost full
          val sat = .5

          Color.getHSBColor(hue.toFloat(), sat.toFloat(), bri.toFloat())
        } else {
          null
        }
      }

      if (composedBuckets[nBucket] != newColor) {
        composedBuckets[nBucket] = newColor
        myMarkupModel.setRangeHighlighterAttributes(highlighters[nBucket], TextAttributes().apply { setAttributes(null, null, null, newColor, EffectType.BOXED, Font.PLAIN) })
      }
    }
  }

  fun detachFromEditor() {
    myHighlighters?.forEach {
      myMarkupModel.removeHighlighter(it)
    }
    myLogFileEditor.editor.putUserData(LogFileMapRendererKey, null)
    detachedFromEditor = true
  }

  private fun beginInvokeComposeBuckets() {
    ApplicationManager.getApplication().invokeLater {
      ApplicationManager.getApplication().runReadAction {
        if ((!myLogFileEditor.editor.isDisposed && !detachedFromEditor)
          && (myLogFileEditor.editor.component.isVisible)
          && (myLogFileEditor.editor.caretModel.isUpToDate))
          composeBuckets()
      }
    }
  }

  private fun recreateHighlighters(): Array<RangeHighlighter> {
    if (detachedFromEditor || myLogFileEditor.editor.isDisposed)
      return arrayOf()

    if (myHighlighters != null)
      for (h in myHighlighters!!)
        myMarkupModel.removeHighlighter(h)

    val docLen = myLogFileEditor.editor.document.textLength.toDouble()
    myHighlighters = Array(numBuckets) { nBucket ->
      myMarkupModel.addRangeHighlighter(
        (docLen / numBuckets * nBucket).toInt(),
        (docLen / numBuckets * (nBucket + 1)).toInt(), 7,
        TextAttributes().apply {
          setAttributes(null, null, null, composedBuckets[nBucket], EffectType.BOXED, Font.PLAIN)
        },
        HighlighterTargetArea.EXACT_RANGE)
    }

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
