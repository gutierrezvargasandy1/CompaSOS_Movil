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

/**
 * modelo de datos que representa una coordenada de ubicación geográfica simplificada.
 *
 * @property latitud coordenada de latitud geográfica en grados decimales.
 * @property longitud coordenada de longitud geográfica en grados decimales.
 */
data class UbicacionActual(
    val latitud: Double,
    val longitud: Double
)

/**
 * repositorio encargado de proveer y gestionar el acceso a los servicios de localización geográfica del dispositivo.
 *
 * @property context contexto de la aplicación para inicializar el cliente de localización.
 */
class LocationRepository(private val context: Context) {

    /**
     * cliente de proveedor de ubicación fusionada de google play services.
     */
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * emite un flujo continuo ([Flow]) con las coordenadas actualizadas en tiempo real del dispositivo.
     *
     * @return un flujo [Flow] que emite objetos [UbicacionActual] a medida que se capturan lecturas de GPS.
     */
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

    /**
     * obtiene de forma síncrona la última ubicación registrada disponible con un tiempo límite de espera.
     *
     * @return la [UbicacionActual] más reciente conocida o null si falla o se agota el tiempo de espera.
     */
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