package com.utng.compasos_movil.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.ContactosModule.ContactosViewModel
import com.utng.compasos_movil.ContactosModule.ContactosViewModelFactory
import com.utng.compasos_movil.data.dao.ContactoEmergenciaDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.ContactoEmergenciaEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.utils.SessionManager

@Composable
fun ContactosEmergenciaScreen(
    navController: NavController? = null,
    usuarioDao: UsuarioDao,
    contactoEmergenciaDao: ContactoEmergenciaDao
) {
    val context = LocalContext.current
    val viewModel: ContactosViewModel = viewModel(
        factory = ContactosViewModelFactory(
            usuarioDao            = usuarioDao,
            contactoEmergenciaDao = contactoEmergenciaDao,
            sessionManager        = SessionManager(context)
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    var mostrarDialogoAgregar by remember { mutableStateOf(false) }

    if (navController != null) {
        BackHandler(enabled = true) { navController.popBackStack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        HeaderContactos(uiState.usuarioActual)

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.cargando -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CompaSOSColors.AccentBlue)
                    }
                }

                uiState.contactos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = CompaSOSColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Sin contactos de emergencia",
                                fontSize = 14.sp,
                                color = CompaSOSColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = uiState.contactos, key = { it.id }) { contacto ->
                            ContactoCard(contacto)
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

        FooterContactos(
            navController = navController,
            onAgregar     = { mostrarDialogoAgregar = true }
        )
    }

    if (mostrarDialogoAgregar) {
        AgregarContactoDialog(
            onConfirmar = { nombre, telefono, correo, parentesco, prioridad ->
                viewModel.agregarContacto(nombre, telefono, correo, parentesco, prioridad)
                mostrarDialogoAgregar = false
            },
            onCancelar = { mostrarDialogoAgregar = false }
        )
    }
}

// ============================================================
// COMPOSABLES PRIVADOS
// ============================================================

@Composable
private fun HeaderContactos(usuario: UsuarioEntity?) {
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
                    else "Mis Contactos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.TextPrimary
                )
                Text(
                    text = "Contactos de Emergencia",
                    fontSize = 11.sp,
                    color = CompaSOSColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ContactoCard(contacto: ContactoEmergenciaEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        color = CompaSOSColors.FieldBackground
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // NOMBRE + BADGE PRIORIDAD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contacto.nombre ?: "Sin nombre",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (contacto.prioridad != null) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        color = when (contacto.prioridad) {
                            1    -> CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
                            2    -> CompaSOSColors.AccentBlue.copy(alpha = 0.15f)
                            else -> CompaSOSColors.FieldBorder.copy(alpha = 0.3f)
                        }
                    ) {
                        Text(
                            text = "P${contacto.prioridad}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (contacto.prioridad) {
                                1, 2 -> CompaSOSColors.AccentBlue
                                else -> CompaSOSColors.TextSecondary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // PARENTESCO
            if (!contacto.parentesco.isNullOrEmpty()) {
                Text(
                    text = contacto.parentesco,
                    fontSize = 11.sp,
                    color = CompaSOSColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Divider(color = CompaSOSColors.FieldBorder.copy(alpha = 0.2f), thickness = 0.5.dp)

            // TELÉFONO
            if (!contacto.telefono.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Call, null, tint = CompaSOSColors.AccentBlue, modifier = Modifier.size(16.dp))
                    Text(
                        text = contacto.telefono,
                        fontSize = 12.sp,
                        color = CompaSOSColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // CORREO
            if (!contacto.correo.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, null, tint = CompaSOSColors.AccentBlue, modifier = Modifier.size(16.dp))
                    Text(
                        text = contacto.correo,
                        fontSize = 12.sp,
                        color = CompaSOSColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AgregarContactoDialog(
    onConfirmar: (String, String, String?, String?, Int?) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre     by remember { mutableStateOf("") }
    var telefono   by remember { mutableStateOf("") }
    var correo     by remember { mutableStateOf("") }
    var parentesco by remember { mutableStateOf("") }
    var prioridad  by remember { mutableStateOf("") }

    val puedeGuardar = nombre.isNotBlank() && telefono.isNotBlank()

    Dialog(onDismissRequest = onCancelar) {
        Surface(shape = RoundedCornerShape(14.dp), color = CompaSOSColors.FieldBackground) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nuevo contacto",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CompaSOSColors.TextPrimary
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *", color = CompaSOSColors.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = campoColores()
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono *", color = CompaSOSColors.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = campoColores(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo", color = CompaSOSColors.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = campoColores(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = parentesco,
                    onValueChange = { parentesco = it },
                    label = { Text("Parentesco", color = CompaSOSColors.TextSecondary) },
                    placeholder = {
                        Text(
                            "Ej. Madre, Hermano",
                            color = CompaSOSColors.TextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = campoColores()
                )

                OutlinedTextField(
                    value = prioridad,
                    onValueChange = { nuevo ->
                        // Solo dígitos, máximo 2 caracteres
                        if (nuevo.length <= 2 && nuevo.all { it.isDigit() }) prioridad = nuevo
                    },
                    label = { Text("Prioridad", color = CompaSOSColors.TextSecondary) },
                    placeholder = {
                        Text(
                            "1 = mayor prioridad",
                            color = CompaSOSColors.TextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = campoColores(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                    ) {
                        Text("Cancelar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (puedeGuardar) {
                                onConfirmar(
                                    nombre,
                                    telefono,
                                    correo.ifBlank { null },
                                    parentesco.ifBlank { null },
                                    prioridad.toIntOrNull()
                                )
                            }
                        },
                        enabled = puedeGuardar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor        = CompaSOSColors.AccentBlue,
                            disabledContainerColor = CompaSOSColors.AccentBlue.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Agregar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterContactos(
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
            ) { Text("+ Agregar", color = CompaSOSColors.TextPrimary, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun campoColores() = OutlinedTextFieldDefaults.colors(
    focusedTextColor        = CompaSOSColors.TextPrimary,
    unfocusedTextColor      = CompaSOSColors.TextPrimary,
    focusedContainerColor   = CompaSOSColors.Background,
    unfocusedContainerColor = CompaSOSColors.Background,
    focusedBorderColor      = CompaSOSColors.AccentBlue,
    unfocusedBorderColor    = CompaSOSColors.FieldBorder,
    cursorColor             = CompaSOSColors.AccentBlue
)