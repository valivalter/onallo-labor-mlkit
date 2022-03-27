package hu.valivalter.onlab.mlkittest.logic

import android.content.Context
import android.graphics.Point
import android.graphics.RectF
import android.view.Display
import android.view.WindowManager
import com.google.mlkit.vision.objects.DetectedObject

class DetectionTrackingLogic(val infoScreen : PoseInfoScreen): Logic {

    var detectedObjects : List<DetectedObject> = listOf()
    var detectedListener : DetectorListener? = null

    fun updateBoundingBoxes(objects : List<DetectedObject>, imageWidth: Int, imageHeight: Int) {
        detectedObjects = objects

        val boundingBoxes: MutableList<RectF> = mutableListOf()
        val trackingIds: MutableList<Int> = mutableListOf()
        val labels: MutableList<String> = mutableListOf()

        for (detectedObject in detectedObjects) {


            /*var wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE)
            var display = wm.getDefaultDisplay()
            var size = Point()
            display.getSize(size)
            var width = size.x
            var height = size.y*/
            //val scaleX = (preview.width*preview.scaleX) / imageWidth.toFloat()
            //val scaleY = (preview.height*preview.scaleY) / imageHeight.toFloat()
            val scaleX = 1f//(1080) / imageWidth.toFloat()
            val scaleY = 1f//(1920) / imageHeight.toFloat()
            val scaledLeft = scaleX * detectedObject.boundingBox.left
            val scaledTop = scaleY * detectedObject.boundingBox.top
            val scaledRight = scaleX * detectedObject.boundingBox.right
            val scaledBottom = scaleY * detectedObject.boundingBox.bottom


            boundingBoxes.add(RectF(scaledLeft, scaledTop, scaledRight, scaledBottom))
            if (detectedObject.trackingId != null) {
                trackingIds.add(detectedObject.trackingId!!)
            }

            if (detectedObject.labels.isNotEmpty()) {
                labels.add(detectedObject.labels[0].text)
            }

            /*for (label in detectedObject.labels) {
                val text = label.text
                if (PredefinedCategory.FOOD == text) {
                    infoScreen.speak("food")
                }
                if (PredefinedCategory.FASHION_GOOD == text) {
                    infoScreen.speak("fashion")
                }
                if (PredefinedCategory.HOME_GOOD == text) {
                    infoScreen.speak("home")
                }
                if (PredefinedCategory.PLACE == text) {
                    infoScreen.speak("place")
                }
                if (PredefinedCategory.PLANT == text) {
                    infoScreen.speak("plant")
                }
                val confidence = label.confidence
                infoScreen.colorInfo(confidence.toString())
            }*/
        }

        infoScreen.colorInfo("${detectedObjects.size}")
        detectedListener?.onObjectsDetected(boundingBoxes, trackingIds, labels)
    }

    interface DetectorListener {
        fun onObjectsDetected(bounds: List<RectF>, trackingIds: List<Int>, detectedObjects: MutableList<String>)
    }
}