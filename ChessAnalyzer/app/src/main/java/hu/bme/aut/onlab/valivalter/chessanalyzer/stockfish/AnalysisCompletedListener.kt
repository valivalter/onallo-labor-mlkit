package hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish

import hu.bme.aut.onlab.valivalter.chessanalyzer.model.Analysis

interface AnalysisCompletedListener {
    fun onAnalysisCompleted(result: Analysis)
}