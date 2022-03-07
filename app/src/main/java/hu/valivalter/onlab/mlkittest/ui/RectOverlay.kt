package hu.valivalter.onlab.mlkittest.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class RectOverlay constructor(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) {

    private val bounds: MutableList<RectF> = mutableListOf()
    private val trackingIds: MutableList<Int> = mutableListOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bounds.isNotEmpty() && trackingIds.isNotEmpty()) {
            var size = max(bounds.size, trackingIds.size)
            for (i in 0 until size) {
                canvas.drawRect(bounds[i], randomPaint(trackingIds[i]))
            }
        }
        //bounds.forEach { canvas.drawRect(it, randomPaint()) }
    }

    fun drawBounds(bounds: List<RectF>, trackingIds: List<Int>) {
        this.bounds.clear()
        this.bounds.addAll(bounds)
        this.trackingIds.clear()
        this.trackingIds.addAll(trackingIds)
        invalidate()
    }

    private fun randomPaint(trackingId: Int): Paint {
        return Paint().apply {
            style = Paint.Style.STROKE
            color = Color.rgb(200, (trackingId*40)%255, (trackingId*40)%255)
            //color = ContextCompat.getColor(context!!, android.R.color.black)
            strokeWidth = 10f
        }
    }
}