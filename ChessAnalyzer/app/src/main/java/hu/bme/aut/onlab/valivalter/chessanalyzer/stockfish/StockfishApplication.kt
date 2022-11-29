package hu.bme.aut.onlab.valivalter.chessanalyzer.stockfish

import android.app.Application
import android.os.Handler
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

enum class MODE {
    ANALYZER,
    RECORDER
}

class StockfishApplication : Application() {

    companion object {
        lateinit var stockfishProcess: Process
        lateinit var mode: MODE
        lateinit var listener: AnalysisCompletedListener
        var analysis: Analysis? = null

        fun runCommandWithListener(command: String, mode: MODE, listener: AnalysisCompletedListener) {
            this.mode = mode
            this.listener = listener
            val ep: Process = stockfishProcess
            if (ep != null) {
                ep.outputStream.write(command.toByteArray())
                ep.outputStream.flush()
            }
        }

        fun runCommand(command: String) {
            val ep: Process = stockfishProcess
            if (ep != null) {
                ep.outputStream.write(command.toByteArray())
                ep.outputStream.flush()
            }
        }

        fun stopCommand() {
            // although the stop command immediately stops the search for the best move, but
            // the best move will be returned regardless. De szerencsére ez elég gyorsan
            // történik ahhoz, hogy közben ne indítsa el a felhasználó újra az activityt
            // és kezdjen egy új analízist, ami átállítaná a listenert,
            // vagyis így duplán jelenne meg az analízis eredménye, de szal ezt kerüljük el szerencsére
            // így mindenképp a régi már megszűnt activityt fogja nézni a lichessinteractor is,
            // bármennyit is piszmogna az api adatokon, ott pedig ellenőrizve van, hogy
            // megszűnt-e már az activity (nyilv azt fogja látni, hogy igen)
            val command = "stop\n"
            val ep: Process = stockfishProcess
            if (ep != null) {
                ep.outputStream.write(command.toByteArray())
                ep.outputStream.flush()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val path = applicationContext.applicationInfo.nativeLibraryDir + "/lib_stockfish.so"
        val file = File(path)

        try {
            val outThread = Thread(Runnable {
                while (true) {
                    stockfishProcess = Runtime.getRuntime().exec(file.path)
                    val processOut = stockfishProcess ?: return@Runnable
                    val out = BufferedReader(InputStreamReader(processOut.inputStream))
                    runCommand("uci\n")
                    var data: String?
                    try {
                        while (out.readLine().also { data = it } != null) {
                            if (data != null) {
                                if ("Final evaluation" in data!!) {
                                    var result: String
                                    if ("none" !in data!!) {
                                        result = data!!.split(" ")[8]
                                        //var resultString = "${results[8]} ${results[9]} ${results[10]}"
                                        //                    score               which color

                                        if (result[0].equals('-')) {
                                            result = result.replaceFirst('-', '+') + "⚫"
                                        } else {
                                            result += "⚪"
                                        }
                                    } else {
                                        result = "- (check)"
                                    }

                                    analysis = Analysis(result)

                                    if (mode == MODE.RECORDER) {
                                        val handler = Handler(mainLooper)
                                        handler.post {
                                            listener.onAnalysisCompleted(analysis!!)
                                        }
                                    }
                                } else if ("bestmove" in data!!) {
                                    val results = data!!.split(" ")

                                    if (analysis != null) {
                                        if (results.size > 2) {
                                            analysis!!.bestMove = results[1]
                                            analysis!!.expectedResponse = results[3]
                                        } else {
                                            if (results[1] == "(none)") {
                                                analysis!!.result = "checkmate"
                                                analysis!!.bestMove = "-"
                                                analysis!!.expectedResponse = "-"
                                            } else {
                                                analysis!!.bestMove = results[1]
                                                analysis!!.expectedResponse = "-"
                                            }
                                        }

                                        if (mode == MODE.ANALYZER) {
                                            val handler = Handler(mainLooper)
                                            handler.post {
                                                listener.onAnalysisCompleted(analysis!!)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Stockfish crash, this happens when the user entered an invalid fen
                        if (mode == MODE.ANALYZER) {
                            val handler = Handler(mainLooper)
                            handler.post {
                                listener.onInvalidFen()
                            }
                        }

                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            })
            outThread.start()
        }
        catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}