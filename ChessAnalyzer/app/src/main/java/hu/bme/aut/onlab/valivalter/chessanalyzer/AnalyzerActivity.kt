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
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.RecognitionCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.Network.LichessInteractor
import hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish.AnalysisCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish.MODE
import hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish.StockfishApplication
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.AnalysisResultBinding
import java.io.*

class AnalyzerActivity : AppCompatActivity(), RecognitionCompletedListener, AnalysisCompletedListener {

    private lateinit var binding: ActivityAnalyzerBinding
    //private val analyzer = Analyzer(this) // if you want to see the analyzed images on the tiles 1/3
    private val analyzer = Analyzer()
    private var chessboard = Chessboard()
    private lateinit var analysis: Analysis
    private lateinit var positionInfo: PositionInfo
    private val lichessInteractor = LichessInteractor()

    private var positionInfoReady = false
    private var stockfishAnalysisReady = false

    companion object{
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyzerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnAnalyze.setOnClickListener {
            val allFens = chessboard.getAllPossibleFens()
            lichessInteractor.getInfos(allFens, this::onGetPositionInfo, this::showError)

            try {
                var command = "position fen ${chessboard.toFen()}\neval\nisready\ngo movetime 8000\n"
                StockfishApplication.runCommandWithListener(command, MODE.ANALYZER, this)
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }

        binding.btngrpNextPlayer.setOnCheckedChangeListener { _, id ->
            if (binding.btnWhite.id == id)
                chessboard.nextPlayer = Player.WHITE
            else
                chessboard.nextPlayer = Player.BLACK
        }

        startCamera()
    }

    private fun onGetPositionInfo(positionInfos: List<PositionInfo>) {
        var white = 0
        var draws = 0
        var black = 0
        var topgames = mutableListOf<TopGame>()
        var opening: Opening? = null

        for (positionInfo in positionInfos) {
            white += positionInfo.white
            draws += positionInfo.draws
            black += positionInfo.black
            topgames.addAll(positionInfo.topGames)
            opening = positionInfo.opening
        }

        positionInfo = PositionInfo(white, draws, black, topgames, opening)
        positionInfoReady = true
        tryOpenDialog()

    }

    private fun showError(e: Throwable) {
        e.printStackTrace()
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

    override fun onRecognitionCompleted(board: Chessboard) {
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

        // for testing
        //chessboard.setDefaultPosition()
    }

    override fun onAnalysisCompleted(result: String) {
        Toast.makeText(this, result, Toast.LENGTH_LONG).show()

        if ("Best move" !in result) {
            analysis = Analysis(result)
        }
        else {
            analysis.bestMove = result

            stockfishAnalysisReady = true
            tryOpenDialog()
        }
    }

    private fun tryOpenDialog() {
        if (positionInfoReady && stockfishAnalysisReady) {
            var dialogBinding = AnalysisResultBinding.inflate(LayoutInflater.from(this))

            if (positionInfo.opening != null) {
                dialogBinding.tvTitle.text = "${positionInfo.opening!!.name} opening"
            }
            dialogBinding.tvStockfishResult.text = "Stockfish result: ${analysis.result}"
            dialogBinding.tvBestMove.text = analysis.bestMove
            //dialogBinding.tvWhite.text = "White wins: ${positionInfo.white}"
            //dialogBinding.tvBlack.text = "Black wins: ${positionInfo.black}"
            //dialogBinding.tvDraws.text = "Draws: ${positionInfo.draws}"

            /*val entries = listOf(
                PieEntry(positionInfo.white.toFloat(), "White wins"),
                PieEntry(positionInfo.black.toFloat(), "Black wins"),
                PieEntry(positionInfo.draws.toFloat(), "Draws")
            )
            val dataSet = PieDataSet(entries, "Winner stats")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            val data = PieData(dataSet)
            dialogBinding.pieMatches.data = data
            dialogBinding.pieMatches.invalidate()*/

            dialogBinding.tvDate.text = "(${positionInfo.topGames[0].month})"
            dialogBinding.tvPlayerWhite.text = "${positionInfo.topGames[0].white.name}⬜"
            dialogBinding.tvPlayerWhiteRating.text = positionInfo.topGames[0].white.rating.toString()
            dialogBinding.tvPlayerBlack.text = "${positionInfo.topGames[0].black.name}⬛"
            dialogBinding.tvPlayerBlackRating.text = positionInfo.topGames[0].black.rating.toString()
            //if (positionInfo.topGames[0].winner != null) {
            //
            //}

            MaterialDialog(this).show {
                customView(view = dialogBinding.root, scrollable = true)
            }

            positionInfoReady = false
            stockfishAnalysisReady = false
        }
    }
}