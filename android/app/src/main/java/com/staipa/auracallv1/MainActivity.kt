package com.staipa.auracallv1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import expo.modules.ReactActivityDelegateWrapper
import expo.modules.splashscreen.SplashScreenManager

class MainActivity : ReactActivity() {

    private val TAG = "MainActivity"
    private val PERMISSIONS_REQUEST_CODE = 100

    override fun getMainComponentName(): String = "auracall"

    override fun onCreate(savedInstanceState: Bundle?) {
        SplashScreenManager.registerOnActivity(this)
        super.onCreate(null)
        checkPermissionsAndDefaultDialer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        handleDialIntent(intent)
    }

    override fun createReactActivityDelegate(): ReactActivityDelegate {
        return ReactActivityDelegateWrapper(
              this,
              BuildConfig.IS_NEW_ARCHITECTURE_ENABLED,
              object : DefaultReactActivityDelegate(
                  this,
                  mainComponentName,
                  fabricEnabled
              ){})
    }

    override fun invokeDefaultOnBackPressed() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (!moveTaskToBack(false)) {
                super.invokeDefaultOnBackPressed()
            }
            return
        }
        super.invokeDefaultOnBackPressed()
    }
    
    // --- START OF DIALER LOGIC REVISIONS ---

    /**
     * Handles incoming ACTION_DIAL or ACTION_VIEW intents with a tel: URI.
     * This logic is crucial for working as a default dialer.
     */
    private fun handleDialIntent(intent: Intent?) {
        val action = intent?.action
        val telUri = intent?.data

        // 1. Check for ACTION_DIAL (user opened the dialer to input a number)
        if (action == Intent.ACTION_DIAL) {
            // No number specified, just show the UI/dialpad
            Log.d(TAG, "Received ACTION_DIAL. Opening dialer UI.")
            // TODO: If you have a specific UI component to show the dialpad, launch it here.
            // Since this is a React Native app, you might just ensure the main component handles the empty state.
            return
        }
        
        // 2. Check for ACTION_VIEW (user wants to call a specific number)
        if (action == Intent.ACTION_VIEW && telUri?.scheme == "tel") {
            // It's a call request, attempt to place the call
            Log.d(TAG, "Received ACTION_VIEW with tel URI: $telUri. Placing call.")
            
            // NOTE: The previous code only called placeCall if isDefaultDialer() was true.
            // When the system launches *any* app to handle ACTION_VIEW/tel,
            // that app should attempt to fulfill the intent, regardless of default status.
            // placeCall will internally check for CALL_PHONE permission.
            if (telUri != null) {
                placeCall(telUri)
            }
        }
    }

    private fun placeCall(address: Uri) {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
             // Requesting permission here might be too late if the app was launched by the intent.
             // It's better to request all necessary permissions upfront in checkPermissionsAndDefaultDialer.
             requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), PERMISSIONS_REQUEST_CODE)
             return
        }

        try {
            // ... (rest of placeCall logic remains the same) ...
            val availableAccounts = telecomManager.getCallCapablePhoneAccounts()

            val accountHandle: PhoneAccountHandle? = when {
                availableAccounts.isEmpty() -> {
                    Log.e(TAG, "No phone accounts available for calling.")
                    Toast.makeText(this, "No SIM or calling service found.", Toast.LENGTH_LONG).show()
                    return
                }
                availableAccounts.size == 1 -> availableAccounts[0]
                else -> {
                    telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
                        ?: availableAccounts[0]
                }
            }

            val extras = Bundle().apply {
                if (accountHandle != null) {
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
                }
            }

            telecomManager.placeCall(address, extras)
            Log.i(TAG, "Attempting to place call to: $address using account: ${accountHandle?.id}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to place call: ${e.message}", e)
            Toast.makeText(this, "Error placing call: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isDefaultDialer(): Boolean {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return packageName == telecomManager.defaultDialerPackage
    }

    private fun checkPermissionsAndDefaultDialer() {
        // Request essential permissions for a dialer
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
            // MANAGE_OWN_CALLS is a system/signature permission granted via Manifest, not requested at runtime.
        )
        
        if (permissionsToRequest.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }

        // Always prompt the user to become the default dialer if they haven't yet
        if (!isDefaultDialer()) {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            // Use try/catch as this can fail on devices where the system setting is unavailable.
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch default dialer change intent: ${e.message}")
                Toast.makeText(this, "Please set 'auracall' as your default phone app manually in settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // Re-check for default dialer after permissions are granted
            if (!isDefaultDialer()) {
                checkPermissionsAndDefaultDialer()
            }
            // You can add logic here to inform React Native that permissions are ready.
        }
    }
    // --- END OF DIALER LOGIC REVISIONS ---
}
