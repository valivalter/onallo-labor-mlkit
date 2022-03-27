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
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class PoseImageAnalyzer(val logic: DetectionTrackingLogic, val infoScreen : PoseInfoScreen) : ImageAnalysis.Analyzer {


    private lateinit var poseDetector : PoseDetector
    private lateinit var objectTracker: ObjectDetector
    private lateinit var imageLabeler: ImageLabeler

    init {
        //val poseOptions = AccuratePoseDetectorOptions.Builder()
        //    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        //    .build()
        //poseDetector = PoseDetection.getClient(poseOptions)

        val localModel = LocalModel.Builder()
            //.setAssetFilePath("lite-model_on_device_vision_classifier_landmarks_classifier_europe_V1_1.tflite")
            //.setAssetFilePath("birds.tflite")
            .setAssetFilePath("chess-pieces.tflite")
            .build()

        // Live detection and tracking
        val trackingOptions = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()  // Optional
            .setClassificationConfidenceThreshold(0.05f)
            .setMaxPerObjectLabelCount(3)
            .build()

        objectTracker = ObjectDetection.getClient(trackingOptions)


        /*val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.001f)
            .setMaxResultCount(5)
            .build()
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)*/
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
            objectTracker.process(image)
            //imageLabeler.process(image)
                .addOnSuccessListener { results ->
                    // Task completed successfully
                    //logic.updatePoseLandmarks(results.allPoseLandmarks)


                    logic.updateBoundingBoxes(results, imageProxy.width, imageProxy.height)

                    //logic.updateLabel(results)
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