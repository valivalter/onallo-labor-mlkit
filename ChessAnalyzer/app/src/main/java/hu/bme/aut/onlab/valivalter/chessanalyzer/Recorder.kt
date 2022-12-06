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

    private var counter = 1

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        this.imageProxy = imageProxy
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            mediaImage.close()

            val imageBitmap = activity.findViewById<PreviewView>(R.id.cameraView).bitmap
            if (imageBitmap != null) {
                //thread {
                    val board = ChessboardDetector.findBoard(imageBitmap)
                    if (board != null) {
                        var boardBitmap = board.second
                        if (imageBitmap.width > imageBitmap.height) {
                            val matrix = Matrix()
                            matrix.postRotate(90F)
                            boardBitmap = Bitmap.createBitmap(boardBitmap, 0, 0, boardBitmap.width, boardBitmap.height, matrix, true)
                        }
                        recognizer.recognize(boardBitmap, withRulesOfChess = false)
                    }
                    else {
                        this.imageProxy.close()
                    }
                //}
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

        if (differences.size == 2) {
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

                    // pawn promotion
                    if ((differences[1].first == 0 || differences[1].first == 7) && currentStateOne.last() == 'p') {
                        if (newStateTwo.last() == 'p' || newStateTwo.last() == 'k') {
                            newStateTwo = newStateTwo.drop(1) + 'q'
                        }
                        currentChessboard.setTile(differences[1].first, differences[1].second, newStateTwo)
                    }
                }
                else {
                    currentChessboard.setTile(differences[0].first, differences[0].second, currentStateTwo)
                    currentChessboard.setTile(differences[1].first, differences[1].second, newStateTwo)

                    // pawn promotion
                    if ((differences[0].first == 0 || differences[0].first == 7) && currentStateTwo.last() == 'p') {
                        if (newStateOne.last() == 'p' || newStateOne.last() == 'k') {
                            newStateOne = newStateOne.drop(1) + 'q'
                        }
                        currentChessboard.setTile(differences[0].first, differences[0].second, newStateOne)
                    }
                }

                try {
                    val command = "position fen ${currentChessboard.toFen()}\neval\nisready\ngo movetime 2000\n"
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
        else if (differences.size == 4) { // castling
            if (Pair(0, 0) in differences && Pair(0, 2) in differences && Pair(0, 3) in differences && Pair(0, 4) in differences &&
                currentChessboard.getTile(0, 0) == "br" && board.getTile(0, 0) == "em" &&
                currentChessboard.getTile(0, 2) == "em" && board.getTile(0, 2).first() == 'b' &&
                currentChessboard.getTile(0, 3) == "em" && board.getTile(0, 3).first() == 'b' &&
                currentChessboard.getTile(0, 4) == "bk" && board.getTile(0, 4) == "em") {

                previousChessboard = currentChessboard.copy()
                when (currentChessboard.nextPlayer) {
                    Player.WHITE -> currentChessboard.nextPlayer = Player.BLACK
                    Player.BLACK -> currentChessboard.nextPlayer = Player.WHITE
                }

                currentChessboard.setTile(0, 0, "em")
                currentChessboard.setTile(0, 2, "bk")
                currentChessboard.setTile(0, 3, "bk")
                currentChessboard.setTile(0, 4, "em")

                try {
                    val command = "position fen ${currentChessboard.toFen()}\neval\nisready\ngo movetime 2000\n"
                    StockfishApplication.runCommandWithListener(command, MODE.RECORDER, this)
                }
                catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        else {
            this.imageProxy.close()
        }
    }

    override fun onAnalysisCompleted(analysis: Analysis) {
        val step = currentChessboard.copy()
        if (game.rounds.size <= stepCounter) {
            // mindenképp a fehér lépett, ha még nem megfelelő méretű a game.state lista
            game.rounds.add(Round(whiteStep = Step(step, analysis)))
        }
        else {
            // mindenképp a fekete lépett, ha már megfelelő méretű volt a game.state lista
            game.rounds[game.rounds.size-1].blackStep = Step(step, analysis)
            stepCounter++
        }

        activity.binding.tvLastStep.text = game.getLastStep()

        this.imageProxy.close()
    }

    override fun onInvalidFen() {
        activity.binding.tvLastStep.text = "Invalid step"
        currentChessboard = previousChessboard.copy()
        this.imageProxy.close()
    }
}