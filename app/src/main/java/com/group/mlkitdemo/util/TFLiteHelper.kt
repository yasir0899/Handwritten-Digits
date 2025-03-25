package com.group.mlkitdemo.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteHelper(private val context: Context) {

    private var interpreter: Interpreter? = null

    init {
        interpreter = Interpreter(loadModelFile())
        val inputShape = interpreter?.getInputTensor(0)?.shape()
        Log.d("Model Input Shape", "Shape: ${inputShape?.joinToString()}")

    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("digits.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun predict(inputData: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(4 * 28 * 28)
            .order(ByteOrder.nativeOrder())

        // Add inputData to ByteBuffer
        inputData.forEach { inputBuffer.putFloat(it) }

        // ✅ Adjusting outputBuffer size (40 bytes = 10 float values)
        val outputSize = 10  // Assuming 10 output classes (0-9 digits)
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize)
            .order(ByteOrder.nativeOrder())

        // Run the model
        interpreter?.run(inputBuffer, outputBuffer)

        // Extract result
        outputBuffer.rewind()
        val resultArray = FloatArray(outputSize) { outputBuffer.float }

        return resultArray
    }

    fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        // Resize the bitmap to 28x28 (expected by model)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true)

        val floatArray = FloatArray(28 * 28)
        val pixels = IntArray(28 * 28)

        // Get pixel values
        resizedBitmap.getPixels(pixels, 0, 28, 0, 0, 28, 28)

        // Normalize pixel values
        for (i in pixels.indices) {
            val pixelValue = pixels[i]
            val r = (pixelValue shr 16 and 0xFF) / 255.0f
            val g = (pixelValue shr 8 and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            // Convert to grayscale (if required)
            floatArray[i] = (r + g + b) / 3.0f
        }

        return floatArray
    }
}
