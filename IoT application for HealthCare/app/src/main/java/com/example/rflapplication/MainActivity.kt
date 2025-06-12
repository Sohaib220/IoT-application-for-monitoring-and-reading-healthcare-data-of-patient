package com.example.rflapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var ipAddressInput: EditText
    private lateinit var connectButton: Button
    private lateinit var bodyTempTextView: TextView
    private lateinit var oxygenTextView: TextView
    private lateinit var pulseTextView: TextView
    private lateinit var weightTextView: TextView
    private lateinit var respirationTextView: TextView
    private lateinit var bloodPressureTextView: TextView
    private val handler = Handler(Looper.getMainLooper()) // Handler for continuous updates
    private var ipAddress: String = ""
    private val refreshInterval: Long = 5000// Refresh data every 5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val name = intent.getStringExtra("NAME")
        val weight = intent.getStringExtra("WEIGHT")?.toDoubleOrNull()
        val height = intent.getStringExtra("HEIGHT")?.toDoubleOrNull()
        val age = intent.getStringExtra("AGE")
        val bloodPressure = intent.getStringExtra("BLOOD_PRESSURE")

        // Log received data for debugging
        Log.d("Received Data", "Name: $name, Weight: $weight, Height: $height, Age: $age, Blood Pressure: $bloodPressure")


        // UI Elements
        ipAddressInput = findViewById(R.id.ipAddressInput)
        connectButton = findViewById(R.id.connectButton)
        bodyTempTextView = findViewById(R.id.bodyTempTextView)
        oxygenTextView = findViewById(R.id.oxygenTextView)
        pulseTextView = findViewById(R.id.pulseTextView)
        weightTextView = findViewById(R.id.weightTextView)
        respirationTextView = findViewById(R.id.respirationTextView)
        bloodPressureTextView = findViewById(R.id.bloodPressureTextView)

        // Display blood pressure if available
        bloodPressure?.let {
            bloodPressureTextView.text = "Blood Pressure: $it"
        }

        // Calculate BMI and categorize weight
        if (weight != null && height != null) {
            val bmi = weight / (height * height) // BMI calculation
            val category = when {
                bmi < 18.5 -> "Underweight"
                bmi in 18.5..24.9 -> "Normal"
                bmi in 25.0..29.9 -> "Overweight"
                else -> "Obese"
            }
            weightTextView.text = "$name's BMI: %.1f ($category)".format(bmi)
        } else {
            weightTextView.text = "Weight & height required for BMI"
        }

        // Generate random normal respiration rate (18 - 20 breaths per minute)
        updateRespirationRate()


        // Button Click Listener
        connectButton.setOnClickListener {
            val inputIp = ipAddressInput.text.toString().trim()
            if (inputIp.isNotEmpty()) {
                ipAddress = inputIp
                fetchData() // Start fetching data
            } else {
                Toast.makeText(this, "Please enter an IP address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchData() {
        Thread {
            try {
                val url = URL("http://$ipAddress/sensorData")
                val urlConnection = url.openConnection() as HttpURLConnection

                urlConnection.requestMethod = "GET"
                urlConnection.connectTimeout = 15000 // 15 sec
                urlConnection.readTimeout = 15000
                urlConnection.doInput = true

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = urlConnection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    inputStream.close()
                    urlConnection.disconnect()

                    updateUI(response.toString())
                } else {
                    showToast("Server Error: $responseCode")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                handler.postDelayed({ fetchData() }, refreshInterval) // Schedule next fetch
            }
        }.start()
    }

    private fun updateUI(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val temperature = jsonObject.optDouble("temperature", Double.NaN)
            val oxygen = jsonObject.optInt("oxygen", -1)
            val pulse = jsonObject.optInt("pulse", -1)

            runOnUiThread {
                if (!temperature.isNaN()) {
                    bodyTempTextView.text = "Body Temperature: $temperature°C"
                }
                if (oxygen != -1) {
                    oxygenTextView.text = "Oxygen Level: $oxygen%"
                }
                if (pulse != -1) {
                    pulseTextView.text = "Pulse: $pulse BPM"
                }
            }
        } catch (e: Exception) {
            showToast("Invalid JSON format")
            e.printStackTrace()
        }
    }

    private fun updateRespirationRate() {
        handler.post(object : Runnable {
            override fun run() {
                val respirationRate = Random.nextInt(18, 21) // Normal range: 18-20 breaths per minute
                respirationTextView.text = "Respiration Rate: $respirationRate BPM"
                handler.postDelayed(this, 5000) // Update every 5 seconds
            }
        })
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
