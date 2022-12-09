package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.*
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.*
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.MODE
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.StockfishApplication
import org.opencv.core.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class AnalyzerActivity : AppCompatActivity() {
    lateinit var binding: ActivityAnalyzerBinding
    private val analyzer = Analyzer(this)

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_GET = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyzerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnAnalyze.setOnClickListener {
            binding.tvAnalyzing.text = getString(R.string.analyzing)
            binding.loadingPanel.visibility = View.VISIBLE

            try {
                val command = "position fen ${analyzer.chessboard.toFen()}\neval\nisready\ngo movetime 3000\n"
                StockfishApplication.runCommandWithListener(command, MODE.ANALYZER, analyzer)
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }

        binding.btngrpNextPlayer.setOnCheckedChangeListener { _, id ->
            if (binding.btnWhite.id == id)
                analyzer.chessboard.nextPlayer = Player.WHITE
            else
                analyzer.chessboard.nextPlayer = Player.BLACK
        }

        binding.btnRotate.setOnClickListener {
            analyzer.chessboard.rotate()
            analyzer.onRecognitionCompleted(analyzer.chessboard)
        }

        when (intent.getStringExtra(MainActivity.MODE)) {
            MainActivity.TAKE_PHOTO -> startCamera()
            MainActivity.PICK_IMAGE -> openImageSelector()
            MainActivity.SANDBOX    -> {
                binding.dividerRotate.visibility = View.GONE
                binding.tvRotate.visibility = View.GONE
                binding.btnRotate.visibility = View.GONE
                binding.btnBlack.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.btnWhite.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                val board = Chessboard()
                board.setDefaultPosition()
                analyzer.onRecognitionCompleted(board)
            }
        }
    }

    private lateinit var photoURI: Uri

    private fun startCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.also {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("File error", "Couldn't create file")
                null
            }

            photoFile?.also {
                photoURI = FileProvider.getUriForFile(
                    this,
                    "hu.bme.aut.onlab.valivalter.chessanalyzer.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openImageSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }
    }

    private lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = this.cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                val contentResolver = applicationContext.contentResolver
                val inputStream: InputStream? = contentResolver.openInputStream(photoURI)
                val imageBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                cacheDir.deleteRecursively()

                thread {
                    analyzer.findBoardRecognizePieces(imageBitmap)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        else if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            data?.data.let {
                if (it != null) {
                    try {
                        val contentResolver = applicationContext.contentResolver
                        val inputStream: InputStream? = contentResolver.openInputStream(it)
                        val imageBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        thread {
                            analyzer.findBoardRecognizePieces(imageBitmap)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        else {
            finish()
        }
    }

    override fun onDestroy() {
        StockfishApplication.stopCommand()
        super.onDestroy()
    }
}