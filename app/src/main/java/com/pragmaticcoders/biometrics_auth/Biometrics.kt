package com.pragmaticcoders.biometrics_auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Biometrics {

    sealed interface AuthenticationResult {
        data object Success : AuthenticationResult
        data object Failure : AuthenticationResult
        data object AuthenticationNotSet : AuthenticationResult
        data object HardwareUnavailable : AuthenticationResult
        data object FeatureUnavailable : AuthenticationResult
        data class Error(val errorMessage: String) : AuthenticationResult
    }

    private val mutableSharedFlow = MutableSharedFlow<AuthenticationResult>()
    val sharedFlow = mutableSharedFlow.asSharedFlow()

    fun authenticate(context: Context) {
        require(context is FragmentActivity) {
            "Context must be an instance of FragmentActivity"
        }

        val manager = BiometricManager.from(context)

        val authenticators = when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R -> {
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            }

            else -> {
                BIOMETRIC_STRONG
            }
        }

        //Create prompt info
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric authentication")
            .setSubtitle("Authenticate using your fingerprint or face")
            .setAllowedAuthenticators(authenticators)

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }

        val canAuthenticate = when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                mutableSharedFlow.tryEmit(AuthenticationResult.HardwareUnavailable)
                false
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                mutableSharedFlow.tryEmit(AuthenticationResult.FeatureUnavailable)
                false
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                mutableSharedFlow.tryEmit(AuthenticationResult.AuthenticationNotSet)
                false
            }

            else -> {
                mutableSharedFlow.tryEmit(AuthenticationResult.Error("Unknown error"))
                false
            }
        }

        if (canAuthenticate) {
            //Create biometric prompt
            val prompt = BiometricPrompt(
                context,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        mutableSharedFlow.tryEmit(AuthenticationResult.Success)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        mutableSharedFlow.tryEmit(AuthenticationResult.Failure)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        mutableSharedFlow.tryEmit(AuthenticationResult.Error(errString.toString()))
                    }
                }
            )

            // Show biometric prompt
            prompt.authenticate(promptInfoBuilder.build())
        }
    }
}