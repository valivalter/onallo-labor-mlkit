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
    private val imageLabeler: ImageLabeler

    init {
        val localModel = LocalModel.Builder()
            .setAssetFilePath("chess-piece-recognizer.tflite")
            .build()

        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.001f)
            .setMaxResultCount(13)
            .build()
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)
    }

    fun recognize(bitmap: Bitmap, withRulesOfChess: Boolean = true) {
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

                        val resultPairs = mutableListOf<Pair<String, Float?>>()
                        for (result in results) {
                            resultPairs.add(Pair(result.text.substring(0, 2), result.confidence))
                        }
                        board[i][j] = resultPairs

                        if (i == 7 && j == 7) {
                            if (withRulesOfChess) {
                                board = useRulesOfChess(board)
                            }

                            val chessboard = Chessboard()
                            for (k in 0 until 8) {
                                for (l in 0 until 8) {
                                    chessboard.setTile(k, l, board[k][l][0].first)
                                }
                            }
                            listener.onRecognitionCompleted(chessboard)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(e.toString(), "Error while labeling images")
                    }
            }
        }
    }

    private fun useRulesOfChess(board: Array<Array<MutableList<Pair<String, Float?>>>>):
            Array<Array<MutableList<Pair<String, Float?>>>> {
        var newBoard = setMinMaxOccurrences(board, "wk", 1, 1)
        newBoard = setMinMaxOccurrences(newBoard, "bk", 1, 1)

        do {
            val chessboardOld = Chessboard()
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    chessboardOld.setTile(i, j, newBoard[i][j][0].first)
                }
            }
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
            val chessboardNew = Chessboard()
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    chessboardNew.setTile(i, j, newBoard[i][j][0].first)
                }
            }
        } while (!chessboardNew.equals(chessboardOld))

        return newBoard
    }

    private fun setMinMaxOccurrences(board: Array<Array<MutableList<Pair<String, Float?>>>>,
                                     piece: String, minOccurrence: Int, maxOccurrence: Int):
            Array<Array<MutableList<Pair<String, Float?>>>> {

        var deleteRemainingOccurrences = false
        val minOccurrence = minOccurrence

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
                    }
                    else {
                        if (board[i][j].size > 0) {
                            val label = board[i][j][0]
                            if (label.first == piece && label.second != null && label.second!! > maxConfidence) {
                                maxConfidence = label.second!!
                                maxConfidencePosition = Pair(i, j)
                            }
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