package com.example.dialer

import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.content.ComponentName

object PhoneAccountRegistrar {
    fun registerPhoneAccount(context: Context) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(context, CallConnectionService::class.java)
        val handle = PhoneAccountHandle(componentName, context.packageName)

        val builder = PhoneAccount.builder(handle, "CipherDialer")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setAddress(null)

        val account = builder.build()

        // Add / register the PhoneAccount if not already present
        try {
            telecomManager.registerPhoneAccount(account)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
