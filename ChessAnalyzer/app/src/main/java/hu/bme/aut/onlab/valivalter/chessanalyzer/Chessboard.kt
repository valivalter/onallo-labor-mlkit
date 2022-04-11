package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.util.Log

class Chessboard {
    private var board = Array(8) { Array(8) { "" } }

    fun getTile(i: Int, j: Int): String {
        return board[i][j]
    }

    fun setTile(i: Int, j: Int, piece: String) {
        board[i][j] = piece
    }

    fun print() {
        var boardText = ""
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                boardText += "[${board[i][j]}] "
            }
            boardText += "\n"
        }
        Log.e("CHESSBOARD", boardText)
    }
}