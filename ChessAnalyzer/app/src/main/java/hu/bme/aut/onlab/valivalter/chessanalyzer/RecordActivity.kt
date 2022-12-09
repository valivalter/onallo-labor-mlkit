package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.ActivityRecordBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log
import android.view.WindowManager
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecordActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Recorder"
    }

    lateinit var binding : ActivityRecordBinding
    private lateinit var cameraExecutor: ExecutorService
    private val recorder = Recorder(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.btnStopRecording.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm")
            val dateTime = LocalDateTime.now().format(formatter)
            val shareIntent = Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Recorded game ended at $dateTime\n${recorder.game}")
                putExtra(Intent.EXTRA_TITLE, "Share the result")
            }, null)
            startActivity(shareIntent)
            finish()
        }

        startCamera()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), recorder)

            val viewFinder = binding.cameraView

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}