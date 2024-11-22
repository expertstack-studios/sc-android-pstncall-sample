package com.es.sc

import android.app.Application
import com.es.pstn.SecuredPSTNCallsSDK
import com.es.pstn.network.model.ScSDKConfigModel

class SCPSTNCallApp: Application() {

    companion object {
        lateinit var instance: SCPSTNCallApp
    }
    init {
        instance = this
    }

    val securedPSTNCallsSDK: SecuredPSTNCallsSDK = SecuredPSTNCallsSDK(this)

    override fun onCreate() {
        super.onCreate()
      securedPSTNCallsSDK.initializeSDK(ScSDKConfigModel("**xxxxxxxSECRETxxxxxxx**", true))
    }
}
