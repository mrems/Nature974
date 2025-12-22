package com.geronimo.geki

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt

class PhoneOrientationSensor(private val context: Context, private val listener: (Float) -> Unit) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravity = FloatArray(3)

    private var currentRotation: Float = 0f
    // Lissage pour une rotation fluide (0.0f = pas de lissage, 1.0f = pas de changement)
    // 0.15f = plus rÃ©actif et fluide pour l'icÃ´ne de galerie
    private val SMOOTHING_ALPHA = 0.15f
    
    // Orientation actuelle du tÃ©lÃ©phone (0, 90, 180, 270)
    private var currentDeviceOrientation: Int = 0

    fun startListening() {
        accelerometer?.also { 
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }
    
    /**
     * Retourne l'orientation actuelle du tÃ©lÃ©phone en degrÃ©s
     * 0 = Portrait normal, 90 = Paysage (rotation Ã  droite), 180 = Portrait inversÃ©, 270 = Paysage (rotation Ã  gauche)
     */
    fun getDeviceOrientation(): Int {
        return currentDeviceOrientation
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Appliquer un filtre passe-bas sur les valeurs de l'accÃ©lÃ©romÃ¨tre
            gravity[0] = SMOOTHING_ALPHA * gravity[0] + (1 - SMOOTHING_ALPHA) * event.values[0]
            gravity[1] = SMOOTHING_ALPHA * gravity[1] + (1 - SMOOTHING_ALPHA) * event.values[1]
            gravity[2] = SMOOTHING_ALPHA * gravity[2] + (1 - SMOOTHING_ALPHA) * event.values[2]

            // Calculer l'angle de rotation pour que l'icÃ´ne reste "droite" par rapport Ã  la gravitÃ©
            // atan2 donne un angle entre -PI et PI
            // Positif pour que l'icÃ´ne compense : tÃ©lÃ©phone penche Ã  gauche â†’ icÃ´ne tourne Ã  droite
            val rotation = Math.toDegrees(Math.atan2(gravity[0].toDouble(), gravity[1].toDouble())).toFloat()
            
            // Appliquer le filtre sur la rotation finale
            currentRotation = SMOOTHING_ALPHA * currentRotation + (1 - SMOOTHING_ALPHA) * rotation
            
            // DÃ©terminer l'orientation du tÃ©lÃ©phone (0, 90, 180, 270) pour la prise de photo
            currentDeviceOrientation = when {
                kotlin.math.abs(gravity[1]) > kotlin.math.abs(gravity[0]) -> {
                    // Portrait ou portrait inversÃ© (gravitÃ© principalement sur l'axe Y)
                    if (gravity[1] > 0) 0 else 180  // Portrait normal : 0Â° (gravity positif), Portrait inversÃ© : 180Â°
                }
                else -> {
                    // Paysage gauche ou paysage droite (gravitÃ© principalement sur l'axe X)
                    if (gravity[0] > 0) 90 else 270  // Paysage droite : 90Â°, Paysage gauche : 270Â°
                }
            }
            
            // Notifier le listener
            listener.invoke(currentRotation)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for this purpose
    }
}


