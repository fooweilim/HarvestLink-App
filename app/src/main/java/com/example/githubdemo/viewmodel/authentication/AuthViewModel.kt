package com.example.githubdemo.viewmodel.authentication

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubdemo.data.local.LocalAccountStorage
import com.example.githubdemo.supabase.CloudAccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private companion object {
        const val DEFAULT_COOLDOWN_SECONDS = 60
        const val PREFERENCES_NAME = "auth_request_cooldown"
        const val DEADLINE_KEY = "email_request_deadline"

        const val INVALID_OTP_MESSAGE =
            "Invalid or expired OTP. Please check the code or request a new one."
    }

    private val preferences = application.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private var cooldownDeadline =
        preferences.getLong(DEADLINE_KEY, 0L)

    private var countdownJob: Job? = null

    var isLoading by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    var messageIsError by mutableStateOf(true)
        private set

    var resendSecondsRemaining by mutableIntStateOf(0)
        private set

    var showRateLimitDialog by mutableStateOf(false)
        private set

    val canRequestOtp: Boolean
        get() = !isLoading && resendSecondsRemaining == 0

    init {
        updateRemainingTime()
        startCountdown()
    }

    fun clearMessage() {
        message = ""
    }

    fun showSuccessMessage(successMessage: String) {
        messageIsError = false
        message = successMessage
    }

    fun showErrorMessage(errorMessage: String) {
        messageIsError = true
        message = errorMessage
    }

    fun dismissRateLimitDialog() {
        showRateLimitDialog = false
    }

    private fun updateRemainingTime() {
        val milliseconds =
            (cooldownDeadline - System.currentTimeMillis())
                .coerceAtLeast(0L)

        resendSecondsRemaining =
            ((milliseconds + 999L) / 1000L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
    }

    private fun startCountdown() {
        countdownJob?.cancel()

        countdownJob = viewModelScope.launch {
            updateRemainingTime()

            while (resendSecondsRemaining > 0) {
                delay(250L)
                updateRemainingTime()
            }

            cooldownDeadline = 0L
            preferences.edit().remove(DEADLINE_KEY).apply()
        }
    }

    private fun beginCooldown(
        seconds: Int = DEFAULT_COOLDOWN_SECONDS
    ) {
        val newDeadline =
            System.currentTimeMillis() +
                    seconds.coerceAtLeast(DEFAULT_COOLDOWN_SECONDS) * 1000L

        cooldownDeadline = maxOf(
            cooldownDeadline,
            newDeadline
        )

        preferences.edit()
            .putLong(DEADLINE_KEY, cooldownDeadline)
            .apply()

        updateRemainingTime()
        startCountdown()
    }

    private fun allowEmailRequest(): Boolean {
        if (isLoading) return false

        updateRemainingTime()

        if (resendSecondsRemaining > 0) {
            showErrorMessage(
                "Please wait before requesting another email."
            )
            showRateLimitDialog = true
            return false
        }

        return true
    }

    private fun handleFailure(
        exception: Throwable?,
        fallback: String
    ) {
        if (exception is CancellationException) {
            throw exception
        }

        val originalMessage = exception?.message.orEmpty()
        val lowerMessage = originalMessage.lowercase()

        // Supabase may return the same error for an incorrect
        // OTP and an expired OTP.
        val invalidOrExpiredOtp = listOf(
            "otp_expired",
            "otp_invalid",
            "invalid_otp",
            "token has expired or is invalid",
            "invalid or expired otp",
            "invalid otp",
            "otp is invalid",
            "otp has expired"
        ).any { lowerMessage.contains(it) }

        if (invalidOrExpiredOtp) {
            // Display a short message instead of the raw
            // exception containing URL and request headers.
            showErrorMessage(INVALID_OTP_MESSAGE)
            return
        }

        val rateLimited = listOf(
            "429",
            "too many requests",
            "too many email",
            "rate limit",
            "rate_limit",
            "over_email_send_rate_limit",
            "over_request_rate_limit",
            "security purposes",
            "request this after"
        ).any { lowerMessage.contains(it) }

        if (rateLimited) {
            val serverSeconds = Regex(
                """(?:after|wait|in)\s+(\d+)\s*(?:seconds?|secs?|s\b)"""
            ).find(lowerMessage)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: DEFAULT_COOLDOWN_SECONDS

            beginCooldown(serverSeconds)

            showErrorMessage(
                "Too many requests. Please wait for the countdown " +
                        "before trying again."
            )
            showRateLimitDialog = true
        } else {
            showErrorMessage(
                originalMessage.ifBlank { fallback }
            )
        }
    }

    private fun <T> performRequest(
        fallback: String,
        request: suspend () -> Result<T>,
        onSuccess: (T) -> Unit
    ) {
        if (isLoading) return

        // Set immediately so a second tap cannot launch another request.
        isLoading = true
        clearMessage()

        viewModelScope.launch {
            try {
                val result = request()

                result.fold(
                    onSuccess = onSuccess,
                    onFailure = {
                        handleFailure(it, fallback)
                    }
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                handleFailure(exception, fallback)
            } finally {
                isLoading = false
            }
        }
    }

    fun login(
        email: String,
        password: String,
        selectedRole: String,
        onSuccess: () -> Unit
    ) {
        performRequest(
            fallback = "Unable to sign in.",
            request = {
                CloudAccountRepository.login(
                    email = email,
                    password = password,
                    selectedRole = selectedRole
                )
            },
            onSuccess = { profile ->
                LocalAccountStorage.saveProfile(
                    context = getApplication(),
                    profile = profile
                )

                LocalAccountStorage.saveSelectedRole(
                    context = getApplication(),
                    userRole = profile.userRole
                )

                onSuccess()
            }
        )
    }

    fun sendSignUpOtp(
        email: String,
        password: String,
        onOtpSent: () -> Unit
    ) {
        if (!allowEmailRequest()) return

        performRequest(
            fallback = "Unable to send OTP.",
            request = {
                CloudAccountRepository.sendSignUpOtp(
                    email = email,
                    password = password
                )
            },
            onSuccess = {
                beginCooldown()
                showSuccessMessage("OTP sent. Check your email.")
                onOtpSent()
            }
        )
    }

    fun resendSignUpOtp(email: String) {
        if (!allowEmailRequest()) return

        performRequest(
            fallback = "Unable to resend OTP.",
            request = {
                CloudAccountRepository.resendSignUpOtp(email)
            },
            onSuccess = {
                beginCooldown()
                showSuccessMessage("A new OTP was sent.")
            }
        )
    }

    fun verifySignUpOtpAndSaveProfile(
        email: String,
        otp: String,
        userRole: String,
        fullName: String,
        phoneNumber: String,
        additionalInformation: String,
        onSuccess: () -> Unit
    ) {
        performRequest(
            fallback = "Invalid or expired OTP.",
            request = {
                CloudAccountRepository.verifySignUpOtpAndSaveProfile(
                    email = email,
                    otp = otp,
                    userRole = userRole,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    additionalInformation = additionalInformation
                )
            },
            onSuccess = {
                showSuccessMessage("Email verified successfully.")
                onSuccess()
            }
        )
    }

    fun sendResetOtp(
        email: String,
        onOtpSent: () -> Unit
    ) {
        if (!allowEmailRequest()) return

        performRequest(
            fallback = "Unable to send OTP.",
            request = {
                CloudAccountRepository.sendResetOtp(email)
            },
            onSuccess = {
                beginCooldown()
                showSuccessMessage("OTP sent. Check your email.")
                onOtpSent()
            }
        )
    }

    fun verifyResetOtp(
        email: String,
        otp: String,
        selectedRole: String,
        onSuccess: () -> Unit
    ) {
        performRequest(
            fallback = "Invalid or expired OTP.",
            request = {
                CloudAccountRepository.verifyResetOtp(
                    email = email,
                    otp = otp,
                    selectedRole = selectedRole
                )
            },
            onSuccess = {
                showSuccessMessage("Email verified successfully.")
                onSuccess()
            }
        )
    }

    fun updatePassword(
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        performRequest(
            fallback = "Unable to reset password.",
            request = {
                CloudAccountRepository.updatePassword(newPassword)
            },
            onSuccess = {
                showSuccessMessage("Password reset successfully.")
                onSuccess()
            }
        )
    }

    fun signOut(onComplete: () -> Unit) {
        if (isLoading) return

        isLoading = true
        clearMessage()

        viewModelScope.launch {
            try {
                val result = CloudAccountRepository.signOut()

                if (result.isFailure) {
                    showErrorMessage(
                        result.exceptionOrNull()?.message
                            ?: "Unable to sign out."
                    )
                }

                LocalAccountStorage.clearAll(getApplication())
                onComplete()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                showErrorMessage(
                    exception.message ?: "Unable to sign out."
                )
            } finally {
                isLoading = false
            }
        }
    }
}