package com.example.domain.service

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricCapability {
    AVAILABLE,
    NONE_ENROLLED,
    HARDWARE_UNAVAILABLE
}

object BiometricAuthHelper {

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun checkCapability(context: Context): BiometricCapability {
        return try {
            val manager = BiometricManager.from(context)
            when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NONE_ENROLLED
                else -> BiometricCapability.HARDWARE_UNAVAILABLE
            }
        } catch (_: Exception) {
            BiometricCapability.HARDWARE_UNAVAILABLE
        }
    }

    fun findFragmentActivity(context: Context): FragmentActivity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return ctx as? FragmentActivity
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String = "Use Security PIN",
        onSuccess: () -> Unit,
        onErrorOrCancelled: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onErrorOrCancelled(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onErrorOrCancelled("Biometric not recognized. Try again or use Security PIN.")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo)
    }
}
