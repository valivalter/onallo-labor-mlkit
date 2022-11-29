package hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.Chessboard

class Recognizer(val listener: RecognitionCompletedListener) {
// class Analyzer(private val activity: AnalyzerActivity) {
// if you want to see the analyzed images on the tiles 2/3

    private val imageLabeler: ImageLabeler

    init {
        val localModel = LocalModel.Builder()
            .setAssetFilePath("chess-pieces-public-dataset.tflite")
            .build()

        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.001f)
            .setMaxResultCount(13)
            .build()
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)
    }

    fun recognize(bitmap: Bitmap) {
        //val board = Chessboard()
        var board: Array<Array<MutableList<Pair<String, Float?>>>> = Array(8) { Array(8) { mutableListOf() } }
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
                        //board.setTile(i, j, results[0].text.substring(0, 2))

                        val resultPairs = mutableListOf<Pair<String, Float?>>()
                        for (result in results) {
                            resultPairs.add(Pair(result.text.substring(0, 2), result.confidence))
                        }
                        board[i][j] = resultPairs

                        // if you want to see the analyzed images on the tiles 3/3
                        //activity.findViewById<ImageButton>(Chessboard.boardRIDs[i][j]).setImageBitmap(tile)

                        if (i == 7 && j == 7) {
                            //board.print()
                            //Log.i("FEN", board.toFen())
                            decide(board)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(e.toString(), "Error while labeling images")
                    }
            }
        }
    }

    private fun decide(board: Array<Array<MutableList<Pair<String, Float?>>>>) {
        var newBoard = setMinMaxOccurrences(board, "wk", 1, 1)
        newBoard = setMinMaxOccurrences(newBoard, "bk", 1, 1)
        newBoard = setMinMaxOccurrences(newBoard, "wq", 0, 1)
        newBoard = setMinMaxOccurrences(newBoard, "bq", 0, 1)
        newBoard = setMinMaxOccurrences(newBoard, "wr", 0, 2)
        newBoard = setMinMaxOccurrences(newBoard, "wn", 0, 2)
        newBoard = setMinMaxOccurrences(newBoard, "wb", 0, 2)
        newBoard = setMinMaxOccurrences(newBoard, "br", 0, 2)
        newBoard = setMinMaxOccurrences(newBoard, "bn", 0, 2)
        newBoard = setMinMaxOccurrences(newBoard, "bb", 0, 2)
        newBoard = setMinMaxOccurrences(newBoard, "wp", 0, 8)
        newBoard = setMinMaxOccurrences(newBoard, "bp", 0, 8)

        val chessboard = Chessboard()
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                chessboard.setTile(i, j, newBoard[i][j][0].first)
            }
        }
        listener.onRecognitionCompleted(chessboard)
    }

    private fun setMinMaxOccurrences(board: Array<Array<MutableList<Pair<String, Float?>>>>,
                                     piece: String, minOccurrence: Int, maxOccurrence: Int):
            Array<Array<MutableList<Pair<String, Float?>>>> {

        var deleteRemainingOccurrences = false
        var minOccurrence = minOccurrence

        for (n in 0 until maxOccurrence) {
            var maxConfidence = 0F
            var maxConfidencePosition: Pair<Int, Int>? = null
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    if (minOccurrence > 0) {
                        for (label in board[i][j]) {
                            if (label.first == piece && label.second != null && label.second!! > maxConfidence) {
                                maxConfidence = label.second!!
                                maxConfidencePosition = Pair(i, j)
                            }
                        }
                        minOccurrence -= 1
                    }
                    else {
                        val label = board[i][j][0]
                        if (label.first == piece && label.second != null && label.second!! > maxConfidence) {
                            maxConfidence = label.second!!
                            maxConfidencePosition = Pair(i, j)
                        }
                    }
                }
            }

            if (maxConfidencePosition != null) {
                val i = maxConfidencePosition.first
                val j = maxConfidencePosition.second
                board[i][j] = mutableListOf(Pair(piece, null))

                if (n == maxOccurrence - 1) {
                    deleteRemainingOccurrences = true
                }
            }
            else {
                break
            }
        }

        if (deleteRemainingOccurrences) {
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    board[i][j].removeIf {
                        it.first == piece && it.second != null
                    }
                }
            }
        }

        return board
    }
}