package com.staipa.auracallv1

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

/**
 * Minimal CallScreeningService implementation for carrier calls.
 * Default behavior: allow the call and let Telecom handle it.
 * Customize to block or silence calls as needed.
 */
class MyCallScreeningService : CallScreeningService() {
    private val TAG = "MyCallScreeningSvc"

    override fun onScreenCall(callDetails: Call.Details) {
        Log.i(TAG, "onScreenCall: incoming=${callDetails.handle} ${callDetails}")

        val response = CallResponse.Builder()
            .setDisallowCall(false) // allow the call
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }
}