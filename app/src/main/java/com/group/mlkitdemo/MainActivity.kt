package com.group.mlkitdemo


import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group.mlkitdemo.ui.theme.MLKitDemoTheme
import com.group.mlkitdemo.util.TFLiteCharacterHelper
import com.group.mlkitdemo.util.TFLiteHelper
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


class MainActivity : ComponentActivity() {

    private lateinit var model: TFLiteCharacterHelper
    private lateinit var model_digit: TFLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = TFLiteCharacterHelper(this)
        model_digit = TFLiteHelper(this)

        setContent {
            MLKitDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { it ->
                    HandwritingRecognitionScreen()
                }
            }
        }
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed!")
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully!")
        }

    }


    @Composable
    fun HandwritingRecognitionScreen() {
        var isDigitMode by remember { mutableStateOf(true) } // Switch state
        var prediction by remember { mutableStateOf("") }
        var recognizedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var showDialog by remember { mutableStateOf(false) }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Switch to toggle between Digit and Alphabet Mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Digits", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = !isDigitMode, // true for alphabets, false for digits
                    onCheckedChange = { isDigitMode = !it })
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Alphabets", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Drawing Canvas
            if (isDigitMode) {
                DrawingCanvas(onBitmapReady = { bitmap ->
//                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.digit_5)
                    prediction = convertBitmapAndPredict(bitmap)
                    recognizedBitmap = bitmap
                    showDialog = true

                }, onClear = {
                    prediction = it
                })
            } else {

                DrawAlphabetsCanvas(onBitmapReady = { bitmap ->

                    prediction = model.predict(bitmap)
                    //saveBitmapToFile(bitmap, "canvas.png", this@MainActivity)
                    recognizedBitmap = bitmap
                    showDialog = true


                }, onClear = { prediction = it })

            }



            if (showDialog && recognizedBitmap != null) {
                AlertDialog(onDismissRequest = { showDialog = false }, confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                }, title = { Text("Prediction Result") }, text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = recognizedBitmap!!.asImageBitmap(),
                            contentDescription = "Recognized Digit",
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Predicted: ${prediction ?: "Unknown"}")
                    }
                })
            }





            Spacer(modifier = Modifier.height(20.dp))

            // Prediction Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) // Light blue
            ) {
                Text(
                    text = "Prediction: $prediction",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun convertBitmapAndPredict(bitmap: Bitmap): String {

        val result = model_digit.predict(bitmap) // Should return a FloatArray of size 26

        if (result.isEmpty()) {
            Log.e("Prediction", "Empty prediction result")
            return "Error"
        }

        val predictedIndex = result.indices.maxByOrNull { result[it] } ?: -1
        Log.d("Prediction", "Predicted Character Index: $predictedIndex")

        return predictedIndex.toString()
    }

    fun saveBitmapToFile(bitmap: Bitmap, filename: String, context: Context) {
        val file = File(context.getExternalFilesDir(null), filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        Log.d("BitmapDebug", "Saved at: ${file.absolutePath}")
    }


}
