package com.labactivity.handa

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.labactivity.handa.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        loadExistingData()

        binding.saveEditId.setOnClickListener {
            val lastName = binding.editLastnameid.text.toString()
            val firstName = binding.editFirstnameId.text.toString()
            val street = binding.editStreetId.text.toString()

            if (lastName.isEmpty() || firstName.isEmpty() || street.isEmpty()) {
                if (lastName.isEmpty()) binding.editLastnameIL.error = "Last name is required!"
                if (firstName.isEmpty()) binding.editFirstnameIL.error = "First name is required!"
                if (street.isEmpty()) binding.editStreetIL.error = "Street address is required!"
            } else {
                saveUserData(lastName, firstName, street)
            }
        }

        val toolbar = binding.editProfileToolbar
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
                    binding.editLastnameid.setText(snapshot.child("lastName").getValue(String::class.java))
                    binding.editFirstnameId.setText(snapshot.child("firstName").getValue(String::class.java))
                    binding.editStreetId.setText(snapshot.child("address").getValue(String::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditProfileActivity, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveUserData(lastName: String, firstName: String, street: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val databaseRef = database.reference.child("users").child(currentUser.uid)
            val userData = mapOf(
                "lastName" to lastName,
                "firstName" to firstName,
                "address" to street
            )
            databaseRef.updateChildren(userData).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, UserProfile::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save changes. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }
}
