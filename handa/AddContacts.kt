package com.labactivity.handa

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.labactivity.handa.databinding.ActivityAddContactsBinding
import com.labactivity.handa.databinding.ActivityCompleteProfileBinding

class AddContacts : AppCompatActivity() {
    private lateinit var binding: ActivityAddContactsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        binding.saveContactId.setOnClickListener {
            val lastName = binding.firstNumid.text.toString()
            val firstName = binding.secNumId.text.toString()
            val address = binding.thirdNumId.text.toString()

            if (lastName.isEmpty() || firstName.isEmpty() || address.isEmpty()) {
                if (lastName.isEmpty()) binding.firstNumIL.error = "Last name is required!"
                if (firstName.isEmpty()) binding.secNumIL.error = "First name is required!"
                if (address.isEmpty()) binding.thirdNumIL.error = "Street address is required!"
            } else {
                saveUserData(lastName, firstName, address)
            }
        }
    }



    private fun saveUserData(firstNum: String, secNum: String, thirdNum: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val databaseRef = database.reference.child("users").child(currentUser.uid)
            val userData = mapOf(
                "firstNum" to firstNum,
                "secNum" to secNum,
                "thirdNum" to thirdNum
            )
            databaseRef.updateChildren(userData).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Contacts updated successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AddDevice::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this, "Failed to save contact data. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
}
