package com.utng.compasos_movil.ui.screens

import android.R.attr.duration
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.style.expressions.dsl.generated.zoom
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.utng.compasos_movil.HistorialModule.HistorialUbicacionesViewModel
import com.utng.compasos_movil.data.dao.HistorialUbicacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistorialUbicacionesScreen(
    historialDao: HistorialUbicacionDao,
    usuarioDao: UsuarioDao,
    sessionManager: SessionManager
) {
    val context = LocalContext.current

    val viewModel: HistorialUbicacionesViewModel = viewModel(
        factory = HistorialUbicacionesViewModel.Factory(
            application = context.applicationContext as Application,
            historialDao = historialDao,
            usuarioDao = usuarioDao,
            sessionManager = sessionManager
        )
    )

    val usuarioConUbicaciones by viewModel.state.collectAsState()
    val ubicacionActual by viewModel.ubicacionActual.collectAsState()

    var selectedLocationId by remember { mutableStateOf<String?>(null) }

    // ── Cámara: coordenadas iniciales de referencia (León, Gto) ──────────────
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-101.1925, 21.1619))
            zoom(14.0)
        }
    }

    // ── Mueve la cámara al llegar nueva posición GPS ──────────────────────────
    LaunchedEffect(ubicacionActual) {
        ubicacionActual?.let { loc ->
            mapViewportState.easeTo(
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(loc.longitud, loc.latitud))
                    .zoom(15.0)
                    .build(),
                animationOptions = MapAnimationOptions.mapAnimationOptions {
                    duration(800L)
                }
            )
        }
    }

    // ── Mueve la cámara cuando el usuario toca un item de la lista ────────────
    LaunchedEffect(selectedLocationId) {
        val historial = usuarioConUbicaciones?.historialUbicaciones ?: return@LaunchedEffect
        val seleccionada = historial.find { it.id == selectedLocationId } ?: return@LaunchedEffect
        val lat = seleccionada.latitud ?: return@LaunchedEffect
        val lon = seleccionada.longitud ?: return@LaunchedEffect

        mapViewportState.easeTo(
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(lon, lat))
                .zoom(16.0)
                .build(),
            animationOptions = MapAnimationOptions.mapAnimationOptions {
                duration(600L)
            }
        )
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    if (usuarioConUbicaciones == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CompaSOSColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Cargando historial...", color = CompaSOSColors.TextPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        // HEADER
        UserHeader(usuarioConUbicaciones!!.usuario)

        // MAPA: 40%
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState   // ← cámara controlada
            ) {
                usuarioConUbicaciones!!.historialUbicaciones.forEach { ubicacion ->
                    if (ubicacion.latitud != null && ubicacion.longitud != null) {
                        PointAnnotation(
                            point = Point.fromLngLat(ubicacion.longitud, ubicacion.latitud)
                        )
                    }
                }
            }
        }

        Divider(color = CompaSOSColors.FieldBorder, thickness = 1.dp)

        // LISTA: 60%
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .background(CompaSOSColors.FieldBackground)
        ) {
            Text(
                text = "Historial de Ubicaciones",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CompaSOSColors.TextPrimary,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = usuarioConUbicaciones!!.historialUbicaciones.size,
                    key = { usuarioConUbicaciones!!.historialUbicaciones[it].id }
                ) { index ->
                    val ubicacion = usuarioConUbicaciones!!.historialUbicaciones[index]
                    ListItemUbicacion(
                        ubicacion = ubicacion,
                        isSelected = selectedLocationId == ubicacion.id,
                        onClick = { selectedLocationId = ubicacion.id }
                    )
                }
            }
        }
    }

    // CARD FLOTANTE
    if (selectedLocationId != null) {
        val seleccionada = usuarioConUbicaciones!!.historialUbicaciones.find {
            it.id == selectedLocationId
        }
        if (seleccionada != null) {
            SelectedLocationCard(seleccionada)
        }
    }
}

// ── Composables privados (sin cambios) ───────────────────────────────────────

@Composable
private fun UserHeader(usuario: UsuarioEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CompaSOSColors.FieldBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CompaSOSColors.AccentBlue,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${usuario.nombre} ${usuario.apellidoPaterno ?: ""}".trim(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.TextPrimary
                )
                Text(text = usuario.correo, fontSize = 11.sp, color = CompaSOSColors.TextSecondary)
                if (!usuario.telefono.isNullOrEmpty()) {
                    Text(text = usuario.telefono, fontSize = 11.sp, color = CompaSOSColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ListItemUbicacion(
    ubicacion: HistorialUbicacionEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) CompaSOSColors.AccentBlue.copy(alpha = 0.15f)
    else CompaSOSColors.Background
    val borderColor = if (isSelected) CompaSOSColors.AccentBlue
    else CompaSOSColors.FieldBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatTimeOnly(ubicacion.fecha),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.AccentBlue
                )
                Text(
                    text = "Lat: ${String.format("%.4f", ubicacion.latitud ?: 0.0)}",
                    fontSize = 10.sp,
                    color = CompaSOSColors.TextPrimary
                )
                Text(
                    text = "Lon: ${String.format("%.4f", ubicacion.longitud ?: 0.0)}",
                    fontSize = 10.sp,
                    color = CompaSOSColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun SelectedLocationCard(ubicacion: HistorialUbicacionEntity) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            color = CompaSOSColors.FieldBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📍 Seleccionada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.AccentBlue
                )
                Text(
                    text = "${String.format("%.6f", ubicacion.latitud ?: 0.0)}",
                    fontSize = 11.sp,
                    color = CompaSOSColors.TextPrimary
                )
                Text(
                    text = "${String.format("%.6f", ubicacion.longitud ?: 0.0)}",
                    fontSize = 11.sp,
                    color = CompaSOSColors.TextPrimary
                )
                Text(
                    text = formatDate(ubicacion.fecha),
                    fontSize = 10.sp,
                    color = CompaSOSColors.TextSecondary
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        outputFormat.format(inputFormat.parse(dateString) ?: Date())
    } catch (e: Exception) { dateString }
}

private fun formatTimeOnly(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outputFormat.format(inputFormat.parse(dateString) ?: Date())
    } catch (e: Exception) { dateString.takeLast(5) }
}