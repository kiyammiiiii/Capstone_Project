package com.labactivity.handa

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AboutApp : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)
        firebaseAuth = FirebaseAuth.getInstance()



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
                        this@AboutApp,
                        "Failed to load user data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}
