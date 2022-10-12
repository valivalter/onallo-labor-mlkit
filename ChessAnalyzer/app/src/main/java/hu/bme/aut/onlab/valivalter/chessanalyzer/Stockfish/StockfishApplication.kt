package hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish

import android.app.Application
import android.os.Handler
import java.io.BufferedReader
import java.io.File
import java.io.IOException
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


    }

    override fun onCreate() {
        super.onCreate()

        val path = applicationContext.applicationInfo.nativeLibraryDir + "/lib_stockfish.so"
        val file = File(path)

        try {
            stockfishProcess = Runtime.getRuntime().exec(file.path)

            val outThread = Thread(Runnable {
                val processOut = stockfishProcess ?: return@Runnable
                val out = BufferedReader(InputStreamReader(processOut.inputStream))
                var data: String?
                try {
                    while (out.readLine().also { data = it } != null) {
                        if (data != null) {
                            if ("Final evaluation" in data!!) {
                                val results = data!!.split(" ")
                                var resultString = "${results[8]} ${results[9]} ${results[10]}"
                                //                    score               which color

                                resultString = formatResult(resultString)

                                if (mode == MODE.ANALYZER || mode == MODE.RECORDER) {
                                    val handler = Handler(mainLooper)
                                    handler.post {
                                        listener.onAnalysisCompleted(resultString)
                                    }
                                }
                            }
                            else if ("bestmove" in data!!) {
                                val results = data!!.split(" ")
                                val resultString = "Best move: ${results[1]}\nResponse: ${results[3]}"

                                if (mode == MODE.ANALYZER) {
                                    val handler = Handler(mainLooper)
                                    handler.post {
                                        listener.onAnalysisCompleted(resultString)
                                    }
                                }
                            }
                        }
                    }
                }
                catch (e: IOException) {
                    e.printStackTrace()
                }
            })

            outThread.start()
            runCommand("uci\n")
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun formatResult(result: String): String {
        var formattedResult = result
        if (formattedResult.endsWith("(white side)")) {
            if (formattedResult[0].equals("-")) {
                formattedResult = formattedResult.replaceFirst('-', '+')
            }
            formattedResult = formattedResult.replace(" (white side)", "⬜")
        }
        else if (formattedResult.endsWith("(black side)")) {
            if (formattedResult[0].equals("-")) {
                formattedResult = formattedResult.replaceFirst('-', '+')
            }
            formattedResult = formattedResult.replace(" (black side)", "⬛")
        }

        return formattedResult
    }
}