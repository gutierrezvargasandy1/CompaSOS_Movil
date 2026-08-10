package com.utng.compasos_movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Watch
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
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors

private val ExitoColor = Color(0xFF4CAF50)
private val BateriaMediaColor = Color(0xFFFFA726)
private val BateriaBajaColor = Color(0xFFE53935)

/**
 * Une un dispositivo con su dueño (opcional). DispositivoEntity solo guarda
 * usuarioId, no los datos del usuario — si no tienes el join hecho todavía,
 * pásalo con usuario = null y la tarjeta funciona igual, solo sin ese dato.
 * No se muestra tokenFcm ni numeroSerie: son campos internos, no de UI.
 */
data class DispositivoConUsuario(
    val dispositivo: DispositivoEntity,
    val usuario: UsuarioEntity? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositivosScreen(
    navController: NavController,
    dispositivos: List<DispositivoConUsuario> = emptyList()
) {
    Scaffold(
        containerColor = CompaSOSColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Dispositivos", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
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
            if (dispositivos.isEmpty()) {
                EstadoSinDispositivos(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dispositivos, key = { it.dispositivo.id }) { item ->
                        TarjetaDispositivo(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoSinDispositivos(modifier: Modifier = Modifier) {
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
                Icons.Filled.Watch,
                contentDescription = null,
                tint = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sin dispositivos vinculados",
            color = CompaSOSColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Vincula un reloj o teléfono para empezar a monitorear.",
            color = CompaSOSColors.TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TarjetaDispositivo(item: DispositivoConUsuario) {
    val dispositivo = item.dispositivo
    val colorConexion = if (dispositivo.conectado) ExitoColor else CompaSOSColors.TextSecondary
    val titulo = listOfNotNull(dispositivo.fabricante, dispositivo.modelo)
        .joinToString(" ")
        .ifBlank { dispositivo.tipo ?: "Dispositivo" }

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
                .background(CompaSOSColors.AccentBlue.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconoTipo(dispositivo.tipo),
                contentDescription = null,
                tint = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = CompaSOSColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            item.usuario?.let { usuario ->
                Spacer(Modifier.height(2.dp))
                Text("De ${usuario.nombre}", color = CompaSOSColors.TextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colorConexion, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (dispositivo.conectado) "Conectado" else "Desconectado",
                    color = colorConexion,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                dispositivo.bateria?.let { bateria ->
                    val colorBateria = colorBateria(bateria)
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Filled.BatteryFull,
                        contentDescription = null,
                        tint = colorBateria,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text("$bateria%", color = colorBateria, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Vinculado desde ${dispositivo.fechaVinculacion}",
                color = CompaSOSColors.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Ícono según el texto libre de `tipo`. Cualquier valor no reconocido cae en
 * un ícono genérico de dispositivo en vez de romperse — agrega tus strings
 * exactos aquí si tu backend usa otros nombres.
 */
private fun iconoTipo(tipo: String?): ImageVector = when (tipo?.trim()?.lowercase()) {
    "reloj", "smartwatch", "watch" -> Icons.Filled.Watch
    "telefono", "teléfono", "celular", "smartphone", "phone" -> Icons.Filled.Smartphone
    "tablet" -> Icons.Filled.Tablet
    else -> Icons.Filled.DeviceUnknown
}

private fun colorBateria(porcentaje: Int): Color = when {
    porcentaje <= 20 -> BateriaBajaColor
    porcentaje <= 50 -> BateriaMediaColor
    else -> ExitoColor
}