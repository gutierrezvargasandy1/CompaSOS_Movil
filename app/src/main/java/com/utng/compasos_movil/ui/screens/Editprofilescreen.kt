package com.utng.compasos_movil.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.ProfileModule.EditProfileViewModel
import com.utng.compasos_movil.ProfileModule.PerfilMedicoRepository
import com.utng.compasos_movil.ProfileModule.ProfileLoadState
import com.utng.compasos_movil.ProfileModule.ProfileUpdateState
import com.utng.compasos_movil.data.repository.UsuarioRepository
import com.utng.compasos_movil.ui.screens.molals.CompaSOSAlertToast
import com.utng.compasos_movil.ui.screens.molals.CompaSOSAlertType
import com.utng.compasos_movil.ui.screens.molals.rememberCompaSOSAlertState
import com.utng.compasos_movil.ui.theme.CompaSOSButtonShapeRadius
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.ui.theme.CompaSOSFieldShapeRadius
import com.utng.compasos_movil.ui.theme.compaSOSTextFieldColors
import com.utng.compasos_movil.utils.SessionManager

/**
 * Factory para EditProfileViewModel
 */
class EditProfileViewModelFactory(
    private val usuarioRepository: UsuarioRepository,
    private val perfilMedicoRepository: PerfilMedicoRepository,
    private val sessionManager: SessionManager
) : androidx.lifecycle.ViewModelProvider.Factory {

    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditProfileViewModel(
                usuarioRepository,
                perfilMedicoRepository,
                sessionManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Pantalla para editar el perfil del usuario
 */
@Composable
fun EditProfileScreen(
    navController: NavController,
    usuarioRepository: UsuarioRepository,
    perfilMedicoRepository: PerfilMedicoRepository,
    sessionManager: SessionManager
) {
    val viewModel: EditProfileViewModel = viewModel(
        factory = EditProfileViewModelFactory(
            usuarioRepository,
            perfilMedicoRepository,
            sessionManager
        )
    )

    // Estados
    val profileLoadState by viewModel.profileLoadState.collectAsState()
    val profileUpdateState by viewModel.profileUpdateState.collectAsState()
    val usuarioEnEdicion by viewModel.usuarioEnEdicion.collectAsState()
    val perfilEnEdicion by viewModel.perfilEnEdicion.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Estados de edición local
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    var tipoSangre by remember { mutableStateOf("") }
    var alergias by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }

    val aviso = rememberCompaSOSAlertState()

    // Efecto: Cargar datos al abrir pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
    }

    // Efecto: Rellenar campos cuando se carguen los datos
    LaunchedEffect(usuarioEnEdicion) {
        usuarioEnEdicion?.let { usuario ->
            nombre = usuario.nombre
            apellidoPaterno = usuario.apellidoPaterno ?: ""
            apellidoMaterno = usuario.apellidoMaterno ?: ""
            telefono = usuario.telefono ?: ""
        }
    }

    LaunchedEffect(perfilEnEdicion) {
        perfilEnEdicion?.let { perfil ->
            tipoSangre = perfil.tipoSangre ?: ""
            alergias = perfil.alergias ?: ""
            peso = perfil.peso?.toString() ?: ""
            altura = perfil.altura?.toString() ?: ""
        }
    }

    // Manejo de retroceso
    BackHandler(enabled = true) {
        navController.popBackStack()
    }

    // Manejo de actualización exitosa
    LaunchedEffect(profileUpdateState) {
        when (profileUpdateState) {
            is ProfileUpdateState.Success -> {
                aviso.mostrar("✓ Perfil actualizado correctamente", CompaSOSAlertType.Exito)
                viewModel.resetUpdateState()
                // Navegar de vuelta después de 1.5 segundos
                kotlinx.coroutines.delay(1500)
                navController.popBackStack()
            }

            is ProfileUpdateState.Error -> {
                val mensaje = (profileUpdateState as ProfileUpdateState.Error).mensaje
                aviso.mostrar("✗ Error: $mensaje", CompaSOSAlertType.Error)
            }

            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = CompaSOSColors.AccentBlue
                        )
                    }

                    Text(
                        "Editar Perfil",
                        color = CompaSOSColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(modifier = Modifier.size(48.dp))
                }
            }

            // Sección: Datos Personales
            item {
                Text(
                    "Datos Personales",
                    color = CompaSOSColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                EditProfileTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Nombre",
                    icon = Icons.Filled.Person
                )
            }

            item {
                EditProfileTextField(
                    value = apellidoPaterno,
                    onValueChange = { apellidoPaterno = it },
                    placeholder = "Apellido Paterno"
                )
            }

            item {
                EditProfileTextField(
                    value = apellidoMaterno,
                    onValueChange = { apellidoMaterno = it },
                    placeholder = "Apellido Materno (opcional)"
                )
            }

            item {
                EditProfileTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    placeholder = "Teléfono",
                    icon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone
                )
            }

            // Sección: Datos Médicos
            item {
                Text(
                    "Información Médica",
                    color = CompaSOSColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                EditProfileTextField(
                    value = tipoSangre,
                    onValueChange = { tipoSangre = it },
                    placeholder = "Tipo de Sangre (ej: O+)"
                )
            }

            item {
                EditProfileTextField(
                    value = alergias,
                    onValueChange = { alergias = it },
                    placeholder = "Alergias (opcional)"
                )
            }

            item {
                EditProfileTextField(
                    value = peso,
                    onValueChange = { peso = it },
                    placeholder = "Peso (kg)",
                    keyboardType = KeyboardType.Number
                )
            }

            item {
                EditProfileTextField(
                    value = altura,
                    onValueChange = { altura = it },
                    placeholder = "Altura (cm)",
                    keyboardType = KeyboardType.Number
                )
            }

            // Botones de acción
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.descartarCambios() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(CompaSOSButtonShapeRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CompaSOSColors.FieldBackground
                        )
                    ) {
                        Text(
                            "Descartar",
                            color = CompaSOSColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.actualizarDatosPersonales(
                                nombre = nombre,
                                apellidoPaterno = apellidoPaterno.takeIf { it.isNotEmpty() },
                                apellidoMaterno = apellidoMaterno.takeIf { it.isNotEmpty() },
                                telefono = telefono.takeIf { it.isNotEmpty() },
                                fechaNacimiento = null,
                                sexo = null
                            )

                            viewModel.actualizarDatosMedicos(
                                tipoSangre = tipoSangre.takeIf { it.isNotEmpty() },
                                alergias = alergias.takeIf { it.isNotEmpty() },
                                padecimientos = null,
                                medicamentos = null,
                                peso = peso.toDoubleOrNull(),
                                altura = altura.toDoubleOrNull(),
                                observaciones = null
                            )

                            viewModel.guardarCambios()
                        },
                        enabled = profileUpdateState != ProfileUpdateState.Saving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(CompaSOSButtonShapeRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CompaSOSColors.AccentBlue
                        )
                    ) {
                        if (profileUpdateState == ProfileUpdateState.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CompaSOSColors.TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Guardar",
                                color = CompaSOSColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }

        // Toast de notificaciones
        CompaSOSAlertToast(
            mensaje = aviso.mensaje,
            tipo = aviso.tipo,
            visible = aviso.visible,
            onFinalizar = aviso::ocultar,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Loading indicator
        if (profileLoadState is ProfileLoadState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = CompaSOSColors.AccentBlue
            )
        }
    }
}

/**
 * Campo de texto reutilizable para edición de perfil
 * Versión simplificada (sin password, sin custom validaciones)
 */
@Composable
private fun EditProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = CompaSOSColors.TextSecondary) },
        leadingIcon = {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = CompaSOSColors.IconTint)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        enabled = enabled,
        colors = compaSOSTextFieldColors(),
        shape = RoundedCornerShape(CompaSOSFieldShapeRadius),
        modifier = Modifier.fillMaxWidth()
    )
}