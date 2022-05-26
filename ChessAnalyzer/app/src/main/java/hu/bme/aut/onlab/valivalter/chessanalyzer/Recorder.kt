package hu.bme.aut.onlab.valivalter.chessanalyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

import android.graphics.Bitmap
import android.os.Environment
import androidx.camera.view.PreviewView
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.AnalysisCompletedListener
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.Analyzer
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Recorder(private val activity: RecordActivity) : ImageAnalysis.Analyzer, AnalysisCompletedListener {

    private val analyzer: Analyzer = Analyzer()
    private var chessboard = Chessboard()
    private var fileName: String? = null

    private lateinit var imageProxy: ImageProxy

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val bitmap = activity.findViewById<PreviewView>(R.id.viewFinder).bitmap
            if (bitmap != null) {
                val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.width)

                analyzer.analyze(resizedBitmap, this)
            }

            mediaImage.close()
            // intentionally not closing imageProxy
            this.imageProxy = imageProxy
        }
    }

    override fun onCompletion(board: Chessboard) {
        if (fileName == null) {
            fileName = "game_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}.txt"
        }
        if (chessboard.toFen() != board.toFen()) {
            chessboard = board
            writeFile(fileName!!, "${chessboard.toFen().dropLast(10)}\n")
            // drops the constant " w - - 0 1" string
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
}