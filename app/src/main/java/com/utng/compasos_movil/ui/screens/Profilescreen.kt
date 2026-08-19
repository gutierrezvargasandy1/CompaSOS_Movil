package com.utng.compasos_movil.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.ProfileModule.PerfilMedicoRepository
import com.utng.compasos_movil.ProfileModule.ProfileState
import com.utng.compasos_movil.ProfileModule.ProfileViewModel
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.data.repository.UsuarioRepository
import com.utng.compasos_movil.navigation.Screen
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * fábrica para la creación de instancias de [ProfileViewModel] proporcionando sus dependencias requeridas.
 *
 * @property usuarioRepository repositorio para la gestión de datos de usuarios.
 * @property perfilMedicoRepository repositorio para la gestión de datos de perfiles médicos.
 * @property sessionManager gestor de la sesión local activa del usuario.
 */
class ProfileViewModelFactory(
    private val usuarioRepository: UsuarioRepository,
    private val perfilMedicoRepository: PerfilMedicoRepository,
    private val sessionManager: SessionManager
) : androidx.lifecycle.ViewModelProvider.Factory {

    /**
     * crea una nueva instancia del [ViewModel] solicitado si coincide con [ProfileViewModel].
     *
     * @param modelClass clase del [ViewModel] a instanciar.
     * @return una instancia de [T] configurada con los repositorios y gestor de sesión.
     * @throws IllegalArgumentException si la clase solicitada no es asignable a [ProfileViewModel].
     */
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                usuarioRepository,
                perfilMedicoRepository,
                sessionManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * pantalla de perfil de usuario que carga y despliega la información personal y médica registrada en la base de datos.
 *
 * @param navController controlador opcional para gestionar la navegación entre pantallas.
 * @param usuarioRepository repositorio opcional de acceso a datos de usuarios.
 * @param perfilMedicoRepository repositorio opcional de acceso a datos del perfil médico.
 * @param sessionManager gestor opcional de la sesión de usuario activa.
 */
@Composable
fun ProfileScreen(
    navController: NavController? = null,
    usuarioRepository: UsuarioRepository? = null,
    perfilMedicoRepository: PerfilMedicoRepository? = null,
    sessionManager: SessionManager? = null
) {
    // Si se pasan los parámetros, usar el ViewModel; si no, mostrar error
    if (usuarioRepository == null || perfilMedicoRepository == null || sessionManager == null) {
        // Fallback para compatibilidad (mostrar placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CompaSOSColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Error: Parámetros no disponibles",
                color = CompaSOSColors.TextSecondary
            )
        }
        return
    }

    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            usuarioRepository,
            perfilMedicoRepository,
            sessionManager
        )
    )

    val profileState by viewModel.profileState.collectAsState()

    // Manejo de botón atrás
    if (navController != null) {
        BackHandler(enabled = true) {
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        when (profileState) {
            is ProfileState.Loading -> {
                // Mostrar indicador de carga
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CompaSOSColors.AccentBlue
                )
            }

            is ProfileState.Success -> {
                val successState = profileState as ProfileState.Success
                val usuario = successState.usuario
                val perfil = successState.perfil

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CompaSOSColors.Background)
                ) {
                    // HEADER con avatar
                    HeaderPerfil(usuario)

                    // CONTENIDO scrolleable
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // INFORMACIÓN BÁSICA
                        item {
                            SectionBasicInfo(usuario)
                        }

                        // INFORMACIÓN MÉDICA (si existe)
                        if (perfil != null) {
                            item {
                                SectionMedicalInfo(perfil)
                            }
                        } else {
                            item {
                                Text(
                                    "Sin información médica registrada",
                                    color = CompaSOSColors.TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // BOTONES en footer
                    FooterButtons(navController = navController)
                }
            }

            is ProfileState.Error -> {
                val errorState = profileState as ProfileState.Error
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Error al cargar el perfil",
                            color = CompaSOSColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            errorState.mensaje,
                            color = CompaSOSColors.TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Button(
                            onClick = { navController?.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CompaSOSColors.AccentBlue
                            )
                        ) {
                            Text("Atrás")
                        }
                    }
                }
            }
        }
    }
}

/**
 * componente composable privado que renderiza el encabezado con avatar, nombre completo, correo y estado de la cuenta del usuario.
 *
 * @param usuario datos de la entidad [UsuarioEntity] a presentar.
 */
@Composable
private fun HeaderPerfil(usuario: UsuarioEntity) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(CompaSOSColors.FieldBackground),
        color = CompaSOSColors.FieldBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar grande
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CompaSOSColors.AccentBlue,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            // Nombre completo
            Text(
                text = "${usuario.nombre} ${usuario.apellidoPaterno ?: ""} ${usuario.apellidoMaterno ?: ""}".trim(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CompaSOSColors.TextPrimary
            )

            // Email
            Text(
                text = usuario.correo,
                fontSize = 12.sp,
                color = CompaSOSColors.TextSecondary
            )

            // Estado
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp)),
                color = if (usuario.activo) {
                    CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
                } else {
                    CompaSOSColors.TextSecondary.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = if (usuario.activo) "✓ Activo" else "✗ Inactivo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (usuario.activo) CompaSOSColors.AccentBlue else CompaSOSColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * componente composable privado que organiza y muestra la sección de información personal del usuario.
 *
 * @param usuario entidad [UsuarioEntity] que contiene los datos personales a desplegar.
 */
@Composable
private fun SectionBasicInfo(usuario: UsuarioEntity) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Información Personal",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CompaSOSColors.TextPrimary
        )

        InfoCard(label = "Nombre", value = usuario.nombre)
        InfoCard(label = "Apellido Paterno", value = usuario.apellidoPaterno ?: "No especificado")
        InfoCard(label = "Apellido Materno", value = usuario.apellidoMaterno ?: "No especificado")
        InfoCard(label = "Email", value = usuario.correo)

        if (!usuario.telefono.isNullOrEmpty()) {
            InfoCard(label = "Teléfono", value = usuario.telefono)
        }

        if (!usuario.fechaNacimiento.isNullOrEmpty()) {
            InfoCard(label = "Fecha de Nacimiento", value = usuario.fechaNacimiento)
        }

        if (!usuario.sexo.isNullOrEmpty()) {
            val sexoTexto = when (usuario.sexo) {
                "M" -> "Masculino"
                "F" -> "Femenino"
                "O" -> "Otro"
                else -> usuario.sexo
            }
            InfoCard(label = "Sexo", value = sexoTexto)
        }

        InfoCard(label = "Registrado", value = formatDate(usuario.fechaRegistro))
    }
}

/**
 * componente composable privado que organiza y muestra la sección con los datos médicos del usuario.
 *
 * @param perfil entidad [PerfilMedicoEntity] con la información médica asociada al usuario.
 */
@Composable
private fun SectionMedicalInfo(perfil: PerfilMedicoEntity) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(
            color = CompaSOSColors.FieldBorder.copy(alpha = 0.5f),
            thickness = 1.dp
        )

        Text(
            text = "Información Médica",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CompaSOSColors.TextPrimary
        )

        if (!perfil.tipoSangre.isNullOrEmpty()) {
            InfoCard(label = "Tipo de Sangre", value = perfil.tipoSangre, highlight = true)
        }

        if (perfil.peso != null) {
            InfoCard(label = "Peso", value = "${perfil.peso} kg")
        }

        if (perfil.altura != null) {
            InfoCard(label = "Altura", value = "${perfil.altura} cm")
        }

        if (!perfil.alergias.isNullOrEmpty()) {
            InfoCard(label = "Alergias", value = perfil.alergias)
        }

        if (!perfil.padecimientos.isNullOrEmpty()) {
            InfoCard(label = "Padecimientos", value = perfil.padecimientos)
        }

        if (!perfil.medicamentos.isNullOrEmpty()) {
            InfoCard(label = "Medicamentos", value = perfil.medicamentos)
        }

        if (!perfil.observaciones.isNullOrEmpty()) {
            InfoCard(label = "Observaciones", value = perfil.observaciones)
        }
    }
}

/**
 * componente composable privado que muestra una tarjeta informativa individual con título y valor.
 *
 * @param label etiqueta descriptiva del campo de información.
 * @param value valor o contenido del campo.
 * @param highlight determina si la tarjeta utiliza un tono acentuado para resaltar.
 */
@Composable
private fun InfoCard(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = if (highlight) {
            CompaSOSColors.AccentBlue.copy(alpha = 0.1f)
        } else {
            CompaSOSColors.FieldBackground
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) CompaSOSColors.AccentBlue else CompaSOSColors.TextSecondary
            )

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CompaSOSColors.TextPrimary
            )
        }
    }
}

/**
 * componente composable privado que muestra la barra inferior con botones para regresar o editar el perfil.
 *
 * @param navController controlador opcional para realizar acciones de navegación.
 */
@Composable
private fun FooterButtons(navController: NavController? = null) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(CompaSOSColors.FieldBackground),
        color = CompaSOSColors.FieldBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    navController?.popBackStack()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompaSOSColors.FieldBorder
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Atrás",
                    color = CompaSOSColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    navController?.navigate(Screen.EditarPerfil.route)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompaSOSColors.AccentBlue
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 6.dp),
                    tint = CompaSOSColors.TextPrimary
                )
                Text(
                    text = "Editar",
                    color = CompaSOSColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * convierte una cadena de fecha dada en formato 'yyyy-MM-dd HH:mm:ss' al formato dd/MM/yyyy.
 *
 * @param dateString texto original de la fecha.
 * @return cadena con el formato de fecha simplificado dd/MM/yyyy.
 */
private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}