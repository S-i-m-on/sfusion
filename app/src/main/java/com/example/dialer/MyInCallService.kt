package com.example.dialer

import android.telecom.InCallService
import android.telecom.Call
import android.os.Bundle
import android.util.Log

class MyInCallService : InCallService() {

    private var currentCall: Call? = null

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        Log.d("MyInCallService", "Call added: ${call.details?.handle}")

        // Listen for state changes
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                Log.d("MyInCallService", "State changed: $state")
            }

            override fun onDisconnected(c: Call, details: Call.Details?) {
                Log.d("MyInCallService", "Call disconnected")
            }
        })

        // Show simple UI or bring MainActivity to front for UI
        // (Detailed UI implementation omitted - this is a skeleton.)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (currentCall == call) currentCall = null
        Log.d("MyInCallService", "Call removed")
    }
}
