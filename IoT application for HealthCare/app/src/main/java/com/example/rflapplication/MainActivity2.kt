package com.example.rflapplication

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val etName = findViewById<EditText>(R.id.etName)
        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etHeight = findViewById<EditText>(R.id.etHeight)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etBloodPressure = findViewById<EditText>(R.id.etBloodPressure)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("NAME", etName.text.toString())
                putExtra("WEIGHT", etWeight.text.toString())
                putExtra("HEIGHT", etHeight.text.toString())
                putExtra("AGE", etAge.text.toString())
                putExtra("BLOOD_PRESSURE", etBloodPressure.text.toString())
            }
            startActivity(intent)
            finish() // 🔥 This prevents returning to MainActivity2 when pressing back
        }
    }
}
