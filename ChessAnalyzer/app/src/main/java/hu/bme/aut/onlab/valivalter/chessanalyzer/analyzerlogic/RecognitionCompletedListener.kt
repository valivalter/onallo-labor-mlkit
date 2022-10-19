package hu.bme.aut.onlab.valivalter.chessanalyzer.analyzerlogic

import hu.bme.aut.onlab.valivalter.chessanalyzer.model.Chessboard

interface RecognitionCompletedListener {
    fun onRecognitionCompleted(chessboard: Chessboard)
}