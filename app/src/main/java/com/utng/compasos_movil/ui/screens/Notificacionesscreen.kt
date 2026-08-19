package com.utng.compasos_movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.NotificacionesModule.NotificacionConAlerta
import com.utng.compasos_movil.NotificacionesModule.NotificacionesViewModel
import com.utng.compasos_movil.NotificacionesModule.NotificacionesViewModelFactory
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.NotificacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.navigation.Screen
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager

/** color que representa el estado exitoso de una notificación. */
private val ExitoColor      = Color(0xFF4CAF50)

/** color que representa el estado pendiente o en proceso de una notificación. */
private val PendienteColor  = Color(0xFFFFA726)

/** color que representa el estado fallido o con error de una notificación. */
private val ErrorColorNotif = Color(0xFFE53935)

/**
 * pantalla principal para la visualización del historial de notificaciones enviadas y alertas vinculadas.
 *
 * @param navController controlador para gestionar la navegación entre pantallas.
 * @param notificacionDao acceso a datos de notificaciones en la base de datos local room.
 * @param alertaDao acceso a datos de alertas en la base de datos local room.
 * @param usuarioDao acceso a datos de usuarios en la base de datos local room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    navController:   NavController,
    notificacionDao: NotificacionDao,
    alertaDao:       AlertaDao,
    usuarioDao:      UsuarioDao          // ← nuevo
) {
    val context = LocalContext.current
    val viewModel: NotificacionesViewModel = viewModel(
        factory = NotificacionesViewModelFactory(
            notificacionDao = notificacionDao,
            alertaDao       = alertaDao,
            usuarioDao      = usuarioDao,  // ← nuevo
            sessionManager  = SessionManager(context)
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = CompaSOSColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notificaciones",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CompaSOSColors.Background)
                .padding(padding)
        ) {
            when {
                uiState.cargando -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CompaSOSColors.AccentBlue)
                    }
                }

                uiState.notificaciones.isEmpty() -> {
                    EstadoSinNotificaciones(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.notificaciones,
                            key   = { it.notificacion.id }
                        ) { item ->
                            TarjetaNotificacion(
                                item         = item,
                                onVerDetalle = { alertaId ->
                                    navController.navigate(
                                        Screen.AlertaDetalle.crearRuta(alertaId)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Composables privados ──────────────────────────────────────────────────────

/**
 * componente composable privado que despliega un mensaje informativo cuando no hay notificaciones registradas.
 *
 * @param modifier modificador de diseño opcional para ajustar la presentación del componente.
 */
@Composable
private fun EstadoSinNotificaciones(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(CompaSOSColors.AccentBlue.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.NotificationsNone,
                contentDescription = null,
                tint     = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "Sin notificaciones por ahora",
            color      = CompaSOSColors.TextPrimary,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = "Aquí vas a ver el historial de alertas enviadas a tus contactos.",
            color     = CompaSOSColors.TextSecondary,
            fontSize  = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * componente composable privado que renderiza la tarjeta individual de una notificación con su estado y detalles de alerta.
 *
 * @param item contenedor de datos [NotificacionConAlerta] con información de la notificación y su alerta asociada.
 * @param onVerDetalle callback invocado al presionar la tarjeta para navegar al detalle de la alerta.
 */
@Composable
private fun TarjetaNotificacion(
    item:         NotificacionConAlerta,
    onVerDetalle: (alertaId: String) -> Unit
) {
    val (icono, color) = estiloEstado(item.notificacion.estado)
    val titulo = item.alerta?.tipoAlerta ?: item.notificacion.tipo ?: "Notificación"

    // Nombre legible: primero el resuelto por el ViewModel, luego el ID como fallback
    val textoDestinatario = item.destinatarioNombre
        ?: item.notificacion.destinatario

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CompaSOSColors.FieldBackground)
            .clickable { onVerDetalle(item.notificacion.alertaId) }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint     = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                color      = CompaSOSColors.TextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )

            item.alerta?.descripcion?.let { descripcion ->
                Spacer(Modifier.height(2.dp))
                Text(descripcion, color = CompaSOSColors.TextSecondary, fontSize = 12.sp)
            }

            if (textoDestinatario != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "Enviado a $textoDestinatario",
                    color    = CompaSOSColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                item.notificacion.fecha,
                color    = CompaSOSColors.TextSecondary,
                fontSize = 11.sp
            )
        }

        item.notificacion.estado?.let { estado ->
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = estado.replaceFirstChar { it.uppercase() },
                    color      = color,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

/**
 * función helper privada que mapea el estado textual de una notificación a su icono vectorial y color representativo.
 *
 * @param estado texto descriptivo del estado de la notificación.
 * @return un par [Pair] que contiene el icono vectorial [ImageVector] y el color [Color] asignado al estado.
 */
private fun estiloEstado(estado: String?): Pair<ImageVector, Color> {
    return when (estado?.trim()?.lowercase()) {
        "enviada", "enviado", "entregada", "entregado", "completada", "exitosa" ->
            Icons.Filled.CheckCircle to ExitoColor
        "pendiente", "en_proceso", "enviando" ->
            Icons.Filled.Schedule to PendienteColor
        "fallida", "fallido", "error", "rechazada" ->
            Icons.Filled.Error to ErrorColorNotif
        else ->
            Icons.Filled.Notifications to CompaSOSColors.TextSecondary
    }
}