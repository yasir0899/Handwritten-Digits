package com.group.mlkitdemo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DrawingCanvas(
    onBitmapReady:  (Bitmap) -> Unit,
    onClear: (String) -> Unit
) {
    val canvasSize = 300 // Define the canvas size

    var path by remember { mutableStateOf(Path()) }
    var shouldRedraw by remember { mutableStateOf(false) }

    val paint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 15f // Increased stroke width to make it visible after resizing
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
    }

    val drawModifier = Modifier
        .size(canvasSize.dp)
        .background(Color.White)
        .border(2.dp, Color.Black)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    path.moveTo(offset.x, offset.y)
                    shouldRedraw = true
                },
                onDrag = { change, _ ->
                    path.lineTo(change.position.x, change.position.y)
                    path = Path().apply { addPath(path) } // 🔹 Force recomposition
                    shouldRedraw = true
                }
            )
        }

    // 🎨 ✅ Drawing appears immediately
    Canvas(modifier = drawModifier) {
        drawContext.canvas.nativeCanvas.drawPath(path, paint)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (!shouldRedraw) {
                Log.d("DrawingCanvas", "Path is empty! No drawing detected.")
                return@Button
            }

            val originalBitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
            val originalCanvas = Canvas(originalBitmap)

            // Fill the background with white
            originalCanvas.drawColor(android.graphics.Color.WHITE)

            // Compute bounding box of drawn path
            val bounds = RectF()
            path.computeBounds(bounds, true)

            // If bounds are too small, return
            if (bounds.width() == 0f || bounds.height() == 0f) {
                Log.d("DrawingCanvas", "Invalid path bounds! Skipping processing.")
                return@Button
            }

            // Translate the path to (0,0)
            val translationMatrix = Matrix()
            translationMatrix.setTranslate(-bounds.left, -bounds.top)
            path.transform(translationMatrix)

            // Scale to fit within the target size (28x28) with margins
            val scaleX = (canvasSize * 0.8f) / bounds.width()
            val scaleY = (canvasSize * 0.8f) / bounds.height()
            val scale = minOf(scaleX, scaleY)

            val scaleMatrix = Matrix()
            scaleMatrix.setScale(scale, scale)
            path.transform(scaleMatrix)

            // Compute new bounds after scaling
            path.computeBounds(bounds, true)

            // Center the path within the canvas
            val dx = (canvasSize - bounds.width()) / 2 - bounds.left
            val dy = (canvasSize - bounds.height()) / 2 - bounds.top
            val centeringMatrix = Matrix()
            centeringMatrix.setTranslate(dx, dy)
            path.transform(centeringMatrix)

            // Draw the scaled and centered path
            originalCanvas.drawPath(path, paint)

            // Resize the bitmap to 28x28 for model input
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 28, 28, true)

            onBitmapReady(resizedBitmap) // Send processed bitmap
        }) {
            Text("Recognize")
        }


        // ✅ Clear Button (Properly clears both UI & Bitmap)
        Button(onClick = {
            path = Path()
            onClear("")
            shouldRedraw = false
        }) {
            Text("Clear")
        }
    }
}
