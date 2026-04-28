package com.labactivity.handa

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore

class AlertActivity : Activity(), GestureDetector.OnGestureListener {

    private lateinit var imageView: ImageView
    private lateinit var dismissText: TextView
    private lateinit var callText: TextView
    private lateinit var mDatabase: DatabaseReference
    private var emergencyNumber: String = "0991511868"
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var notificationManager: NotificationManager
    private val CHANNEL_ID = "sensor_alert_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        vibrator = getSystemService(Vibrator::class.java)
        gestureDetector = GestureDetector(this, this)

        startContinuousVibration()
        startAlarmSound()

        dismissText = findViewById(R.id.dismissText)
        callText = findViewById(R.id.callText)

        val pulseAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.pulse)

        dismissText.startAnimation(pulseAnimation)
        callText.startAnimation(pulseAnimation)

        imageView = findViewById(R.id.imageView)
        mDatabase = FirebaseDatabase.getInstance().getReference()

        val firestore = FirebaseFirestore.getInstance()
        val documentId = "fire_image_capture"

        firestore.collection("fire_images").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val base64String = document.getString("image")
                    if (!base64String.isNullOrEmpty()) {
                        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        imageView.setImageBitmap(bitmap)
                        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
                        imageView.startAnimation(pulseAnimation)
                    } else {
                        Toast.makeText(this, "Image data is empty!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Document does not exist!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error retrieving document: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        retrieveBarangayAndHotline()
    }

    private fun retrieveBarangayAndHotline() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            mDatabase.child("users").child(userId).child("barangay").get()
                .addOnCompleteListener { userTask ->
                    if (userTask.isSuccessful && userTask.result.exists()) {
                        val barangay = userTask.result.getValue(String::class.java)
                        barangay?.let { fetchedBarangay ->
                            mDatabase.child("QC_hotlines").child(fetchedBarangay).child("p1").get()
                                .addOnCompleteListener { barangayTask ->
                                    if (barangayTask.isSuccessful && barangayTask.result.exists()) {
                                        emergencyNumber =
                                            barangayTask.result.getValue(String::class.java) ?: emergencyNumber
                                    }
                                }
                        }
                    }
                }
        }
    }

    private fun makeEmergencyCall() {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyNumber"))
        startActivity(intent)
    }

    private fun startContinuousVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect =
                VibrationEffect.createWaveform(longArrayOf(0, 500, 100, 500, 100, 500), 0)
            vibrator?.vibrate(vibrationEffect)
        } else {
            vibrator?.vibrate(longArrayOf(0, 500, 100, 500, 100, 500), 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun startAlarmSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean = true

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent):Boolean = false

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean = false

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 != null) {
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                    if (diffX > 0) {
                        // Swipe Right: Call Emergency
                        stopVibration()
                        stopAlarmSound()
                        makeEmergencyCall()
                        finish()
                    } else {
                        // Swipe Left: Dismiss Alert
                        stopVibration()
                        stopAlarmSound()
                        sendNotification()
                        finish() // Close the activity
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun sendNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dismiss Alert Notification")
            .setContentText("You have dismissed the alert notification")
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(2, notification)
    }
}
