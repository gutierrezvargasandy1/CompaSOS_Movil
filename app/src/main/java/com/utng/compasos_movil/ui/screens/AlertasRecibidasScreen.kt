package com.utng.compasos_movil.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.NotificacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.navigation.Screen
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * color distintivo asignado a las alertas de emergencia.
 */
private val RojoAlerta = Color(0xFFE53935)

/**
 * etiqueta utilizada para la depuración en logcat.
 */
private const val TAG  = "AlertasRecibidas"

// ── Data ──────────────────────────────────────────────────────────────────────

/**
 * representa un elemento individual de la lista de alertas recibidas con su información asociada.
 *
 * @property notificacion entidad de la notificación de alerta guardada en la base de datos local.
 * @property alerta entidad detallada de la alerta recibida o nulo si no se encuentra.
 * @property emisorNombre nombre del usuario o familiar que generó la alerta.
 */
data class AlertaRecibidaItem(
    val notificacion: NotificacionEntity,
    val alerta:       AlertaEntity?,
    val emisorNombre: String?           // ← nombre de quien disparó la alerta
)

/**
 * estado ui de la pantalla de alertas recibidas.
 *
 * @property cargando indica si los datos están siendo procesados o cargados desde room.
 * @property alertas lista de elementos [AlertaRecibidaItem] para ser renderizada en la interfaz.
 */
data class AlertasRecibidasState(
    val cargando: Boolean                  = true,
    val alertas:  List<AlertaRecibidaItem> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * viewmodel responsable de obtener y observar las alertas recibidas por el usuario autenticado.
 *
 * @param application instancia de la aplicación android.
 * @property notificacionDao dao para acceder a las notificaciones recibidas en la base de datos.
 * @property alertaDao dao para consultar la información específica de las alertas.
 * @property usuarioDao dao para recuperar la información del emisor de la alerta.
 * @property sessionManager gestor de sesión para obtener el identificador del usuario activo.
 */
class AlertasRecibidasViewModel(
    application:                 Application,
    private val notificacionDao: NotificacionDao,
    private val alertaDao:       AlertaDao,
    private val usuarioDao:      UsuarioDao,     // ← nuevo
    private val sessionManager:  SessionManager
) : AndroidViewModel(application) {

    /**
     * flujo interno mutable para gestionar el estado de las alertas recibidas.
     */
    private val _state = MutableStateFlow(AlertasRecibidasState())

    /**
     * flujo observable público para consumir el estado en la interfaz de usuario.
     */
    val state: StateFlow<AlertasRecibidasState> = _state.asStateFlow()

    init { observar() }

    /**
     * observa en tiempo real las notificaciones emitidas por el dao de room para el usuario actual,
     * relacionando cada notificación con su alerta y el nombre del usuario emisor.
     */
    private fun observar() {
        viewModelScope.launch {
            val userId = sessionManager.obtenerUsuarioId()
            Log.d(TAG, "═══ observar() iniciado ═══")
            Log.d(TAG, "userId de sesión: $userId")

            if (userId == null) {
                Log.e(TAG, "❌ userId es null — sin sesión activa")
                _state.value = AlertasRecibidasState(cargando = false)
                return@launch
            }

            Log.d(TAG, "✅ Suscribiendo Flow de Room para userId=$userId")

            notificacionDao.observarRecibidasPorUsuario(userId)
                .catch { e ->
                    Log.e(TAG, "❌ Error en el Flow de Room: ${e.message}")
                    _state.value = AlertasRecibidasState(cargando = false)
                }
                .collect { notificaciones ->
                    Log.d(TAG, "═══ Flow emitió ${notificaciones.size} notificacion(es) ═══")

                    val items = mutableListOf<AlertaRecibidaItem>()
                    for (notif in notificaciones) {
                        val alerta = alertaDao.obtenerPorId(notif.alertaId)

                        // Resolver nombre del emisor desde usuarioId de la alerta
                        val emisorNombre = alerta?.usuarioId?.let { uid ->
                            val u = usuarioDao.obtenerPorId(uid)
                            if (u != null) "${u.nombre} ${u.apellidoPaterno ?: ""}".trim()
                            else null
                        }

                        Log.d(TAG, "  alertaId=${notif.alertaId}" +
                                " emisor=$emisorNombre" +
                                " alerta=${if (alerta != null) "ok" else "null"}")

                        items.add(AlertaRecibidaItem(notif, alerta, emisorNombre))
                    }

                    Log.d(TAG, "Items finales: ${items.size}")
                    _state.value = AlertasRecibidasState(cargando = false, alertas = items)
                }
        }
    }

    /**
     * fábrica para crear instancias del viewmodel [AlertasRecibidasViewModel] con sus dependencias requeridas.
     *
     * @property app instancia de la aplicación android.
     * @property notificacionDao dao de notificaciones.
     * @property alertaDao dao de alertas.
     * @property usuarioDao dao de usuarios.
     * @property sessionManager gestor de sesión.
     */
    class Factory(
        private val app:             Application,
        private val notificacionDao: NotificacionDao,
        private val alertaDao:       AlertaDao,
        private val usuarioDao:      UsuarioDao,   // ← nuevo
        private val sessionManager:  SessionManager
    ) : ViewModelProvider.Factory {
        /**
         * crea una nueva instancia de [AlertasRecibidasViewModel].
         *
         * @param modelClass tipo de clase del viewmodel.
         * @return instancia de [AlertasRecibidasViewModel].
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AlertasRecibidasViewModel(
                app, notificacionDao, alertaDao, usuarioDao, sessionManager
            ) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * componente composable principal que muestra la lista de alertas enviadas por familiares,
 * permitiendo navegar al detalle de cada una.
 *
 * @param navController controlador de navegación.
 * @param notificacionDao dao para acceso a notificaciones.
 * @param alertaDao dao para acceso a alertas.
 * @param usuarioDao dao para acceso a la información de usuarios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertasRecibidasScreen(
    navController:   NavController,
    notificacionDao: NotificacionDao,
    alertaDao:       AlertaDao,
    usuarioDao:      UsuarioDao        // ← nuevo
) {
    val context = LocalContext.current
    val vm: AlertasRecibidasViewModel = viewModel(
        factory = AlertasRecibidasViewModel.Factory(
            app             = context.applicationContext as Application,
            notificacionDao = notificacionDao,
            alertaDao       = alertaDao,
            usuarioDao      = usuarioDao,   // ← nuevo
            sessionManager  = SessionManager(context)
        )
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        Log.d(TAG, "UI state → cargando=${state.cargando} alertas=${state.alertas.size}")
    }

    Scaffold(
        containerColor = CompaSOSColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Alertas de familiares",
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
                state.cargando -> {
                    CircularProgressIndicator(
                        color    = CompaSOSColors.AccentBlue,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.alertas.isEmpty() -> {
                    SinAlertasRecibidas(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.alertas,
                            key   = { it.notificacion.id }
                        ) { item ->
                            AlertaRecibidaCard(
                                item         = item,
                                onVerDetalle = {
                                    navController.navigate(
                                        Screen.AlertaDetalle.crearRuta(
                                            item.notificacion.alertaId
                                        )
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
 * componente composable privado que muestra un estado vacío informando que no existen alertas recibidas.
 *
 * @param modifier modificador de composición para ajustar la posición o espacio del componente.
 */
@Composable
private fun SinAlertasRecibidas(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(RojoAlerta.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint     = RojoAlerta,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text       = "Sin alertas recibidas",
            color      = CompaSOSColors.TextPrimary,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Text(
            text      = "Cuando un familiar active su alarma de emergencia, aparecerá aquí automáticamente.",
            color     = CompaSOSColors.TextSecondary,
            fontSize  = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * componente composable privado que renderiza la tarjeta individual para una alerta recibida.
 *
 * @param item contenedor con la información de la notificación, la alerta y el emisor.
 * @param onVerDetalle función invocada al solicitar la navegación al detalle de la alerta.
 */
@Composable
private fun AlertaRecibidaCard(
    item:         AlertaRecibidaItem,
    onVerDetalle: () -> Unit
) {
    val alerta = item.alerta

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onVerDetalle() },
        color = CompaSOSColors.FieldBackground
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(RojoAlerta.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint     = RojoAlerta,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text       = alerta?.tipoAlerta ?: "SOS",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RojoAlerta
                )

                // ── Emisor ────────────────────────────────────────────────────
                if (item.emisorNombre != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint     = RojoAlerta.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text       = item.emisorNombre,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = RojoAlerta.copy(alpha = 0.9f)
                        )
                    }
                }

                Text(
                    text     = alerta?.descripcion ?: "Alerta de emergencia",
                    fontSize = 12.sp,
                    color    = CompaSOSColors.TextSecondary
                )

                alerta?.dispositivoId?.let {
                    Text(
                        text     = "Desde: $it",
                        fontSize = 11.sp,
                        color    = CompaSOSColors.TextSecondary
                    )
                }

                Text(
                    text     = item.notificacion.fecha,
                    fontSize = 10.sp,
                    color    = CompaSOSColors.TextSecondary
                )

                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.clickable { onVerDetalle() }
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint     = CompaSOSColors.AccentBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text       = "Ver ubicación en mapa",
                        color      = CompaSOSColors.AccentBlue,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(RojoAlerta.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = alerta?.estado?.replaceFirstChar { it.uppercase() } ?: "Activa",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color      = RojoAlerta
                )
            }
        }
    }
}