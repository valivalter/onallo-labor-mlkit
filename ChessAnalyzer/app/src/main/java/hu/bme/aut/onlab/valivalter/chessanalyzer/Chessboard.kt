package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.util.Log
import java.util.HashMap

enum class Player {
    WHITE, BLACK
}

class Chessboard {
    private var board = Array(8) { Array(8) { "" } }
    private var nextPlayer = Player.WHITE

    companion object {

        val boardRIDs = arrayOf(
            intArrayOf(R.id.row0col0, R.id.row0col1, R.id.row0col2, R.id.row0col3, R.id.row0col4, R.id.row0col5, R.id.row0col6, R.id.row0col7),
            intArrayOf(R.id.row1col0, R.id.row1col1, R.id.row1col2, R.id.row1col3, R.id.row1col4, R.id.row1col5, R.id.row1col6, R.id.row1col7),
            intArrayOf(R.id.row2col0, R.id.row2col1, R.id.row2col2, R.id.row2col3, R.id.row2col4, R.id.row2col5, R.id.row2col6, R.id.row2col7),
            intArrayOf(R.id.row3col0, R.id.row3col1, R.id.row3col2, R.id.row3col3, R.id.row3col4, R.id.row3col5, R.id.row3col6, R.id.row3col7),
            intArrayOf(R.id.row4col0, R.id.row4col1, R.id.row4col2, R.id.row4col3, R.id.row4col4, R.id.row4col5, R.id.row4col6, R.id.row4col7),
            intArrayOf(R.id.row5col0, R.id.row5col1, R.id.row5col2, R.id.row5col3, R.id.row5col4, R.id.row5col5, R.id.row5col6, R.id.row5col7),
            intArrayOf(R.id.row6col0, R.id.row6col1, R.id.row6col2, R.id.row6col3, R.id.row6col4, R.id.row6col5, R.id.row6col6, R.id.row6col7),
            intArrayOf(R.id.row7col0, R.id.row7col1, R.id.row7col2, R.id.row7col3, R.id.row7col4, R.id.row7col5, R.id.row7col6, R.id.row7col7)
        )

        val pieces = arrayListOf("em", "wr", "wn", "wb", "wk", "wq", "wp", "br", "bn", "bb", "bk", "bq", "bp")

        var mapStringsToResources: HashMap<String, Int> = HashMap<String, Int>().also {
            it["em"] = android.R.color.transparent
            it["wr"] = R.drawable.white_rook
            it["wn"] = R.drawable.white_knight
            it["wb"] = R.drawable.white_bishop
            it["wk"] = R.drawable.white_king
            it["wq"] = R.drawable.white_queen
            it["wp"] = R.drawable.white_pawn
            it["br"] = R.drawable.black_rook
            it["bn"] = R.drawable.black_knight
            it["bb"] = R.drawable.black_bishop
            it["bk"] = R.drawable.black_king
            it["bq"] = R.drawable.black_queen
            it["bp"] = R.drawable.black_pawn
        }
    }

    fun getTile(i: Int, j: Int): String {
        return board[i][j]
    }

    fun setTile(i: Int, j: Int, piece: String) {
        board[i][j] = piece
    }

    fun setNextPlayer(color: Player) {
        if (color == Player.WHITE)
            nextPlayer = Player.WHITE
        else
            nextPlayer = Player.BLACK
    }

    fun print() {
        var boardText = ""
        for (i in 0 until 8) {
            boardText += "\n"
            for (j in 0 until 8) {
                boardText += "[${board[i][j]}] "
            }
        }
        Log.i("Chessboard", boardText)
    }

    fun toFen(): String {
        var fen = ""
        for (i in 0 until 8) {
            var j = 0
            var emptyTilesInRow = 0
            var emptyLastTile = false
            while (j < 8) {
                when (board[i][j]) {
                    "wr" -> fen += "R"
                    "br" -> fen += "r"
                    "wn" -> fen += "N"
                    "bn" -> fen += "n"
                    "wb" -> fen += "B"
                    "bb" -> fen += "b"
                    "wk" -> fen += "K"
                    "bk" -> fen += "k"
                    "wq" -> fen += "Q"
                    "bq" -> fen += "q"
                    "wp" -> fen += "P"
                    "bp" -> fen += "p"
                    else -> {
                        emptyTilesInRow++
                        emptyLastTile = true
                    }
                }
                if ((!emptyLastTile && emptyTilesInRow > 0) || j == 7) {
                    if (j == 7 && emptyLastTile) {
                        fen += emptyTilesInRow
                    }
                    else {
                        fen = fen.dropLast(1) + "$emptyTilesInRow" + fen.takeLast(1)
                        emptyTilesInRow = 0
                    }
                }
                emptyLastTile = false
                j++
            }
            fen += "/"
        }
        fen = fen.dropLast(1)

        if (nextPlayer == Player.WHITE)
            fen += " w - - 0 1"
        else
            fen += " b - - 0 1"

        return fen
    }
}