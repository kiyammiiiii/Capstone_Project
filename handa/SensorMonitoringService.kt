package com.labactivity.handa

import android.app.*
import android.content.*
import android.graphics.PorterDuff
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SensorMonitoringService : Service() {
    private val CHANNEL_ID = "sensor_alert_channel"
    private var isSmokeDetected = false
    private var lowHumidity = false
    private var isFireDetected = false
    private var isFireAlertActive = false
    private var temperatureValue = 0f
    private var smokeValue = 0f
    private var humidityValue = 0f
    private var lastFireAlertTimestamp: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var isFireCheckDelayed = false
    private var previousTemperature = 0f
    private var temperatureRiseStartTime: Long = 0L
    private val TEMPERATURE_RISE_THRESHOLD = 10f
    private val TEMPERATURE_RISE_WINDOW_MS = 60000L


    private val fireAlertDismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if ("com.labactivity.handa.FIRE_ALERT_DISMISSED" == intent?.action) {
                isFireAlertActive = false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        SensorMonitor()

        val filter = IntentFilter("com.labactivity.handa.FIRE_ALERT_DISMISSED")
        registerReceiver(fireAlertDismissReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createForegroundNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fireAlertDismissReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoring Sensors")
            .setSmallIcon(R.drawable.logo)
            .setContentText("Monitoring temperature, humidity, and smoke level")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setDefaults(0)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = Uri.parse("android.resource://" + packageName + "/raw/alarm")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                enableLights(true)
                setSound(alarmSound, AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build())
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun SensorMonitor() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val database = FirebaseDatabase.getInstance()

        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = database.reference.child("users").child(uid)

            userRef.child("deviceID").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deviceID = snapshot.getValue(String::class.java)
                    if (deviceID != null) {
                        monitorSensorData(deviceID)
                    } else {
                        Toast.makeText(this@SensorMonitoringService, "No device ID found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SensorMonitoringService, "Failed to fetch device ID.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun monitorSensorData(deviceID : String) {
        val userSensorRef = FirebaseDatabase.getInstance().getReference("sensor_data").child(deviceID)

        userSensorRef.child("temperature").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newTemp = snapshot.getValue(Float::class.java) ?: 0f

                // Detect rapid rise in temperature
                val currentTime = System.currentTimeMillis()
                if (previousTemperature == 0f) {
                    previousTemperature = newTemp
                    temperatureRiseStartTime = currentTime
                } else {
                    if (newTemp - previousTemperature >= TEMPERATURE_RISE_THRESHOLD &&
                        (currentTime - temperatureRiseStartTime <= TEMPERATURE_RISE_WINDOW_MS)) {
                        sendFireAlertNotification()
                    }

                    if (currentTime - temperatureRiseStartTime > TEMPERATURE_RISE_WINDOW_MS) {
                        previousTemperature = newTemp
                        temperatureRiseStartTime = currentTime
                    }
                }

                temperatureValue = newTemp
                checkAndSendNotification()
            }

            override fun onCancelled(error: DatabaseError) {}
        })


        userSensorRef.child("fire_detected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newFireDetected = snapshot.getValue(Boolean::class.java) ?: false

                if (newFireDetected && !isFireDetected) {
                    isFireDetected = true
                    sendFireAlertNotification()

                    isFireCheckDelayed = true
                    handler.postDelayed({
                        isFireCheckDelayed = false
                    }, 60000) // Delay only for re-alert, but reset immediately when false
                } else if (!newFireDetected) {
                    isFireDetected = false
                    isFireAlertActive = false
                    isFireCheckDelayed = false // Reset delay when fire is no longer detected
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        userSensorRef.child("smoke").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newSmokeValue = snapshot.getValue(Float::class.java) ?: 0f
                isSmokeDetected = newSmokeValue > 200
                smokeValue = newSmokeValue
                checkAndSendNotification()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        userSensorRef.child("humidity").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentHumidity: Float = snapshot.getValue(Float::class.java) ?: 0f

                lowHumidity = currentHumidity < 30
                humidityValue = currentHumidity
                checkAndSendNotification()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkAndSendNotification() {
        when {
            isFireDetected && !isFireAlertActive -> sendFireAlertNotification()
            isSmokeDetected -> sendSmokeDetectedNotification()
            (lowHumidity || isSmokeDetected) && temperatureValue >= 50 ->  sendFireAlertNotification()
            temperatureValue >= 60 -> sendDangerousTemperatureNotification()
            temperatureValue >= 50 -> sendHighTemperatureNotification()


        }
    }

    private fun sendHighTemperatureNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("High Temperature")
            .setContentText("Temperature is $temperatureValue°C.")
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        getSystemService(NotificationManager::class.java).notify(3, notification)
    }

    private fun sendDangerousTemperatureNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dangerously High Temperature")
            .setContentText("Temperature is $temperatureValue°C - caution advised!")
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        getSystemService(NotificationManager::class.java).notify(4, notification)
    }

    private fun sendSmokeDetectedNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smoke Detected")
            .setContentText("Smoke level at ${smokeValue}!  Check for possible fire!")
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        getSystemService(NotificationManager::class.java).notify(4, notification)
    }


    private fun sendFireAlertNotification() {
        if (isFireAlertActive) return
        isFireAlertActive = true
        lastFireAlertTimestamp = System.currentTimeMillis()

        val intent = Intent(this, FireAlertActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Critical Fire Alert")
            .setContentText("Fire Detected!")
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(6, notification)
    }
}
