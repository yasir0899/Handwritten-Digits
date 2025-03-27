package com.group.mlkitdemo


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
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


class MainActivity : ComponentActivity() {

    private lateinit var model: TFLiteCharacterHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = TFLiteCharacterHelper(this)
        setContent {
            MLKitDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { it ->
                    HandwritingRecognitionScreen()
                }
            }
        }

    }


    @Composable
    fun HandwritingRecognitionScreen() {
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
            // Drawing Canvas
            DrawingCanvas(onBitmapReady = { bitmap ->
//                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.digit_5)
                prediction = convertBitmapAndPredict(bitmap)
                recognizedBitmap = bitmap
                showDialog = true

            }, onClear = {
                prediction = it
            })
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

//    private fun convertBitmapAndPredict(bitmap: Bitmap): String {
//
////        val inputArray = model.bitmapToFloatArray(bitmap)
////        val result = model.predict(inputArray)
//
//        val result = model.predict(bitmap)
//
//        // Print the top prediction
//        val predictedDigit = result.indices.maxByOrNull { result[it] } ?: -1
//        println("Predicted Digit: $predictedDigit")
//        return predictedDigit.toString()
//    }

    private fun convertBitmapAndPredict(bitmap: Bitmap): String {
        val bitmapTest = BitmapFactory.decodeResource(resources, R.drawable.letter_k)
        val result = model.predict(model.convertToGrayscale(bitmapTest)) // Should return a FloatArray of size 26

        if (result.isEmpty()) {
            Log.e("Prediction", "Empty prediction result")
            return "Error"
        }

        val predictedIndex = result.indices.maxByOrNull { result[it] } ?: -1
        Log.d("Prediction", "Predicted Character Index: $predictedIndex")

        return predictedIndex.toString()
    }


}
