package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import hu.bme.aut.onlab.valivalter.chessanalyzer.chessboarddetector.ChessboardDetector
import hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer.Recognizer
import hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer.RecognitionCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.*
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.*
import hu.bme.aut.onlab.valivalter.chessanalyzer.network.LichessInteractor
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.Analysis
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.AnalysisCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.MODE
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.StockfishApplication
import org.opencv.core.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread


class AnalyzerActivity : AppCompatActivity(), RecognitionCompletedListener, AnalysisCompletedListener {

    private lateinit var binding: ActivityAnalyzerBinding
    //private val analyzer = Analyzer(this) // if you want to see the analyzed images on the tiles 1/3
    private val recognizer = Recognizer(this)
    private var chessboard = Chessboard()
    private lateinit var analysis: Analysis
    private lateinit var positionInfo: PositionInfo
    private val lichessInteractor = LichessInteractor(this)

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

            try {
                var command = "position fen ${chessboard.toFen()}\neval\nisready\ngo movetime 6000\n"
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

        binding.btnRotate.setOnClickListener {
            chessboard.rotate()
            onRecognitionCompleted(chessboard)
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
                onRecognitionCompleted(board)
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

                thread {
                    val board = ChessboardDetector.findBoard(imageBitmap)

                    if (board != null) {
                        val cornersBitmap = board.first
                        var boardBitmap = board.second
                        runOnUiThread {
                            binding.ivCorners.setImageBitmap(cornersBitmap)
                            binding.ivCropped.setImageBitmap(boardBitmap)
                        }
                        val matrix = Matrix()
                        matrix.postRotate(90F)
                        boardBitmap = Bitmap.createBitmap(boardBitmap, 0, 0, boardBitmap.width, boardBitmap.height, matrix, true)
                        imageBitmap.recycle()
                        //val resizedBitmap = Bitmap.createBitmap(rotatedImg, 0, 0, rotatedImg.width, rotatedImg.width)

                        recognizer.recognize(boardBitmap)
                    }
                    else {
                        runOnUiThread {
                            Toast.makeText(this, "Couldn't detect the chessboard", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
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
                        var imageBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        thread {
                            val board = ChessboardDetector.findBoard(imageBitmap)

                            if (board != null) {
                                val cornersBitmap = board.first
                                var boardBitmap = board.second
                                runOnUiThread {
                                    //binding.ivCorners.setImageBitmap(cornersBitmap)
                                    //binding.ivCropped.setImageBitmap(boardBitmap)
                                }
                                if (imageBitmap.width > imageBitmap.height) {
                                    val matrix = Matrix()
                                    matrix.postRotate(90F)
                                    boardBitmap = Bitmap.createBitmap(boardBitmap!!, 0, 0, boardBitmap!!.width, boardBitmap!!.height, matrix, true)
                                }
                                recognizer.recognize(boardBitmap!!)
                                /*val resizedBitmap: Bitmap
                                if (imageBitmap.width > imageBitmap.height) {
                                    val matrix = Matrix()
                                    matrix.postRotate(90F)
                                    val rotatedImg = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.height, matrix, true)
                                    imageBitmap.recycle()
                                    resizedBitmap = Bitmap.createBitmap(rotatedImg, 0, 0, rotatedImg.width, rotatedImg.width)
                                }
                                else {
                                    resizedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.width)
                                }
                                //analyzer.analyze(resizedBitmap, this)*/
                            }
                            else {
                                runOnUiThread {
                                    Toast.makeText(this, "Couldn't detect the chessboard", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                            }
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
                        piecePickerBinding.ivEmpty to "em"
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
                tile.setImageResource(Chessboard.mapStringsToResources[piece]!!)
            }
        }
        binding.loadingPanel.visibility = View.INVISIBLE
    }

    override fun onAnalysisCompleted(analysis: Analysis) {
        this.analysis = analysis
        //stockfishAnalysisReady = true

        val allFens = chessboard.getAllPossibleFens()
        lichessInteractor.getInfos(allFens, this::onGetPositionInfo, this::showError)
    }

    override fun onInvalidFen() {
        val invalidPositionBinding = InvalidPositionBinding.inflate(LayoutInflater.from(this))
        MaterialDialog(this).show {
            customView(view = invalidPositionBinding.root, scrollable = true)
        }
        binding.loadingPanel.visibility = View.INVISIBLE
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
        //positionInfoReady = true

        openResultDialog()
    }

    private fun showError(e: Throwable) {
        // kamu, hiányzó királyra is ez van
        // This happens when the player set to next player has already won the game
        if (e is NullPointerException) {
            val checkmateDialogBinding = AnalysisResultNoInfoBinding.inflate(LayoutInflater.from(this))
            checkmateDialogBinding.tvStockfishResultNoInfo.text = "Stockfish result: checkmate"
            checkmateDialogBinding.tvBestMoveNoInfo.text = "Best move: -"
            checkmateDialogBinding.tvExpectedResponseNoInfo.text = "Expected response: -"

            MaterialDialog(this).show {
                customView(view = checkmateDialogBinding.root, scrollable = true)
            }
        }
        else {
            //val networkErrorBinding = NetworkErrorBinding.inflate(LayoutInflater.from(this))
            //runOnUiThread {
            //    MaterialDialog(this).show {
            //        customView(view = networkErrorBinding.root, scrollable = true)
            //    }
            //}

            val dialogBinding = AnalysisResultNoInfoBinding.inflate(LayoutInflater.from(this))
            dialogBinding.tvStockfishResultNoInfo.text = "Stockfish result: ${analysis.result}"
            dialogBinding.tvBestMoveNoInfo.text = "Best move: ${analysis.bestMove}"
            dialogBinding.tvExpectedResponseNoInfo.text = "Expected response: ${analysis.expectedResponse}"
            dialogBinding.tvNoInfo.text = "Check your connection for more infos!"

            MaterialDialog(this).show {
                customView(view = dialogBinding.root, scrollable = true)
            }
        }
        binding.loadingPanel.visibility = View.INVISIBLE
    }

    private fun openResultDialog() {
        //if (positionInfoReady && stockfishAnalysisReady) {

            binding.loadingPanel.visibility = View.INVISIBLE

            // then there aren't any matches in the Lichess database
            if (positionInfo.white == 0 && positionInfo.black == 0 && positionInfo.draws == 0) {
                val dialogBinding = AnalysisResultNoInfoBinding.inflate(LayoutInflater.from(this))

                dialogBinding.tvStockfishResultNoInfo.text = "Stockfish result: ${analysis.result}"
                dialogBinding.tvBestMoveNoInfo.text = "Best move: ${analysis.bestMove}"
                dialogBinding.tvExpectedResponseNoInfo.text = "Expected response: ${analysis.expectedResponse}"

                MaterialDialog(this).show {
                    customView(view = dialogBinding.root, scrollable = true)
                }
            }
            else {
                val dialogBinding = AnalysisResultBinding.inflate(LayoutInflater.from(this))

                if (positionInfo.opening != null) {
                    if (positionInfo.opening!!.name.length < 13) {
                        dialogBinding.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42F)
                    }
                    else if (positionInfo.opening!!.name.length < 23) {
                        dialogBinding.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35F)
                    }
                    else {
                        dialogBinding.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28F)
                    }
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
                    if (positionInfo.topGames[0].month != null) {
                        dialogBinding.tvDate.text = "(${positionInfo.topGames[0].month}"
                    }
                    else {
                        dialogBinding.tvDate.text = "(Missing date"
                    }
                    dialogBinding.tvPlayerWhite.text = "⚪ ${positionInfo.topGames[0].white.name} (${positionInfo.topGames[0].white.rating})"
                    dialogBinding.tvPlayerBlack.text = "⚫ ${positionInfo.topGames[0].black.name} (${positionInfo.topGames[0].black.rating})"
                    when (positionInfo.topGames[0].winner) {
                        "white" -> {
                            dialogBinding.tvPlayerWhite.setTypeface(null, Typeface.BOLD)
                            dialogBinding.tvDate.text = dialogBinding.tvDate.text.toString() + ", white won)"
                        }
                        "black" -> {
                            dialogBinding.tvPlayerBlack.setTypeface(null, Typeface.BOLD)
                            dialogBinding.tvDate.text = dialogBinding.tvDate.text.toString() + ", black won)"
                        }
                        null -> {
                            dialogBinding.tvDate.text = dialogBinding.tvDate.text.toString() + ", draw)"
                        }
                    }
                }

                dialogBinding.tvStockfishResult.text = "Stockfish result: ${analysis.result}"
                dialogBinding.tvBestMove.text = "Best move: ${analysis.bestMove}"
                dialogBinding.tvExpectedResponse.text = "Expected response: ${analysis.expectedResponse}"

                MaterialDialog(this).show {
                    customView(view = dialogBinding.root, scrollable = true)
                }
            }

            //positionInfoReady = false
            //stockfishAnalysisReady = false
        //}
    }

    override fun onDestroy() {
        StockfishApplication.stopCommand()
        super.onDestroy()
    }
}