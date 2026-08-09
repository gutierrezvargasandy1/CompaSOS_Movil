package com.utng.compasos_movil.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.utng.compasos_movil.navigation.Screen
import com.utng.compasos_movil.ui.screens.molals.CompaSOSAlertBanner
import com.utng.compasos_movil.ui.screens.molals.CompaSOSAlertType
import com.utng.compasos_movil.ui.screens.molals.MenuLateralContent
import com.utng.compasos_movil.ui.screens.molals.MenuUsuario
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EmergenciaColor = Color(0xFFE53935)
private val EmergenciaColorClaro = Color(0xFFFF6F61)
private val ExitoColor = Color(0xFF4CAF50)

private const val DURACION_HOLD_MS = 2000
private const val PASOS_HOLD = 40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    usuario: MenuUsuario,
    latitud: Double = 21.1619,
    longitud: Double = -101.1925,
    relojConectado: Boolean = true,
    familiaresEnLinea: Int = 0,
    alertaZona: String? = null,
    onActivarPanico: () -> Unit = {},
    onLlamar911: () -> Unit = {},
    onCompartirUbicacion: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var opcionActual by remember { mutableStateOf<String?>(null) }
    // Ancho real del panel del menú, medido en tiempo real (no adivinado),
    // para que el overlay de "cerrar al tocar afuera" no tape sus botones.
    var anchoMenu by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // Con gesturesEnabled = false también se apaga el "toca afuera para
    // cerrar" que trae Material3 por defecto (los dos comparten el mismo
    // flag). El botón de atrás queda como cierre confiable en su lugar.
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    fun navegarDesdeMenu(opcion: String) {
        scope.launch { drawerState.close() }
        opcionActual = opcion
        when (opcion) {
            "Contactos de emergencia" -> navController.navigate(Screen.ContactosEmergencia.route)
            "Familia" -> navController.navigate(Screen.Familia.route)
            "Dispositivos" -> navController.navigate(Screen.Dispositivos.route)
            "Historial de ubicaciones" -> navController.navigate(Screen.HistorialUbicaciones.route)
            "Notificaciones" -> navController.navigate(Screen.Notificaciones.route)
            "Configuración" -> navController.navigate(Screen.Configuracion.route)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // El swipe para abrir el menú competía con el gesto de arrastrar el
            // mapa, así que queda apagado. Para cerrar: el mismo ícono (toggle),
            // el botón/gesto de atrás (BackHandler arriba), o tocar afuera del
            // panel (el Box condicional después de este ModalNavigationDrawer).
            gesturesEnabled = false,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = CompaSOSColors.Background,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        anchoMenu = with(density) { coordinates.size.width.toDp() }
                    }
                ) {
                    MenuLateralContent(
                        usuario = usuario,
                        opcionSeleccionada = opcionActual,
                        onOpcionSeleccionada = ::navegarDesdeMenu,
                        onCerrarSesion = {
                            scope.launch { drawerState.close() }
                            onCerrarSesion()
                        },
                        onEditarPerfil = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.PerfilMedico.route)
                        }
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = CompaSOSColors.Background,
                topBar = {
                    TopAppBar(
                        title = {
                            Text("CompaSOS", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                }
                            ) {
                                Icon(
                                    if (drawerState.isOpen) Icons.Filled.Close else Icons.Filled.Menu,
                                    contentDescription = if (drawerState.isOpen) "Cerrar menú" else "Abrir menú",
                                    tint = CompaSOSColors.IconTint
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CompaSOSColors.Background)
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
                        text = "Hola, ${usuario.nombreCompleto.substringBefore(" ")}",
                        color = CompaSOSColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Todo está tranquilo por ahora",
                        color = CompaSOSColors.TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))
                    EstadoDispositivos(relojConectado = relojConectado, familiaresEnLinea = familiaresEnLinea)

                    Spacer(Modifier.height(16.dp))

                    CompaSOSAlertBanner(
                        mensaje = alertaZona ?: "",
                        tipo = CompaSOSAlertType.Advertencia,
                        visible = alertaZona != null
                    )
                    if (alertaZona != null) Spacer(Modifier.height(16.dp))

                    MapaUbicacion(
                        latitud = latitud,
                        longitud = longitud,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        BotonPanico(onActivarPanico = onActivarPanico)
                    }

                    AccionesRapidas(
                        onLlamar911 = onLlamar911,
                        onCompartirUbicacion = onCompartirUbicacion,
                        onVerContactos = { navegarDesdeMenu("Contactos de emergencia") }
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Cubre solo el área fuera del panel (el ancho medido arriba), para
        // no robarle los toques a los botones del menú mismo.
        if (drawerState.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = anchoMenu)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch { drawerState.close() }
                    }
            )
        }
    }
}

@Composable
private fun EstadoDispositivos(relojConectado: Boolean, familiaresEnLinea: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        EstadoPill(
            icono = Icons.Filled.Watch,
            texto = if (relojConectado) "Reloj conectado" else "Reloj desconectado",
            color = if (relojConectado) ExitoColor else CompaSOSColors.TextSecondary
        )
        EstadoPill(
            icono = Icons.Filled.PersonPin,
            texto = "$familiaresEnLinea en línea",
            color = CompaSOSColors.AccentBlue
        )
    }
}

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

@SuppressLint("RememberReturnType")
@OptIn(MapboxExperimental::class)
@Composable
private fun MapaUbicacion(
    latitud: Double,
    longitud: Double,
    modifier: Modifier = Modifier,
    zoom: Double = 15.0
) {
    // Token pegado directo — ya confirmamos que este valor funciona. Cuando
    // quieras, lo movemos de vuelta a developer-config.xml (bueno para no
    // subirlo a git), pero no es urgente: es el token público, no el secreto.
    remember {
        MapboxOptions.accessToken = "pk.eyJ1IjoiYW5keXNzMTIiLCJhIjoiY21zbHVtYmJkMTh6YTJ4b29zb25pcjMzOSJ9.388hSDi65h3MrNr5v-rSgQ"
    }

    // OJO orden: Point.fromLngLat va (longitud, latitud) — al revés que GeoPoint
    // de osmdroid, que iba (latitud, longitud). Es la fuente #1 de bugs al migrar.
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
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ExitoColor, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text("En vivo", color = CompaSOSColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                scaleBar = {},
                style = { MapStyle(style = Style.DARK) }
            )

            // El mapa siempre se recentra en (latitud, longitud), así que un punto
            // fijo en el centro del recuadro representa la ubicación actual.
            PuntoUbicacionActual(
                color = CompaSOSColors.AccentBlue,
                modifier = Modifier.align(Alignment.Center)
            )

            Text(
                text = "%.4f, %.4f".format(latitud, longitud),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PuntoUbicacionActual(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulso_gps")
    val escalaPulso by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "escala_pulso_gps"
    )
    val alfaPulso by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alfa_pulso_gps"
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
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
private fun BotonPanico(onActivarPanico: () -> Unit) {
    var presionando by remember { mutableStateOf(false) }
    var progreso by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulso_panico")
    val escala by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    LaunchedEffect(presionando) {
        if (presionando) {
            val pasoMs = (DURACION_HOLD_MS / PASOS_HOLD).toLong()
            for (paso in 1..PASOS_HOLD) {
                delay(pasoMs)
                if (!presionando) {
                    progreso = 0f
                    return@LaunchedEffect
                }
                progreso = paso / PASOS_HOLD.toFloat()
            }
            onActivarPanico()
            presionando = false
            progreso = 0f
        } else {
            progreso = 0f
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Se ajusta al espacio real disponible en cada teléfono (con un tope
        // máximo para que no se vea gigante en pantallas más altas), en vez
        // de un tamaño fijo que podía verse desproporcionado.
        val diametroAnillo = min(min(maxWidth, maxHeight) * 0.62f, 190.dp)
        val diametroCirculo = diametroAnillo * 0.84f

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.size(diametroAnillo),
                    color = EmergenciaColor,
                    trackColor = EmergenciaColor.copy(alpha = 0.15f),
                    strokeWidth = 5.dp
                )
                Box(
                    modifier = Modifier
                        .size(diametroCirculo)
                        .graphicsLayer(scaleX = escala, scaleY = escala)
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            ambientColor = EmergenciaColor,
                            spotColor = EmergenciaColor
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
                    Text("SOS", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Mantén presionado 2 segundos para activar",
                color = CompaSOSColors.TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AccionesRapidas(
    onLlamar911: () -> Unit,
    onCompartirUbicacion: () -> Unit,
    onVerContactos: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AccionRapidaBoton(
            texto = "Llamar 911",
            icono = Icons.Filled.Phone,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = onLlamar911
        )
        AccionRapidaBoton(
            texto = "Compartir\nubicación",
            icono = Icons.Filled.Share,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = onCompartirUbicacion
        )
        AccionRapidaBoton(
            texto = "Contactos",
            icono = Icons.Filled.PersonPin,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = onVerContactos
        )
    }
}

@Composable
private fun AccionRapidaBoton(
    texto: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CompaSOSColors.FieldBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(CompaSOSColors.AccentBlue.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = texto,
            color = CompaSOSColors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}