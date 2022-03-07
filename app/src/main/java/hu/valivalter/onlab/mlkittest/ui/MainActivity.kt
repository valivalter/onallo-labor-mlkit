package hu.valivalter.onlab.mlkittest.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import hu.valivalter.onlab.mlkittest.R

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import hu.valivalter.onlab.mlkittest.databinding.ActivityMainBinding
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val viewModel : MainViewModel by viewModels()

    lateinit var context : MainActivity
    private lateinit var binding : ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "PoseDetection"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = viewModel.tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","Initilization Failed!")
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel.infoText.observe(this, Observer<String> {
                infoText ->
            val tvInfo = binding.tvInfo
            tvInfo.setTextColor(Color.rgb(200, Random.nextInt(255), Random.nextInt(255)))
            tvInfo.setText(infoText)
        })


        //viewModel.preview = binding.viewFinder.getChildAt(0)
        viewModel.rectOverlay = binding.rectOverlay

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        viewModel.tts = TextToSpeech(this, this)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 360))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), viewModel.analiser)


            val viewFinder = binding.viewFinder

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis, preview
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }


        }, /*cameraExecutor*/ ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStop() {
        with(NotificationManagerCompat.from(this)) {
            cancelAll()
        }

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        if (viewModel.tts != null) {
            viewModel.tts!!.stop()
            viewModel.tts!!.shutdown()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                // finish()
            }
        }
    }
}