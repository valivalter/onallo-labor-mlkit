package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.util.Log
import java.util.HashMap

enum class Player {
    WHITE, BLACK
}

class Chessboard {
    private var board = Array(8) { Array(8) { "" } }
    var nextPlayer = Player.WHITE

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

        val castlingAvailabilityPossibilities = listOf(
            "KQkq", "KQk", "KQq", "KQ",
            "Kkq", "Kk", "Kq", "K",
            "Qkq", "Qk", "Qq", "Q",
            "kq", "k", "q", "-"
        )
    }

    fun getTile(i: Int, j: Int): String {
        return board[i][j]
    }

    fun setTile(i: Int, j: Int, piece: String) {
        board[i][j] = piece
    }

    /*fun setNextPlayer(color: Player) {
        if (color == Player.WHITE)
            nextPlayer = Player.WHITE
        else
            nextPlayer = Player.BLACK
    }*/

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

    fun indicesToSquare(i: Int, j: Int): String {
        return "abcdefgh"[j] + "${8-i}"
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
                if ((!emptyLastTile && emptyTilesInRow > 0) || (j == 7 && emptyTilesInRow > 0)) {
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

        // a sima egy fotó alapján analizálás esetén
        // castling availability cannot be determined, because we can never be sure that the rook was not moved before
        // (még az alaphelyzetben sem, hiszen mi van, ha már lépett egyet a huszár kifele, aztán a bástya, aztán fordított sorrendben visszaléptek és újra alaphelyzet)
        // en passant target square cannot be determined as well
        // halfmove and fullmove number cannot be determined as well
        if (nextPlayer == Player.WHITE)
            fen += " w - - 0 1"
        else
            fen += " b - - 0 1"

        return fen
    }

    fun getAllPossibleFens(): MutableList<String> {
        var castlingAvailabilities = Chessboard.castlingAvailabilityPossibilities
        if (board[0][4] != "bk") {
            castlingAvailabilities = castlingAvailabilities.filter { "k" !in it && "q" !in it }
        }
        else {
            if (board[0][0] != "br") {
                castlingAvailabilities = castlingAvailabilities.filter { "q" !in it }
            }
            if (board[0][7] != "br") {
                castlingAvailabilities = castlingAvailabilities.filter { "k" !in it }
            }
        }

        if (board[7][4] != "wk") {
            castlingAvailabilities = castlingAvailabilities.filter { "K" !in it && "Q" !in it }
        }
        else {
            if (board[7][0] != "wr") {
                castlingAvailabilities = castlingAvailabilities.filter { "Q" !in it }
            }
            if (board[7][7] != "wr") {
                castlingAvailabilities = castlingAvailabilities.filter { "K" !in it }
            }
        }

        var fens = mutableListOf<String>()
        for (castling in castlingAvailabilities) {
            fens.add(this.toFen().dropLast(7) + castling + " - 0 1")
        }

        return fens
    }

    fun equals(other: Chessboard): Boolean {
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                if (board[i][j] != other.getTile(i, j))
                    return false
            }
        }
        return true
    }

    fun isDifferenceOneMove(other: Chessboard): Boolean {
        var differences = 0
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                if (board[i][j] != other.getTile(i, j))
                    differences++
            }
        }

        return differences == 2
    }

    fun getLastMoveSan(previous: Chessboard): String {
        var fromTile: Pair<Int, Int>? = null
        var toTile: Pair<Int, Int>? = null
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                if (board[i][j] != previous.getTile(i, j)) {
                    if (previous.getTile(i, j) != "em" && board[i][j] == "em")
                        fromTile = Pair(i, j)
                    else {
                        toTile = Pair(i, j)
                    }
                }
            }
        }

        var san = ""
        if (fromTile != null && toTile != null) {
            when (previous.getTile(fromTile.first, fromTile.second)) {
                "wr", "br" -> san += "R"
                "wn", "bn" -> san += "N"
                "wb", "bb" -> san += "B"
                "wk", "bk" -> san += "K"
                "wq", "bq" -> san += "Q"
                // "wp", "bp" -> pgn += "P"  usually not used in PGN notation
            }
            if (previous.getTile(toTile.first, toTile.second) != "em") {
                if (previous.getTile(fromTile.first, fromTile.second) == "wp" ||
                    previous.getTile(fromTile.first, fromTile.second) == "bp") {
                    san += indicesToSquare(fromTile.first, fromTile.second)[0]
                }
                san += "x"
            }
            san += indicesToSquare(toTile.first, toTile.second)
        }
        return san
    }

    // for testing
    fun setDefaultPosition() {
        board[0][0] = "br"
        board[0][1] = "bn"
        board[0][2] = "bb"
        board[0][3] = "bq"
        board[0][4] = "bk"
        board[0][5] = "bb"
        board[0][6] = "bn"
        board[0][7] = "br"
        for (j in 0 until 8) {
            board[1][j] = "bp"
        }
        for (j in 0 until 8) {
            board[6][j] = "wp"
        }
        board[7][0] = "wr"
        board[7][1] = "wn"
        board[7][2] = "wb"
        board[7][3] = "wq"
        board[7][4] = "wk"
        board[7][5] = "wb"
        board[7][6] = "wn"
        board[7][7] = "wr"
    }
}