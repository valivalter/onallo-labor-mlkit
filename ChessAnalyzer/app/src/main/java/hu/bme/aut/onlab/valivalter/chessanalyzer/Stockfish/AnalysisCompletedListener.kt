package hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish

import hu.bme.aut.onlab.valivalter.chessanalyzer.Chessboard

interface AnalysisCompletedListener {
    fun onAnalysisCompleted(result: String)
}