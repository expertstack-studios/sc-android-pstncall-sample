package com.es.sc.notification

import android.util.Log
import com.es.sc.SCPSTNCallApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ScFirebaseMessagingService : FirebaseMessagingService() {

    private val securedPSTNCallsSDK = SCPSTNCallApp.instance.securedPSTNCallsSDK

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        securedPSTNCallsSDK.savePushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("onMessageReceived", message.data.toString())
        if (securedPSTNCallsSDK.isPSTNSDKPush(message)) {
            securedPSTNCallsSDK.processingIncomingPush(message)
        }
    }
}