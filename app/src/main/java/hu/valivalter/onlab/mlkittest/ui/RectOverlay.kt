package hu.valivalter.onlab.mlkittest.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.objects.DetectedObject
import kotlin.math.min

class RectOverlay constructor(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) {

    private val bounds: MutableList<RectF> = mutableListOf()
    private val trackingIds: MutableList<Int> = mutableListOf()
    private val labels: MutableList<String> = mutableListOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bounds.isNotEmpty() && trackingIds.isNotEmpty()) {
            var size = min(bounds.size, trackingIds.size)
            for (i in 0 until size) {
                canvas.drawRect(bounds[i], randomPaint(trackingIds[i]))
                if (labels.size > i) {
                    canvas.drawText(labels[i], bounds[i].left, bounds[i].centerY(), randomPaint(trackingIds[i]))
                }
            }
        }
        //bounds.forEach { canvas.drawRect(it, randomPaint()) }
    }

    fun drawBounds(bounds: List<RectF>, trackingIds: List<Int>, labels: MutableList<String>) {
        this.bounds.clear()
        this.bounds.addAll(bounds)
        this.trackingIds.clear()
        this.trackingIds.addAll(trackingIds)
        this.labels.clear()
        this.labels.addAll(labels)
        invalidate()
    }

    private fun randomPaint(trackingId: Int): Paint {
        return Paint().apply {
            style = Paint.Style.STROKE
            color = Color.rgb(200, (trackingId*40)%255, (trackingId*40)%255)
            //color = ContextCompat.getColor(context!!, android.R.color.black)
            strokeWidth = 10f
            textSize = 100f
        }
    }
}