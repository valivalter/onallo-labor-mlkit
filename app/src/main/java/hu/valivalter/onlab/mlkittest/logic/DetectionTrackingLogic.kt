package hu.valivalter.onlab.mlkittest.logic

import android.graphics.RectF
import android.view.View
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.defaults.PredefinedCategory

class DetectionTrackingLogic(val infoScreen : PoseInfoScreen): Logic {

    var detectedObjects : List<DetectedObject> = listOf()
    var detectedListener : DetectorListener? = null

    fun updateBoundingBoxes(objects : List<DetectedObject>, imageWidth: Int, imageHeight: Int) {
        detectedObjects = objects

        val boundingBoxes: MutableList<RectF> = mutableListOf()
        val trackingIds: MutableList<Int> = mutableListOf()

        for (detectedObject in detectedObjects) {

            //val scaleX = (preview.width*preview.scaleX) / imageWidth.toFloat()
            //val scaleY = (preview.height*preview.scaleY) / imageHeight.toFloat()
            val scaleX = (1440) / imageWidth.toFloat()
            val scaleY = (2560) / imageHeight.toFloat()
            val scaledLeft = scaleX * detectedObject.boundingBox.left
            val scaledTop = scaleY * detectedObject.boundingBox.top
            val scaledRight = scaleX * detectedObject.boundingBox.right
            val scaledBottom = scaleY * detectedObject.boundingBox.bottom


            boundingBoxes.add(RectF(scaledLeft, scaledTop, scaledRight, scaledBottom))
            if (detectedObject.trackingId != null) {
                trackingIds.add(detectedObject.trackingId!!)
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

        detectedListener?.onObjectsDetected(boundingBoxes, trackingIds)
    }

    interface DetectorListener {
        fun onObjectsDetected(bounds: List<RectF>, trackingIds: List<Int>)
    }
}