package hu.bme.aut.onlab.valivalter.chessanalyzer.Stockfish

import android.app.Application
import android.os.Handler
import android.widget.Toast
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class StockfishApplication : Application() {
    companion object {
        lateinit var stockfishProcess: Process

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
                                val resultString = "${results[8]} ${results[9]} ${results[10]}"
                                //                    score               which color

                                val handler = Handler(mainLooper)
                                handler.post {
                                    Toast.makeText(this, resultString, Toast.LENGTH_LONG).show()
                                }
                            }
                            else if ("bestmove" in data!!) {
                                val results = data!!.split(" ")
                                val resultString = "Best move: ${results[1]}\nResponse: ${results[3]}"

                                val handler = Handler(mainLooper)
                                handler.post {
                                    Toast.makeText(this, resultString, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
                catch (e: IOException) { }
            })

            outThread.start()
            runCommand("uci\n")
        }
        catch (e: IOException) { }
    }
}