package hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import hu.bme.aut.onlab.valivalter.chessanalyzer.Chessboard

class Analyzer {
// class Analyzer(private val activity: AnalyzerActivity) {
// if you want to see the analyzed images on the tiles 2/3

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

    fun analyze(bitmap: Bitmap, listener: RecognitionCompletedListener) {
        val board = Chessboard()
        val tileWidth = bitmap.width / 8

        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val tile = Bitmap.createBitmap(bitmap,
                    j * tileWidth,
                    i * tileWidth,
                    tileWidth,
                    tileWidth)
                val tileImage = InputImage.fromBitmap(tile, 0)
                imageLabeler.process(tileImage)
                    .addOnSuccessListener { results ->
                        Log.i("Analysis", "Analyzing tile: ($i $j)")
                        board.setTile(i, j, results[0].text.substring(0, 2))

                        // if you want to see the analyzed images on the tiles 3/3
                        //activity.findViewById<ImageButton>(Chessboard.boardRIDs[i][j]).setImageBitmap(tile)

                        if (i == 7 && j == 7) {
                            board.print()
                            Log.i("FEN", board.toFen())
                            listener.onRecognitionCompleted(board)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(e.toString(), "Error while labeling images")
                    }
            }
        }
    }
}