package com.utng.compasos_movil.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.UbicacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.UbicacionEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val RojoAlerta = Color(0xFFE53935)

// ── ViewModel ────────────────────────────────────────────────────────────────

data class AlertaDetalleState(
    val alerta:       AlertaEntity?    = null,
    val ubicacion:    UbicacionEntity? = null,
    val emisorNombre: String?          = null,  // ← nuevo
    val cargando:     Boolean          = true
)

class AlertaDetalleViewModel(
    application: Application,
    private val alertaId:    String,
    private val alertaDao:   AlertaDao,
    private val ubicacionDao: UbicacionDao,
    private val usuarioDao:  UsuarioDao        // ← nuevo
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AlertaDetalleState())
    val state: StateFlow<AlertaDetalleState> = _state.asStateFlow()

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            val alerta    = alertaDao.obtenerPorId(alertaId)
            val ubicacion = ubicacionDao.obtenerUltimaPorAlerta(alertaId)

            // Resolver nombre del emisor
            val emisorNombre = alerta?.usuarioId?.let { uid ->
                val u = usuarioDao.obtenerPorId(uid)
                if (u != null) "${u.nombre} ${u.apellidoPaterno ?: ""}".trim() else null
            }

            _state.value = AlertaDetalleState(
                alerta       = alerta,
                ubicacion    = ubicacion,
                emisorNombre = emisorNombre,
                cargando     = false
            )
        }
    }

    class Factory(
        private val app:          Application,
        private val alertaId:     String,
        private val alertaDao:    AlertaDao,
        private val ubicacionDao: UbicacionDao,
        private val usuarioDao:   UsuarioDao    // ← nuevo
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AlertaDetalleViewModel(app, alertaId, alertaDao, ubicacionDao, usuarioDao) as T
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertaDetalleScreen(
    alertaId:      String,
    navController: NavController,
    alertaDao:     AlertaDao,
    ubicacionDao:  UbicacionDao,
    usuarioDao:    UsuarioDao           // ← nuevo
) {
    val context = LocalContext.current
    val vm: AlertaDetalleViewModel = viewModel(
        factory = AlertaDetalleViewModel.Factory(
            app          = context.applicationContext as Application,
            alertaId     = alertaId,
            alertaDao    = alertaDao,
            ubicacionDao = ubicacionDao,
            usuarioDao   = usuarioDao   // ← nuevo
        )
    )
    val state by vm.state.collectAsState()

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-101.1925, 21.1619))
            zoom(14.0)
        }
    }

    LaunchedEffect(state.ubicacion) {
        val lat = state.ubicacion?.latitud  ?: return@LaunchedEffect
        val lon = state.ubicacion?.longitud ?: return@LaunchedEffect
        mapViewportState.setCameraOptions(
            CameraOptions.Builder().center(Point.fromLngLat(lon, lat)).zoom(15.5).build()
        )
    }

    Scaffold(
        containerColor = CompaSOSColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Alerta de Emergencia",
                        color      = CompaSOSColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint               = CompaSOSColors.IconTint
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CompaSOSColors.Background
                )
            )
        }
    ) { padding ->

        if (state.cargando) {
            Box(
                Modifier.fillMaxSize().background(CompaSOSColors.Background),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = CompaSOSColors.AccentBlue) }
            return@Scaffold
        }

        val alerta = state.alerta
        if (alerta == null) {
            Box(
                Modifier.fillMaxSize().background(CompaSOSColors.Background).padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Alerta no encontrada.", color = CompaSOSColors.TextSecondary) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CompaSOSColors.Background)
                .padding(padding)
        ) {
            // ── Card de información ───────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = CompaSOSColors.FieldBackground
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape    = RoundedCornerShape(10.dp),
                        color    = RojoAlerta.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint     = RojoAlerta,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }

                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            alerta.tipoAlerta ?: "SOS",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = RojoAlerta
                        )

                        // ── Emisor ────────────────────────────────────────────
                        state.emisorNombre?.let { nombre ->
                            Row(
                                verticalAlignment      = Alignment.CenterVertically,
                                horizontalArrangement  = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint     = CompaSOSColors.AccentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text       = nombre,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = CompaSOSColors.AccentBlue
                                )
                            }
                        }

                        Text(
                            alerta.descripcion ?: "",
                            fontSize = 12.sp,
                            color    = CompaSOSColors.TextSecondary
                        )
                        Text(
                            "Dispositivo: ${alerta.dispositivoId ?: "desconocido"}",
                            fontSize = 11.sp,
                            color    = CompaSOSColors.TextSecondary
                        )
                        Text(
                            alerta.fecha,
                            fontSize = 10.sp,
                            color    = CompaSOSColors.TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RojoAlerta.copy(alpha = 0.12f)
                    ) {
                        Text(
                            (alerta.estado ?: "activa").replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp,
                            color    = RojoAlerta,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Mapa ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (state.ubicacion?.latitud != null && state.ubicacion?.longitud != null) {
                    MapboxMap(
                        modifier         = Modifier.fillMaxSize(),
                        mapViewportState = mapViewportState
                    ) {
                        PointAnnotation(
                            point = Point.fromLngLat(
                                state.ubicacion!!.longitud!!,
                                state.ubicacion!!.latitud!!
                            )
                        )
                    }
                } else {
                    Box(
                        Modifier.fillMaxSize().background(CompaSOSColors.FieldBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ubicación no disponible aún",
                            color    = CompaSOSColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // ── Coordenadas ───────────────────────────────────────────────────
            state.ubicacion?.let { u ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = CompaSOSColors.FieldBackground
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Latitud", fontSize = 10.sp, color = CompaSOSColors.TextSecondary)
                            Text(
                                String.format("%.6f", u.latitud ?: 0.0),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = CompaSOSColors.TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Longitud", fontSize = 10.sp, color = CompaSOSColors.TextSecondary)
                            Text(
                                String.format("%.6f", u.longitud ?: 0.0),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = CompaSOSColors.TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}