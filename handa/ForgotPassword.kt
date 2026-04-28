package com.labactivity.handa

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.core.view.isInvisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.labactivity.handa.databinding.ActivityForgotPasswordBinding

class ForgotPassword : AppCompatActivity() {
    private  lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    lateinit var strEmail: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = Intent(this, ForgotPassword::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.resetId.setOnClickListener {
            strEmail = binding.emailFo.text.toString().trim()
            if (!TextUtils.isEmpty(strEmail)) {
                ResetPassword();
            } else {
                binding.emailIL.setError("Email field can't be empty");
            }
        }

        binding.backId.setOnClickListener {
            val intent = Intent(this, LogInScreen::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun ResetPassword() {

        binding.resetId.isInvisible

        auth.sendPasswordResetEmail(strEmail).addOnSuccessListener {
            Toast.makeText(this, "Reset Password has been sent to your Email", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LogInScreen::class.java)
            startActivity(intent)
            finish()
        }.addOnFailureListener{
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()



        }
    }
}