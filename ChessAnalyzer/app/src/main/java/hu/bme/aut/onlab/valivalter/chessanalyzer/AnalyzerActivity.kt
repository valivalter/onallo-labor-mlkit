package hu.bme.aut.onlab.valivalter.chessanalyzer

import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.ActivityAnalyzerBinding
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.FileProvider
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.Analyzer
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.View
import android.widget.ImageButton
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.AnalysisCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish.StockfishApplication
import java.io.*

class AnalyzerActivity : AppCompatActivity(), AnalysisCompletedListener {

    private lateinit var binding: ActivityAnalyzerBinding
    //private val analyzer = Analyzer(this) // if you want to see the analyzed images on the tiles 1/3
    private val analyzer = Analyzer()
    private var chessboard = Chessboard()

    companion object{
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyzerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnAnalyze.setOnClickListener {
            try {
                var command = "position fen ${chessboard.toFen()}\neval\nisready\ngo movetime 8000\n"
                StockfishApplication.runCommand(command)
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }

        binding.btngrpNextPlayer.setOnCheckedChangeListener { _, id ->
            if (binding.btnWhite.id == id)
                chessboard.setNextPlayer(Player.WHITE)
            else
                chessboard.setNextPlayer(Player.BLACK)
        }

        startCamera()
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

    private lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
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

                val matrix = Matrix()
                matrix.postRotate(90F)
                val rotatedImg = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.height, matrix, true)
                imageBitmap.recycle()
                val resizedBitmap = Bitmap.createBitmap(rotatedImg, 0, 0, rotatedImg.width, rotatedImg.width)

                analyzer.analyze(resizedBitmap, this)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCompletion(board: Chessboard) {
        chessboard = board
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val piece = board.getTile(i, j)
                val tile = findViewById<ImageButton>(Chessboard.boardRIDs[i][j])
                tile.setOnClickListener {
                    val actualPiece = board.getTile(i, j)
                    var newIndex = Chessboard.pieces.indexOf(actualPiece) + 1
                    if (newIndex == Chessboard.pieces.size) {
                        newIndex = 0
                    }
                    val newPiece = Chessboard.pieces[newIndex]
                    board.setTile(i, j, newPiece)
                    tile.setImageResource(Chessboard.mapStringsToResources[newPiece]!!)
                    board.print()
                }
                if (piece != "em") {
                    tile.setImageResource(Chessboard.mapStringsToResources[piece]!!)
                }
            }
        }
        binding.btnAnalyze.isEnabled = true
        binding.btnBlack.isEnabled = true
        binding.btnWhite.isEnabled = true
        binding.loadingPanel.visibility = View.INVISIBLE
    }
}