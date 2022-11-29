package hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish

data class Analysis(var result: String, var bestMove: String? = null, var expectedResponse: String? = null)

interface AnalysisCompletedListener {
    fun onAnalysisCompleted(result: Analysis)
    fun onInvalidFen()
}