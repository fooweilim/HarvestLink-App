package com.example.githubdemo.screen.foodbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.githubdemo.data.FoodBoxData
import com.example.githubdemo.location.CurrentLocationAddressField
import com.example.githubdemo.viewmodel.foodbox.FoodBoxViewModel

@Composable
fun DeliveryScheduleScreen(
    foodBoxViewModel: FoodBoxViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val state by foodBoxViewModel.uiState
    val plan = foodBoxViewModel.getSelectedPlan()

    var address by rememberSaveable {
        mutableStateOf(state.deliveryAddress)
    }
    var locationLoading by remember {
        mutableStateOf(false)
    }
    var addressSubmitted by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        FoodBoxFlowHeader(
            title = "Delivery Schedule",
            currentStep = 4,
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Text(
                    text = "Setting up your delivery",
                    color = FoodBoxSecondaryText,
                    fontSize = 17.sp
                )

                Text(
                    text = plan?.name.orEmpty(),
                    color = FoodBoxMainText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ScheduleCard(
                    title = "01   Choose Delivery Day"
                ) {
                    Text(
                        text = "Select your preferred day each week.",
                        color = FoodBoxSecondaryText
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FoodBoxData.deliveryDays.forEach { day ->
                            DayButton(
                                day = day,
                                selected = state.deliveryDay == day,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    foodBoxViewModel.selectDeliveryDay(day)
                                }
                            )
                        }
                    }

                    if (state.deliveryDay.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FoodBoxPrimaryGreen
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Delivering every ${state.deliveryDay}",
                                color = FoodBoxPrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                ScheduleCard(
                    title = "02   Delivery Address"
                ) {
                    CurrentLocationAddressField(
                        address = address,
                        onAddressChange = {
                            address = it
                            addressSubmitted = false

                            // Save valid address changes using the existing
                            // ViewModel and local draft storage.
                            if (it.trim().length >= 10) {
                                foodBoxViewModel.updateDeliveryAddress(it)
                            }
                        },
                        autoLocate = true,
                        isError = addressSubmitted &&
                                address.trim().length < 10,
                        onLoadingChange = {
                            locationLoading = it
                        }
                    )
                }
            }

            if (!state.message.isNullOrBlank()) {
                item {
                    FoodBoxMessage(message = state.message)
                }
            }

            item {
                FoodBoxPrimaryButton(
                    text = if (locationLoading) {
                        "Getting address..."
                    } else {
                        "Proceed to Checkout"
                    },
                    enabled = !locationLoading &&
                            state.deliveryDay.isNotBlank() &&
                            address.isNotBlank(),
                    onClick = {
                        addressSubmitted = true

                        if (
                            foodBoxViewModel.updateDeliveryAddress(address)
                        ) {
                            onContinueClick()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, FoodBoxBorder),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                color = FoodBoxMainText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun DayButton(
    day: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                FoodBoxPrimaryGreen
            } else {
                FoodBoxBorder
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                FoodBoxPrimaryGreen
            } else {
                FoodBoxPageBackground
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.take(3),
                color = if (selected) {
                    Color.White
                } else {
                    FoodBoxSecondaryText
                },
                fontWeight = FontWeight.Bold
            )

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.White,
                    unselectedColor = FoodBoxSecondaryText
                )
            )
        }
    }
}

@Composable
fun AddressEditDialog(
    currentAddress: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var address by rememberSaveable(currentAddress) {
        mutableStateOf(currentAddress)
    }
    var locationLoading by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery Address") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                )
            ) {
                CurrentLocationAddressField(
                    address = address,
                    onAddressChange = { address = it },
                    autoLocate = currentAddress.isBlank(),
                    onLoadingChange = {
                        locationLoading = it
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(address) },
                enabled = !locationLoading && address.isNotBlank()
            ) {
                Text(
                    text = "Save",
                    color = FoodBoxPrimaryGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}