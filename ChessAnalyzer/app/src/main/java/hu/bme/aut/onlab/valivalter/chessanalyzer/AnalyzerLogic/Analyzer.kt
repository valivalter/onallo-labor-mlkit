package hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic

import android.graphics.Bitmap
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import hu.bme.aut.onlab.valivalter.chessanalyzer.Chessboard
import hu.bme.aut.onlab.valivalter.chessanalyzer.MainActivity
import hu.bme.aut.onlab.valivalter.chessanalyzer.R
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Analyzer(private val activity: MainActivity, private val bitmap: Bitmap) {

    private val tileWidth = bitmap.width / 8
    private val imageLabeler: ImageLabeler

    init {
        val localModel = LocalModel.Builder()
            .setAssetFilePath("chess-pieces-public-dataset.tflite")
            .build()

        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.001f)
            .setMaxResultCount(5)
            .build()
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)
    }

    fun analyze(): Chessboard {
        var board = Chessboard()

        for (i in 0 until 8) {
            for (j in 0 until 8) {
                var tile = Bitmap.createBitmap(bitmap, j * tileWidth, i * tileWidth, tileWidth, tileWidth)
                tile = Bitmap.createScaledBitmap(tile, 277, 277, false)



                //rawAnalyze(bitmapTo4DArray(tile))



                //Log.e("KÉPMÉRET", tile.height.toString())
                val tileImage = InputImage.fromBitmap(tile, 0)
                imageLabeler.process(tileImage)
                    .addOnSuccessListener { results ->
                        Log.e("$i $j", "Chessboardkl")
                        board.setTile(i, j, results[0].text.substring(0, 2))


                        activity.findViewById<ImageButton>(Chessboard.boardRIDs[i][j]).setImageBitmap(tile)


                        if (i == 7 && j == 7) {
                            board.print()
                            Log.e("FEN", board.toFen())
                            showBoard(board)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(e.toString(), "Error")
                    }
            }
        }
        return board
    }

    private fun showBoard(board: Chessboard) {
        //activity.binding.row0col2.setImageResource(R.drawable.black_pawn)
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val piece = board.getTile(i, j)
                val tile = activity.findViewById<ImageButton>(Chessboard.boardRIDs[i][j])
                tile.setOnClickListener {
                    val actualPiece = board.getTile(i, j)
                    var newIndex = Chessboard.pieces.indexOf(actualPiece) + 1
                    if (newIndex == Chessboard.pieces.size) {
                        newIndex = 0
                    }
                    val newPiece = Chessboard.pieces[newIndex]
                    board.setTile(i, j, newPiece)
                    tile.setImageResource(Chessboard.mapStringsToResources[newPiece]!!)
                    board.print()
                }
                if (piece != "em") {
                    tile.setImageResource(Chessboard.mapStringsToResources[piece]!!)
                }
            }
        }
        activity.findViewById<Button>(R.id.btnAnalyze).isEnabled = true
    }




    
    /*private var interpreter: Interpreter = Interpreter(getModelByteBuffer("chess-pieces-public-dataset.tflite"), null)
    private var output: Array<IntArray> = Array(1) { IntArray(3)}

    private fun rawAnalyze(image: Array<Array<Array<FloatArray>>>) {
        interpreter.run(image, output)
        Log.e("EREDMÉNY", output.toString())
    }

    //override fun onDestroy() {
    //    interpreter.close()
    //    super.onDestroy()
    //}

    private fun bitmapTo4DArray(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var outputArray = Array(1) { Array(width) { Array(height) { FloatArray(3) } } }

        for (i in 0 until width) {
            for (j in 0 until height) {
                val pixel = pixels[i * width + j]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val rFloat = (r.toFloat()/255) * 2 - 1
                val gFloat = (g.toFloat()/255) * 2 - 1
                val bFloat = (b.toFloat()/255) * 2 - 1

                outputArray[0][i][j][0] = rFloat
                outputArray[0][i][j][1] = gFloat
                outputArray[0][i][j][2] = bFloat
            }
        }

        return outputArray
    }

    @Throws(IOException::class)
    private fun getModelByteBuffer(modelPath: String): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }*/
}