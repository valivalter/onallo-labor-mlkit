package hu.bme.aut.onlab.valivalter.chessanalyzer.recognizer

import hu.bme.aut.onlab.valivalter.chessanalyzer.model.Chessboard

interface RecognitionCompletedListener {
    fun onRecognitionCompleted(chessboard: Chessboard)
}