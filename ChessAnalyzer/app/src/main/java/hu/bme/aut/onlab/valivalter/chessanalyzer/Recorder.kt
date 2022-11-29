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
                        recognizer.recognize(boardBitmap)
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
            when (currentChessboard.nextPlayer) {
                Player.WHITE -> {
                    previousChessboard.nextPlayer = Player.WHITE
                    currentChessboard.nextPlayer = Player.BLACK
                }
                Player.BLACK -> {
                    previousChessboard.nextPlayer = Player.BLACK
                    currentChessboard.nextPlayer = Player.WHITE
                }
            }
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    previousChessboard.setTile(i, j, currentChessboard.getTile(i, j))
                }
            }


            for ((i, j) in differences) {
                // lehet hogy amúgy az kéne hogy arról a mezőről átrakja a bábut ami üres lett, és nem erről
                // a kapottról másolja be
                currentChessboard.setTile(i, j, board.getTile(i, j))
            }

            try {
                val command = "position fen ${board.toFen()}\neval\nisready\ngo movetime 4000\n"
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

    override fun onAnalysisCompleted(analysis: Analysis) {
        if (game.rounds.size <= stepCounter) {
            // mindenképp a fehér lépett, ha még nem megfelelő méretű a game.state lista
            val step = Chessboard()
            when (currentChessboard.nextPlayer) {
                Player.WHITE -> step.nextPlayer = Player.WHITE
                Player.BLACK -> step.nextPlayer = Player.BLACK
            }
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    step.setTile(i, j, currentChessboard.getTile(i, j))
                }
            }
            game.rounds.add(Round(whiteStep = Step(step, analysis)))
        }
        else {
            // mindenképp a fekete lépett, ha már megfelelő méretű volt a game.state lista
            val step = Chessboard()
            when (currentChessboard.nextPlayer) {
                Player.WHITE -> step.nextPlayer = Player.WHITE
                Player.BLACK -> step.nextPlayer = Player.BLACK
            }
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    step.setTile(i, j, currentChessboard.getTile(i, j))
                }
            }
            game.rounds[game.rounds.size-1].blackStep = Step(step, analysis)
            stepCounter++
        }

        activity.binding.tvLastStep.text = game.getLastStep()

        this.imageProxy.close()
    }

    override fun onInvalidFen() {
        activity.binding.tvLastStep.text = "Invalid position"

        // ide is deep copy kéne am
        currentChessboard = previousChessboard

        this.imageProxy.close()
    }
}