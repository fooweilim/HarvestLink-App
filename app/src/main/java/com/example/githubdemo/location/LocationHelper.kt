package com.example.githubdemo.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocationHelper {

    suspend fun getCurrentLocation(
        context: Context
    ): String {
        if (!hasLocationPermission(context)) {
            return "Please allow location permission"
        }

        return try {
            val location = withTimeoutOrNull(25_000L) {
                fetchCurrentLocation(context)
            } ?: return "Unable to get location. Please turn on Location."

            if (!Geocoder.isPresent()) {
                return "Address lookup unavailable on this device"
            }

            val addresses = withTimeoutOrNull(20_000L) {
                getAddresses(context, location)
            } ?: return "Address lookup timed out"

            val address = addresses.firstOrNull()
                ?: return "No address found for this location"

            val fullAddress = (0..address.maxAddressLineIndex)
                .mapNotNull { index ->
                    address.getAddressLine(index)
                }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")

            fullAddress.ifBlank {
                listOfNotNull(
                    address.subThoroughfare,
                    address.thoroughfare,
                    address.subLocality,
                    address.locality,
                    address.postalCode,
                    address.adminArea,
                    address.countryName
                )
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .ifBlank {
                        "No address found for this location"
                    }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SecurityException) {
            "Please allow location permission"
        } catch (exception: Exception) {
            "Unable to load address. Check your connection."
        }
    }

    private fun hasLocationPermission(
        context: Context
    ): Boolean {
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

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(
        context: Context
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationTokenSource = CancellationTokenSource()

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(20_000L)
            .build()

        try {
            LocationServices
                .getFusedLocationProviderClient(context)
                .getCurrentLocation(
                    request,
                    cancellationTokenSource.token
                )
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
    private suspend fun getAddresses(
        context: Context,
        location: Location
    ): List<Address> {
        val geocoder = Geocoder(
            context,
            Locale.getDefault()
        )

        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
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

                            override fun onError(
                                errorMessage: String?
                            ) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        IOException(
                                            errorMessage
                                                ?: "Address lookup failed"
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