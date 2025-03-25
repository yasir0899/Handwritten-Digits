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
        Log.d("Model Input Shape", "Shape: ${inputShape?.joinToString()}")  // Should log: [1, 28, 28, 1]
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("digits.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize the bitmap to 28x28
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true)

        // Allocate a ByteBuffer with 4 bytes per float (batch size 1, height 28, width 28, channel 1)
        val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * 28 * 28 * 1)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(28 * 28)
        resizedBitmap.getPixels(pixels, 0, 28, 0, 0, 28, 28)

        for (i in pixels.indices) {
            val pixelValue = pixels[i]

            // Extract RGB values
            val r = (pixelValue shr 16 and 0xFF).toFloat()
            val g = (pixelValue shr 8 and 0xFF).toFloat()
            val b = (pixelValue and 0xFF).toFloat()

            // Convert to grayscale and normalize
            val grayscale = ((r + g + b) / 3.0f) / 255.0f
            byteBuffer.putFloat(grayscale) // Store in ByteBuffer
        }

        return byteBuffer
    }


    // ✅ Run inference with correct input shape (1,28,28,1)
    fun predict(bitmap: Bitmap): FloatArray {
        val inputBuffer = preprocessImage(bitmap)

        val outputSize = 10
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize).order(ByteOrder.nativeOrder())

        interpreter?.run(inputBuffer, outputBuffer)

        // Convert ByteBuffer to FloatArray
        outputBuffer.rewind()
        return FloatArray(outputSize) { outputBuffer.float }
    }
}
