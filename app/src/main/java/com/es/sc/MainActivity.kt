package com.es.sc

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.es.pstn.SecuredPSTNCallBack
import com.es.pstn.SecuredPSTNCallsSDK
import com.es.sc.theme.SCVoiceCallSampleTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), SecuredPSTNCallBack {
    private lateinit var securedPSTNCallsSDK: SecuredPSTNCallsSDK
    private val userIdentifier = "userIdentifier"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        securedPSTNCallsSDK = SCPSTNCallApp.instance.securedPSTNCallsSDK
        lifecycleScope.launch { securedPSTNCallsSDK.initializeSDKOnLaunch() } //Use this function to initialize SDK session on app launch
        setContent {
            setScreenContent()
        }
    }

    @Composable
    fun setScreenContent() {
        SCVoiceCallSampleTheme {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Secured PSTN Call",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(10.dp)
                )
                Button(
                    onClick = {
                        if (securedPSTNCallsSDK.isInternetAvailable && !securedPSTNCallsSDK.isConsumerRegistered()) {
                            registerConsumerNumber(userIdentifier, this@MainActivity)
                        }
                    },
                    modifier = Modifier,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.White)
                ) {
                    if (securedPSTNCallsSDK.isConsumerRegistered()) {
                        val registeredNumber = securedPSTNCallsSDK.getRegisteredMobileNumber()
                        Text(text = "Registered No.- $registeredNumber")
                    } else {
                        Text(text = "Register Consumer Number")
                    }
                }
            }
        }
    }

    private fun registerConsumerNumber(userIdentifier: String, securedPSTNCallBack: SecuredPSTNCallBack) {
        securedPSTNCallsSDK.setRegisterDeviceCallBack(securedPSTNCallBack)
        securedPSTNCallsSDK.login(userIdentifier)
    }

    private fun checkPermissions() {
        if (securedPSTNCallsSDK.hasCallPhonePermission()) {
            if (securedPSTNCallsSDK.hasContactPermission()) {
                if (securedPSTNCallsSDK.hasNotificationPermission()) {
                    securedPSTNCallsSDK.registerDevicePushToken()
                    setContent {
                        setScreenContent()
                    }
                } else {
                    securedPSTNCallsSDK.requestNotificationPermission(this@MainActivity)
                }
            } else {
                securedPSTNCallsSDK.requestContactPermission(this@MainActivity)
            }
        } else {
            securedPSTNCallsSDK.requestCallPhonePermission(this@MainActivity)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            securedPSTNCallsSDK.PERMISSIONS_REQUEST_CALL_PHONE,
            securedPSTNCallsSDK.PERMISSIONS_REQUEST_WRITE_CONTACTS,
            securedPSTNCallsSDK.PERMISSIONS_REQUEST_POST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissions()
                }
                return
            }
        }
    }

    override fun onLoginError(message: String) {
        Log.d("onLoginError", message)
    }

    override fun onLoginSuccess() {
        Log.d("onLoginSuccess", "success")
        checkPermissions()
    }

}