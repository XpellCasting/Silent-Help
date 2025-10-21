package com.icc.silent_help.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.icc.silent_help.R

class RegisterStep2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_step2)

        val btnVerifyCode: Button = findViewById(R.id.btn_verify_code)
        btnVerifyCode.setOnClickListener {
            val intent = Intent(this, RegisterStep3Activity::class.java)
            startActivity(intent)
        }
    }
}
