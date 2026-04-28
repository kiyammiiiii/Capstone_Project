package com.labactivity.handa

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.labactivity.handa.databinding.ActivitySignUpScreenBinding

class SignUpScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpScreenBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setSignIn()
        setTermsAndPolicyText()

        binding.signupId.setOnClickListener {
            val username = binding.usernameId.text.toString()
            val email = binding.emailId.text.toString()
            val password = binding.passwordId.text.toString()
            val confirmPass = binding.confirmId.text.toString()
            val terms = binding.termsId

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                if (username.isEmpty()) binding.usernameIL.error = "Username is Required!"
                if (email.isEmpty()) binding.emailIL.error = "Email is Required!"
                if (password.isEmpty()) binding.passwordIL.error = "Password is Required!"
                if (confirmPass.isEmpty()) binding.confirmIL.error = "Re-enter Password!"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailIL.error = "Please Enter Valid Email Address"
            } else if (!terms.isChecked) {
                Toast.makeText(this, "Please agree to the Terms and Conditions to continue", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                binding.passwordIL.error = "Password needs to be at least 6 characters long!"
            } else if (password != confirmPass) {
                binding.passwordIL.error = "Password does not match!"
                binding.confirmIL.error = "Password does not match!"
            } else {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val currentUserUid = auth.currentUser!!.uid
                        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                        val sensorRef = database.reference.child("users").child(currentUserUid)

                        // Check if device is already authorized
                        sensorRef.child("authorizedPhones").addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val authorizedMap = snapshot.value as? Map<String, String> ?: emptyMap()

                                if (authorizedMap.containsValue(androidId)) {
                                    val intent = Intent(this@SignUpScreen, CompleteProfile::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    finish()
                                } else if (authorizedMap.size < 2) {
                                    // Room for one more device
                                    val newKey = if (!authorizedMap.containsKey("phone1")) "phone1" else "phone2"
                                    sensorRef.child("authorizedPhones").child(newKey).setValue(androidId)
                                        .addOnSuccessListener {
                                            val intent = Intent(this@SignUpScreen, CompleteProfile::class.java)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            startActivity(intent)
                                            finish()

                                        }
                                } else {
                                    Toast.makeText(this@SignUpScreen, "Only 2 devices are allowed per user", Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@SignUpScreen, "Error checking authorized devices", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        if (it.exception?.message?.contains("email address is already in use") == true) {
                            binding.emailIL.error = "Email is already registered"
                        } else {
                            Toast.makeText(this, "Something went wrong! Try again!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setTermsAndPolicyText() {
        val termsAndPolicyText = getString(R.string.terms_conditions)
        val spannableString = SpannableString(termsAndPolicyText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                openTermsAndPolicy()
            }
        }

        val startIndex = termsAndPolicyText.indexOf("Terms and Conditions")
        val endIndex = startIndex + "Terms and Conditions".length

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.termsTxt.text = spannableString
        binding.termsTxt.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openTermsAndPolicy() {
        val termsText = getString(R.string.terms)
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Terms and Conditions")
            .setMessage(termsText)
            .setPositiveButton("I agree") { dialog, _ ->
                binding.termsId.isChecked = true
                dialog.dismiss()
            }.create()

        alertDialog.show()
    }

    private fun setSignIn() {
        val signInText = getString(R.string.sign_in)
        val spannableString = SpannableString(signInText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                openSignIn()
            }
        }

        val startIndex = signInText.indexOf("Sign In")
        val endIndex = startIndex + "Sign In".length

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.signinTxt.text = spannableString
        binding.signinTxt.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openSignIn() {
        val intent = Intent(this, LogInScreen::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
