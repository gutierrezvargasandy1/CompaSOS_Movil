package com.utng.compasos_movil.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.FamiliaModule.FamiliaViewModel
import com.utng.compasos_movil.FamiliaModule.FamiliaViewModelFactory
import com.utng.compasos_movil.dao.MiembroConDatos
import com.utng.compasos_movil.data.dao.FamiliaDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.FamiliaEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager

/**
 * pantalla principal del módulo de familia que muestra los grupos familiares vinculados, sus miembros y opciones para crear e invitar usuarios.
 *
 * @param navController controlador opcional para gestionar la navegación entre pantallas.
 * @param usuarioDao acceso a datos de los usuarios en la base de datos local room.
 * @param familiaDao acceso a datos de los grupos familiares en la base de datos local room.
 * @param familiaUsuarioDao acceso a la relación entre familias y usuarios en la base de datos local room.
 */
@Composable
fun FamiliaScreen(
    navController: NavController? = null,
    usuarioDao: UsuarioDao,
    familiaDao: FamiliaDao,
    familiaUsuarioDao: FamiliaUsuarioDao
) {
    val context = LocalContext.current
    val viewModel: FamiliaViewModel = viewModel(
        factory = FamiliaViewModelFactory(
            usuarioDao        = usuarioDao,
            familiaDao        = familiaDao,
            familiaUsuarioDao = familiaUsuarioDao,
            sessionManager    = SessionManager(context)
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    var mostrarDialogoCrear  by remember { mutableStateOf(false) }
    var familiaParaInvitar   by remember { mutableStateOf<FamiliaEntity?>(null) }

    if (navController != null) {
        BackHandler(enabled = true) { navController.popBackStack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        HeaderFamilia(uiState.usuarioActual)

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.cargando -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CompaSOSColors.AccentBlue)
                    }
                }

                uiState.familias.isEmpty() -> {
                    EmptyFamilia(onCrear = { mostrarDialogoCrear = true })
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(items = uiState.familias, key = { it.familia.id }) { item ->
                            FamiliaCard(
                                familia  = item.familia,
                                miembros = item.miembros,
                                onInvitar = { familiaParaInvitar = item.familia }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

        FooterFamilia(
            navController = navController,
            onAgregar     = { mostrarDialogoCrear = true }
        )
    }

    if (mostrarDialogoCrear) {
        CrearFamiliaDialog(
            onConfirmar = { nombre ->
                viewModel.crearFamilia(nombre)
                mostrarDialogoCrear = false
            },
            onCancelar = { mostrarDialogoCrear = false }
        )
    }

    familiaParaInvitar?.let { familia ->
        InvitarMiembroDialog(
            familia   = familia,
            resultados = uiState.resultadosBusqueda,
            buscando  = uiState.buscando,
            onBuscar  = { texto -> viewModel.buscarUsuarios(texto, familia.id) },
            onAgregar = { usuario -> viewModel.agregarMiembro(familia.id, usuario) },
            onCerrar  = {
                viewModel.limpiarBusqueda()
                familiaParaInvitar = null
            }
        )
    }
}

// ============================================================
// COMPOSABLES PRIVADOS — sin cambios respecto a la versión anterior
// ============================================================

/**
 * componente composable privado que muestra el encabezado con la información e icono del usuario actual.
 *
 * @param usuario información de la entidad [UsuarioEntity] del usuario autenticado.
 */
@Composable
private fun HeaderFamilia(usuario: UsuarioEntity?) {
    Surface(modifier = Modifier.fillMaxWidth(), color = CompaSOSColors.FieldBackground) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)),
                color = CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CompaSOSColors.AccentBlue,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (usuario != null)
                        "${usuario.nombre} ${usuario.apellidoPaterno ?: ""}".trim()
                    else "Mi Familia",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.TextPrimary
                )
                Text(text = "Mi Familia", fontSize = 11.sp, color = CompaSOSColors.TextSecondary)
            }
        }
    }
}

/**
 * componente composable privado que muestra una vista informativa cuando el usuario no pertenece a ninguna familia.
 *
 * @param onCrear callback invocado al presionar el botón para crear una nueva familia.
 */
@Composable
private fun EmptyFamilia(onCrear: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = CompaSOSColors.TextSecondary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Aún no tienes una familia",
                fontSize = 15.sp,
                color = CompaSOSColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Crea una familia para invitar a tus seres queridos y compartir tu ubicación en caso de emergencia.",
                fontSize = 12.sp,
                color = CompaSOSColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onCrear,
                colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.AccentBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = CompaSOSColors.TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Crear familia", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * componente composable privado que renderiza una tarjeta informativa con los datos de un grupo familiar y sus miembros.
 *
 * @param familia entidad [FamiliaEntity] que contiene los datos del grupo familiar.
 * @param miembros lista de objetos [MiembroConDatos] asociados a la familia.
 * @param onInvitar callback invocado para abrir el diálogo de invitación de un nuevo miembro.
 */
@Composable
private fun FamiliaCard(
    familia: FamiliaEntity,
    miembros: List<MiembroConDatos>,
    onInvitar: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        color = CompaSOSColors.FieldBackground
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Group, null,
                    tint = CompaSOSColors.AccentBlue,
                    modifier = Modifier.size(20.dp))
                Text(
                    text = familia.nombre ?: "Sin nombre",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                    color = CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${miembros.size} miembros",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CompaSOSColors.AccentBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Divider(
                color = CompaSOSColors.FieldBorder.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            if (miembros.isEmpty()) {
                Text(
                    text = "Sin miembros todavía",
                    fontSize = 11.sp,
                    color = CompaSOSColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    miembros.forEach { miembro ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person, null,
                                tint = CompaSOSColors.AccentBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                // ✅ los campos viven en miembro.usuario.*
                                Text(
                                    text = "${miembro.usuario.nombre} " +
                                            "${miembro.usuario.apellidoPaterno ?: ""}".trim(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CompaSOSColors.TextPrimary
                                )
                                Text(
                                    text = miembro.rol ?: "Sin especificar",
                                    fontSize = 10.sp,
                                    color = CompaSOSColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Divider(
                color = CompaSOSColors.FieldBorder.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            TextButton(onClick = onInvitar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PersonAdd, null,
                    tint = CompaSOSColors.AccentBlue,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Invitar miembro",
                    color = CompaSOSColors.AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * componente composable privado que presenta un diálogo modal para ingresar el nombre y registrar un nuevo grupo familiar.
 *
 * @param onConfirmar callback que recibe el nombre ingresado y procesa la creación de la familia.
 * @param onCancelar callback invocado para descartar o cerrar el diálogo modal.
 */
@Composable
private fun CrearFamiliaDialog(
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancelar) {
        Surface(shape = RoundedCornerShape(14.dp), color = CompaSOSColors.FieldBackground) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Nueva familia", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CompaSOSColors.TextPrimary)

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = { Text("Nombre de la familia", color = CompaSOSColors.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = campoColores()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCancelar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.FieldBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Cancelar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { if (nombre.isNotBlank()) onConfirmar(nombre) },
                        enabled = nombre.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CompaSOSColors.AccentBlue,
                            disabledContainerColor = CompaSOSColors.AccentBlue.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Crear", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/**
 * componente composable privado que presenta un diálogo modal de búsqueda para encontrar e invitar usuarios a una familia.
 *
 * @param familia entidad [FamiliaEntity] a la cual se invitará al nuevo integrante.
 * @param resultados lista de entidades [UsuarioEntity] coincidentes con la búsqueda.
 * @param buscando indicador booleano de estado de carga en la búsqueda.
 * @param onBuscar callback invocado con el texto de consulta para iniciar la búsqueda de usuarios.
 * @param onAgregar callback invocado al seleccionar un usuario para añadirlo a la familia.
 * @param onCerrar callback invocado para cerrar y limpiar el diálogo modal.
 */
@Composable
private fun InvitarMiembroDialog(
    familia: FamiliaEntity,
    resultados: List<UsuarioEntity>,
    buscando: Boolean,
    onBuscar: (String) -> Unit,
    onAgregar: (UsuarioEntity) -> Unit,
    onCerrar: () -> Unit
) {
    var texto by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCerrar) {
        Surface(shape = RoundedCornerShape(14.dp), color = CompaSOSColors.FieldBackground) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Invitar a ${familia.nombre ?: "la familia"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.TextPrimary
                )

                OutlinedTextField(
                    value = texto,
                    onValueChange = {
                        texto = it
                        onBuscar(it)
                    },
                    placeholder = { Text("Buscar por nombre o correo", color = CompaSOSColors.TextSecondary) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = CompaSOSColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = campoColores()
                )

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 320.dp)) {
                    when {
                        buscando -> {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = CompaSOSColors.AccentBlue,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        texto.isBlank() -> {
                            Text(
                                text = "Escribe para buscar usuarios",
                                fontSize = 12.sp,
                                color = CompaSOSColors.TextSecondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        resultados.isEmpty() -> {
                            Text(
                                text = "No se encontraron usuarios",
                                fontSize = 12.sp,
                                color = CompaSOSColors.TextSecondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        else -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items = resultados, key = { it.id }) { usuario ->
                                    ResultadoUsuario(usuario = usuario, onAgregar = { onAgregar(usuario) })
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onCerrar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.FieldBorder),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Cerrar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/**
 * componente composable privado que muestra la información de un usuario hallado en la búsqueda y un botón para agregarlo.
 *
 * @param usuario datos de la entidad [UsuarioEntity] correspondiente al resultado.
 * @param onAgregar callback invocado para agregar al usuario a la familia seleccionada.
 */
@Composable
private fun ResultadoUsuario(usuario: UsuarioEntity, onAgregar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        color = CompaSOSColors.Background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, null, tint = CompaSOSColors.AccentBlue.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "${usuario.nombre} ${usuario.apellidoPaterno ?: ""}".trim(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CompaSOSColors.TextPrimary
                )
                Text(text = usuario.correo, fontSize = 10.sp, color = CompaSOSColors.TextSecondary)
            }
            IconButton(onClick = onAgregar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Agregar", tint = CompaSOSColors.AccentBlue)
            }
        }
    }
}

/**
 * componente composable privado que muestra la barra inferior con botones de navegación y creación de un nuevo grupo.
 *
 * @param navController controlador opcional para retroceder en la pila de navegación.
 * @param onAgregar callback invocado al presionar el botón para crear un nuevo grupo familiar.
 */
@Composable
private fun FooterFamilia(
    navController: NavController? = null,
    onAgregar: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = CompaSOSColors.FieldBackground) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController?.popBackStack() },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.FieldBorder),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Atrás", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold) }

            Button(
                onClick = onAgregar,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.AccentBlue),
                shape = RoundedCornerShape(8.dp)
            ) { Text("+ Nueva familia", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * función helper composable privada que define la paleta de colores personalizada para los campos de texto [OutlinedTextField].
 *
 * @return un objeto [TextFieldColors] con el esquema de colores configurado.
 */
@Composable
private fun campoColores() = OutlinedTextFieldDefaults.colors(
    focusedTextColor       = CompaSOSColors.TextPrimary,
    unfocusedTextColor     = CompaSOSColors.TextPrimary,
    focusedContainerColor  = CompaSOSColors.Background,
    unfocusedContainerColor = CompaSOSColors.Background,
    focusedBorderColor     = CompaSOSColors.AccentBlue,
    unfocusedBorderColor   = CompaSOSColors.FieldBorder,
    cursorColor            = CompaSOSColors.AccentBlue
)