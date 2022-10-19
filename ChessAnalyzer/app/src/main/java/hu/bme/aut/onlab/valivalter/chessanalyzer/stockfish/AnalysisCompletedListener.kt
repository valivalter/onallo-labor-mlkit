package hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish

interface AnalysisCompletedListener {
    fun onAnalysisCompleted(result: String)
}