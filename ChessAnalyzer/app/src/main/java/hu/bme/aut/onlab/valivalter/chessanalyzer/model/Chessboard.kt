package hu.bme.aut.onlab.valivalter.chessanalyzer.model

import hu.bme.aut.onlab.valivalter.chessanalyzer.R
import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.Analysis
import java.util.HashMap

enum class Player {
    WHITE, BLACK
}

class Chessboard {
    var board = Array(8) { Array(8) { "" } }
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

            it["wkwr"] = R.drawable.white_king_and_rook
            it["bkbr"] = R.drawable.black_king_and_rook
        }

        val castlingAvailabilityPossibilities = listOf(
            "KQkq", "KQk", "KQq", "KQ",
            "Kkq", "Kk", "Kq", "K",
            "Qkq", "Qk", "Qq", "Q",
            "kq", "k", "q", "-"
        )

        fun indicesToTileNotation(i: Int, j: Int): String {
            return "abcdefgh"[j] + "${8-i}"
        }

        fun getPieceFromStepLan(step: String): String {
            if ("White" in step) {
                when (step[7]) {
                    'K' -> return "wk"
                    'Q' -> return "wq"
                    'R' -> return "wr"
                    'N' -> return "wn"
                    'B' -> return "wb"
                    '0' -> return "wkwr"
                    else -> return "wp"
                }
            }
            else {
                when (step[7]) {
                    'K' -> return "bk"
                    'Q' -> return "bq"
                    'R' -> return "br"
                    'N' -> return "bn"
                    'B' -> return "bb"
                    '0' -> return "bkbr"
                    else -> return "bp"
                }
            }
        }
    }

    fun getTile(i: Int, j: Int): String {
        return board[i][j]
    }

    fun setTile(i: Int, j: Int, piece: String) {
        board[i][j] = piece
    }

    fun copy(): Chessboard {
        val chessboard = Chessboard()
        when (this.nextPlayer) {
            Player.WHITE -> chessboard.nextPlayer = Player.WHITE
            Player.BLACK -> chessboard.nextPlayer = Player.BLACK
        }
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                chessboard.setTile(i, j, this.getTile(i, j))
            }
        }
        return chessboard
    }

    fun setDefaultPosition() {
        nextPlayer = Player.WHITE
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
        for (i in 2 until 6) {
            for (j in 0 until 8) {
                board[i][j] = "em"
            }
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

    fun rotate() {
        val newBoard = Array(8) { Array(8) { "" } }
        for (i in 0..7) {
            for (j in 7 downTo 0) {
                newBoard[i][7-j] = this.board[j][i]
            }
        }
        board = newBoard
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

        // Egy fotó alapján lehetetlen meghatározni a FEN utolsó négy összetevőjét, ezért "- - 0 1" lesz mindig
        if (nextPlayer == Player.WHITE)
            fen += " w - - 0 1"
        else
            fen += " b - - 0 1"

        return fen
    }

    fun getAllPossibleFens(): MutableList<String> {
        var castlingAvailabilities = castlingAvailabilityPossibilities
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

    // Itt a különbség az alábbi három lehetőségből jelenti valamelyiket:
    // az egyik világos, a másik sötét
    // az egyik világos, a másik üres
    // az egyik sötét, a másik üres
    fun getDifferentTiles(other: Chessboard): MutableList<Pair<Int, Int>> {
        val differences = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                if (board[i][j].first() != other.getTile(i, j).first())
                    differences.add(Pair(i, j))
            }
        }

        return differences
    }

    fun getLastMoveLan(previous: Chessboard, analysis: Analysis? = null): String {
        val castlingType = didCastle(previous)
        if (castlingType == 'K' || castlingType == 'k') {
            return "0-0"
        }
        else if (castlingType == 'Q' || castlingType == 'q') {
            return "0-0-0"
        }

        var fromTile: Pair<Int, Int>? = null
        var toTile: Pair<Int, Int>? = null
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                if (board[i][j].first() != previous.getTile(i, j).first()) {
                    if (previous.getTile(i, j) != "em" && board[i][j] == "em")
                        fromTile = Pair(i, j)
                    else {
                        toTile = Pair(i, j)
                    }
                }
            }
        }

        var lan = ""
        if (fromTile != null && toTile != null) {
            when (previous.getTile(fromTile.first, fromTile.second)) {
                "wr", "br" -> lan += "R"
                "wn", "bn" -> lan += "N"
                "wb", "bb" -> lan += "B"
                "wk", "bk" -> lan += "K"
                "wq", "bq" -> lan += "Q"
            }
            lan += indicesToTileNotation(fromTile.first, fromTile.second)
            if (previous.getTile(toTile.first, toTile.second) != "em") {
                lan += "x"
            }
            lan += indicesToTileNotation(toTile.first, toTile.second)

            if ((toTile.first == 0 || toTile.first == 7) && previous.getTile(fromTile.first, fromTile.second).last() == 'p') {
                when (board[toTile.first][toTile.second]) {
                    "wr", "br" -> lan += "R"
                    "wn", "bn" -> lan += "N"
                    "wb", "bb" -> lan += "B"
                    "wq", "bq" -> lan += "Q"
                }
            }
        }

        if (analysis?.result == "check") {
            lan += '+'
        }
        else if (analysis?.result == "checkmate") {
            lan += '#'
        }
        return lan
    }



    fun didCastle(previous: Chessboard): Char? {
        val differences = this.getDifferentTiles(previous)
        if (differences.size == 4) {
            if (previous.getTile(0, 0) == "br" && this.getTile(0, 0) == "em" &&
                previous.getTile(0, 2) == "em" && this.getTile(0, 2).first() == 'b' &&
                previous.getTile(0, 3) == "em" && this.getTile(0, 3).first() == 'b' &&
                previous.getTile(0, 4) == "bk" && this.getTile(0, 4) == "em") {
                return 'q'
            }
            else if (previous.getTile(0, 4) == "bk" && this.getTile(0, 4) == "em" &&
                previous.getTile(0, 5) == "em" && this.getTile(0, 5).first() == 'b' &&
                previous.getTile(0, 6) == "em" && this.getTile(0, 6).first() == 'b' &&
                previous.getTile(0, 7) == "br" && this.getTile(0, 7) == "em") {
                return 'k'
            }
            else if (previous.getTile(7, 0) == "wr" && this.getTile(7, 0) == "em" &&
                previous.getTile(7, 2) == "em" && this.getTile(7, 2).first() == 'w' &&
                previous.getTile(7, 3) == "em" && this.getTile(7, 3).first() == 'w' &&
                previous.getTile(7, 4) == "wk" && this.getTile(7, 4) == "em") {
                return 'Q'
            }
            else if (previous.getTile(7, 4) == "wk" && this.getTile(7, 4) == "em" &&
                previous.getTile(7, 5) == "em" && this.getTile(7, 5).first() == 'w' &&
                previous.getTile(7, 6) == "em" && this.getTile(7, 6).first() == 'w' &&
                previous.getTile(7, 7) == "wr" && this.getTile(7, 7) == "em") {
                return 'K'
            }
        }
        return null
    }

    fun castle(fenAvailabilityNotation: Char) {
        when (fenAvailabilityNotation) {
            'q' -> {
                this.setTile(0, 0, "em")
                this.setTile(0, 2, "bk")
                this.setTile(0, 3, "br")
                this.setTile(0, 4, "em")
            }
            'k' -> {
                this.setTile(0, 4, "em")
                this.setTile(0, 5, "br")
                this.setTile(0, 6, "bk")
                this.setTile(0, 7, "em")

            }
            'Q' -> {
                this.setTile(7, 0, "em")
                this.setTile(7, 2, "wk")
                this.setTile(7, 3, "wr")
                this.setTile(7, 4, "em")

            }
            'K' -> {
                this.setTile(7, 4, "em")
                this.setTile(7, 5, "wr")
                this.setTile(7, 6, "wk")
                this.setTile(7, 7, "em")
            }
        }
    }
}