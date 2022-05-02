package hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic

import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageButton
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import hu.bme.aut.onlab.valivalter.chessanalyzer.Chessboard
import hu.bme.aut.onlab.valivalter.chessanalyzer.MainActivity

class Analyzer(private val activity: MainActivity, private val bitmap: Bitmap) {

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
                        Log.e("$i $j", "Chessboardkl")
                        board.setTile(i, j, results[0].text.substring(0, 2))


                        //activity.findViewById<ImageButton>(Chessboard.boardRIDs[i][j]).setImageBitmap(tile)


                        if (i == 7 && j == 7) {
                            board.print()
                            Log.e("FEN", board.toFen())
                            showBoard(board)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(e.toString(), "Error")
                    }
            }
        }
        return board
    }

    private fun showBoard(board: Chessboard) {
        //activity.binding.row0col2.setImageResource(R.drawable.black_pawn)
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val piece = board.getTile(i, j)
                val tile = activity.findViewById<ImageButton>(Chessboard.boardRIDs[i][j])
                tile.setOnClickListener {
                    val actualPiece = board.getTile(i, j)
                    var newIndex = Chessboard.pieces.indexOf(actualPiece) + 1
                    if (newIndex == Chessboard.pieces.size) {
                        newIndex = 0
                    }
                    val newPiece = Chessboard.pieces[newIndex]
                    board.setTile(i, j, newPiece)
                    tile.setImageResource(Chessboard.mapStringsToResources[newPiece]!!)
                    board.print()
                }
                if (piece != "em") {
                    tile.setImageResource(Chessboard.mapStringsToResources[piece]!!)
                }
            }
        }
    }
}