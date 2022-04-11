package hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import hu.bme.aut.onlab.valivalter.chessanalyzer.Chessboard

class Analyzer(private val bitmap: Bitmap) {

    private val tileWidth = bitmap.width / 8
    private val imageLabeler: ImageLabeler


    init {
        val localModel = LocalModel.Builder()
            .setAssetFilePath("chess-pieces-public-dataset.tflite")
            .build()

        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.001f)
            .setMaxResultCount(5)
            .build()
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)
    }

    fun analyze(): Chessboard {

        var board = Chessboard()

        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val tile = Bitmap.createBitmap(bitmap, i * tileWidth, j * tileWidth, tileWidth, tileWidth)
                val tileImage = InputImage.fromBitmap(tile, 0)
                imageLabeler.process(tileImage)
                    .addOnSuccessListener { results ->

                        board.setTile(i, j, results[0].text)
                        if (i == 7 && j == 7)
                            board.print()
                    }
                    .addOnFailureListener { e ->
                        Log.e(e.toString(), "Error")
                    }
            }
        }
        return board
    }
}