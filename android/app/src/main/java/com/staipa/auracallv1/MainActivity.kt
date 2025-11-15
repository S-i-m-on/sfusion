package com.staipa.auracallv1

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.facebook.react.ReactActivity

private const val TAG = "MainActivity"
private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CALL_PHONE,
    Manifest.permission.ANSWER_PHONE_CALLS,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.WRITE_CALL_LOG
)

class MainActivity : ReactActivity() {
    private lateinit var requestRoleLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activity Result launcher to request ROLE_DIALER (if available)
        requestRoleLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Role granted (user confirmed).")
                } else {
                    Log.i(TAG, "Role not granted or user canceled.")
                }
            }
        )

        // Permission launcher using Activity Result API (requests all at once)
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val denied = perms.filter { !it.value }.map { it.key }
            if (denied.isEmpty()) {
                Log.i(TAG, "All required permissions granted.")
            } else {
                Log.w(TAG, "Some permissions denied: $denied")
            }
        }

        // Kick off flows: permissions and role
        checkAndRequestPermissions()
        requestDialerRoleIfNeeded()
    }

    private fun checkAndRequestPermissions() {
        val toRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest)
        } else {
            Log.i(TAG, "All required permissions already granted.")
        }
    }

    private fun requestDialerRoleIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                requestRoleLauncher.launch(intent)
            } else {
                Log.i(TAG, "Role not available or already held.")
            }
        } else {
            // Fallback for older devices: ask Telecom to change default dialer
            try {
                val telecomIntent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                telecomIntent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                requestRoleLauncher.launch(telecomIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback default-dialer intent failed: $e")
            }
        }
    }

    // Your existing ReactActivity overrides remain unchanged
}