package com.icc.silent_help.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.icc.silent_help.R

class RegisterStep1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_step1)

        val btnContinue: Button = findViewById(R.id.btn_continue)
        btnContinue.setOnClickListener {
            val intent = Intent(this, RegisterStep2Activity::class.java)
            startActivity(intent)
        }
    }
}
