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
            ep.outputStream.write(command.toByteArray())
            ep.outputStream.flush()
        }

        fun runCommand(command: String) {
            val ep: Process = stockfishProcess
            ep.outputStream.write(command.toByteArray())
            ep.outputStream.flush()
        }

        fun stopCommand() {
            val command = "stop\n"
            val ep: Process = stockfishProcess
            ep.outputStream.write(command.toByteArray())
            ep.outputStream.flush()
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
                    val processOut = stockfishProcess
                    val out = BufferedReader(InputStreamReader(processOut.inputStream))
                    runCommand("uci\n")
                    var data: String?
                    try {
                        while (out.readLine().also { data = it } != null) {
                            if ("Final evaluation" in data!!) {
                                var result: String
                                if ("none" !in data!!) {
                                    // A 8. helyen van a végső pontszám
                                    result = data!!.split(" ")[8]

                                    if (result[0].equals('-')) {
                                        result = result.replaceFirst('-', '+') + "⚫"
                                    } else {
                                        result += "⚪"
                                    }
                                } else {
                                    result = "check"
                                }

                                analysis = Analysis(result)
                            } else if ("bestmove" in data!!) {
                                val results = data!!.split(" ")

                                if (analysis != null) {
                                    if (results.size > 2) {
                                        analysis!!.bestMove = results[1]
                                        analysis!!.expectedResponse = results[3]
                                    } else {
                                        if (results[1] == "(none)") {
                                            if (analysis!!.result == "check") {
                                                analysis!!.result = "checkmate"
                                            }
                                            else {
                                                analysis!!.result = "stalemate"
                                            }
                                            analysis!!.bestMove = "-"
                                            analysis!!.expectedResponse = "-"
                                        } else {
                                            analysis!!.bestMove = results[1]
                                            analysis!!.expectedResponse = "-"
                                        }
                                    }

                                    val handler = Handler(mainLooper)
                                    handler.post {
                                        listener.onAnalysisCompleted(analysis!!)
                                    }
                                }
                            }
                        }

                        // Szabálytalan pozíció megadásakor kerül végrehajtásra ez a 3 sor
                        val handler = Handler(mainLooper)
                        handler.post {
                            listener.onInvalidFen()
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