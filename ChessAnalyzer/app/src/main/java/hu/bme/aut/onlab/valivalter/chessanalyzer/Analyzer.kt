package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import hu.bme.aut.onlab.valivalter.chessanalyzer.chessboarddetector.ChessboardDetector
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.AnalysisResultBinding
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.AnalysisResultNoInfoBinding
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.InvalidPositionBinding
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.PiecePickerBinding
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.Chessboard
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.Opening
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.PositionInfo
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.TopGame
import hu.bme.aut.onlab.valivalter.chessanalyzer.network.LichessInteractor
import hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer.RecognitionCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer.Recognizer
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.Analysis
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.AnalysisCompletedListener

class Analyzer(private val activity: AnalyzerActivity) : RecognitionCompletedListener, AnalysisCompletedListener {
    private val recognizer = Recognizer(this)
    var chessboard = Chessboard()
    private lateinit var analysis: Analysis
    private lateinit var positionInfo: PositionInfo
    private val lichessInteractor = LichessInteractor(activity)

    fun findBoardRecognizePieces(imageBitmap: Bitmap) {
        var boardBitmap = ChessboardDetector.findBoard(imageBitmap)

        if (boardBitmap != null) {

            if (imageBitmap.width > imageBitmap.height) {
                val matrix = Matrix()
                matrix.postRotate(90F)
                boardBitmap = Bitmap.createBitmap(boardBitmap, 0, 0, boardBitmap.width, boardBitmap.height, matrix, true)
            }
            imageBitmap.recycle()
            recognizer.recognize(boardBitmap!!)
        }
        else {
            activity.runOnUiThread {
                Toast.makeText(activity, "Couldn't detect the chessboard", Toast.LENGTH_LONG).show()
                activity.finish()
            }
        }
    }

    override fun onRecognitionCompleted(board: Chessboard) {
        chessboard = board
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val piece = board.getTile(i, j)
                val tile = activity.findViewById<ImageButton>(Chessboard.boardRIDs[i][j])
                tile.setOnClickListener {

                    val piecePickerDialog = MaterialDialog(activity)
                    val piecePickerBinding = PiecePickerBinding.inflate(LayoutInflater.from(activity))

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
                }
                tile.setImageResource(Chessboard.mapStringsToResources[piece]!!)
            }
        }
        activity.binding.loadingPanel.visibility = View.INVISIBLE
    }

    override fun onAnalysisCompleted(analysis: Analysis) {
        this.analysis = analysis
        val allFens = chessboard.getAllPossibleFens()
        lichessInteractor.getInfos(allFens, this::onGetPositionInfo, this::showError)
    }

    override fun onInvalidFen() {
        val invalidPositionBinding = InvalidPositionBinding.inflate(LayoutInflater.from(activity))
        MaterialDialog(activity).show {
            customView(view = invalidPositionBinding.root, scrollable = true)
        }
        activity.binding.loadingPanel.visibility = View.INVISIBLE
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

        activity.runOnUiThread {
            openResultDialog()
        }
    }

    private fun showError(e: Throwable) {
        activity.runOnUiThread {
            if (e is NullPointerException) {
                // Akkor fordul elő ez a hiba a Lichess API-nál, ha a soron következőnek megadott játékos már nyert
                val checkmateDialogBinding = AnalysisResultNoInfoBinding.inflate(LayoutInflater.from(activity))
                checkmateDialogBinding.tvStockfishResultNoInfo.text = activity.getString(R.string.stockfish_checkmate)
                checkmateDialogBinding.tvBestMoveNoInfo.text = activity.getString(R.string.no_best_move)
                checkmateDialogBinding.tvExpectedResponseNoInfo.text = activity.getString(R.string.no_expected_response)

                MaterialDialog(activity).show {
                    customView(view = checkmateDialogBinding.root, scrollable = true)
                }
            }
            else {
                // Nem megfelelő internetelérés
                val dialogBinding = AnalysisResultNoInfoBinding.inflate(LayoutInflater.from(activity))
                dialogBinding.tvStockfishResultNoInfo.text = "Stockfish result: ${analysis.result}"
                dialogBinding.tvBestMoveNoInfo.text = "Best move: ${analysis.bestMove}"
                dialogBinding.tvExpectedResponseNoInfo.text = "Expected response: ${analysis.expectedResponse}"
                dialogBinding.tvNoInfo.text = activity.getString(R.string.network_error_message)

                MaterialDialog(activity).show {
                    customView(view = dialogBinding.root, scrollable = true)
                }
            }
            activity.binding.loadingPanel.visibility = View.INVISIBLE
        }
    }

    private fun openResultDialog() {
        activity.binding.loadingPanel.visibility = View.INVISIBLE

        if (positionInfo.white == 0 && positionInfo.black == 0 && positionInfo.draws == 0) {
            val dialogBinding = AnalysisResultNoInfoBinding.inflate(LayoutInflater.from(activity))

            dialogBinding.tvStockfishResultNoInfo.text = "Stockfish result: ${analysis.result}"
            dialogBinding.tvBestMoveNoInfo.text = "Best move: ${analysis.bestMove}"
            dialogBinding.tvExpectedResponseNoInfo.text = "Expected response: ${analysis.expectedResponse}"

            MaterialDialog(activity).show {
                customView(view = dialogBinding.root, scrollable = true)
            }
        }
        else {
            val dialogBinding = AnalysisResultBinding.inflate(LayoutInflater.from(activity))

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

            dataSet.colors = ColorTemplate.LIBERTY_COLORS.toList()
            val data = PieData(dataSet)
            data.setValueTextSize(15F)
            data.setValueFormatter(formatter)
            dialogBinding.pieMatches.data = data
            dialogBinding.pieMatches.description.isEnabled = false
            dialogBinding.pieMatches.legend.isEnabled = false
            dialogBinding.pieMatches.setEntryLabelColor(ContextCompat.getColor(activity, R.color.black))
            dialogBinding.pieMatches.invalidate()

            if (positionInfo.topGames.size > 0) {
                if (positionInfo.topGames[0].month != null) {
                    dialogBinding.tvDate.text = "(${positionInfo.topGames[0].month}"
                }
                else {
                    dialogBinding.tvDate.text = activity.getString(R.string.no_date)
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

            MaterialDialog(activity).show {
                customView(view = dialogBinding.root, scrollable = true)
            }
        }
    }
}