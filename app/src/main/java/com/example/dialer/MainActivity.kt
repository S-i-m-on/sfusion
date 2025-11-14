package com.example.dialer

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.net.Uri
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PhoneAccountRegistrar.registerPhoneAccount(this)

        val btnSetDefault = findViewById<Button>(R.id.btn_set_default)
        btnSetDefault.setOnClickListener {
            setDefaultDialer()
        }

        val btnDial = findViewById<Button>(R.id.btn_dial)
        btnDial.setOnClickListener {
            dialNumber("+15551234567")
        }

    }

    private fun setDefaultDialer() {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(packageName, MyInCallService::class.java.name)
        // Starting Android Q (API 29+), use RoleManager or ACTION_CHANGE_DEFAULT_DIALER.
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
        intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        startActivity(intent)
    }

    private fun dialNumber(number: String) {
        val uri = Uri.parse("tel:$number")
        val intent = Intent(Intent.ACTION_CALL, uri)
        startActivity(intent)
    }
}
