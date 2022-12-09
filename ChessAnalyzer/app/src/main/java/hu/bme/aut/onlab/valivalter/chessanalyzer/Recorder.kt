package hu.bme.aut.onlab.valivalter.chessanalyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

import android.graphics.Bitmap
import hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer.RecognitionCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer.Recognizer
import java.io.IOException
import android.graphics.Matrix
import androidx.camera.view.PreviewView
import hu.bme.aut.onlab.valivalter.chessanalyzer.chessboarddetector.ChessboardDetector
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.*
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.Analysis
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.AnalysisCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.MODE
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.StockfishApplication
import kotlin.math.abs

class Recorder(private val activity: RecordActivity) : ImageAnalysis.Analyzer, RecognitionCompletedListener, AnalysisCompletedListener {

    private val recognizer: Recognizer = Recognizer(this)
    private var currentChessboard = Chessboard().apply {
        this.setDefaultPosition()
    }
    private var previousChessboard = Chessboard().apply {
        this.setDefaultPosition()
    }
    var game = Game()
    private var stepCounter = 0

    private lateinit var imageProxy: ImageProxy

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        this.imageProxy = imageProxy
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            mediaImage.close()

            val imageBitmap = activity.findViewById<PreviewView>(R.id.cameraView).bitmap
            if (imageBitmap != null) {
                var boardBitmap = ChessboardDetector.findBoard(imageBitmap)
                if (boardBitmap != null) {
                    if (imageBitmap.width > imageBitmap.height) {
                        val matrix = Matrix()
                        matrix.postRotate(90F)
                        boardBitmap = Bitmap.createBitmap(boardBitmap, 0, 0, boardBitmap.width, boardBitmap.height, matrix, true)
                    }
                    recognizer.recognize(boardBitmap!!, withRulesOfChess = false)
                }
                else {
                    this.imageProxy.close()
                }
            }
            else {
                this.imageProxy.close()
            }
        }
        else {
            this.imageProxy.close()
        }
    }

    override fun onRecognitionCompleted(board: Chessboard) {
        val differences = currentChessboard.getDifferentTiles(board)
        when (differences.size) {
            2 -> { checkNormalStep(differences, board) }
            4 -> { checkCastling(board) }
            3 -> { checkEnPassant(differences, board) }
            else -> { this.imageProxy.close() }
        }
    }

    private fun checkNormalStep(differences: MutableList<Pair<Int, Int>>, board: Chessboard) {
        val currentStateOne = currentChessboard.getTile(differences[0].first, differences[0].second)
        var newStateOne = board.getTile(differences[0].first, differences[0].second)
        val currentStateTwo = currentChessboard.getTile(differences[1].first, differences[1].second)
        var newStateTwo = board.getTile(differences[1].first, differences[1].second)

        if ((currentStateOne != "em" && newStateOne == "em" && newStateTwo.first() == currentStateOne.first()) ||
            (currentStateTwo != "em" && newStateTwo == "em" && newStateOne.first() == currentStateTwo.first())) {

            previousChessboard = currentChessboard.copy()
            when (currentChessboard.nextPlayer) {
                Player.WHITE -> currentChessboard.nextPlayer = Player.BLACK
                Player.BLACK -> currentChessboard.nextPlayer = Player.WHITE
            }

            if (newStateOne == "em") {
                currentChessboard.setTile(differences[0].first, differences[0].second, newStateOne)
                currentChessboard.setTile(differences[1].first, differences[1].second, currentStateOne)

                // gyalog átalakulása
                if ((differences[1].first == 0 || differences[1].first == 7) && currentStateOne.last() == 'p') {
                    if (newStateTwo.last() == 'p' || newStateTwo.last() == 'k') {
                        newStateTwo = newStateTwo.dropLast(1) + 'q'
                    }
                    currentChessboard.setTile(differences[1].first, differences[1].second, newStateTwo)
                }
            }
            else {
                currentChessboard.setTile(differences[0].first, differences[0].second, currentStateTwo)
                currentChessboard.setTile(differences[1].first, differences[1].second, newStateTwo)

                // gyalog átalakulása
                if ((differences[0].first == 0 || differences[0].first == 7) && currentStateTwo.last() == 'p') {
                    if (newStateOne.last() == 'p' || newStateOne.last() == 'k') {
                        newStateOne = newStateOne.dropLast(1) + 'q'
                    }
                    currentChessboard.setTile(differences[0].first, differences[0].second, newStateOne)
                }
            }

            try {
                val command = "position fen ${currentChessboard.toFen()}\neval\nisready\ngo movetime 500\n"
                StockfishApplication.runCommandWithListener(command, MODE.RECORDER, this)
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
        else {
            this.imageProxy.close()
        }
    }

    private fun checkCastling(board: Chessboard) {
        val castlingType = board.didCastle(currentChessboard)
        if (castlingType != null) {
            previousChessboard = currentChessboard.copy()
            when (currentChessboard.nextPlayer) {
                Player.WHITE -> currentChessboard.nextPlayer = Player.BLACK
                Player.BLACK -> currentChessboard.nextPlayer = Player.WHITE
            }
            currentChessboard.castle(castlingType)

            try {
                val command = "position fen ${currentChessboard.toFen()}\neval\nisready\ngo movetime 500\n"
                StockfishApplication.runCommandWithListener(command, MODE.RECORDER, this)
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
        else {
            this.imageProxy.close()
        }
    }

    private fun checkEnPassant(differences: MutableList<Pair<Int, Int>>, board: Chessboard) {
        val blackPawnTile = differences.find { currentChessboard.getTile(it.first, it.second).first() == 'b' }
        val whitePawnTile = differences.find { currentChessboard.getTile(it.first, it.second).first() == 'w' }
        val emptyTile = differences.find { currentChessboard.getTile(it.first, it.second) == "em" }

        if (blackPawnTile != null && whitePawnTile != null && emptyTile != null) {
            var movingpiece: String? = null
            if (blackPawnTile.first == 3 && whitePawnTile.first == 3 && emptyTile.first == 2 &&
                abs(whitePawnTile.second-blackPawnTile.second) < 2 && emptyTile.second == blackPawnTile.second &&
                board.getTile(blackPawnTile.first, blackPawnTile.second) == "em" &&
                board.getTile(whitePawnTile.first, whitePawnTile.second) == "em" &&
                board.getTile(emptyTile.first, emptyTile.second).first() == 'w') {
                movingpiece = "wp"
            }
            else if (blackPawnTile.first == 4 && whitePawnTile.first == 4 && emptyTile.first == 5 &&
                abs(whitePawnTile.second-blackPawnTile.second) < 2 && emptyTile.second == whitePawnTile.second &&
                board.getTile(blackPawnTile.first, blackPawnTile.second) == "em" &&
                board.getTile(whitePawnTile.first, whitePawnTile.second) == "em" &&
                board.getTile(emptyTile.first, emptyTile.second).first() == 'b') {
                movingpiece = "bp"
            }

            if (movingpiece != null) {
                previousChessboard = currentChessboard.copy()
                when (currentChessboard.nextPlayer) {
                    Player.WHITE -> currentChessboard.nextPlayer = Player.BLACK
                    Player.BLACK -> currentChessboard.nextPlayer = Player.WHITE
                }
                currentChessboard.setTile(blackPawnTile.first, blackPawnTile.second, "em")
                currentChessboard.setTile(whitePawnTile.first, whitePawnTile.second, "em")
                currentChessboard.setTile(emptyTile.first, emptyTile.second, movingpiece)

                try {
                    val command = "position fen ${currentChessboard.toFen()}\neval\nisready\ngo movetime 500\n"
                    StockfishApplication.runCommandWithListener(command, MODE.RECORDER, this)
                }
                catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            else {
                this.imageProxy.close()
            }
        }
        else {
            this.imageProxy.close()
        }
    }

    override fun onAnalysisCompleted(analysis: Analysis) {
        val step = currentChessboard.copy()
        if (game.rounds.size <= stepCounter) {
            // mindenképp a fehér lépett, ha még nem megfelelő méretű a game.rounds lista
            game.rounds.add(Round(whiteStep = Step(step, analysis)))
        }
        else {
            // mindenképp a fekete lépett, ha már megfelelő méretű a game.rounds lista
            game.rounds[game.rounds.size-1].blackStep = Step(step, analysis)
            stepCounter++
        }

        val lastStep = game.getLastStep()
        val lastMovingPiece = Chessboard.getPieceFromStepLan(lastStep)
        activity.binding.tvLastStep.text = lastStep
        activity.binding.ivLastMovingPiece.setImageResource(Chessboard.mapStringsToResources[lastMovingPiece]!!)

        if ('#' !in lastStep) {
            this.imageProxy.close()
        }
        else {
            activity.binding.btnStopRecording.text = activity.getString(R.string.share_results)
        }
    }

    override fun onInvalidFen() {
        activity.binding.tvLastStep.text = activity.getString(R.string.invalid_step_check)
        currentChessboard = previousChessboard.copy()
        this.imageProxy.close()
    }
}