package hu.bme.aut.onlab.valivalter.chessanalyzer.model

import hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish.Analysis

data class Step(val chessboard: Chessboard, val analysis: Analysis)

data class Round(var whiteStep: Step? = null, var blackStep: Step? = null)

class Game() {
    var rounds: MutableList<Round> = mutableListOf()
    val initialState = Chessboard().also {
        it.setDefaultPosition()
    }

    fun getLastStep(): String {
        val lastRound = rounds.last()
        if (lastRound.blackStep == null) {
            if (rounds.size == 1) {
                return "White: ${lastRound.whiteStep!!.chessboard.getLastMoveLan(initialState)} (${lastRound.whiteStep!!.analysis.result})"
            }
            else {
                val secondToLastRound = rounds[rounds.size - 2]
                return "White: ${lastRound.whiteStep!!.chessboard.getLastMoveLan(secondToLastRound.blackStep!!.chessboard)} (${lastRound.whiteStep!!.analysis.result})"
            }
        }
        else {
            return "Black: ${lastRound.blackStep!!.chessboard.getLastMoveLan(lastRound.whiteStep!!.chessboard)} (${lastRound.blackStep!!.analysis.result})"
        }
    }

    override fun toString(): String {
        var string = ""
        rounds.forEachIndexed { i, round ->
            if (i == 0) {
                if (round.whiteStep != null) {
                    if (round.blackStep != null) {
                        string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveLan(initialState)} ${round.blackStep!!.chessboard.getLastMoveLan(round.whiteStep!!.chessboard)} (${round.whiteStep!!.analysis.result}, ${round.blackStep!!.analysis.result})\n"
                    }
                    else {
                        string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveLan(initialState)} - (${round.whiteStep!!.analysis.result}, -)\n"
                    }
                }
                else {
                    string += "${i+1}. - ${round.blackStep!!.chessboard.getLastMoveLan(initialState)} (-, ${round.blackStep!!.analysis.result})\n"
                }
            }
            else {
                if (round.blackStep != null) {
                    string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveLan(rounds[i-1].blackStep!!.chessboard)} ${round.blackStep!!.chessboard.getLastMoveLan(round.whiteStep!!.chessboard)} (${round.whiteStep!!.analysis.result}, ${round.blackStep!!.analysis.result})\n"
                }
                else {
                    string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveLan(rounds[i-1].blackStep!!.chessboard)} - (${round.whiteStep!!.analysis.result}, -)\n"
                }
            }
        }
        return string
    }
}