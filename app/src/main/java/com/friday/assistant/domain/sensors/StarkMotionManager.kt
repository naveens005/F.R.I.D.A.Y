package com.friday.assistant.domain.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Stark Industries Tactical Motion & Gyro Gesture Engine.
 * Detects:
 * 1. Double-twist wrist / quick shake -> Triggers instant voice assistant summon.
 * 2. Flip face-down -> Instantly silences active speech (Barge-in / tactical mute).
 */
class StarkMotionManager(
    private val context: Context,
    private val onShakeTriggered: () -> Unit,
    private val onFaceDownDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTimestamp: Long = 0
    private var shakeCount = 0
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var shakeThreshold = 14.5f

    private var isFaceDown = false
    private var isRegistered = false

    fun startListening() {
        if (isRegistered || accelerometer == null) return
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        isRegistered = true
    }

    fun stopListening() {
        if (!isRegistered) return
        sensorManager?.unregisterListener(this)
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Detect Flip Face-Down (Tactical Mute)
        // Normal phone face-up on table has z ≈ +9.8. Face-down has z ≈ -9.8.
        if (z < -7.5f && Math.abs(x) < 4.0f && Math.abs(y) < 4.0f) {
            if (!isFaceDown) {
                isFaceDown = true
                onFaceDownDetected()
            }
        } else if (z > 2.0f) {
            isFaceDown = false
        }

        // 2. Detect Wrist Shake / Double-Twist Summon
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = magnitude - currentAcceleration
        currentAcceleration = magnitude
        lastAcceleration = lastAcceleration * 0.9f + delta

        if (Math.abs(lastAcceleration) > shakeThreshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTimestamp > 300) {
                shakeCount++
                lastShakeTimestamp = now
                if (shakeCount >= 2) {
                    shakeCount = 0
                    onShakeTriggered()
                }
            }
        }

        // Reset shake count if too much time elapsed
        if (System.currentTimeMillis() - lastShakeTimestamp > 1200) {
            shakeCount = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

