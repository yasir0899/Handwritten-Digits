package com.group.mlkitdemo.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.android.Utils.bitmapToMat
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class TFLiteCharacterHelper(private val context: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("character.tflite")
            val inputStream = fileDescriptor.createInputStream()
            val model = inputStream.readBytes()
            val buffer = ByteBuffer.allocateDirect(model.size)
            buffer.put(model)
            buffer.rewind()
            interpreter = Interpreter(buffer)
            val inputShape = interpreter!!.getInputTensor(0).shape()
            Log.d("TFLite", "Expected input shape: ${inputShape.contentToString()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun predict(bitmap: Bitmap): String {
//    val img=R.drawable.letter_q
//    val bitmap  = BitmapFactory.decodeResource(context.resources, img)
//    val resourceId = img
//   val name = context.resources.getResourceEntryName(resourceId)
        val name = "canvas_"
        // Step 1: Convert Bitmap to OpenCV Mat
        val mat = Mat()
        bitmapToMat(bitmap, mat)

        // Step 2: Clone the original for processing
        val imgCopy = mat.clone()

        // Step 3: Convert RGBA to BGR (OpenCV reads Bitmap as RGBA by default)
        Imgproc.cvtColor(imgCopy, imgCopy, Imgproc.COLOR_RGBA2BGR)
        // Step 4: Resize the image to (400, 440) (similar to Python code)
        val resizedMat = Mat()
        Imgproc.resize(mat, resizedMat, Size(400.0, 440.0))
        saveBitmapToFile(matToBitmap(resizedMat), "${name}resized.png", context)

        // Step 5: Apply Gaussian Blur to imgCopy (for noise reduction)
        Imgproc.GaussianBlur(imgCopy, imgCopy, Size(7.0, 7.0), 0.0)
        saveBitmapToFile(matToBitmap(imgCopy), "${name}blurred.png", context)
        // Step 6: Convert to Grayscale
        val imgGray = Mat()
        Imgproc.cvtColor(imgCopy, imgGray, Imgproc.COLOR_BGR2GRAY)
        saveBitmapToFile(matToBitmap(imgGray), "${name}grayscale.png", context)

        // Step 7: Apply Thresholding (binary inverse)
        val imgThresh = Mat()
        Imgproc.threshold(imgGray, imgThresh, 100.0, 255.0, Imgproc.THRESH_BINARY_INV)
        saveBitmapToFile(matToBitmap(imgThresh), "${name}threshold.png", context)

        // Step 8: Resize to 28x28 (to match model input size)
        val imgFinal = Mat()
        Imgproc.resize(imgThresh, imgFinal, Size(28.0, 28.0))
        saveBitmapToFile(matToBitmap(imgFinal), "${name}28*28.png", context)


        // Step 9: Prepare the input array with shape (1, 28, 28, 1)
        val inputArray = Array(1) { Array(28) { Array(28) { FloatArray(1) } } }
        for (i in 0 until 28) {
            for (j in 0 until 28) {
                val pixel = imgFinal.get(i, j)[0].toFloat()
                inputArray[0][i][j][0] = pixel   // Normalize the pixel value between 0 and 1
            }
        }

        // Step 10: Run the model (assume interpreter is initialized)
        val outputArray = Array(1) { FloatArray(26) }  // 26 letters A-Z

        // Run the model to get the predictions
        interpreter?.run(inputArray, outputArray)

        // Mapping from the model output index to the corresponding character
        val wordDict = mapOf(
            0 to 'A',
            1 to 'B',
            2 to 'C',
            3 to 'D',
            4 to 'E',
            5 to 'F',
            6 to 'G',
            7 to 'H',
            8 to 'I',
            9 to 'J',
            10 to 'K',
            11 to 'L',
            12 to 'M',
            13 to 'N',
            14 to 'O',
            15 to 'P',
            16 to 'Q',
            17 to 'R',
            18 to 'S',
            19 to 'T',
            20 to 'U',
            21 to 'V',
            22 to 'W',
            23 to 'X',
            24 to 'Y',
            25 to 'Z'
        )

        // Get the prediction index (the highest probability)
        val predictionIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1
        val predictedChar = wordDict[predictionIndex] ?: '?'  // Get the corresponding character

        // Log the prediction result
        Log.d("Prediction", "Predicted Character: $predictedChar")

        return predictedChar.toString()
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap, filename: String, context: Context) {
        val file = File(context.getExternalFilesDir(null), filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        Log.d("BitmapDebug", "Saved at: ${file.absolutePath}")
    }
}
