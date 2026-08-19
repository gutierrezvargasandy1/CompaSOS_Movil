package com.utng.compasos_movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.DispositivosModule.DispositivoConUsuario
import com.utng.compasos_movil.DispositivosModule.DispositivosViewModel
import com.utng.compasos_movil.DispositivosModule.DispositivosViewModelFactory
import com.utng.compasos_movil.DispositivosModule.EstadoVinculacion
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager

/**
 * color para indicar operaciones exitosas o dispositivos conectados.
 */
private val ExitoColor        = Color(0xFF4CAF50)

/**
 * color para indicar un nivel medio de batería.
 */
private val BateriaMediaColor = Color(0xFFFFA726)

/**
 * color para indicar un nivel bajo de batería o estados de error.
 */
private val BateriaBajaColor  = Color(0xFFE53935)

/**
 * pantalla principal de gestión de dispositivos que lista los equipos vinculados y permite iniciar procesos de vinculación para wear os o pantallas tv.
 *
 * @param navController controlador para la navegación entre pantallas.
 * @param dispositivoDao acceso a datos de dispositivos en la base de datos local room.
 * @param usuarioDao acceso a datos de usuarios en la base de datos local room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositivosScreen(
    navController: NavController,
    dispositivoDao: DispositivoDao,
    usuarioDao: UsuarioDao
) {
    val context = LocalContext.current
    val viewModel: DispositivosViewModel = viewModel(
        factory = DispositivosViewModelFactory(
            dispositivoDao = dispositivoDao,
            usuarioDao     = usuarioDao,
            sessionManager = SessionManager(context)
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = CompaSOSColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Dispositivos",
                            color      = CompaSOSColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        // Punto verde si MQTT está conectado
                        if (uiState.mqttConectado) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(ExitoColor, CircleShape)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = CompaSOSColors.IconTint
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(com.utng.compasos_movil.navigation.Screen.VincularTv.route) }) {
                        Icon(
                            Icons.Filled.Tv,
                            contentDescription = "Vincular pantalla TV",
                            tint = CompaSOSColors.AccentBlue
                        )
                    }
                    IconButton(onClick = { viewModel.iniciarVinculacion() }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Vincular dispositivo",
                            tint = CompaSOSColors.AccentBlue
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

                uiState.dispositivos.isEmpty() -> {
                    EstadoSinDispositivos(
                        modifier   = Modifier.align(Alignment.Center),
                        onVincular = { viewModel.iniciarVinculacion() }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier        = Modifier.fillMaxSize(),
                        contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.dispositivos,
                            key   = { it.dispositivo.id }
                        ) { item ->
                            TarjetaDispositivo(item)
                        }
                    }
                }
            }
        }
    }

    // Diálogo de vinculación
    val estado = uiState.estadoVinculacion
    if (estado !is EstadoVinculacion.Inactivo) {
        DialogVinculacion(
            estado    = estado,
            onCancelar = { viewModel.cancelarVinculacion() },
            onCerrar   = { viewModel.cerrarDialogoVinculacion() }
        )
    }
}

// ============================================================
// DIÁLOGO DE VINCULACIÓN
// ============================================================

/**
 * componente modal que presenta el cuadro de diálogo para los distintos estados del proceso de vinculación.
 *
 * @param estado estado actual del proceso de vinculación ([EstadoVinculacion]).
 * @param onCancelar callback invocado para cancelar el proceso activo de vinculación.
 * @param onCerrar callback invocado para cerrar la ventana modal tras finalizar o fallar.
 */
@Composable
private fun DialogVinculacion(
    estado: EstadoVinculacion,
    onCancelar: () -> Unit,
    onCerrar: () -> Unit
) {
    Dialog(onDismissRequest = {
        when (estado) {
            is EstadoVinculacion.EsperandoWearOS -> onCancelar()
            else                                 -> onCerrar()
        }
    }) {
        Surface(shape = RoundedCornerShape(16.dp), color = CompaSOSColors.FieldBackground) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (estado) {
                    is EstadoVinculacion.EsperandoWearOS ->
                        ContenidoEsperando(codigo = estado.codigo, onCancelar = onCancelar)
                    is EstadoVinculacion.Exitosa ->
                        ContenidoExitoso(nombre = estado.nombreDispositivo, onCerrar = onCerrar)
                    is EstadoVinculacion.Error ->
                        ContenidoError(mensaje = estado.mensaje, onCerrar = onCerrar)
                    is EstadoVinculacion.Inactivo -> Unit
                }
            }
        }
    }
}

/**
 * componente composable privado que muestra el código numérico y los pasos para sincronizar un reloj wear os.
 *
 * @param codigo código numérico generado para la vinculación.
 * @param onCancelar callback para cancelar la espera de confirmación.
 */
@Composable
private fun ContenidoEsperando(codigo: String, onCancelar: () -> Unit) {
    Icon(
        Icons.Filled.Watch,
        contentDescription = null,
        tint = CompaSOSColors.AccentBlue,
        modifier = Modifier.size(40.dp)
    )

    Text(
        "Vincular Wear OS",
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = CompaSOSColors.TextPrimary
    )

    // Código grande y visible
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = CompaSOSColors.Background
    ) {
        Text(
            text          = codigo,
            fontSize      = 36.sp,
            fontWeight    = FontWeight.Bold,
            fontFamily    = FontFamily.Monospace,
            color         = CompaSOSColors.AccentBlue,
            textAlign     = TextAlign.Center,
            letterSpacing = 8.sp,
            modifier      = Modifier.padding(vertical = 16.dp)
        )
    }

    // Instrucciones
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PasoInstruccion("1", "Abre CompaSOS en tu Wear OS")
        PasoInstruccion("2", "Ve a \"Vincular dispositivo\"")
        PasoInstruccion("3", "Ingresa el código de arriba")
    }

    // Indicador de espera
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier    = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color       = CompaSOSColors.AccentBlue
        )
        Text(
            "Esperando confirmación...",
            fontSize = 12.sp,
            color    = CompaSOSColors.TextSecondary
        )
    }

    Button(
        onClick  = onCancelar,
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.FieldBorder),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Text("Cancelar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
    }
}

/**
 * componente composable privado para desplegar un paso numerado de las instrucciones de vinculación.
 *
 * @param numero secuencia o número del paso.
 * @param texto indicación detallada del paso.
 */
@Composable
private fun PasoInstruccion(numero: String, texto: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape    = CircleShape,
            color    = CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    numero,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = CompaSOSColors.AccentBlue
                )
            }
        }
        Text(texto, fontSize = 12.sp, color = CompaSOSColors.TextSecondary)
    }
}

/**
 * componente composable privado que informa al usuario la vinculación exitosa del dispositivo.
 *
 * @param nombre nombre o descripción del dispositivo vinculado.
 * @param onCerrar callback para descartar la ventana de diálogo.
 */
@Composable
private fun ContenidoExitoso(nombre: String, onCerrar: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(ExitoColor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint     = ExitoColor,
            modifier = Modifier.size(30.dp)
        )
    }
    Text(
        "¡Vinculado correctamente!",
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = CompaSOSColors.TextPrimary,
        textAlign  = TextAlign.Center
    )
    Text(
        nombre,
        fontSize   = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color      = CompaSOSColors.AccentBlue,
        textAlign  = TextAlign.Center
    )
    Text(
        "El dispositivo ya aparece en tu lista.",
        fontSize  = 12.sp,
        color     = CompaSOSColors.TextSecondary,
        textAlign = TextAlign.Center
    )
    Button(
        onClick  = onCerrar,
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.AccentBlue),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Text("Listo", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
    }
}

/**
 * componente composable privado que muestra un estado de error surgido durante el proceso de vinculación.
 *
 * @param mensaje texto con la descripción detallada del problema.
 * @param onCerrar callback para cerrar la ventana modal de error.
 */
@Composable
private fun ContenidoError(mensaje: String, onCerrar: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(BateriaBajaColor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Error,
            contentDescription = null,
            tint     = BateriaBajaColor,
            modifier = Modifier.size(30.dp)
        )
    }
    Text(
        "Error de vinculación",
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = CompaSOSColors.TextPrimary,
        textAlign  = TextAlign.Center
    )
    Text(mensaje, fontSize = 12.sp, color = CompaSOSColors.TextSecondary, textAlign = TextAlign.Center)
    Button(
        onClick  = onCerrar,
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.FieldBorder),
        shape    = RoundedCornerShape(8.dp)
    ) {
        Text("Cerrar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
    }
}

// ============================================================
// COMPOSABLES PRIVADOS (sin cambios de lógica)
// ============================================================

/**
 * componente composable privado que se muestra cuando la lista de dispositivos vinculados está vacía.
 *
 * @param modifier modificador de diseño del contenedor.
 * @param onVincular callback para activar la acción de vincular un dispositivo.
 */
@Composable
private fun EstadoSinDispositivos(modifier: Modifier = Modifier, onVincular: () -> Unit) {
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
                tint     = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Sin dispositivos vinculados",
            color      = CompaSOSColors.TextPrimary,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Vincula un reloj o teléfono para empezar a monitorear.",
            color     = CompaSOSColors.TextSecondary,
            fontSize  = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onVincular,
            colors  = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.AccentBlue),
            shape   = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Add, null, tint = CompaSOSColors.TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Vincular dispositivo", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * componente composable privado que renderiza la tarjeta individual de un dispositivo vinculado con su estado y nivel de batería.
 *
 * @param item estructura [DispositivoConUsuario] con los datos del dispositivo y su usuario asociado.
 */
@Composable
private fun TarjetaDispositivo(item: DispositivoConUsuario) {
    val dispositivo   = item.dispositivo
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
                tint     = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = CompaSOSColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            item.usuario?.let {
                Spacer(Modifier.height(2.dp))
                Text("De ${it.nombre}", color = CompaSOSColors.TextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(colorConexion, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (dispositivo.conectado) "Conectado" else "Desconectado",
                    color      = colorConexion,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                dispositivo.bateria?.let { bateria ->
                    val cb = colorBateria(bateria)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Filled.BatteryFull, null, tint = cb, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("$bateria%", color = cb, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Vinculado desde ${dispositivo.fechaVinculacion}",
                color    = CompaSOSColors.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * retorna el icono vectorial adecuado de acuerdo al tipo de dispositivo recibido.
 *
 * @param tipo tipo o categoría del dispositivo en formato texto.
 * @return icono vectorial [ImageVector] representativo.
 */
private fun iconoTipo(tipo: String?): ImageVector = when (tipo?.trim()?.lowercase()) {
    "reloj", "smartwatch", "watch"                           -> Icons.Filled.Watch
    "telefono", "teléfono", "celular", "smartphone", "phone" -> Icons.Filled.Smartphone
    "tablet"                                                  -> Icons.Filled.Tablet
    else                                                      -> Icons.Filled.DeviceUnknown
}

/**
 * determina el color según el nivel de porcentaje de batería.
 *
 * @param pct porcentaje de batería del dispositivo (0-100).
 * @return [Color] acorde al nivel de carga.
 */
private fun colorBateria(pct: Int): Color = when {
    pct <= 20 -> BateriaBajaColor
    pct <= 50 -> BateriaMediaColor
    else      -> ExitoColor
}