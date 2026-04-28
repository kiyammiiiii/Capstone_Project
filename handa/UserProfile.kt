package com.labactivity.handa

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener

class UserProfile : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mDatabase: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference
        retrieveUserProfile()

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
            true

        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24)

        fetchUserData()

        findViewById<ImageView>(R.id.edit_name).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<ImageView>(R.id.edit_address).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<ImageView>(R.id.edit_contacts).setOnClickListener {
            startActivity(Intent(this, EditContacts::class.java))
        }

        findViewById<ImageView>(R.id.edit_deviceID).setOnClickListener {
            startActivity(Intent(this, EditDevice::class.java))
        }

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

            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
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

    private fun retrieveUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            println("DEBUG: Current user ID: $userId")

            mDatabase.child("users").child(userId).get().addOnCompleteListener { userTask ->
                if (userTask.isSuccessful && userTask.result.exists()) {
                    val userData = userTask.result.value as? Map<*, *>
                    println("DEBUG: Fetched user data: $userData")

                    val firstName = userData?.get("firstName") as? String
                    val lastName = userData?.get("lastName") as? String
                    val address = userData?.get("address") as? String

                    // Capitalize first and last name before displaying them
                    findViewById<TextView>(R.id.name).text =
                        "${capitalizeFirstLetter(firstName)} ${capitalizeFirstLetter(lastName)}"
                    findViewById<TextView>(R.id.address).text = address

                    // Get emergency contacts directly from the user data
                    val firstNum = userData?.get("firstNum") as? String
                    val secNum = userData?.get("secNum") as? String
                    val thirdNum = userData?.get("thirdNum") as? String

                    // Update emergency contact numbers in UI
                    findViewById<TextView>(R.id.contact_1).text = firstNum ?: "Not available"
                    findViewById<TextView>(R.id.contact_2).text = secNum ?: "Not available"
                    findViewById<TextView>(R.id.contact_3).text = thirdNum ?: "Not available"

                    val deviceID = userData?.get("deviceID") as? String

                    findViewById<TextView>(R.id.deviceID).text = deviceID ?: "Not available"


                } else {
                    println("ERROR: Failed to retrieve user data for user ID: $userId. Exception: ${userTask.exception}")
                }
            }
        } else {
            println("ERROR: No authenticated user found.")
        }
    }


    // Helper function to capitalize the first letter of a string
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
                        this@UserProfile,
                        "Failed to load user data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

}
