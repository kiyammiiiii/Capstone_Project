package com.labactivity.handa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.labactivity.handa.databinding.ActivityAddDeviceBinding

class AddDevice : AppCompatActivity() {
    private lateinit var binding: ActivityAddDeviceBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        binding.saveDeviceID.setOnClickListener {
            val deviceID = binding.deviceID.text.toString()

            if (deviceID.isEmpty()) {
                if (deviceID.isEmpty()) binding.deviceIL.error = "Field cannot be empty!"
            } else {
                saveDeviceID(deviceID)
            }
        }
    }


    private fun saveDeviceID(deviceID: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userDeviceRef = database.reference.child("users").child(uid)
            val userData = mapOf("deviceID" to deviceID)

            userDeviceRef.updateChildren(userData).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Device ID saved!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save Device ID. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
}
