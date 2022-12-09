package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.ActivityMainBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var ocvLoader: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> { }
                else -> {
                    binding.btnTakePhoto.isEnabled = false
                    binding.btnAnalyzeImage.isEnabled = false
                    binding.btnRecorder.isEnabled = false
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, ocvLoader)
        } else {
            ocvLoader.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 11
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        const val MODE = "MODE"
        const val TAKE_PHOTO = "TAKE_PHOTO"
        const val PICK_IMAGE = "PICK_IMAGE"
        const val SANDBOX = "SANDBOX"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnSandbox.setOnClickListener {
            val intent = Intent(this, AnalyzerActivity::class.java)
            intent.putExtra(MODE, SANDBOX)
            startActivity(intent)
        }

        binding.btnTakePhoto.setOnClickListener {
            val intent = Intent(this, AnalyzerActivity::class.java)
            intent.putExtra(MODE, TAKE_PHOTO)
            startActivity(intent)
        }

        binding.btnAnalyzeImage.setOnClickListener {
            val intent = Intent(this, AnalyzerActivity::class.java)
            intent.putExtra(MODE, PICK_IMAGE)
            startActivity(intent)
        }

        binding.btnRecorder.setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}