package com.labactivity.handa

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.labactivity.handa.databinding.ActivityEditContactsBinding
import com.labactivity.handa.databinding.ActivityEditDeviceBinding
import com.labactivity.handa.databinding.ActivityEditProfileBinding

class EditDevice : AppCompatActivity() {

    private lateinit var binding: ActivityEditDeviceBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        loadExistingData()

        binding.saveDeviceId.setOnClickListener {
            val deviceID = binding.editDeviceID.text.toString()

            if (deviceID.isEmpty()) {
                if (deviceID.isEmpty()) binding.editDeviceIL.error = "Field Cannot be Empty!"
            } else {
                saveDeviceID(deviceID)
            }
        }

        val toolbar = binding.editContactsToolbar
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, UserProfile::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun loadExistingData() {
        val currentUser = auth.currentUser
        currentUser?.let {
            val userRef = database.reference.child("users").child(currentUser.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.editDeviceID.setText(snapshot.child("deviceID").getValue(String::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditDevice, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
            })
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
