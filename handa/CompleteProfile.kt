package com.labactivity.handa

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.labactivity.handa.databinding.ActivityCompleteProfileBinding

class CompleteProfile : AppCompatActivity() {
    private lateinit var binding: ActivityCompleteProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        binding.signupId.setOnClickListener {
            val lastName = binding.lastnameid.text.toString()
            val firstName = binding.firstnameId.text.toString()
            val address = binding.streetId.text.toString()

            if (lastName.isEmpty() || firstName.isEmpty() || address.isEmpty()) {
                if (lastName.isEmpty()) binding.lastnameIL.error = "Last name is required!"
                if (firstName.isEmpty()) binding.firstnameIL.error = "First name is required!"
                if (address.isEmpty()) binding.streetIL.error = "Street address is required!"
            } else {
                saveUserData(lastName, firstName, address)
            }
        }
    }



    private fun saveUserData(lastName: String, firstName: String, address: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val databaseRef = database.reference.child("users").child(currentUser.uid)
            val userData = mapOf(
                "lastName" to lastName,
                "firstName" to firstName,
                "address" to address
            )
            databaseRef.updateChildren(userData).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AddContacts::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this, "Failed to save profile data. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
}
