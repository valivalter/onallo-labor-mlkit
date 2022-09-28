package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.util.Log
import java.util.HashMap
data class Analysis(val result: String)

data class Step(val chessboard: Chessboard, val analysis: Analysis)

data class Round(var whiteStep: Step? = null, var blackStep: Step? = null)

class Game(private val initialState: Chessboard) {
    var states: MutableList<Round> = mutableListOf()
}