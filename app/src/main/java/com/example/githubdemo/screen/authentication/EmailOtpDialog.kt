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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.githubdemo.ui.theme.PrimaryGreen
import com.example.githubdemo.viewmodel.authentication.AuthViewModel

@Composable
fun EmailOtpDialog(
    email: String,
    authViewModel: AuthViewModel,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit,
    onDismiss: () -> Unit
) {
    var otp by rememberSaveable(email) {
        mutableStateOf("")
    }

    val remaining = authViewModel.resendSecondsRemaining

    AlertDialog(
        onDismissRequest = {
            if (!authViewModel.isLoading) {
                authViewModel.clearMessage()
                onDismiss()
            }
        },
        title = { Text("Verify Email") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "An 8-digit OTP was sent to $email.",
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = otp,
                    onValueChange = {
                        otp = it.filter(Char::isDigit).take(8)
                        authViewModel.clearMessage()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email OTP") },
                    placeholder = { Text("12345678") },
                    singleLine = true,
                    enabled = !authViewModel.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    )
                )

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

                TextButton(
                    onClick = onResendOtp,
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (otp.length != 8) {
                        authViewModel.showErrorMessage(
                            "Enter the 8-digit OTP."
                        )
                    } else {
                        onVerifyOtp(otp)
                    }
                },
                enabled = !authViewModel.isLoading
            ) {
                Text(
                    if (authViewModel.isLoading) {
                        "Please wait..."
                    } else {
                        "Verify"
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

@Composable
fun AuthRateLimitDialog(
    authViewModel: AuthViewModel
) {
    if (!authViewModel.showRateLimitDialog) return

    val remaining = authViewModel.resendSecondsRemaining

    AlertDialog(
        onDismissRequest = {
            authViewModel.dismissRateLimitDialog()
        },
        title = { Text("Please wait before trying again") },
        text = {
            Text(
                if (remaining > 0) {
                    "Too many requests, or an email was requested " +
                            "recently.\n\nTry again in $remaining seconds."
                } else {
                    "The countdown has finished. You can try again now."
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    authViewModel.dismissRateLimitDialog()
                }
            ) {
                Text("OK")
            }
        }
    )
}