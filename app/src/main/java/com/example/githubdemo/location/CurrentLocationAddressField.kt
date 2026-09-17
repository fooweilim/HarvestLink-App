package com.example.githubdemo.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun CurrentLocationAddressField(
    address: String,
    onAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoLocate: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    onLoadingChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val latestOnAddressChange by rememberUpdatedState(onAddressChange)
    val latestOnLoadingChange by rememberUpdatedState(onLoadingChange)

    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var automaticAttempted by rememberSaveable {
        mutableStateOf(false)
    }

    fun loadAddress() {
        if (loading) return

        loading = true
        latestOnLoadingChange(true)
        errorMessage = null

        scope.launch {
            try {
                val detectedAddress =
                    DeliveryAddressLookup.getAddress(context)

                latestOnAddressChange(detectedAddress)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message
                    ?: "Unable to get your address. Please try again."
            } finally {
                loading = false
                latestOnLoadingChange(false)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (DeliveryAddressLookup.hasPermission(context)) {
            loadAddress()
        } else {
            errorMessage =
                "Location permission was not granted. " +
                        "Allow location access in app settings, " +
                        "or enter your delivery address manually."
        }
    }

    fun requestAddress() {
        if (!enabled || loading) return

        if (DeliveryAddressLookup.hasPermission(context)) {
            loadAddress()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(autoLocate, enabled) {
        if (autoLocate && enabled && !automaticAttempted) {
            automaticAttempted = true
            requestAddress()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            latestOnLoadingChange(false)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = address,
            onValueChange = latestOnAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Delivery Address") },
            placeholder = {
                Text("Your current-location address")
            },
            enabled = enabled && !loading,
            isError = isError,
            minLines = 2,
            maxLines = 5
        )

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Text("Getting your current address...")
        }

        TextButton(
            onClick = { requestAddress() },
            enabled = enabled && !loading
        ) {
            Text("Use current location")
        }

        Text(
            text = "Check the address and add your house or unit number.",
            style = MaterialTheme.typography.bodySmall
        )

        if (isError) {
            Text(
                text = "Please enter a complete delivery address.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Location unavailable") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = { errorMessage = null }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

private object DeliveryAddressLookup {

    fun hasPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    suspend fun getAddress(context: Context): String {
        if (!hasPermission(context)) {
            throw IOException("Please allow location permission.")
        }

        val location = withTimeoutOrNull(25_000L) {
            getFreshLocation(context)
        } ?: throw IOException(
            "Unable to find your current location. " +
                    "Turn on your phone's Location setting and try again."
        )

        if (!Geocoder.isPresent()) {
            throw IOException(
                "Address lookup is unavailable on this device. " +
                        "Please enter your delivery address manually."
            )
        }

        val addresses = withTimeoutOrNull(20_000L) {
            reverseGeocode(context, location)
        } ?: throw IOException(
            "Address lookup timed out. Check your internet connection."
        )

        val address = addresses.firstOrNull()
            ?: throw IOException(
                "No street address was found. " +
                        "Please enter your delivery address manually."
            )

        val fullAddress = (0..address.maxAddressLineIndex)
            .mapNotNull { index -> address.getAddressLine(index) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .trim()

        if (fullAddress.isBlank()) {
            throw IOException(
                "No street address was found. " +
                        "Please enter your delivery address manually."
            )
        }

        return fullAddress
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(
        context: Context
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()

        continuation.invokeOnCancellation {
            cancellation.cancel()
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(20_000L)
            .build()

        try {
            LocationServices
                .getFusedLocationProviderClient(context)
                .getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnCanceledListener {
                    continuation.cancel()
                }
        } catch (exception: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(
        context: Context,
        location: Location
    ): List<Address> {
        val geocoder = Geocoder(context, Locale.getDefault())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(
                                addresses: MutableList<Address>
                            ) {
                                if (continuation.isActive) {
                                    continuation.resume(addresses)
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        IOException(
                                            "Unable to look up your address. " +
                                                    "Check your internet connection."
                                        )
                                    )
                                }
                            }
                        }
                    )
                } catch (exception: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ).orEmpty()
            }
        }
    }
}