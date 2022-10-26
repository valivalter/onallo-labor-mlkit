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
import hu.bme.aut.onlab.valivalter.chessanalyzer.analyzerlogic.Analyzer
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import hu.bme.aut.onlab.valivalter.chessanalyzer.analyzerlogic.RecognitionCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.network.LichessInteractor
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.AnalysisCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.MODE
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.StockfishApplication
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.AnalysisResultBinding
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.*
import java.io.*
import com.github.mikephil.charting.formatter.ValueFormatter
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.AnalysisResultNoInfoBinding
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.PiecePickerBinding


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

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_GET = 2
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyzerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnAnalyze.setOnClickListener {
            binding.tvAnalyzing.text = "Analyzing"
            binding.loadingPanel.visibility = View.VISIBLE

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

        when (intent.getStringExtra(MainActivity.MODE)) {
            MainActivity.TAKE_PHOTO -> startCamera()
            MainActivity.PICK_IMAGE -> openImageSelector()
            MainActivity.SANDBOX    -> {
                val board = Chessboard()
                board.setDefaultPosition()
                onRecognitionCompleted(board)
            }
        }
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
            if (positionInfo.opening != null) {
                opening = positionInfo.opening
            }
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
        else if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            //val thumbnail: Bitmap = data.getParcelableExtra("data")

            data?.data.let {
                if (it != null) {
                    try {
                        val contentResolver = applicationContext.contentResolver
                        val inputStream: InputStream? = contentResolver.openInputStream(it)
                        val imageBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        val resizedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.width)

                        analyzer.analyze(resizedBitmap, this)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
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

                    val piecePickerDialog = MaterialDialog(this)
                    val piecePickerBinding = PiecePickerBinding.inflate(LayoutInflater.from(this))

                    val imageViewToPieceCode = mapOf(
                        piecePickerBinding.ivWhitePawn to "wp",
                        piecePickerBinding.ivWhiteKnight to "wn",
                        piecePickerBinding.ivWhiteBishop to "wb",
                        piecePickerBinding.ivWhiteRook to "wr",
                        piecePickerBinding.ivWhiteQueen to "wq",
                        piecePickerBinding.ivWhiteKing to "wk",
                        piecePickerBinding.ivBlackPawn to "bp",
                        piecePickerBinding.ivBlackKnight to "bn",
                        piecePickerBinding.ivBlackBishop to "bb",
                        piecePickerBinding.ivBlackRook to "br",
                        piecePickerBinding.ivBlackQueen to "bq",
                        piecePickerBinding.ivBlackKing to "bk",
                    )

                    imageViewToPieceCode.forEach { entry ->
                        entry.key.setOnClickListener {
                            board.setTile(i, j, entry.value)
                            tile.setImageResource(Chessboard.mapStringsToResources[entry.value]!!)
                            piecePickerDialog.dismiss()
                        }
                    }

                    piecePickerDialog.show {
                        customView(view = piecePickerBinding.root, scrollable = true)
                    }

                    /*val actualPiece = board.getTile(i, j)
                    var newIndex = Chessboard.pieces.indexOf(actualPiece) + 1
                    if (newIndex == Chessboard.pieces.size) {
                        newIndex = 0
                    }
                    val newPiece = Chessboard.pieces[newIndex]
                    board.setTile(i, j, newPiece)
                    tile.setImageResource(Chessboard.mapStringsToResources[newPiece]!!)
                    board.print()*/
                }
                if (piece != "em") {
                    tile.setImageResource(Chessboard.mapStringsToResources[piece]!!)
                }
            }
        }
        binding.loadingPanel.visibility = View.INVISIBLE
    }

    override fun onAnalysisCompleted(analysis: Analysis) {
        this.analysis = analysis
        stockfishAnalysisReady = true
        tryOpenDialog()
    }

    private fun tryOpenDialog() {
        if (positionInfoReady && stockfishAnalysisReady) {

            binding.loadingPanel.visibility = View.GONE

            // then there aren't any matches in the Lichess database
            if (positionInfo.white == 0 && positionInfo.black == 0 && positionInfo.draws == 0) {
                val dialogBinding = AnalysisResultNoInfoBinding.inflate(LayoutInflater.from(this))

                dialogBinding.tvStockfishResultNoInfo.text = "Stockfish result: ${analysis.result}"
                dialogBinding.tvBestMoveNoInfo.text = "Best move: ${analysis.bestMove}"
                dialogBinding.tvBestResponseNoInfo.text = "Best response: ${analysis.bestResponse}"

                MaterialDialog(this).show {
                    customView(view = dialogBinding.root, scrollable = true)
                }
            }
            else {
                val dialogBinding = AnalysisResultBinding.inflate(LayoutInflater.from(this))

                if (positionInfo.opening != null) {
                    dialogBinding.tvTitle.text = "${positionInfo.opening!!.name}"
                }

                val entries = listOf(
                    PieEntry(positionInfo.white.toFloat(), "White won"),
                    PieEntry(positionInfo.black.toFloat(), "Black won"),
                    PieEntry(positionInfo.draws.toFloat(), "Draw")
                )
                val dataSet = PieDataSet(entries, null)

                val formatter: ValueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }

                dataSet.colors = ColorTemplate.LIBERTY_COLORS.toList()//listOf(
                //ContextCompat.getColor(this, R.color.white),
                //ContextCompat.getColor(this, R.color.black),
                //ContextCompat.getColor(this, R.color.brown_light))
                val data = PieData(dataSet)
                data.setValueTextSize(15F)
                data.setValueFormatter(formatter)
                //data.setValueTextColor(ContextCompat.getColor(this, R.color.teal_700))
                dialogBinding.pieMatches.data = data
                dialogBinding.pieMatches.description.isEnabled = false
                //dialogBinding.pieMatches.legend.isWordWrapEnabled = true
                //dialogBinding.pieMatches.legend.textSize = 15F
                dialogBinding.pieMatches.legend.isEnabled = false
                dialogBinding.pieMatches.setEntryLabelColor(ContextCompat.getColor(this, R.color.black))
                dialogBinding.pieMatches.invalidate()

                if (positionInfo.topGames.size > 0) {
                    dialogBinding.tvDate.text = "(${positionInfo.topGames[0].month})"
                    dialogBinding.tvPlayerWhite.text = "⬜${positionInfo.topGames[0].white.name} (${positionInfo.topGames[0].white.rating})"
                    dialogBinding.tvPlayerBlack.text = "⬛${positionInfo.topGames[0].black.name} (${positionInfo.topGames[0].black.rating})"
                    when (positionInfo.topGames[0].winner) {
                        "white" -> dialogBinding.tvPlayerWhite.setTypeface(null, Typeface.BOLD)
                        "black" ->dialogBinding.tvPlayerBlack.setTypeface(null, Typeface.BOLD)
                    }
                }

                dialogBinding.tvStockfishResult.text = "Stockfish result: ${analysis.result}"
                dialogBinding.tvBestMove.text = "Best move: ${analysis.bestMove}"
                dialogBinding.tvBestResponse.text = "Best response: ${analysis.bestResponse}"

                MaterialDialog(this).show {
                    customView(view = dialogBinding.root, scrollable = true)
                }
            }

            positionInfoReady = false
            stockfishAnalysisReady = false
        }
    }
}