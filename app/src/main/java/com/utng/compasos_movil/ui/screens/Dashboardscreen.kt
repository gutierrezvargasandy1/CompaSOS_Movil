package com.utng.compasos_movil.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.utng.compasos_movil.AlertaPhoneRepository.AlertaPhoneRepository
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.navigation.Screen
import com.utng.compasos_movil.ui.screens.molals.MenuLateralContent
import com.utng.compasos_movil.ui.screens.molals.MenuUsuario
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * color principal de estado de emergencia.
 */
private val EmergenciaColor      = Color(0xFFE53935)

/**
 * color secundario/claro para gradientes de emergencia.
 */
private val EmergenciaColorClaro = Color(0xFFFF6F61)

/**
 * color representativo de operaciones o estados exitosos.
 */
private val ExitoColor           = Color(0xFF4CAF50)

/**
 * tiempo necesario de pulsación continua en milisegundos para activar el botón de pánico.
 */
private const val DURACION_HOLD_MS = 2000

/**
 * número de intervalos para calcular la animación de sostenimiento del botón de pánico.
 */
private const val PASOS_HOLD       = 40

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * viewmodel encargado de coordinar la lógica del dashboard principal, el envío de alertas sos y la verificación del estado de dispositivos.
 *
 * @param application instancia de la aplicación android.
 * @property sessionManager gestor de sesión para obtener la información del usuario autenticado.
 */
class DashboardViewModel(
    application: Application,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    /**
     * instancia de la base de datos local room.
     */
    private val db = AppDatabase.getInstance(application)

    /**
     * repositorio unificado para la gestión de alertas, ubicación y notificaciones en el dispositivo móvil.
     */
    private val repository = AlertaPhoneRepository(
        alertaDao = db.alertaDao(),
        ubicacionDao = db.ubicacionDao(),
        audioDao = db.audioDao(),
        notificacionDao = db.notificacionDao(),
        familiaUsuarioDao = db.familiaUsuarioDao(),
        sessionManager = sessionManager,
        dispositivoDao = db.dispositivoDao(),
        context = application
    )

    /**
     * flujo interno mutable que indica si hay un reloj inteligente vinculado.
     */
    private val _relojConectado  = MutableStateFlow(false)

    /**
     * flujo observable público con el estado de vinculación del reloj inteligente.
     */
    val relojConectado: StateFlow<Boolean> = _relojConectado.asStateFlow()

    /**
     * flujo interno mutable que guarda el identificador de la alerta generada recientemente.
     */
    private val _alertaEnviadaId = MutableStateFlow<String?>(null)

    /**
     * flujo observable público con el identificador de la alerta enviada.
     */
    val alertaEnviadaId: StateFlow<String?> = _alertaEnviadaId.asStateFlow()

    init { verificarReloj() }

    /**
     * verifica de forma asíncrona en la base de datos si el usuario posee registrado un dispositivo wear os.
     */
    private fun verificarReloj() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId  = sessionManager.obtenerUsuarioId() ?: return@launch
            val alertas = db.alertaDao().obtenerPorUsuario(userId)
            _relojConectado.value =
                alertas.any { it.dispositivoId?.startsWith("wear_") == true }
        }
    }

    /**
     * dispara una alerta de emergencia sos, registrándola en room, capturando la ubicación actual e iniciando el rastreo en vivo.
     */
    fun dispararSOS() {
        viewModelScope.launch {
            // Crea la alerta en Room, obtiene ubicación del teléfono
            // y notifica a los familiares — todo sin pasar por el broker
            val alerta = repository.crearSOSDesdeMovil() ?: return@launch
            // Inicia rastreo continuo de ubicación
            repository.iniciarRastreoEnVivo(alerta.id, viewModelScope)
            _alertaEnviadaId.value = alerta.id
        }
    }

    /**
     * limpia el identificador de alerta enviada una vez que la interfaz ha consumido la notificación.
     */
    fun consumirAlertaEnviada() { _alertaEnviadaId.value = null }

    /**
     * fábrica de viewmodel para instanciar [DashboardViewModel] pasando las dependencias requeridas.
     *
     * @property app instancia de la aplicación android.
     * @property sessionManager gestor de la sesión actual.
     */
    class Factory(
        private val app:            Application,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        /**
         * crea una nueva instancia de [DashboardViewModel].
         *
         * @param modelClass tipo de la clase viewmodel requerida.
         * @return instancia construida de [DashboardViewModel].
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(app, sessionManager) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * pantalla principal del dashboard que despliega el mapa interactivo, estado de dispositivos, botón de pánico sos y navegación lateral.
 *
 * @param navController controlador para la navegación entre pantallas.
 * @param usuario información del usuario autenticado para mostrar en el encabezado y menú.
 * @param latitud latitud actual para la visualización en el mapa.
 * @param longitud longitud actual para la visualización en el mapa.
 * @param sessionManager gestor de sesión local.
 * @param onCerrarSesion callback ejecutado cuando el usuario solicita cerrar su sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController:  NavController,
    usuario:        MenuUsuario,
    latitud:        Double = 21.1619,
    longitud:       Double = -101.1925,
    sessionManager: SessionManager,
    onCerrarSesion: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            app            = context.applicationContext as Application,
            sessionManager = sessionManager
        )
    )

    val relojConectado  by vm.relojConectado.collectAsState()
    val alertaEnviadaId by vm.alertaEnviadaId.collectAsState()

    val drawerState       = rememberDrawerState(DrawerValue.Closed)
    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var opcionActual      by remember { mutableStateOf<String?>(null) }
    var anchoMenu         by remember { mutableStateOf(0.dp) }
    val density           = LocalDensity.current

    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var tiempoRestante      by remember { mutableIntStateOf(5) }

    LaunchedEffect(mostrarConfirmacion) {
        if (mostrarConfirmacion) {
            tiempoRestante = 5
            repeat(5) {
                delay(1_000L)
                tiempoRestante--
            }
            if (mostrarConfirmacion) {
                mostrarConfirmacion = false
                vm.dispararSOS()
            }
        }
    }

    LaunchedEffect(alertaEnviadaId) {
        if (alertaEnviadaId != null) {
            snackbarHostState.showSnackbar("✓ Alerta enviada a tus familiares")
            vm.consumirAlertaEnviada()
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    fun navegarDesdeMenu(opcion: String) {
        scope.launch { drawerState.close() }
        opcionActual = opcion
        when (opcion) {
            "Contactos de emergencia"  -> navController.navigate(Screen.ContactosEmergencia.route)
            "Familia"                  -> navController.navigate(Screen.Familia.route)
            "Dispositivos"             -> navController.navigate(Screen.Dispositivos.route)
            "Historial de ubicaciones" -> navController.navigate(Screen.HistorialUbicaciones.route)
            "Notificaciones"           -> navController.navigate(Screen.Notificaciones.route)
            "Configuración"            -> navController.navigate(Screen.Configuracion.route)
            "Alertas de familiares"    -> navController.navigate(Screen.AlertasRecibidas.route)
        }
    }

    if (mostrarConfirmacion) {
        SOSConfirmacionDialog(
            tiempoRestante = tiempoRestante,
            onEnviarAhora  = {
                mostrarConfirmacion = false
                vm.dispararSOS()
            },
            onCancelar = { mostrarConfirmacion = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState     = drawerState,
            gesturesEnabled = false,
            drawerContent   = {
                ModalDrawerSheet(
                    drawerContainerColor = CompaSOSColors.Background,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        anchoMenu = with(density) { coords.size.width.toDp() }
                    }
                ) {
                    MenuLateralContent(
                        usuario              = usuario,
                        opcionSeleccionada   = opcionActual,
                        onOpcionSeleccionada = ::navegarDesdeMenu,
                        onCerrarSesion = {
                            scope.launch { drawerState.close() }
                            onCerrarSesion()
                        },
                        onEditarPerfil = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Perfil.route)
                        }
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = CompaSOSColors.Background,
                snackbarHost   = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "CompaSOS",
                                color      = CompaSOSColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close()
                                    else drawerState.open()
                                }
                            }) {
                                Icon(
                                    if (drawerState.isOpen) Icons.Filled.Close else Icons.Filled.Menu,
                                    contentDescription = null,
                                    tint = CompaSOSColors.IconTint
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CompaSOSColors.Background
                        )
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CompaSOSColors.Background)
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text       = "Hola, ${usuario.nombreCompleto.substringBefore(" ")}",
                        color      = CompaSOSColors.TextPrimary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "Todo está tranquilo por ahora",
                        color    = CompaSOSColors.TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))
                    EstadoDispositivos(relojConectado = relojConectado)

                    Spacer(Modifier.height(16.dp))
                    MapaUbicacion(
                        latitud  = latitud,
                        longitud = longitud,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )

                    Box(
                        modifier         = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        BotonPanico(onActivarPanico = { mostrarConfirmacion = true })
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (drawerState.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = anchoMenu)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { scope.launch { drawerState.close() } }
            )
        }
    }
}

// ── SOSConfirmacionDialog ─────────────────────────────────────────────────────

/**
 * componente composable privado que muestra un cuadro de diálogo con temporizador regresivo previo al envío automático de la alerta sos.
 *
 * @param tiempoRestante segundos faltantes antes de disparar la alerta.
 * @param onEnviarAhora callback para forzar el envío inmediato de la alerta.
 * @param onCancelar callback para cancelar el envío de emergencia.
 */
@Composable
private fun SOSConfirmacionDialog(
    tiempoRestante: Int,
    onEnviarAhora:  () -> Unit,
    onCancelar:     () -> Unit
) {
    Dialog(onDismissRequest = onCancelar) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CompaSOSColors.FieldBackground
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(EmergenciaColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint     = EmergenciaColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text       = "¿Activar alerta de emergencia?",
                    color      = CompaSOSColors.TextPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )

                Text(
                    text      = "Se notificará a todos tus familiares con tu ubicación en tiempo real.",
                    color     = CompaSOSColors.TextSecondary,
                    fontSize  = 13.sp,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier         = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress    = { tiempoRestante / 5f },
                        modifier    = Modifier.fillMaxSize(),
                        color       = EmergenciaColor,
                        trackColor  = EmergenciaColor.copy(alpha = 0.15f),
                        strokeWidth = 6.dp
                    )
                    Text(
                        text       = "$tiempoRestante",
                        color      = EmergenciaColor,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text     = "Enviando automáticamente en $tiempoRestante s",
                    color    = CompaSOSColors.TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick  = onCancelar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = CompaSOSColors.TextPrimary
                    )
                ) {
                    Text("Cancelar — no es emergencia", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick  = onEnviarAhora,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = EmergenciaColor)
                ) {
                    Text("Enviar ahora", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── EstadoDispositivos ────────────────────────────────────────────────────────

/**
 * componente composable privado que muestra el estado de vinculación con el reloj inteligente del usuario.
 *
 * @param relojConectado indica si existe un reloj vinculado.
 */
@Composable
private fun EstadoDispositivos(relojConectado: Boolean) {
    EstadoPill(
        icono = Icons.Filled.Watch,
        texto = if (relojConectado) "Reloj vinculado" else "Sin reloj vinculado",
        color = if (relojConectado) ExitoColor else CompaSOSColors.TextSecondary
    )
}

/**
 * componente composable privado que renderiza una pastilla/badge con icono y texto explicativo.
 *
 * @param icono vector del icono a presentar.
 * @param texto cadena descriptiva.
 * @param color color representativo del estado.
 */
@Composable
private fun EstadoPill(icono: ImageVector, texto: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── MapaUbicacion ─────────────────────────────────────────────────────────────

/**
 * componente composable privado para desplegar el mapa interactivo de mapbox centrado en las coordenadas del usuario.
 *
 * @param latitud latitud actual del usuario.
 * @param longitud longitud actual del usuario.
 * @param modifier modificador para el contenedor del mapa.
 * @param zoom nivel de zoom inicial del mapa.
 */
@SuppressLint("RememberReturnType")
@OptIn(MapboxExperimental::class)
@Composable
private fun MapaUbicacion(
    latitud:  Double,
    longitud: Double,
    modifier: Modifier = Modifier,
    zoom:     Double   = 15.0
) {
    remember {
        MapboxOptions.accessToken =
            "pk.eyJ1IjoiYW5keXNzMTIiLCJhIjoiY21zbHVtYmJkMTh6YTJ4b29zb25pcjMzOSJ9.388hSDi65h3MrNr5v-rSgQ"
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(longitud, latitud))
            zoom(zoom)
        }
    }

    LaunchedEffect(latitud, longitud, zoom) {
        mapViewportState.setCameraOptions {
            center(Point.fromLngLat(longitud, latitud))
            zoom(zoom)
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.align(Alignment.End).padding(bottom = 8.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(ExitoColor, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                "En vivo",
                color      = CompaSOSColors.TextSecondary,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp))
        ) {
            MapboxMap(
                modifier         = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                scaleBar         = {},
                style            = { MapStyle(style = Style.DARK) }
            )
            PuntoUbicacionActual(
                color    = CompaSOSColors.AccentBlue,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text       = "%.4f, %.4f".format(latitud, longitud),
                color      = Color.White,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * componente composable privado que dibuja un indicador con animación de pulso sobre el mapa para representar la posición actual.
 *
 * @param color color del punto central y del indicador de pulso.
 * @param modifier modificador para la composición.
 */
@Composable
private fun PuntoUbicacionActual(color: Color, modifier: Modifier = Modifier) {
    val transition  = rememberInfiniteTransition(label = "pulso_gps")
    val escalaPulso by transition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label         = "escala_pulso_gps"
    )
    val alfaPulso by transition.animateFloat(
        initialValue  = 0.45f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label         = "alfa_pulso_gps"
    )

    Box(modifier = modifier.size(54.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = escalaPulso, scaleY = escalaPulso, alpha = alfaPulso)
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(11.dp).background(color, CircleShape))
        }
    }
}

// ── BotonPanico ───────────────────────────────────────────────────────────────

/**
 * componente composable privado que renderiza el botón interactivo sos, requiriendo pulsación prolongada de 2 segundos para confirmación.
 *
 * @param onActivarPanico callback que se invoca tras completar con éxito la pulsación continuada.
 */
@Composable
private fun BotonPanico(onActivarPanico: () -> Unit) {
    var presionando by remember { mutableStateOf(false) }
    var progreso    by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulso_panico")
    val escala by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.06f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "escala"
    )

    LaunchedEffect(presionando) {
        if (presionando) {
            val pasoMs = (DURACION_HOLD_MS / PASOS_HOLD).toLong()
            for (paso in 1..PASOS_HOLD) {
                delay(pasoMs)
                if (!presionando) { progreso = 0f; return@LaunchedEffect }
                progreso = paso / PASOS_HOLD.toFloat()
            }
            onActivarPanico()
            presionando = false
            progreso    = 0f
        } else {
            progreso = 0f
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diametroAnillo  = min(min(maxWidth, maxHeight) * 0.62f, 190.dp)
        val diametroCirculo = diametroAnillo * 0.84f

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress    = { progreso },
                    modifier    = Modifier.size(diametroAnillo),
                    color       = EmergenciaColor,
                    trackColor  = EmergenciaColor.copy(alpha = 0.15f),
                    strokeWidth = 5.dp
                )
                Box(
                    modifier = Modifier
                        .size(diametroCirculo)
                        .graphicsLayer(scaleX = escala, scaleY = escala)
                        .shadow(
                            24.dp, CircleShape,
                            ambientColor = EmergenciaColor,
                            spotColor    = EmergenciaColor
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(EmergenciaColorClaro, EmergenciaColor),
                                radius = 170f
                            ),
                            CircleShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    presionando = true
                                    tryAwaitRelease()
                                    presionando = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SOS",
                        color      = Color.White,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text      = "Mantén presionado 2 segundos para activar",
                color     = CompaSOSColors.TextSecondary,
                fontSize  = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}