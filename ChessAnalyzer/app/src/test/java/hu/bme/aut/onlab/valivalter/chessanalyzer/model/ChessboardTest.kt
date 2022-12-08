package hu.bme.aut.onlab.valivalter.chessanalyzer.model

import org.hamcrest.CoreMatchers.*
import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ChessboardTest {
    private val chessboard = Chessboard().also { it.setDefaultPosition() }

    @Test
    fun copy() {
        val copy = chessboard.copy()

        assertThat(copy, not(sameInstance(chessboard)))
        assertThat(copy.board, not(sameInstance(chessboard.board)))
        assertThat(copy.board, Is(chessboard.board))
        assertThat(copy.nextPlayer, Is(chessboard.nextPlayer))
    }

    @Test
    fun setDefaultPosition() {
        val defaultChessboard = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "bn", "br"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wp", "wp", "wp", "wp", "wp", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr"))
            it.nextPlayer = Player.WHITE
        }

        assertThat(chessboard.board, not(sameInstance(defaultChessboard.board)))
        assertThat(chessboard.board, Is(defaultChessboard.board))
        assertThat(chessboard.nextPlayer, Is(defaultChessboard.nextPlayer))
    }

    @Test
    fun rotate() {
        val rotated = Chessboard().also {
            it.board = arrayOf( arrayOf("wr", "wp", "em", "em", "em", "em", "bp", "br"),
                                arrayOf("wn", "wp", "em", "em", "em", "em", "bp", "bn"),
                                arrayOf("wb", "wp", "em", "em", "em", "em", "bp", "bb"),
                                arrayOf("wq", "wp", "em", "em", "em", "em", "bp", "bq"),
                                arrayOf("wk", "wp", "em", "em", "em", "em", "bp", "bk"),
                                arrayOf("wb", "wp", "em", "em", "em", "em", "bp", "bb"),
                                arrayOf("wn", "wp", "em", "em", "em", "em", "bp", "bn"),
                                arrayOf("wr", "wp", "em", "em", "em", "em", "bp", "br"))
            it.nextPlayer = Player.WHITE
        }

        chessboard.rotate()

        assertThat(rotated, not(sameInstance(chessboard)))
        assertThat(rotated.board, not(sameInstance(chessboard.board)))
        assertThat(rotated.board, Is(chessboard.board))
        assertThat(rotated.nextPlayer, Is(chessboard.nextPlayer))
    }

    @Test
    fun indicesToTileNotation() {
        assertThat(Chessboard.indicesToTileNotation(0, 0), Is("a8"))
        assertThat(Chessboard.indicesToTileNotation(6, 1), Is("b2"))
        assertThat(Chessboard.indicesToTileNotation(2, 3), Is("d6"))
        assertThat(Chessboard.indicesToTileNotation(3, 6), Is("g5"))
        assertThat(Chessboard.indicesToTileNotation(4, 7), Is("h4"))
    }

    @Test
    fun toFen() {
        val chessboard2 = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "bq", "bk", "bb", "em", "br"),
                                arrayOf("bp", "bp", "em", "em", "bp", "em", "bp", "bp"),
                                arrayOf("bn", "em", "bp", "em", "em", "bp", "em", "bn"),
                                arrayOf("em", "em", "em", "bp", "em", "bb", "em", "em"),
                                arrayOf("em", "wq", "em", "wp", "em", "wb", "em", "em"),
                                arrayOf("em", "em", "wn", "em", "em", "wn", "em", "em"),
                                arrayOf("wp", "wp", "wp", "em", "wp", "wp", "wp", "wp"),
                                arrayOf("wr", "em", "em", "em", "wk", "wb", "em", "wr"))
            it.nextPlayer = Player.BLACK
        }

        val chessboard3 = Chessboard().also {
            it.board = arrayOf( arrayOf("em", "em", "em", "bk", "em", "em", "em", "em"),
                                arrayOf("bp", "em", "em", "em", "em", "em", "bp", "bp"),
                                arrayOf("em", "em", "em", "bb", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "bp", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wp", "em", "em", "wp", "wk", "wn", "em", "em"),
                                arrayOf("em", "br", "em", "em", "em", "em", "wp", "wp"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "wr"))
            it.nextPlayer = Player.BLACK
        }

        assertThat(chessboard.toFen(), Is("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"))
        assertThat(chessboard2.toFen(), Is("r2qkb1r/pp2p1pp/n1p2p1n/3p1b2/1Q1P1B2/2N2N2/PPP1PPPP/R3KB1R b - - 0 1"))
        assertThat(chessboard3.toFen(), Is("3k4/p5pp/3b4/3p4/8/P2PKN2/1r4PP/7R b - - 0 1"))
    }

    @Test
    fun getAllPossibleFens() {
        val chessboard16Possibilities = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "bk", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "wk", "em", "em", "wr"))
            it.nextPlayer = Player.WHITE
        }
        val fens16Possibilities = arrayListOf("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w KQk - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w KQq - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w KQ - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w Kkq - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w Kk - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w Kq - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w K - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w Qkq - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w Qk - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w Qq - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w Q - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w kq - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w k - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w q - 0 1",
                                        "r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1")

        val chessboard8Possibilities = Chessboard().also {
            it.board = arrayOf( arrayOf("em", "em", "em", "em", "bk", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "wk", "em", "em", "wr"))
            it.nextPlayer = Player.WHITE
        }
        val fens8Possibilities = arrayListOf("4k2r/8/8/8/8/8/8/R3K2R w KQk - 0 1",
                                        "4k2r/8/8/8/8/8/8/R3K2R w KQ - 0 1",
                                        "4k2r/8/8/8/8/8/8/R3K2R w Kk - 0 1",
                                        "4k2r/8/8/8/8/8/8/R3K2R w K - 0 1",
                                        "4k2r/8/8/8/8/8/8/R3K2R w Qk - 0 1",
                                        "4k2r/8/8/8/8/8/8/R3K2R w Q - 0 1",
                                        "4k2r/8/8/8/8/8/8/R3K2R w k - 0 1",
                                        "4k2r/8/8/8/8/8/8/R3K2R w - - 0 1")

        val chessboard4Possibilities = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "bk", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "wk", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "em", "em", "em", "wr"))
            it.nextPlayer = Player.WHITE
        }
        val fens4Possibilities = arrayListOf("r3k2r/8/8/8/8/8/4K3/R6R w kq - 0 1",
                                        "r3k2r/8/8/8/8/8/4K3/R6R w k - 0 1",
                                        "r3k2r/8/8/8/8/8/4K3/R6R w q - 0 1",
                                        "r3k2r/8/8/8/8/8/4K3/R6R w - - 0 1")

        val chessboard2Possibilities = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "em", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "bk", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "wk", "em", "em", "wr"))
            it.nextPlayer = Player.WHITE
        }
        val fens2Possibilities = arrayListOf("r6r/4k3/8/8/8/8/8/4K2R w K - 0 1",
                                        "r6r/4k3/8/8/8/8/8/4K2R w - - 0 1")

        val chessboard1Possibility = Chessboard().also {
            it.board = arrayOf( arrayOf("em", "em", "em", "em", "bk", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "wk", "em", "em", "em"))
            it.nextPlayer = Player.WHITE
        }
        val fens1Possibility = arrayListOf("4k3/8/8/8/8/8/8/4K3 w - - 0 1")


        assertThat(chessboard16Possibilities.getAllPossibleFens(), Is(fens16Possibilities))
        assertThat(chessboard8Possibilities.getAllPossibleFens(), Is(fens8Possibilities))
        assertThat(chessboard4Possibilities.getAllPossibleFens(), Is(fens4Possibilities))
        assertThat(chessboard2Possibilities.getAllPossibleFens(), Is(fens2Possibilities))
        assertThat(chessboard1Possibility.getAllPossibleFens(), Is(fens1Possibility))
    }

    @Test
    fun testEquals() {
        val defaultChessboard = Chessboard().also { it.setDefaultPosition() }

        assertThat(chessboard.board, not(sameInstance(defaultChessboard.board)))
        assertTrue(chessboard.equals(defaultChessboard))

        defaultChessboard.setTile(0, 0, "em")
        assertFalse(chessboard.equals(defaultChessboard))
    }

    @Test
    fun getDifferentTiles() {
        val chessboard4Differences = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "bn", "br"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "em", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "bp", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "wp", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wp", "wp", "wp", "em", "wp", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr"))
            it.nextPlayer = Player.WHITE
        }

        val chessboard1Difference = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "bn", "br"),
                                arrayOf("bb", "bb", "bb", "bb", "bb", "bb", "bb", "bb"),
                                arrayOf("em", "em", "em", "em", "em", "bp", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wp", "wp", "wp", "wp", "wp", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr"))
            it.nextPlayer = Player.WHITE
        }

        assertThat(arrayListOf(Pair(1, 5), Pair(2, 5), Pair(4, 3), Pair(6, 3)), Is(chessboard4Differences.getDifferentTiles(chessboard)))
        assertThat(arrayListOf(Pair(2, 5)), Is(chessboard1Difference.getDifferentTiles(chessboard)))
    }

    @Test
    fun getLastMoveLan() {
        val chessboardPawnE4 = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "bn", "br"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "wp", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wp", "wp", "wp", "wp", "em", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr"))
        }

        val chessboardKnightF6 = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "em", "br"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "bn", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "wp", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wp", "wp", "wp", "wp", "em", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr"))
        }

        val chessboardBishopD3 = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "em", "br"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "bn", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "wp", "em", "em", "em"),
                                arrayOf("em", "em", "em", "wb", "em", "em", "em", "em"),
                                arrayOf("wp", "wp", "wp", "wp", "em", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "em", "wn", "wr"))
        }

        val chessboardKnightE4 = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "em", "br"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "bn", "em", "em", "em"),
                                arrayOf("em", "em", "em", "wb", "em", "em", "em", "em"),
                                arrayOf("wp", "wp", "wp", "wp", "em", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "em", "wn", "wr"))
        }

        val chessboardKnightH3 = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "em", "br"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "bn", "em", "em", "em"),
                                arrayOf("em", "em", "em", "wb", "em", "em", "em", "wn"),
                                arrayOf("wp", "wp", "wp", "wp", "em", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "em", "em", "wr"))
        }

        val chessboardRookG8 = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "br", "em"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "bn", "em", "em", "em"),
                                arrayOf("em", "em", "em", "wb", "em", "em", "em", "wn"),
                                arrayOf("wp", "wp", "wp", "wp", "em", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "wk", "em", "em", "wr"))
        }

        val chessboardCastleK = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "bn", "bb", "bq", "bk", "bb", "br", "em"),
                                arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "bn", "em", "em", "em"),
                                arrayOf("em", "em", "em", "wb", "em", "em", "em", "wn"),
                                arrayOf("wp", "wp", "wp", "wp", "em", "wp", "wp", "wp"),
                                arrayOf("wr", "wn", "wb", "wq", "em", "wr", "wk", "em"))
        }

        assertThat(chessboardPawnE4.getLastMoveLan(chessboard), Is("e2e4"))
        assertThat(chessboardKnightF6.getLastMoveLan(chessboardPawnE4), Is("Ng8f6"))
        assertThat(chessboardBishopD3.getLastMoveLan(chessboardKnightF6), Is("Bf1d3"))
        assertThat(chessboardKnightE4.getLastMoveLan(chessboardBishopD3), Is("Nf6xe4"))
        assertThat(chessboardKnightH3.getLastMoveLan(chessboardKnightE4), Is("Ng1h3"))
        assertThat(chessboardRookG8.getLastMoveLan(chessboardKnightH3), Is("Rh8g8"))
        assertThat(chessboardCastleK.getLastMoveLan(chessboardRookG8), Is("0-0"))
    }

    @Test
    fun didCastle() {
        val chessboardPrevious = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "bk", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "wk", "em", "em", "wr"))
        }

        val chessboard_K = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "bk", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "em", "wr", "wk", "em"))
        }

        val chessboard_Q = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "bk", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "wk", "wr", "em", "em", "em", "wr"))
        }

        val chessboard_k = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "em", "br", "bk", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "wk", "em", "em", "wr"))
        }

        val chessboard_q = Chessboard().also {
            it.board = arrayOf( arrayOf("em", "em", "bk", "br", "em", "em", "em", "br"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "wk", "em", "em", "wr"))
        }

        val chessboardNoCastle = Chessboard().also {
            it.board = arrayOf( arrayOf("em", "em", "bk", "br", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                                arrayOf("em", "em", "em", "em", "wk", "em", "em", "em"),
                                arrayOf("wr", "em", "em", "em", "em", "em", "em", "wr"))
        }

        assertThat(chessboard_K.didCastle(chessboardPrevious), Is('K'))
        assertThat(chessboard_Q.didCastle(chessboardPrevious), Is('Q'))
        assertThat(chessboard_k.didCastle(chessboardPrevious), Is('k'))
        assertThat(chessboard_q.didCastle(chessboardPrevious), Is('q'))
        assertThat(chessboardNoCastle.didCastle(chessboardPrevious), nullValue())
    }

    @Test
    fun castle() {
        val chessboard_K_previous = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "bk", "em", "em", "br"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("wr", "em", "em", "em", "wk", "em", "em", "wr"))
        }
        val chessboard_q_previous = chessboard_K_previous.copy()

        val chessboard_K = Chessboard().also {
            it.board = arrayOf( arrayOf("br", "em", "em", "em", "bk", "em", "em", "br"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("wr", "em", "em", "em", "em", "wr", "wk", "em"))
        }

        val chessboard_q = Chessboard().also {
            it.board = arrayOf( arrayOf("em", "em", "bk", "br", "em", "em", "em", "br"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("em", "em", "em", "em", "em", "em", "em", "em"),
                arrayOf("wr", "em", "em", "em", "wk", "em", "em", "wr"))
        }

        chessboard_K_previous.castle('K')
        assertThat(chessboard_K_previous, not(sameInstance(chessboard_K)))
        assertThat(chessboard_K_previous.board, Is(chessboard_K.board))

        chessboard_q_previous.castle('q')
        assertThat(chessboard_q_previous, not(sameInstance(chessboard_q)))
        assertThat(chessboard_q_previous.board, Is(chessboard_q.board))
    }
}