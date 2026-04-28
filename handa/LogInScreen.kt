package com.labactivity.handa

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.labactivity.handa.databinding.ActivityLogInScreenBinding
import com.labactivity.handa.databinding.ActivitySignUpScreenBinding

class LogInScreen : AppCompatActivity() {
    private lateinit var binding: ActivityLogInScreenBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setSignUp()
        setForgot()

        binding.loginId.setOnClickListener {
            val email = binding.emailLi.text.toString()
            val password = binding.passwordLi.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                if (email.isEmpty()) {
                    binding.emailIL.error = "Please Enter your Email!"
                }
                if (password.isEmpty()) {
                    binding.passwordIL.error = "Please Enter Password!"
                }
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailIL.error = "Please Enter Valid Email Address"

            } else if (password.length < 6) {
                binding.passwordIL.error = "Password needs to be at least 6 characters long!"
            } else {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {

                        val currentUserUid = auth.currentUser!!.uid
                        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                        val sensorRef = database.reference.child("users").child(currentUserUid)

                        // Check if device is already authorized
                        sensorRef.child("authorizedPhones").addListenerForSingleValueEvent(object :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val authorizedMap = snapshot.value as? Map<String, String> ?: emptyMap()

                                if (authorizedMap.containsValue(androidId)) {
                                    val intent = Intent(this@LogInScreen, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    finish()
                                } else if (authorizedMap.size < 2) {
                                    // Room for one more device
                                    val newKey = if (!authorizedMap.containsKey("phone1")) "phone1" else "phone2"
                                    sensorRef.child("authorizedPhones").child(newKey).setValue(androidId)
                                        .addOnSuccessListener {
                                            val intent = Intent(this@LogInScreen, MainActivity::class.java)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            startActivity(intent)
                                            finish()
                                        }
                                } else {
                                    Toast.makeText(this@LogInScreen, "Only 2 devices are allowed per user", Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@LogInScreen, "Error checking authorized devices", Toast.LENGTH_SHORT).show()
                            }
                        })


                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }

    }


    private fun setSignUp() {
        val signUpText = getString(R.string.sign_up)
        val spannableString = SpannableString(signUpText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                openSignUp()
            }
        }

        val startIndex = signUpText.indexOf("Sign Up")
        val endIndex = startIndex + "Sign Up".length

        spannableString.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.signupTxt.text = spannableString
        binding.signupTxt.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openSignUp() {
        val intent = Intent(this, SignUpScreen::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun setForgot() {
        val forgotText = getString(R.string.forgot)
        val spannableString = SpannableString(forgotText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                openForgot()
            }
        }

        val startIndex = forgotText.indexOf("Forgot Password")
        val endIndex = startIndex + "Forgot Password".length

        spannableString.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.forgotPass.text = spannableString
        binding.forgotPass.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openForgot() {
        val intent = Intent(this, ForgotPassword::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}