package com.example.asset_charts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.sign_in).setOnClickListener {
            val email = findViewById<EditText>(R.id.email).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast(this).apply {
                            setText("Sign in successful")
                        }.show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast(this).apply {
                            setText("Sign in failed")
                        }.show()
                    }
                }
        }

        findViewById<Button>(R.id.sign_up).setOnClickListener {
            val email = findViewById<EditText>(R.id.email).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast(this).apply {
                            setText("Sign up successful")
                        }.show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast(this).apply {
                            setText("Sign up failed")
                        }.show()
                    }
                }
        }
    }
}