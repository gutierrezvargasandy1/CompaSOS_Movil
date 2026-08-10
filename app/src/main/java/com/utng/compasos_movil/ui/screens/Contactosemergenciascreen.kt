package com.utng.compasos_movil.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.utng.compasos_movil.data.entity.ContactoEmergenciaEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors

@Composable
fun ContactosEmergenciaScreen(navController: NavController? = null) {

    // ============================================================
    // DATOS ESTÁTICOS - INTEGRADOS EN EL COMPOSABLE
    // ============================================================

    val usuarioEstático = UsuarioEntity(
        id = "user_001",
        nombre = "Juan",
        apellidoPaterno = "Pérez",
        apellidoMaterno = "García",
        correo = "juan.perez@example.com",
        password = "hashedPassword",
        telefono = "+52 8123456789",
        foto = null,
        fechaNacimiento = "1995-05-15",
        sexo = "M",
        activo = true,
        fechaRegistro = "2024-01-15"
    )

    val contactosEstáticos = listOf(
        ContactoEmergenciaEntity(
            id = "contacto_001",
            usuarioId = "user_001",
            nombre = "María Pérez",
            telefono = "+52 8181234567",
            correo = "maria.perez@example.com",
            parentesco = "Madre",
            prioridad = 1
        ),
        ContactoEmergenciaEntity(
            id = "contacto_002",
            usuarioId = "user_001",
            nombre = "Carlos García",
            telefono = "+52 8187654321",
            correo = "carlos.garcia@example.com",
            parentesco = "Hermano",
            prioridad = 2
        ),
        ContactoEmergenciaEntity(
            id = "contacto_003",
            usuarioId = "user_001",
            nombre = "Dr. López",
            telefono = "+52 8191234567",
            correo = "dr.lopez@hospital.com",
            parentesco = "Médico de Confianza",
            prioridad = 3
        ),
        ContactoEmergenciaEntity(
            id = "contacto_004",
            usuarioId = "user_001",
            nombre = "Ana Rodríguez",
            telefono = "+52 8195678901",
            correo = null,
            parentesco = "Amiga",
            prioridad = 4
        ),
        ContactoEmergenciaEntity(
            id = "contacto_005",
            usuarioId = "user_001",
            nombre = "Policía Local",
            telefono = "911",
            correo = null,
            parentesco = "Emergencia",
            prioridad = 1
        )
    )

    // ============================================================
    // FIN DATOS ESTÁTICOS
    // ============================================================

    // ✅ MANEJAR BOTÓN FÍSICO DE ATRÁS
    if (navController != null) {
        BackHandler(enabled = true) {
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        // HEADER
        HeaderContactos(usuarioEstático)

        // LISTA DE CONTACTOS
        if (contactosEstáticos.isEmpty()) {
            // Sin contactos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
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
        } else {
            // Con contactos - ordenar por prioridad
            val contactosOrdenados = contactosEstáticos.sortedBy { it.prioridad ?: Int.MAX_VALUE }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = contactosOrdenados.size,
                    key = { contactosOrdenados[it].id }
                ) { index ->
                    val contacto = contactosOrdenados[index]
                    ContactoCard(contacto)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // FOOTER CON BOTONES
        FooterContactos(navController = navController)
    }
}

@Composable
private fun HeaderContactos(usuario: UsuarioEntity) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CompaSOSColors.AccentBlue,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${usuario.nombre} ${usuario.apellidoPaterno ?: ""}".trim(),
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        color = CompaSOSColors.FieldBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // NOMBRE Y PRIORIDAD
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

                // Badge de prioridad
                if (contacto.prioridad != null) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        color = when (contacto.prioridad) {
                            1 -> CompaSOSColors.AccentBlue.copy(alpha = 0.2f)
                            2 -> CompaSOSColors.AccentBlue.copy(alpha = 0.15f)
                            else -> CompaSOSColors.FieldBorder.copy(alpha = 0.3f)
                        }
                    ) {
                        Text(
                            text = "P${contacto.prioridad}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (contacto.prioridad) {
                                1 -> CompaSOSColors.AccentBlue
                                2 -> CompaSOSColors.AccentBlue
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

            Divider(
                color = CompaSOSColors.FieldBorder.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            // TELÉFONO
            if (!contacto.telefono.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = CompaSOSColors.AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = contacto.telefono,
                        fontSize = 12.sp,
                        color = CompaSOSColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // EMAIL
            if (!contacto.correo.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = CompaSOSColors.AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
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
private fun FooterContactos(navController: NavController? = null) {
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
                    println("Agregar contacto")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompaSOSColors.AccentBlue
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "+ Agregar",
                    color = CompaSOSColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}