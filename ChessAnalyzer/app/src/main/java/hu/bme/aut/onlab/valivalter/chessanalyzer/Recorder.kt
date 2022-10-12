package hu.bme.aut.onlab.valivalter.chessanalyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

import android.graphics.Bitmap
import android.os.Environment
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.RecognitionCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.Analyzer
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.core.graphics.rotationMatrix
import hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish.AnalysisCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish.MODE
import hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish.StockfishApplication


class Recorder(private val activity: RecordActivity) : ImageAnalysis.Analyzer, RecognitionCompletedListener, AnalysisCompletedListener {

    private val analyzer: Analyzer = Analyzer()
    private var currentChessboard: Chessboard? = null
    private lateinit var game: Game
    private var stepCounter = 0
    private var fileName: String? = null

    private lateinit var imageProxy: ImageProxy

    private var counter = 0

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        /*val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val bitmap = activity.findViewById<PreviewView>(R.id.viewFinder).bitmap
            if (bitmap != null) {
                val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.width)

                analyzer.analyze(resizedBitmap, this)
            }

            mediaImage.close()
            // intentionally not closing imageProxy
            this.imageProxy = imageProxy
        }*/
        var bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla)
        when (counter) {
            0 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla)
            1 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla01)
            2 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla02)
            3 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla03)
            4 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla04)
            5 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla05)
            6 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla06)
            7 -> bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.sakktabla07)
        }
        val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.width)
        analyzer.analyze(resizedBitmap, this)

        counter++
        this.imageProxy = imageProxy
    }

    override fun onRecognitionCompleted(board: Chessboard) {
        if (fileName == null) {
            fileName = "game_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}.txt"
        }
        if (currentChessboard == null) {
            currentChessboard = board
            game = Game(board)
            //writeFile(fileName!!, "${currentChessboard!!.toFen().dropLast(10)}\n")
        }
        else {
            if (!currentChessboard!!.equals(board) && currentChessboard!!.isDifferenceOneMove(board)) {
                currentChessboard = board

                try {
                    var command = "position fen ${board.toFen()}\neval\nisready\ngo movetime 4000\n"
                    StockfishApplication.runCommandWithListener(command, MODE.RECORDER, this)






                }
                catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        imageProxy.close()
    }

    private fun writeFile(fileName: String, data: String) {
        val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        try {
            file.appendText(data)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onAnalysisCompleted(result: String) {
        if (game.rounds.size <= stepCounter) {
            if (currentChessboard!!.nextPlayer == Player.BLACK) {
                game.rounds.add(Round(blackStep = Step(currentChessboard!!, Analysis(result))))
                stepCounter++
            }
            else if (currentChessboard!!.nextPlayer == Player.WHITE) {
                game.rounds.add(Round(whiteStep = Step(currentChessboard!!, Analysis(result))))
            }
        }
        else {
            // mindenképp fekete lépett, ha már megfelelő méretű volt a game.state lista
            game.rounds[game.rounds.size-1].blackStep = Step(currentChessboard!!, Analysis(result))
            stepCounter++
        }

        Toast.makeText(activity.applicationContext, game.getLastStep(), Toast.LENGTH_LONG).show()

        ///////////////// TESZTELÉSRE /////////////////////
        if (stepCounter == 3) {
            val gameString = game.toString()
        }

        //writeFile(fileName!!, "${currentChessboard!!.getLastMoveSan(board)}\n")
        // drops the constant " w - - 0 1" string
    }
}