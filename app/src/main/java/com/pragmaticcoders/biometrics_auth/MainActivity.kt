package com.pragmaticcoders.biometrics_auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.pragmaticcoders.biometrics_auth.Biometrics.AuthenticationResult
import com.pragmaticcoders.biometrics_auth.ui.theme.Biometrics_authTheme

class MainActivity : FragmentActivity() {

    private val biometrics by lazy { Biometrics() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Biometrics_authTheme {
                //collect state of biometric authentication
                val authResult = biometrics.sharedFlow.collectAsState(initial = null).value
                val enrollLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = {
                        println("Activity result: $it")
                    }
                )
                LaunchedEffect(authResult) {
                    if (authResult is AuthenticationResult.AuthenticationNotSet) {
                        if (Build.VERSION.SDK_INT >= 30) {
                            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                putExtra(
                                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                                )
                            }
                            enrollLauncher.launch(enrollIntent)
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        authResult?.let { result ->
                            Text(
                                text = when (result) {
                                    is AuthenticationResult.Error -> {
                                        result.errorMessage
                                    }

                                    AuthenticationResult.Failure -> {
                                        "Authentication failed"
                                    }

                                    AuthenticationResult.AuthenticationNotSet -> {
                                        "Authentication not set"
                                    }

                                    AuthenticationResult.Success -> {
                                        "Authentication success"
                                    }

                                    AuthenticationResult.FeatureUnavailable -> {
                                        "Feature unavailable"
                                    }

                                    AuthenticationResult.HardwareUnavailable -> {
                                        "Hardware unavailable"
                                    }
                                }
                            )

                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                biometrics.authenticate(this@MainActivity)
                            }
                        ) {
                            Text("Authenticate")
                        }

                    }
                }
            }
        }
    }
}