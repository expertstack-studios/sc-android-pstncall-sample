package com.es.sc

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.es.pstn.SecuredPSTNCallBack
import com.es.pstn.SecuredPSTNCallsSDK
import com.es.pstn.views.compose.pages.NonDismissibleBottomDialogSheet
import com.es.pstn.views.compose.pages.PermissionRequiredContent
import com.es.sc.theme.SCVoiceCallSampleTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), SecuredPSTNCallBack {
    private lateinit var securedPSTNCallsSDK: SecuredPSTNCallsSDK
    private val userIdentifier = "userIdentifier"
    private var needToCheckPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        securedPSTNCallsSDK = SCPSTNCallApp.instance.securedPSTNCallsSDK
        if (securedPSTNCallsSDK.isConsumerRegistered()) {
            needToCheckPermission = true
        }
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

                val showPermissionRequiredBottomSheet by PermissionState.showPermissionRequiredBottomSheet
                val hasContactPermission by PermissionState.hasContactPermission
                val hasNotificationPermission by PermissionState.hasNotificationPermission

                NonDismissibleBottomDialogSheet(
                    showBottomSheet = showPermissionRequiredBottomSheet,
                    onDismissRequest = {
                        PermissionState.showPermissionRequiredBottomSheet.value = false
                    },
                ) {
                    PermissionRequiredContent(
                        modifier = Modifier,
                        hasContactPermission = hasContactPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestContactPermission = {
                            if (securedPSTNCallsSDK.isPermissionDeniedTwice(securedPSTNCallsSDK.CONTACT_PERMISSION_DENIED)) {
                                securedPSTNCallsSDK.openAppPermissionsSettings(this@MainActivity)
                                needToCheckPermission = true
                            } else {
                                securedPSTNCallsSDK.requestContactPermission(this@MainActivity, true)
                            }
                        },
                        onRequestNotificationPermission = {
                            if (securedPSTNCallsSDK.isPermissionDeniedTwice(securedPSTNCallsSDK.NOTIFICATION_PERMISSION_DENIED)) {
                                securedPSTNCallsSDK.openAppPermissionsSettings(this@MainActivity)
                                needToCheckPermission = true
                            } else {
                                securedPSTNCallsSDK.requestNotificationPermission(this@MainActivity, true)
                            }
                        }
                    )
                }
            }
        }
    }

    object PermissionState {
        var showPermissionRequiredBottomSheet = mutableStateOf(false)
        var hasContactPermission = mutableStateOf(false)
        var hasNotificationPermission = mutableStateOf(false)
    }

    private fun registerConsumerNumber(userIdentifier: String, securedPSTNCallBack: SecuredPSTNCallBack) {
        securedPSTNCallsSDK.setRegisterDeviceCallBack(securedPSTNCallBack)
        securedPSTNCallsSDK.login(userIdentifier)
    }

    private fun checkPermissions() {
        if (securedPSTNCallsSDK.hasContactPermission()) {
            if (securedPSTNCallsSDK.hasNotificationPermission()) {
                securedPSTNCallsSDK.registerDevicePushToken()
                setContent {
                    setScreenContent()
                }
                needToCheckPermission = true
            } else {
                securedPSTNCallsSDK.requestNotificationPermission(this@MainActivity)
            }
        } else {
            securedPSTNCallsSDK.requestContactPermission(this@MainActivity)
        }
    }

    private fun checkPermissionsToShowPermissionSheet() {
        if (securedPSTNCallsSDK.isConsumerRegistered()) {
            if (!securedPSTNCallsSDK.shouldShowPermissionSheet || securedPSTNCallsSDK.areAllPermissionsGranted) {
                PermissionState.showPermissionRequiredBottomSheet.value = false
            } else {
                PermissionState.showPermissionRequiredBottomSheet.value = true
                PermissionState.hasContactPermission.value = securedPSTNCallsSDK.hasContactPermission()
                PermissionState.hasNotificationPermission.value = securedPSTNCallsSDK.hasNotificationPermission()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            securedPSTNCallsSDK.PERMISSIONS_REQUEST_WRITE_CONTACTS,
            securedPSTNCallsSDK.PERMISSIONS_REQUEST_POST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissions()
                }
                return
            }

            securedPSTNCallsSDK.PERMISSIONS_REQUEST_WRITE_CONTACTS_POPUP -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                        securedPSTNCallsSDK.handlePermissionDenied(securedPSTNCallsSDK.CONTACT_PERMISSION_DENIED)
                    } else {
                        checkPermissionsToShowPermissionSheet()
                    }
                }
                return
            }

            securedPSTNCallsSDK.PERMISSIONS_REQUEST_POST_NOTIFICATIONS_POPUP -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                        securedPSTNCallsSDK.handlePermissionDenied(securedPSTNCallsSDK.NOTIFICATION_PERMISSION_DENIED)
                    } else {
                        checkPermissionsToShowPermissionSheet()
                    }
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

    override fun onResume() {
        super.onResume()
        if (needToCheckPermission) {
            checkPermissionsToShowPermissionSheet()
        }
    }

}