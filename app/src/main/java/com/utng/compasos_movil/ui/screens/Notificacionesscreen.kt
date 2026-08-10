package com.utng.compasos_movil.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors

private val ExitoColor = Color(0xFF4CAF50)
private val PendienteColor = Color(0xFFFFA726)
private val ErrorColorNotif = Color(0xFFE53935)

/**
 * Une una notificación con su alerta relacionada (opcional). Si tu DAO ya
 * hace el join por alertaId, mapea el resultado a esto; si solo tienes la
 * lista de NotificacionEntity, pásala con alerta = null — la tarjeta usa lo
 * que haya disponible y no depende de que el join exista.
 */
data class NotificacionConAlerta(
    val notificacion: NotificacionEntity,
    val alerta: AlertaEntity? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    navController: NavController,
    notificaciones: List<NotificacionConAlerta> = emptyList()
) {
    Scaffold(
        containerColor = CompaSOSColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Notificaciones", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = CompaSOSColors.IconTint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CompaSOSColors.Background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CompaSOSColors.Background)
                .padding(padding)
        ) {
            if (notificaciones.isEmpty()) {
                EstadoSinNotificaciones(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notificaciones, key = { it.notificacion.id }) { item ->
                        TarjetaNotificacion(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoSinNotificaciones(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
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
                tint = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sin notificaciones por ahora",
            color = CompaSOSColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Aquí vas a ver el historial de alertas enviadas a tus contactos.",
            color = CompaSOSColors.TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TarjetaNotificacion(item: NotificacionConAlerta) {
    val (icono, color) = estiloEstado(item.notificacion.estado)
    val titulo = item.alerta?.tipoAlerta ?: item.notificacion.tipo ?: "Notificación"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CompaSOSColors.FieldBackground)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = CompaSOSColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            item.alerta?.descripcion?.let { descripcion ->
                Spacer(Modifier.height(2.dp))
                Text(descripcion, color = CompaSOSColors.TextSecondary, fontSize = 12.sp)
            }

            item.notificacion.destinatario?.let { destinatario ->
                Spacer(Modifier.height(4.dp))
                Text("Enviado a $destinatario", color = CompaSOSColors.TextSecondary, fontSize = 11.sp)
            }

            Spacer(Modifier.height(6.dp))
            Text(item.notificacion.fecha, color = CompaSOSColors.TextSecondary, fontSize = 11.sp)
        }

        item.notificacion.estado?.let { estado ->
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = estado.replaceFirstChar { it.uppercase() },
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Ícono y color según el texto libre de `estado`. Cubre las variantes en
 * español más comunes; cualquier valor que no reconozca cae en el estado
 * neutro en vez de romperse, así que es seguro aunque tu backend use otros
 * nombres — solo agrega tus strings exactos a la lista correspondiente.
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