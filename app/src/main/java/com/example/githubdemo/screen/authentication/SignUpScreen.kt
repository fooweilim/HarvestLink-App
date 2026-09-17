package com.example.githubdemo.screen.authentication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.githubdemo.data.AuthValidation
import com.example.githubdemo.data.UserRole
import com.example.githubdemo.location.CurrentLocationAddressField
import com.example.githubdemo.ui.theme.MainText
import com.example.githubdemo.ui.theme.PageBackground
import com.example.githubdemo.ui.theme.PrimaryGreen
import com.example.githubdemo.ui.theme.SecondaryText
import com.example.githubdemo.viewmodel.authentication.AuthViewModel

@Composable
fun SignUpScreen(
    userRole: String,
    onSignUpSuccess: (String) -> Unit,
    onLoginClick: (String) -> Unit,
    onBackClick: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var additionalInformation by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var formSubmitted by rememberSaveable { mutableStateOf(false) }
    var showOtpDialog by rememberSaveable { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }

    val roleName = UserRole.getRoleName(userRole)

    val nameValid = AuthValidation.isValidName(fullName)
    val emailValid = AuthValidation.isValidEmail(email)
    val phoneValid = AuthValidation.isValidPhoneNumber(phoneNumber)

    val addressValid =
        AuthValidation.isValidAdditionalInformation(
            additionalInformation
        )

    val passwordValid = AuthValidation.isValidPassword(password)
    val passwordsMatch = AuthValidation.passwordsMatch(
        password,
        confirmPassword
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                AuthenticationHeader(
                    userRole = userRole,
                    title = "Create account",
                    subtitle = "Join HarvestLink as a $roleName",
                    onBackClick = {
                        authViewModel.clearMessage()
                        onBackClick()
                    }
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            top = 350.dp,
                            end = 24.dp
                        ),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        AuthenticationTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it.take(50)
                                authViewModel.clearMessage()
                            },
                            label = "Full Name",
                            placeholder = "Enter your full name",
                            leadingIcon = Icons.Outlined.Person,
                            isError = formSubmitted && !nameValid,
                            errorMessage = "Enter at least 2 characters.",
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )

                        AuthenticationTextField(
                            value = email,
                            onValueChange = {
                                email = it.take(60)
                                authViewModel.clearMessage()
                            },
                            label = "Email Address",
                            placeholder = "$userRole@email.com",
                            leadingIcon = Icons.Outlined.Email,
                            isError = formSubmitted && !emailValid,
                            errorMessage = "Enter a valid email address.",
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )

                        AuthenticationTextField(
                            value = phoneNumber,
                            onValueChange = {
                                if (
                                    it.length <= 12 &&
                                    it.all(Char::isDigit)
                                ) {
                                    phoneNumber = it
                                    authViewModel.clearMessage()
                                }
                            },
                            label = "Phone Number",
                            placeholder = "0123456789",
                            leadingIcon = Icons.Outlined.Phone,
                            isError = formSubmitted && !phoneValid,
                            errorMessage = "Enter 9 to 12 numbers.",
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )

                        CurrentLocationAddressField(
                            address = additionalInformation,
                            onAddressChange = {
                                additionalInformation = it
                                authViewModel.clearMessage()
                            },
                            autoLocate = additionalInformation.isBlank(),
                            enabled = !authViewModel.isLoading &&
                                    !showOtpDialog,
                            isError = formSubmitted && !addressValid,
                            onLoadingChange = {
                                locationLoading = it
                            }
                        )

                        AuthenticationTextField(
                            value = password,
                            onValueChange = {
                                password = it.take(30)
                                authViewModel.clearMessage()
                            },
                            label = "Password",
                            placeholder = "Create a password",
                            leadingIcon = Icons.Outlined.Lock,
                            isError = formSubmitted && !passwordValid,
                            errorMessage =
                                "Use at least 8 characters, " +
                                        "one letter and one number.",
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityChange = {
                                passwordVisible = !passwordVisible
                            }
                        )

                        AuthenticationTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it.take(30)
                                authViewModel.clearMessage()
                            },
                            label = "Confirm Password",
                            placeholder = "Enter the password again",
                            leadingIcon = Icons.Outlined.Lock,
                            isError = formSubmitted && !passwordsMatch,
                            errorMessage = "Passwords do not match.",
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            isPassword = true,
                            passwordVisible = confirmPasswordVisible,
                            onPasswordVisibilityChange = {
                                confirmPasswordVisible =
                                    !confirmPasswordVisible
                            }
                        )

                        if (authViewModel.message.isNotEmpty()) {
                            Text(
                                text = authViewModel.message,
                                modifier = Modifier.fillMaxWidth(),
                                color = if (authViewModel.messageIsError) {
                                    Color(0xFFB3261E)
                                } else {
                                    PrimaryGreen
                                },
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                formSubmitted = true
                                authViewModel.clearMessage()

                                if (
                                    nameValid &&
                                    emailValid &&
                                    phoneValid &&
                                    addressValid &&
                                    passwordValid &&
                                    passwordsMatch
                                ) {
                                    authViewModel.sendSignUpOtp(
                                        email = email,
                                        password = password,
                                        onOtpSent = {
                                            showOtpDialog = true
                                        }
                                    )
                                }
                            },
                            enabled = authViewModel.canRequestOtp &&
                                    !locationLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen
                            ),
                            contentPadding = PaddingValues(
                                vertical = 16.dp
                            )
                        ) {
                            Text(
                                text = when {
                                    authViewModel.isLoading ->
                                        "Sending OTP..."

                                    locationLoading ->
                                        "Getting address..."

                                    authViewModel.resendSecondsRemaining > 0 ->
                                        "Try again in " +
                                                "${authViewModel.resendSecondsRemaining}s"

                                    else -> "Create Account"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        top = 30.dp,
                        end = 24.dp
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    color = SecondaryText,
                    fontSize = 15.sp
                )

                TextButton(
                    onClick = {
                        authViewModel.clearMessage()
                        onLoginClick(userRole)
                    },
                    enabled = !authViewModel.isLoading
                ) {
                    Text(
                        text = "Sign In",
                        color = MainText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showOtpDialog) {
        EmailOtpDialog(
            email = email.trim().lowercase(),
            authViewModel = authViewModel,
            onVerifyOtp = { enteredOtp ->
                authViewModel.verifySignUpOtpAndSaveProfile(
                    email = email,
                    otp = enteredOtp,
                    userRole = userRole,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    additionalInformation = additionalInformation,
                    onSuccess = {
                        showOtpDialog = false

                        Toast.makeText(
                            context,
                            "Email verified. Account created in Supabase.",
                            Toast.LENGTH_SHORT
                        ).show()

                        onSignUpSuccess(userRole)
                    }
                )
            },
            onResendOtp = {
                authViewModel.resendSignUpOtp(email)
            },
            onDismiss = {
                showOtpDialog = false
            }
        )
    }
}