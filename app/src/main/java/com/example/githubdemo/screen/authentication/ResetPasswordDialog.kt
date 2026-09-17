package com.example.githubdemo.screen.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.githubdemo.data.AuthValidation
import com.example.githubdemo.ui.theme.PrimaryGreen
import com.example.githubdemo.viewmodel.authentication.AuthViewModel

private const val RESET_EMAIL_STEP = "email"
private const val RESET_OTP_STEP = "otp"
private const val RESET_PASSWORD_STEP = "password"

@Composable
fun ResetPasswordDialog(
    userRole: String,
    initialEmail: String,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onPasswordReset: () -> Unit
) {
    var email by rememberSaveable {
        mutableStateOf(initialEmail)
    }
    var otp by rememberSaveable {
        mutableStateOf("")
    }
    var newPassword by rememberSaveable {
        mutableStateOf("")
    }
    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }
    var resetStep by rememberSaveable {
        mutableStateOf(RESET_EMAIL_STEP)
    }

    val remaining = authViewModel.resendSecondsRemaining

    AlertDialog(
        onDismissRequest = {
            if (!authViewModel.isLoading) {
                authViewModel.clearMessage()
                onDismiss()
            }
        },
        title = {
            Text(
                when (resetStep) {
                    RESET_EMAIL_STEP -> "Verify your email"
                    RESET_OTP_STEP -> "Enter email OTP"
                    else -> "Create new password"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (resetStep) {
                    RESET_EMAIL_STEP -> {
                        Text(
                            "Enter your registered email. " +
                                    "We will send a verification OTP."
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it.take(60)
                                authViewModel.clearMessage()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email address") },
                            enabled = !authViewModel.isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            )
                        )
                    }

                    RESET_OTP_STEP -> {
                        Text("Enter the 8-digit OTP sent to $email.")

                        OutlinedTextField(
                            value = otp,
                            onValueChange = {
                                otp = it.filter(Char::isDigit).take(8)
                                authViewModel.clearMessage()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email OTP") },
                            enabled = !authViewModel.isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            )
                        )

                        TextButton(
                            onClick = {
                                authViewModel.sendResetOtp(
                                    email = email,
                                    onOtpSent = {}
                                )
                            },
                            enabled = authViewModel.canRequestOtp
                        ) {
                            Text(
                                if (remaining > 0) {
                                    "Resend OTP in ${remaining}s"
                                } else {
                                    "Resend OTP"
                                }
                            )
                        }
                    }

                    RESET_PASSWORD_STEP -> {
                        Text(
                            "Email verified. Create a new password.",
                            color = PrimaryGreen
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it.take(30)
                                authViewModel.clearMessage()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New password") },
                            visualTransformation =
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            enabled = !authViewModel.isLoading,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it.take(30)
                                authViewModel.clearMessage()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm password") },
                            visualTransformation =
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            enabled = !authViewModel.isLoading,
                            singleLine = true
                        )
                    }
                }

                if (authViewModel.message.isNotEmpty()) {
                    Text(
                        text = authViewModel.message,
                        color = if (authViewModel.messageIsError) {
                            Color(0xFFB3261E)
                        } else {
                            PrimaryGreen
                        },
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (resetStep) {
                        RESET_EMAIL_STEP -> {
                            if (!AuthValidation.isValidEmail(email)) {
                                authViewModel.showErrorMessage(
                                    "Enter a valid email address."
                                )
                            } else {
                                authViewModel.sendResetOtp(
                                    email = email,
                                    onOtpSent = {
                                        email = email.trim().lowercase()
                                        resetStep = RESET_OTP_STEP
                                    }
                                )
                            }
                        }

                        RESET_OTP_STEP -> {
                            if (otp.length != 8) {
                                authViewModel.showErrorMessage(
                                    "Enter the 8-digit OTP."
                                )
                            } else {
                                authViewModel.verifyResetOtp(
                                    email = email,
                                    otp = otp,
                                    selectedRole = userRole,
                                    onSuccess = {
                                        resetStep = RESET_PASSWORD_STEP
                                    }
                                )
                            }
                        }

                        RESET_PASSWORD_STEP -> {
                            when {
                                !AuthValidation.isValidPassword(
                                    newPassword
                                ) -> {
                                    authViewModel.showErrorMessage(
                                        "Use at least 8 characters, " +
                                                "one letter and one number."
                                    )
                                }

                                !AuthValidation.passwordsMatch(
                                    newPassword,
                                    confirmPassword
                                ) -> {
                                    authViewModel.showErrorMessage(
                                        "Passwords do not match."
                                    )
                                }

                                else -> {
                                    authViewModel.updatePassword(
                                        newPassword = newPassword,
                                        onSuccess = onPasswordReset
                                    )
                                }
                            }
                        }
                    }
                },
                enabled = !authViewModel.isLoading &&
                        (
                                resetStep != RESET_EMAIL_STEP ||
                                        remaining == 0
                                )
            ) {
                Text(
                    when {
                        authViewModel.isLoading -> "Please wait..."

                        resetStep == RESET_EMAIL_STEP &&
                                remaining > 0 -> "Send OTP in ${remaining}s"

                        resetStep == RESET_EMAIL_STEP -> "Send OTP"
                        resetStep == RESET_OTP_STEP -> "Verify OTP"
                        else -> "Reset Password"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    authViewModel.clearMessage()
                    onDismiss()
                },
                enabled = !authViewModel.isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}