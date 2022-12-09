package hu.bme.aut.onlab.valivalter.chessanalyzer.model

import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.Analysis

data class Step(val chessboard: Chessboard, val analysis: Analysis)

data class Round(var whiteStep: Step? = null, var blackStep: Step? = null)

class Game {
    var rounds: MutableList<Round> = mutableListOf()
    private val initialState = Chessboard().also {
        it.setDefaultPosition()
    }

    fun getLastStep(): String {
        val lastRound = rounds.last()
        if (lastRound.blackStep == null) {
            if (rounds.size == 1) {
                val lastStep = lastRound.whiteStep!!
                val analysis = lastStep.analysis
                val lan = lastStep.chessboard.getLastMoveLan(initialState, analysis)
                return "White: $lan (${analysis.result})"
            }
            else {
                val lastStep = lastRound.whiteStep!!
                val analysis = lastStep.analysis
                val secondToLastStep = rounds[rounds.size - 2].blackStep!!
                val lan = lastStep.chessboard.getLastMoveLan(secondToLastStep.chessboard, analysis)
                return "White: $lan (${analysis.result})"
            }
        }
        else {
            val lastStep = lastRound.blackStep!!
            val secondToLastStep = lastRound.whiteStep!!
            val analysis = lastStep.analysis
            val lan = lastStep.chessboard.getLastMoveLan(secondToLastStep.chessboard, analysis)
            return "Black: $lan (${analysis.result})"
        }
    }

    override fun toString(): String {
        var string = ""
        rounds.forEachIndexed { i, round ->
            val previousRoundState = if (i == 0) initialState else rounds[i-1].blackStep!!.chessboard
            if (round.blackStep != null) {
                val white = round.whiteStep!!
                val black = round.blackStep!!
                val whiteLan = white.chessboard.getLastMoveLan(previousRoundState, white.analysis)
                val blackLan = black.chessboard.getLastMoveLan(white.chessboard, black.analysis)
                string += "${i+1}. $whiteLan $blackLan (${white.analysis.result}, ${black.analysis.result})\n"
            }
            else {
                val white = round.whiteStep!!
                val whiteLan = white.chessboard.getLastMoveLan(previousRoundState, white.analysis)
                string += "${i+1}. $whiteLan - (${white.analysis.result}, -)\n"
            }
        }
        return string
    }
}