package com.pastaga.geronimo

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
    // 0.15f = plus réactif et fluide pour l'icône de galerie
    private val SMOOTHING_ALPHA = 0.15f
    
    // Orientation actuelle du téléphone (0, 90, 180, 270)
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
     * Retourne l'orientation actuelle du téléphone en degrés
     * 0 = Portrait normal, 90 = Paysage (rotation à droite), 180 = Portrait inversé, 270 = Paysage (rotation à gauche)
     */
    fun getDeviceOrientation(): Int {
        return currentDeviceOrientation
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Appliquer un filtre passe-bas sur les valeurs de l'accéléromètre
            gravity[0] = SMOOTHING_ALPHA * gravity[0] + (1 - SMOOTHING_ALPHA) * event.values[0]
            gravity[1] = SMOOTHING_ALPHA * gravity[1] + (1 - SMOOTHING_ALPHA) * event.values[1]
            gravity[2] = SMOOTHING_ALPHA * gravity[2] + (1 - SMOOTHING_ALPHA) * event.values[2]

            // Calculer l'angle de rotation pour que l'icône reste "droite" par rapport à la gravité
            // atan2 donne un angle entre -PI et PI
            // Positif pour que l'icône compense : téléphone penche à gauche → icône tourne à droite
            val rotation = Math.toDegrees(Math.atan2(gravity[0].toDouble(), gravity[1].toDouble())).toFloat()
            
            // Appliquer le filtre sur la rotation finale
            currentRotation = SMOOTHING_ALPHA * currentRotation + (1 - SMOOTHING_ALPHA) * rotation
            
            // Déterminer l'orientation du téléphone (0, 90, 180, 270) pour la prise de photo
            currentDeviceOrientation = when {
                kotlin.math.abs(gravity[1]) > kotlin.math.abs(gravity[0]) -> {
                    // Portrait ou portrait inversé (gravité principalement sur l'axe Y)
                    if (gravity[1] > 0) 0 else 180  // Portrait normal : 0° (gravity positif), Portrait inversé : 180°
                }
                else -> {
                    // Paysage gauche ou paysage droite (gravité principalement sur l'axe X)
                    if (gravity[0] > 0) 90 else 270  // Paysage droite : 90°, Paysage gauche : 270°
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


