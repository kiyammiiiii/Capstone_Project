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
import com.labactivity.handa.databinding.ActivityEditProfileBinding

class EditContacts : AppCompatActivity() {

    private lateinit var binding: ActivityEditContactsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        loadExistingData()

        binding.saveContactId.setOnClickListener {
            val firstNumber = binding.editFirstNumid.text.toString()
            val secondNumber = binding.editSecNumId.text.toString()
            val thirdNumber = binding.editThirdNumId.text.toString()

            if (firstNumber.isEmpty() || secondNumber.isEmpty() || thirdNumber.isEmpty()) {
                if (firstNumber.isEmpty()) binding.editFirstNumIL.error = "Field Cannot be Empty!"
                if (secondNumber.isEmpty()) binding.editSecNumIL.error = "Field Cannot be Empty!"
                if (thirdNumber.isEmpty()) binding.editThirdNumIL.error = "Field Cannot be Empty!"
            } else {
                saveUserData(firstNumber, secondNumber, thirdNumber)
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
                    binding.editFirstNumid.setText(snapshot.child("firstNum").getValue(String::class.java))
                    binding.editSecNumId.setText(snapshot.child("secNum").getValue(String::class.java))
                    binding.editThirdNumId.setText(snapshot.child("thirdNum").getValue(String::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditContacts, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveUserData(firstNumber: String, secondNumber: String, thirdNumber: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val databaseRef = database.reference.child("users").child(currentUser.uid)
            val userData = mapOf(
                "firstNum" to firstNumber,
                "secNum" to secondNumber,
                "thirdNum" to thirdNumber,
            )
            databaseRef.updateChildren(userData).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Contacts updated successfully!", Toast.LENGTH_SHORT).show()
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
