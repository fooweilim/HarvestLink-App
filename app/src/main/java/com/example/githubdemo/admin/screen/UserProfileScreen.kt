package com.example.githubdemo.admin.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.githubdemo.admin.viewmodel.UserProfileViewModel
import com.example.githubdemo.nav.popBackStackSafely

private val UserProfileGreen = Color(0xFF195B45)

@Composable
fun UserProfileScreen(
    navController: NavHostController,
    userId: String,
    viewModel: UserProfileViewModel
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val screenEntry = remember(navController, userId) {
        navController.currentBackStackEntry
    }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        navController.popBackStackSafely(screenEntry)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "User Profile",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = UserProfileGreen
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            item {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        profile?.let { userProfile ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileInformationRow(
                            label = "Name",
                            value = userProfile.full_name
                        )

                        ProfileInformationRow(
                            label = "Email",
                            value = userProfile.email
                        )

                        ProfileInformationRow(
                            label = "Phone",
                            value = userProfile.phone_number.ifBlank {
                                "Not provided"
                            }
                        )

                        ProfileInformationRow(
                            label = "Role",
                            value = userProfile.user_role
                        )

                        ProfileInformationRow(
                            label = "Additional information",
                            value = userProfile.additional_information
                                .ifBlank { "Not provided" }
                        )

                        ProfileInformationRow(
                            label = "Registered date",
                            value = userProfile.created_at
                                .substringBefore("T")
                                .ifBlank { "Unknown" }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (
                                    userProfile.email_verified
                                ) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = if (userProfile.email_verified) {
                                    UserProfileGreen
                                } else {
                                    Color.Red
                                }
                            )

                            Text(
                                text = if (userProfile.email_verified) {
                                    " Email verified"
                                } else {
                                    " Email not verified"
                                }
                            )
                        }

                        ProfileInformationRow(
                            label = "Status",
                            value = if (userProfile.is_banned) {
                                "Banned"
                            } else {
                                "Active"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInformationRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}