package com.group.mlkitdemo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.android.Utils.bitmapToMat
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class TFLiteCharacterHelper(private val context: Context) {

//    private var interpreter: Interpreter? = null
//
//    init {
//        interpreter = Interpreter(loadModelFile())
//        val inputShape = interpreter?.getInputTensor(0)?.shape()
//        Log.d(
//            "Model Input Shape",
//            "Shape: ${inputShape?.joinToString()}"
//        )  // Should log: [1, 28, 28, 1]
//    }
//
//    private fun loadModelFile(): MappedByteBuffer {
//        val fileDescriptor = context.assets.openFd("model.tflite")
//        val inputStream = fileDescriptor.createInputStream()
//        val fileChannel = inputStream.channel
//        return fileChannel.map(
//            FileChannel.MapMode.READ_ONLY,
//            fileDescriptor.startOffset,
//            fileDescriptor.declaredLength
//        )
//    }
//
//    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
//        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true) // Resize image to 28x28
//
//        val byteBuffer =
//            ByteBuffer.allocateDirect(4 * 1 * 28 * 28) // 4 bytes per float (1 batch, 28x28, 1 channel)
//        byteBuffer.order(ByteOrder.nativeOrder())
//
//        val pixels = IntArray(28 * 28)
//        resizedBitmap.getPixels(pixels, 0, 28, 0, 0, 28, 28)
//
//        for (i in pixels.indices) {
//            val pixelValue = pixels[i]
//            val r = (pixelValue shr 16 and 0xFF) / 255.0f  // Normalize Red
//            val g = (pixelValue shr 8 and 0xFF) / 255.0f   // Normalize Green
//            val b = (pixelValue and 0xFF) / 255.0f         // Normalize Blue
//
//            var grayscale = (r + g + b) / 3.0f // Convert to grayscale (average of RGB)
//
//            // Check if inversion is needed (if digits are black on white instead of white on black)
//            grayscale = 1.0f - grayscale // Invert if necessary
//
//            byteBuffer.putFloat(grayscale) // Add normalized value to buffer
//        }
//
//        return byteBuffer
//    }
//
//
//
//    // Run inference with correct input shape (1,28,28,1)
//    fun predict(bitmap: Bitmap): FloatArray {
//        val inputBuffer = preprocessImage(bitmap)
//
//        // Correct output size (26 classes)
//        val outputSize = 26 // Based on last Dense layer (None, 26)
//
//        // Allocate correct buffer size
//        val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize).order(ByteOrder.nativeOrder())
//
//        interpreter?.run(inputBuffer, outputBuffer)
//
//        // Convert ByteBuffer to FloatArray
//        outputBuffer.rewind()
//        return FloatArray(outputSize) { outputBuffer.float }
//    }
//
//    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
//        val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(grayscaleBitmap)
//        val paint = Paint()
//        val colorMatrix = ColorMatrix()
//        colorMatrix.setSaturation(0f)  // Remove color
//        val filter = ColorMatrixColorFilter(colorMatrix)
//        paint.colorFilter = filter
//        canvas.drawBitmap(bitmap, 0f, 0f, paint)
//        return grayscaleBitmap
//    }
//
private var interpreter: Interpreter? = null

    init {
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("model.tflite")
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

    fun predict(input: ByteBuffer): FloatArray {
        val output = Array(1) { FloatArray(26) } // 1 batch, 26 classes (A-Z)
        interpreter?.run(input, output)
        return output[0]
    }


    fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Convert the image to 28x28
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true)

        // Allocate buffer for 1x28x28x1 floats (4 bytes per float)
        val byteBuffer = ByteBuffer.allocateDirect(1 * 28 * 28 * 1 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Convert pixels to grayscale
        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = resizedBitmap.getPixel(x, y)

                // Convert RGB to grayscale using weighted average
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val grayscale = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f

                byteBuffer.putFloat(grayscale)
            }
        }

        byteBuffer.rewind() // Reset buffer position
        return byteBuffer
    }

    fun loadAndResizeImage(context: Context, resId: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        return Bitmap.createScaledBitmap(bitmap, 400, 440, false)  // Resize to match Python
    }
//    fun applyGaussianBlur(context: Context, bitmap: Bitmap): Bitmap {
//        val rs = RenderScript.create(context)
//        val input = Allocation.createFromBitmap(rs, bitmap)
//        val output = Allocation.createFromBitmap(rs, bitmap)
//
//        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
//        blurScript.setRadius(7f)  // Matches Python's 7x7 kernel
//        blurScript.setInput(input)
//        blurScript.forEach(output)
//
//        output.copyTo(bitmap)
//        rs.destroy()
//        return bitmap
//    }

    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }


    fun applyGaussianBlurOpenCV(bitmap: Bitmap): Bitmap {
        val mat = Mat().apply { bitmapToMat(bitmap, this) }
        Imgproc.GaussianBlur(mat, mat, Size(7.0, 7.0), 0.0)  // Same as Python (7x7)
        return matToBitmap(mat)
    }


    fun toGrayscale(bitmap: Bitmap): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)  // Remove color
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    fun applyThreshold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val binarizedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val gray = Color.red(pixel)  // Since grayscale, R=G=B
                val binarizedColor = if (gray > 127) Color.WHITE else Color.BLACK  // Adaptive thresholding
                binarizedBitmap.setPixel(x, y, binarizedColor)
            }
        }
        return binarizedBitmap
    }

    fun resizeTo28x28(bitmap: Bitmap): Bitmap {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, false)  // Avoid blurring
        return resizedBitmap
    }


    fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 28 * 28 * 4)  // 1 batch, 28x28, 1 channel (float32)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = bitmap.getPixel(x, y)
                val gray = Color.red(pixel) / 255.0f  // Normalize
                byteBuffer.putFloat(gray)
            }
        }
        return byteBuffer
    }

    fun preprocessImageNew(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Resize to (400, 440)
        val resizedMat = Mat()
        Imgproc.resize(mat, resizedMat, Size(400.0, 440.0))

        // Convert to RGB
        Imgproc.cvtColor(resizedMat, resizedMat, Imgproc.COLOR_BGR2RGB)

        // Apply Gaussian Blur
        val blurredMat = Mat()
        Imgproc.GaussianBlur(resizedMat, blurredMat, Size(7.0, 7.0), 0.0)

        // Convert to Grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(blurredMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Apply Thresholding (Binary Inversion)
        val thresholdMat = Mat()
        Imgproc.threshold(grayMat, thresholdMat, 100.0, 255.0, Imgproc.THRESH_BINARY_INV)

        // Resize to (28, 28) for model input
        val finalMat = Mat()
        Imgproc.resize(thresholdMat, finalMat, Size(28.0, 28.0))

        // Convert back to RGB (to avoid OpenCV format error)
        val rgbMat = Mat()
        Imgproc.cvtColor(finalMat, rgbMat, Imgproc.COLOR_GRAY2RGB)

        // Convert back to Bitmap (Use ARGB_8888)
        val processedBitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgbMat, processedBitmap)

        // Release Mats to free memory
        mat.release()
        resizedMat.release()
        blurredMat.release()
        grayMat.release()
        thresholdMat.release()
        finalMat.release()
        rgbMat.release()

        return processedBitmap
    }


    fun bitmapToByteBufferNew(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 28 * 28 * 1 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = bitmap.getPixel(x, y)
                val value = Color.red(pixel) / 255.0f
                byteBuffer.putFloat(value)
            }
        }
        return byteBuffer
    }
    fun runModel( bitmap: Bitmap): Int {
        val inputBuffer = bitmapToByteBufferNew(bitmap)
        val outputArray = Array(1) { FloatArray(26) } // Assuming 26 classes (A-Z)

        interpreter?.run(inputBuffer, outputArray)

        val predictionIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1
        Log.d("MLKitDemo", "Predicted Index: $predictionIndex")
        return predictionIndex
    }



}
