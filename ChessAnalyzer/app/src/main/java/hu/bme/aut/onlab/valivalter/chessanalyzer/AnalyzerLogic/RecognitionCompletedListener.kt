package hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic

import hu.bme.aut.onlab.valivalter.chessanalyzer.Chessboard

interface RecognitionCompletedListener {
    fun onRecognitionCompleted(chessboard: Chessboard)
}