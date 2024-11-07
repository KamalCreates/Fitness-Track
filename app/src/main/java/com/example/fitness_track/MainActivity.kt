package com.example.fitness_track

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null

    private lateinit var stepCountTextView: TextView
    private var initialStepCount = 0  // Store initial step count value from SENSOR_TYPE_STEP_COUNTER
    private var currentStepCount = 0  // Calculate steps since the app was launched

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1
        private const val PREFS_NAME = "FitnessPrefs"
        private const val KEY_STEP_COUNT = "step_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the UI components
        stepCountTextView = findViewById(R.id.step_count_text_view)

        // Load the saved step count from SharedPreferences
        loadStepCount()

        // Request the necessary permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), REQUEST_CODE_PERMISSION)
        }

        // Get the system sensor service
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Get the step counter sensor and step detector sensor
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepCounterSensor == null) {
            Toast.makeText(this, "Step Counter sensor not available", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the listeners for the sensors with normal delay
        stepCounterSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Optionally, you can register the step detector for more detailed step events
        stepDetectorSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listeners to save resources
        sensorManager.unregisterListener(this)

        // Save the current step count to SharedPreferences before the app is paused
        saveStepCount()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // Handle step counter sensor
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            // If it's the first time the sensor triggers, save the initial count value
            if (initialStepCount == 0) {
                initialStepCount = event.values[0].toInt()
            }
            // Calculate steps based on the difference between the current step count and the initial value
            val steps = event.values[0].toInt() - initialStepCount
            stepCountTextView.text = "Steps: $steps"
        }

        // Handle step detector sensor (detect individual steps)
        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            val stepsDetected = event.values[0].toInt()
            if (stepsDetected > 0) {
                currentStepCount += stepsDetected
                stepCountTextView.text = "Steps: $currentStepCount"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required for this example
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Save the current step count to SharedPreferences
    private fun saveStepCount() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_STEP_COUNT, currentStepCount)
        editor.apply()
    }

    // Load the saved step count from SharedPreferences
    private fun loadStepCount() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentStepCount = sharedPreferences.getInt(KEY_STEP_COUNT, currentStepCount)
        stepCountTextView.text = "Count: $currentStepCount"
    }
}
