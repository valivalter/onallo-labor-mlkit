package hu.bme.aut.onlab.valivalter.chessanalyzer

import android.Manifest
import android.R.attr
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import hu.bme.aut.onlab.valivalter.chessanalyzer.AnalyzerLogic.Analyzer
import hu.bme.aut.onlab.valivalter.chessanalyzer.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.ImageButton
import android.R.attr.process
import android.view.View
import java.io.*
import android.widget.RadioButton

import android.widget.RadioGroup





class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var chessboard: Chessboard
    private lateinit var stockfishProcess: Process

    companion object{
        const val REQUEST_IMAGE_CAPTURE = 1

        private const val REQUEST_CODE_PERMISSIONS = 11
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnAnalyze.setOnClickListener {
            val path = applicationContext.applicationInfo.nativeLibraryDir + "/lib_stockfish.so"
            val file = File(path)

            try {
                var command = "position fen ${chessboard.toFen()}\neval\nisready\ngo movetime 5000"
                //var command = "isready"
                command += "\n"

                val ep: Process = stockfishProcess
                if (ep != null) {
                    ep.outputStream.write(command.toByteArray())
                    ep.outputStream.flush()
                }
            }
            catch (e: IOException) { }
        }

        binding.btngrpNextPlayer.setOnCheckedChangeListener { _, id ->
            if (binding.btnWhite.id == id)
                chessboard.setNextPlayer(Player.WHITE)
            else
                chessboard.setNextPlayer(Player.BLACK)
        }

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

                        Log.e("üzenet jött", data ?: "semmi")

                        if (data != null) {
                            if ("Final evaluation" in data!!) {
                                var result = data!!
                                runOnUiThread {
                                    Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                                }
                            }
                            else if ("bestmove" in data!!) {
                                var result = data!!
                                runOnUiThread {
                                    Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                    }
                } catch (e: IOException) { }
            })

            outThread.start()

            // to give commands
            var command = "uci" //or other command
            command += "\n"

            val ep: Process = stockfishProcess
            if (ep != null) {
                ep.outputStream.write(command.toByteArray())
                ep.outputStream.flush()
            }
        }

        catch (e: IOException) { }






        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    lateinit var photoURI: Uri

    private fun startCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.also {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("File error", "Couldn't create file")
                null
            }
            // Continue only if the File was successfully created
            photoFile?.also {
                photoURI = FileProvider.getUriForFile(
                    this,
                    "hu.bme.aut.onlab.valivalter.chessanalyzer.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                } catch (e: ActivityNotFoundException) {
                    // display error state to the user
                }
            }
        }
    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            /*data?.also {
                val imageBitmap = it.extras?.get("data") as Bitmap
                val resizedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.width)
                binding.ivPhoto.setImageBitmap(resizedBitmap)
                chessboard = Analyzer(resizedBitmap).analyze()
            }*/

            try {
                val contentResolver = applicationContext.contentResolver
                val inputStream: InputStream? = contentResolver.openInputStream(photoURI)
                val imageBitmap = BitmapFactory.decodeStream(inputStream)
                if (inputStream != null) inputStream.close()

                val matrix = Matrix()
                matrix.postRotate(90F)
                val rotatedImg = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true)
                imageBitmap.recycle()

                /*val imageBitmap = MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    Uri.parse(currentPhotoPath)
                )*/
                val resizedBitmap = Bitmap.createBitmap(rotatedImg, 0, 0, rotatedImg.width, rotatedImg.width)
                //binding.ivPhoto.setImageBitmap(resizedBitmap)
                val analyzer = Analyzer(this, resizedBitmap)
                chessboard = analyzer.analyze()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                // finish()
            }
        }
    }
}