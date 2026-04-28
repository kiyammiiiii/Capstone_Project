package com.labactivity.handa

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.Animation
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class FireAlertActivity : Activity(), GestureDetector.OnGestureListener {

    private lateinit var imageView: ImageView
    private lateinit var timerTextView: TextView
    private lateinit var dismiss: ImageView
    private lateinit var call: ImageView
    private lateinit var mDatabase: DatabaseReference
    private var emergencyNumbers: List<String> = listOf()
    private var countDownTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var callData: CallData? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private lateinit var notificationManager: NotificationManager
    private val CHANNEL_ID = "sensor_alert_channel"
    private lateinit var firestore: FirebaseFirestore
    private lateinit var gestureDetector: GestureDetector
    private val currentUser = FirebaseAuth.getInstance().currentUser



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fire_alert)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        firestore = FirebaseFirestore.getInstance()

        imageView = findViewById(R.id.imageView)
        gestureDetector = GestureDetector(this, this)

        dismiss = findViewById(R.id.swipeleft)
        call = findViewById(R.id.swiperight)

        val pulseAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.pulse)

        // Start the animation on both TextViews
        dismiss.startAnimation(pulseAnimation)
        call.startAnimation(pulseAnimation)


        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager?.getCameraCharacteristics(id)
                    ?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        turnFlashlightOn()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        vibrator = getSystemService(Vibrator::class.java)

        currentUser?.uid?.let { listenForAlertStatusChanges(it) }
        startContinuousVibration()
        startAlarmSound()

        imageView = findViewById(R.id.imageView)
        timerTextView = findViewById(R.id.timerTextView)
        mDatabase = FirebaseDatabase.getInstance().getReference()


        fetchAndDisplayImage()
        retrieveUserContactsAndSensorData()
        startCountdownTimer()

    }

    private fun listenForAlertStatusChanges(userId: String) {
        val alertStatusRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        alertStatusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("alert_acknowledged").getValue(Boolean::class.java)
                if (status == true) {
                    stopAlert()

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun retrieveUserContactsAndSensorData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            println("DEBUG: Current user ID: $userId")

            mDatabase.child("users").child(userId).get().addOnCompleteListener { userTask ->
                if (userTask.isSuccessful && userTask.result.exists()) {
                    val userData = userTask.result.value as? Map<*, *>
                    println("DEBUG: Fetched user data: $userData")

                    val address = userData?.get("address") as? String
                    val firstName = userData?.get("firstName") as? String
                    val lastName = userData?.get("lastName") as? String

                    val firstNum = userData?.get("firstNum") as? String
                    val secNum = userData?.get("secNum") as? String
                    val thirdNum = userData?.get("thirdNum") as? String

                    emergencyNumbers = listOfNotNull(firstNum, secNum, thirdNum)

                    val sensorRef = mDatabase.child("sensor_data").child(userId)

                    sensorRef.get().addOnCompleteListener { sensorTask ->
                        if (sensorTask.isSuccessful && sensorTask.result.exists()) {
                            val sensorData = sensorTask.result.value as? Map<*, *>
                            val temperature = sensorData?.get("temperature")?.toString()?.toFloatOrNull()
                            val smokeLevel = sensorData?.get("smoke_level")?.toString()?.toIntOrNull()

                            val temperatureMessage = if (temperature != null) {
                                "The current temperature is ${"%.1f".format(temperature)} degrees Celsius."
                            } else {
                                "Temperature data is unavailable."
                            }

                            val smokeMessage = if (smokeLevel != null) {
                                "The smoke level is $smokeLevel."
                            } else {
                                "Smoke data is unavailable."
                            }

                            val emergencyMessage = if (!address.isNullOrEmpty()) {
                                "My house is on fire! Please send help immediately to $firstName $lastName residence $address. $temperatureMessage $smokeMessage, " +
                                        "My house is on fire! Please send help immediately to $firstName $lastName residence $address. $temperatureMessage $smokeMessage"
                            } else {
                                "House is on fire. Please send help! $temperatureMessage $smokeMessage"
                            }

                            callData = CallData(
                                emergency_numbers = emergencyNumbers,
                                message = emergencyMessage,
                                address = address.orEmpty()
                            )

                            println("DEBUG: Call data prepared: emergencyNumbers=$emergencyNumbers, message=${callData?.message}, address=${callData?.address}")
                        } else {
                            println("ERROR: Failed to retrieve sensor data. Exception: ${sensorTask.exception}")
                        }
                    }
                } else {
                    println("ERROR: Failed to retrieve user data for user ID: $userId. Exception: ${userTask.exception}")
                }
            }
        } else {
            println("ERROR: No authenticated user found.")
        }
    }




    private fun startCountdownTimer() {
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                makeAutomaticCall()
                stopVibration()
                stopAlarmSound()
                turnFlashlightOff()
                sendNotification()
                finish()
            }
        }.start()
    }

    private fun makeAutomaticCall() {
        if (callData == null) {
            println("ERROR: Call data is not initialized.")
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://1fbf-119-92-72-19.ngrok-free.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(TwilioApi::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val callPayload = CallData(
                    emergency_numbers = emergencyNumbers,
                    message = callData!!.message,
                    address = callData!!.address // Send address here
                )
                val response = api.makeCall(callPayload)
                if (response.isSuccessful) {
                    println("Call initiated, SID: ${response.body()?.call_sid}")
                    checkCallStatus()
                } else {
                    println("Error: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                println("Error making call: ${e.message}")
            }
        }
    }

    private fun checkCallStatus() {
        val statusRef = FirebaseDatabase.getInstance().getReference("call_status/final_status")
        statusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "failure") {
                    sendCallStatusNotification()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun makeManualCall() {
        if (emergencyNumbers.isEmpty()) {
            println("ERROR: No emergency numbers available.")
            return
        }
        val emergencyNumber = emergencyNumbers.first()
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyNumber"))
        currentUser?.uid?.let { updateAlertStatus(it, true) }
        startActivity(dialIntent)
    }

    private fun applyPulseAnimation() {
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        imageView.startAnimation(pulseAnimation)
    }

    private fun startContinuousVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 500, 100), 0)
            vibrator?.vibrate(vibrationEffect)
        } else {
            vibrator?.vibrate(longArrayOf(0, 500, 100), 0)
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

    private fun turnFlashlightOn() {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager?.setTorchMode(cameraId!!, true)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun turnFlashlightOff() {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager?.setTorchMode(cameraId!!, false)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVibration()
        stopAlarmSound()
        turnFlashlightOff()
    }

    //Automatic Call Notification

    private fun sendNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calling Emergency Numbers")
            .setContentText("We are calling your emergency contacts now.")
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(2, notification)
    }

    //Call Status Notification

    private fun sendCallStatusNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("All call attemps failed!")
            .setContentText("We were unable to reach any emergency contact.")
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(2, notification)
    }

    //Decode and Display Image

    private fun fetchAndDisplayImage() {
        val firestore = FirebaseFirestore.getInstance()
        val documentId = "fire_image_capture"

        // Introduce a 3-second delay before fetching the image
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(3000) // Delay for 3 seconds

            firestore.collection("fire_images").document(documentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val base64String = document.getString("image")
                        if (!base64String.isNullOrEmpty()) {
                            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                            imageView.setImageBitmap(bitmap)

                            val pulseAnimation = AnimationUtils.loadAnimation(this@FireAlertActivity, R.anim.pulse)
                            imageView.startAnimation(pulseAnimation)
                        } else {
                            Toast.makeText(this@FireAlertActivity, "Image data is empty!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@FireAlertActivity, "Document does not exist!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this@FireAlertActivity, "Error retrieving document: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun updateAlertStatus(userId: String, acknowledged: Boolean) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("alert_acknowledged")

        ref.setValue(acknowledged)
    }

    fun stopAlertAndResetStatus(userId: String) {
        stopAlert()

        Handler(Looper.getMainLooper()).postDelayed({
            val ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("alert_acknowledged")

            ref.setValue(false)
        }, 10000) // 10 seconds delay
    }



    private fun stopAlert() {
        stopVibration()
        stopAlarmSound()
        turnFlashlightOff()
        countDownTimer?.cancel()
        finish()
    }





    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean = true

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean = false

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
                        makeManualCall()
                        currentUser?.uid?.let { updateAlertStatus(it, true) }
                        stopAlert()
                        currentUser?.uid?.let { stopAlertAndResetStatus(it) }

                        finish()
                    } else {
                        currentUser?.uid?.let { updateAlertStatus(it, true) }
                        stopAlert()
                        currentUser?.uid?.let { stopAlertAndResetStatus(it) }

                        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("Emergency Call Dismissed")
                            .setContentText("You dismissed the emergency alert.")
                            .setSmallIcon(R.drawable.baseline_warning_24)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setAutoCancel(true)
                            .build()

                        getSystemService(NotificationManager::class.java).notify(2, notification)
                        finish()
                    }
                    return true
                }
            }
        }
        return false
    }

}