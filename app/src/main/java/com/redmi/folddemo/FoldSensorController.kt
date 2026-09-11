package com.redmi.folddemo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.exp

class FoldSensorController(
    context: Context,
    private val onProgress: (Float) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var baseline: Quaternion? = null
    private var filteredProgress = 0f
    private var lastTimestampNanos = 0L
    private var registered = false

    val isSupported: Boolean
        get() = gyroscope != null && orientationSensor != null

    val sensorName: String
        get() = orientationSensor?.name ?: "unavailable"

    fun start(): Boolean {
        if (registered) return true
        val sensor = orientationSensor ?: return false
        if (gyroscope == null) return false
        resetBaseline()
        registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        Log.i(TAG, "Orientation sensor=${sensor.name}; gyroscope=${gyroscope.name}; registered=$registered")
        return registered
    }

    fun stop() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        registered = false
    }

    fun resetBaseline() {
        baseline = null
        filteredProgress = 0f
        lastTimestampNanos = 0L
        onProgress(0f)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_ROTATION_VECTOR
        ) return

        val current = Quaternion.fromRotationVector(event.values)
        val origin = baseline
        if (origin == null) {
            baseline = current
            lastTimestampNanos = event.timestamp
            onProgress(0f)
            return
        }

        val angle = FoldMath.relativeTwistYDegrees(origin, current)
        val target = FoldMath.progressFromAngle(angle)
        val elapsedSeconds = if (lastTimestampNanos == 0L) {
            0f
        } else {
            ((event.timestamp - lastTimestampNanos) * 1e-9).toFloat().coerceIn(0f, 0.1f)
        }
        lastTimestampNanos = event.timestamp
        val alpha = if (elapsedSeconds <= 0f) 1f else 1f - exp(-elapsedSeconds / SMOOTHING_SECONDS)
        filteredProgress += (target - filteredProgress) * alpha
        onProgress(filteredProgress.coerceIn(0f, 1f))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "FoldSensor"
        private const val SMOOTHING_SECONDS = 0.055f
    }
}
