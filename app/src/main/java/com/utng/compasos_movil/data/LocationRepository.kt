package com.utng.compasos_movil.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

data class UbicacionActual(
    val latitud: Double,
    val longitud: Double
)

class LocationRepository(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun ubicacionEnVivo(): Flow<UbicacionActual> = callbackFlow {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5_000L
        )
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        UbicacionActual(
                            latitud  = location.latitude,
                            longitud = location.longitude
                        )
                    )
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    fun obtenerUltimaUbicacion(): UbicacionActual? {
        return try {
            val loc: Location? = Tasks.await(
                fusedClient.lastLocation,
                3L,
                TimeUnit.SECONDS
            )
            if (loc != null) {
                UbicacionActual(
                    latitud  = loc.latitude,
                    longitud = loc.longitude
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}