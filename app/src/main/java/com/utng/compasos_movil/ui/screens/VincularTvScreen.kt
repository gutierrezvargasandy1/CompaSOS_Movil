package com.utng.compasos_movil.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.TvVinculacionModule.EstadoVinculacionTv
import com.utng.compasos_movil.TvVinculacionModule.TvVinculacionViewModel
import com.utng.compasos_movil.TvVinculacionModule.TvVinculacionViewModelFactory
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.HistorialUbicacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.ui.theme.CompaSOSButtonShapeRadius
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager

/**
 * pantalla para gestionar el proceso de vinculación de una pantalla inteligente compasos tv.
 * genera un código de vinculación único de 6 dígitos y monitorea los estados del proceso (generando, exitoso, error).
 *
 * @param navController controlador para gestionar la navegación entre pantallas.
 * @param usuarioDao acceso a datos de usuarios en la base de datos local room.
 * @param dispositivoDao acceso a datos de dispositivos en la base de datos local room.
 * @param familiaUsuarioDao acceso a datos de relaciones familiares en la base de datos local room.
 * @param historialUbicacionDao acceso a datos del historial de ubicaciones en la base de datos local room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VincularTvScreen(
    navController: NavController,
    usuarioDao: UsuarioDao,
    dispositivoDao: DispositivoDao,
    familiaUsuarioDao: FamiliaUsuarioDao,   // ← NUEVO
    historialUbicacionDao: HistorialUbicacionDao        // ← agregar


) {
    val context = LocalContext.current
    val viewModel: TvVinculacionViewModel = viewModel(
        factory = TvVinculacionViewModelFactory(
            usuarioDao     = usuarioDao,
            dispositivoDao = dispositivoDao,
            sessionManager = SessionManager(context),
            familiaUsuarioDao = familiaUsuarioDao,
            historialDao      = historialUbicacionDao,   // ← agregar


        )
    )
    val estado by viewModel.estado.collectAsState()

    BackHandler(enabled = true) { navController.popBackStack() }

    LaunchedEffect(Unit) {
        if (estado is EstadoVinculacionTv.Inactivo) viewModel.iniciarVinculacion()
    }

    Scaffold(
        containerColor = CompaSOSColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Vincular pantalla TV", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().background(CompaSOSColors.Background).padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(Icons.Filled.Tv, contentDescription = null, tint = CompaSOSColors.AccentBlue, modifier = Modifier.size(48.dp))

                when (val e = estado) {
                    is EstadoVinculacionTv.Generando -> {
                        Text(
                            "Escribe este código en tu pantalla CompaSOS TV",
                            color = CompaSOSColors.TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                            color = CompaSOSColors.FieldBackground
                        ) {
                            Text(
                                text = e.codigo.chunked(3).joinToString("  "),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = CompaSOSColors.AccentBlue,
                                textAlign = TextAlign.Center,
                                letterSpacing = 4.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CompaSOSColors.AccentBlue
                            )
                            Text("Esperando a la TV...", fontSize = 12.sp, color = CompaSOSColors.TextSecondary)
                        }
                    }

                    is EstadoVinculacionTv.Exitosa -> {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                        Text("¡TV vinculada correctamente!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CompaSOSColors.TextPrimary, textAlign = TextAlign.Center)
                        Text(e.modeloTv, fontSize = 14.sp, color = CompaSOSColors.AccentBlue)
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(CompaSOSButtonShapeRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.AccentBlue)
                        ) {
                            Text("Listo", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    is EstadoVinculacionTv.Error -> {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(56.dp))
                        Text(e.mensaje, fontSize = 13.sp, color = CompaSOSColors.TextSecondary, textAlign = TextAlign.Center)
                        Button(
                            onClick = { viewModel.iniciarVinculacion() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(CompaSOSButtonShapeRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.AccentBlue)
                        ) {
                            Text("Reintentar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> CircularProgressIndicator(color = CompaSOSColors.AccentBlue)
                }
            }
        }
    }
}