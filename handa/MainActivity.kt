package com.labactivity.handa

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.material.navigation.NavigationView
import com.labactivity.handa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val CHANNEL_ID = "sensor_alert_channel"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private val tempThreshold = 40.0f
    private val smokeThreshold = 50.0f
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tempIcon: ImageView
    private lateinit var smokeIcon: ImageView
    private lateinit var humidIcon: ImageView
    private lateinit var firebaseAuth: FirebaseAuth
    private val firebaseHelper = FirebaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseHelper.sendBarangaysToFirebase()

        // Check if user is signed in
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, LogInScreen::class.java))
            finish()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

         tempIcon = findViewById(R.id.temp_disp)
         smokeIcon = findViewById(R.id.smoke_disp)
         humidIcon = findViewById(R.id.humidity_disp)


        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
            true
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Display hamburger icon
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24)  // Make sure ic_menu exists in drawable

        startService(Intent(this, SensorMonitoringService::class.java))

        FetchAndDisplay()
        fetchUserData()
        createNotificationChannel()
    }

    private fun handleNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_logout -> {
                firebaseAuth.signOut()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LogInScreen::class.java))
                finish()
            }
            R.id.nav_about -> {
                val intent = Intent(this, AboutApp::class.java)
                startActivity(intent)
            }

            R.id.nav_profile -> {
                val intent = Intent(this, UserProfile::class.java)
                startActivity(intent)
            }

        }

        drawerLayout.closeDrawers()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun FetchAndDisplay() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val database = FirebaseDatabase.getInstance()

        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = database.reference.child("users").child(uid)

            userRef.child("deviceID").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deviceID = snapshot.getValue(String::class.java)
                    if (deviceID != null) {
                        displaySensorData(deviceID)
                    } else {
                        Toast.makeText(this@MainActivity, "No device ID found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Failed to fetch device ID.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun displaySensorData(deviceID : String) {
        val userSensorRef = FirebaseDatabase.getInstance().getReference("sensor_data").child(deviceID)


        userSensorRef.child("temperature").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperature: Float = snapshot.getValue(Float::class.java) ?: 0f
                val formattedTemperature = String.format("%.1f\u00B0C", temperature)
                binding.tempValueDisp.text = formattedTemperature
                if (temperature >= tempThreshold) sendAlertBroadcast("Temperature Alert")

                when {
                    temperature <= 0.0 -> tempIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.blue), PorterDuff.Mode.SRC_IN)
                    temperature >= 50 -> tempIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.red), PorterDuff.Mode.SRC_IN)
                    temperature >= 40 -> tempIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.orange), PorterDuff.Mode.SRC_IN)
                    else -> tempIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.blue), PorterDuff.Mode.SRC_IN)
                }



            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to read temperature data", Toast.LENGTH_SHORT).show()
            }
        })

        userSensorRef.child("smoke").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val smoke: Float = snapshot.getValue(Float::class.java) ?: 0f
                val formattedSmoke = String.format("%.1f", smoke)
                binding.smokeValueDisp.text = formattedSmoke
                if (smoke >= smokeThreshold) sendAlertBroadcast("Smoke Alert")

                when {
                    smoke <= 0.0 -> smokeIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.blue), PorterDuff.Mode.SRC_IN)
                    smoke >= 200 -> smokeIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.red), PorterDuff.Mode.SRC_IN)
                    smoke >= 170 -> smokeIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.orange), PorterDuff.Mode.SRC_IN)
                    else -> smokeIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.blue), PorterDuff.Mode.SRC_IN)
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to read smoke data", Toast.LENGTH_SHORT).show()
            }
        })


        userSensorRef.child("humidity").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val humidity: Float = snapshot.getValue(Float::class.java) ?: 0f
                val formattedHumidity = String.format("%.1f%%", humidity)
                binding.humidityValueDisp.text = formattedHumidity

                when {
                    humidity <= 0 -> humidIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.blue), PorterDuff.Mode.SRC_IN)
                    humidity < 15 -> humidIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.red), PorterDuff.Mode.SRC_IN)
                    humidity <= 30 -> humidIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.orange), PorterDuff.Mode.SRC_IN)
                    else -> humidIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.blue), PorterDuff.Mode.SRC_IN)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to read humidity data", Toast.LENGTH_SHORT).show()
            }
        })

        // Similar logic for smoke and humidity data
    }

    private fun sendAlertBroadcast(alertType: String) {
        val intent = Intent("com.labactivity.handa.SENSOR_ALERT")
        intent.putExtra("alert_type", alertType)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for sensor-based alerts"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted for notifications
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun capitalizeFirstLetter(input: String?): String {
        return input?.let {
            it.trim().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        } ?: ""
    }

    private fun fetchUserData() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance().reference
            val userRef = database.child("users").child(currentUser.uid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val firstName =
                            snapshot.child("firstName").getValue(String::class.java) ?: "First Name"
                        val lastName =
                            snapshot.child("lastName").getValue(String::class.java) ?: "Last Name"
                        val fullName = "${capitalizeFirstLetter(firstName)} ${capitalizeFirstLetter(lastName)}"

                        // Update the TextView with the user's full name
                        findViewById<TextView>(R.id.userNameTextView).text = fullName
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load user data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}
