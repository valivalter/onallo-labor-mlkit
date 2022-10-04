package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.util.Log
import java.util.HashMap
import kotlin.math.round

data class Analysis(val result: String)

data class Step(val chessboard: Chessboard, val analysis: Analysis)

data class Round(var whiteStep: Step? = null, var blackStep: Step? = null)

class Game(private val initialState: Chessboard) {
    var rounds: MutableList<Round> = mutableListOf()

    override fun toString(): String {
        var string = ""
        rounds.forEachIndexed { i, round ->
            if (i == 0) {
                if (round.whiteStep != null) {
                    if (round.blackStep != null) {
                        string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveSan(initialState)} ${round.blackStep!!.chessboard.getLastMoveSan(round.whiteStep!!.chessboard)} (${round.whiteStep!!.analysis.result}, ${round.blackStep!!.analysis.result})\n"
                    }
                    else {
                        string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveSan(initialState)} - (${round.whiteStep!!.analysis.result}, -)\n"
                    }
                }
                else {
                    string += "${i+1}. - ${round.blackStep!!.chessboard.getLastMoveSan(initialState)} (-, ${round.blackStep!!.analysis.result})\n"
                }
            }
            else {
                if (round.blackStep != null) {
                    string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveSan(rounds[i-1].blackStep!!.chessboard)} ${round.blackStep!!.chessboard.getLastMoveSan(round.whiteStep!!.chessboard)} (${round.whiteStep!!.analysis.result}, ${round.blackStep!!.analysis.result})\n"
                }
                else {
                    string += "${i+1}. ${round.whiteStep!!.chessboard.getLastMoveSan(rounds[i-1].blackStep!!.chessboard)} - (${round.whiteStep!!.analysis.result}, -)\n"
                }
            }
        }
        return string
    }
}