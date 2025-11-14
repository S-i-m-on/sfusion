package com.staipa.auracallv1

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

/**
 * MyInCallService
 *
 * Compliant implementation of an InCallService that:
 * - Provides only call UI and control
 * - Handles all calls managed by the Telecom framework
 * - Makes no assumptions about call type
 */
class MyInCallService : InCallService() {

    private val TAG = "MyInCallService"
    private val callMap = mutableMapOf<String, Call>()

    // Safe fallback for ON_HOLD for all Android versions
    private val STATE_ON_HOLD_FALLBACK = 3

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)

            val stateName = stateToString(state)
            Log.d(TAG, "Call ${call.details.handle} state changed to: $stateName")

            when (state) {
                Call.STATE_RINGING -> sendUiUpdate(call, "RINGING")
                Call.STATE_ACTIVE -> sendUiUpdate(call, "ACTIVE")
                STATE_ON_HOLD_FALLBACK -> sendUiUpdate(call, "ON_HOLD")
                Call.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Call disconnected. Cause: ${call.details.disconnectCause.reason}")
                    sendUiUpdate(call, "DISCONNECTED")
                }
                Call.STATE_DIALING -> sendUiUpdate(call, "DIALING")
                Call.STATE_CONNECTING -> sendUiUpdate(call, "CONNECTING")
            }
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            sendUiUpdate(call, stateToString(call.state))
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val callId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            call.details.handle?.toString() ?: call.toString()
        } else {
            call.toString()
        }

        callMap[callId] = call
        call.registerCallback(callCallback)

        Log.d(TAG, "Call added: $callId. Total active calls: ${callMap.size}")

        if (callMap.size == 1 || call.state == Call.STATE_RINGING) {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = "ACTION_SHOW_CALL_UI"
                putExtra("CALL_ID", callId)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        sendUiUpdate(call, stateToString(call.state))
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)

        val callId = callMap.entries.firstOrNull { it.value == call }?.key ?: call.toString()
        callMap.remove(callId)
        call.unregisterCallback(callCallback)

        Log.d(TAG, "Call removed: $callId. Remaining calls: ${callMap.size}")

        if (callMap.isEmpty()) {
            sendEmptyCallUpdate()
        } else {
            sendUiUpdate(call, "REMOVED")
        }
    }

    fun answerCall(callId: String) {
        callMap[callId]?.let { call ->
            if (call.state == Call.STATE_RINGING) {
                call.answer(call.details.videoState)
            }
        }
    }

    fun disconnectCall(callId: String) {
        callMap[callId]?.disconnect()
    }

    fun toggleHold(callId: String) {
        callMap[callId]?.let { call ->
            when (call.state) {
                Call.STATE_ACTIVE -> call.hold()
                STATE_ON_HOLD_FALLBACK -> call.unhold()
            }
        }
    }

    private fun sendUiUpdate(call: Call, status: String) {
        Log.d(TAG, "UI Update: $status for ${call.details.handle}")
        // TODO: React Native bridge call here
    }

    private fun sendEmptyCallUpdate() {
        Log.d(TAG, "All calls cleared. Closing UI.")
        // TODO: RN bridge to close UI
    }

    private fun stateToString(state: Int): String {
        return when (state) {
            Call.STATE_NEW -> "NEW"
            Call.STATE_DIALING -> "DIALING"
            Call.STATE_RINGING -> "RINGING"
            Call.STATE_ACTIVE -> "ACTIVE"
            STATE_ON_HOLD_FALLBACK -> "ON_HOLD"
            Call.STATE_DISCONNECTED -> "DISCONNECTED"
            Call.STATE_CONNECTING -> "CONNECTING"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_ACCOUNT"
            Call.STATE_DISCONNECTING -> "DISCONNECTING"
            else -> "UNKNOWN ($state)"
        }
    }
}
