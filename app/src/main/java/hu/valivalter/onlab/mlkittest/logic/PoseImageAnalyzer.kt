package hu.valivalter.onlab.mlkittest.logic

import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class PoseImageAnalyzer(val logic: ImageLabelerLogic, val infoScreen : PoseInfoScreen) : ImageAnalysis.Analyzer {


    private lateinit var poseDetector : PoseDetector
    private lateinit var objectTracker: ObjectDetector
    private lateinit var imageLabeler: ImageLabeler

    init {
        //val poseOptions = AccuratePoseDetectorOptions.Builder()
        //    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        //    .build()
        //poseDetector = PoseDetection.getClient(poseOptions)



        // Live detection and tracking
        //val trackingOptions = ObjectDetectorOptions.Builder()
        //    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        //    .enableMultipleObjects()
        //    .enableClassification()  // Optional
        //    .build()
        //objectTracker = ObjectDetection.getClient(trackingOptions)



        val localModel = LocalModel.Builder()
            //.setAssetFilePath("lite-model_on_device_vision_classifier_landmarks_classifier_europe_V1_1.tflite")
            .setAssetFilePath("birds.tflite")
            .build()
        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f)
            .setMaxResultCount(5)
            .build()
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)
    }


    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            //poseDetector.process(image)
            //objectTracker.process(image)
            imageLabeler.process(image)
                .addOnSuccessListener { results ->
                    // Task completed successfully
                    //logic.updatePoseLandmarks(results.allPoseLandmarks)

                    //logic.updateBoundingBoxes(results, imageProxy.width, imageProxy.height)

                    logic.updateLabel(results)
                }
                .addOnFailureListener { e ->
                    infoScreen.colorInfo("{${e.toString()}")

                    // ...
                }.addOnCompleteListener {
                    mediaImage.close()
                    imageProxy.close()

                }
        }
    }
}