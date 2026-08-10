package com.utng.compasos_movil.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
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
import com.utng.compasos_movil.data.entity.FamiliaEntity
import com.utng.compasos_movil.data.entity.FamiliaUsuarioEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.ui.theme.CompaSOSColors

@Composable
fun FamiliaScreen(navController: NavController? = null) {

    // ============================================================
    // DATOS ESTÁTICOS - INTEGRADOS
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

    // Familias
    val familiasEstáticas = listOf(
        FamiliaEntity(
            id = "familia_001",
            nombre = "Familia Pérez"
        ),
        FamiliaEntity(
            id = "familia_002",
            nombre = "Familia García"
        ),
        FamiliaEntity(
            id = "familia_003",
            nombre = "Familia Rodríguez"
        )
    )

    // Relaciones Usuario-Familia
    val familiaUsuariosEstáticos = listOf(
        // Familia Pérez
        FamiliaUsuarioEntity(
            familiaId = "familia_001",
            usuarioId = "user_001",
            rol = "Padre"
        ),
        FamiliaUsuarioEntity(
            familiaId = "familia_001",
            usuarioId = "user_002",
            rol = "Madre"
        ),
        FamiliaUsuarioEntity(
            familiaId = "familia_001",
            usuarioId = "user_003",
            rol = "Hijo"
        ),

        // Familia García
        FamiliaUsuarioEntity(
            familiaId = "familia_002",
            usuarioId = "user_001",
            rol = "Tío"
        ),
        FamiliaUsuarioEntity(
            familiaId = "familia_002",
            usuarioId = "user_004",
            rol = "Primo"
        ),

        // Familia Rodríguez
        FamiliaUsuarioEntity(
            familiaId = "familia_003",
            usuarioId = "user_001",
            rol = "Amigo"
        ),
        FamiliaUsuarioEntity(
            familiaId = "familia_003",
            usuarioId = "user_005",
            rol = "Colega"
        )
    )

    // Usuarios (para mostrar en las familias)
    val usuariosDisponibles = mapOf(
        "user_001" to "Juan Pérez",
        "user_002" to "María García",
        "user_003" to "Carlos Pérez",
        "user_004" to "Ana López",
        "user_005" to "Roberto Rodríguez"
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
        HeaderFamilia(usuarioEstático)

        // LISTA DE FAMILIAS
        if (familiasEstáticas.isEmpty()) {
            // Sin familias
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
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = CompaSOSColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Sin familias",
                        fontSize = 14.sp,
                        color = CompaSOSColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            // Con familias
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    count = familiasEstáticas.size,
                    key = { familiasEstáticas[it].id }
                ) { index ->
                    val familia = familiasEstáticas[index]

                    // Obtener miembros de esta familia
                    val miembros = familiaUsuariosEstáticos
                        .filter { it.familiaId == familia.id }
                        .map { relacion ->
                            Pair(
                                usuariosDisponibles[relacion.usuarioId] ?: "Desconocido",
                                relacion.rol ?: "Sin especificar"
                            )
                        }

                    FamiliaCard(familia = familia, miembros = miembros)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // FOOTER CON BOTONES
        FooterFamilia(navController = navController)
    }
}

@Composable
private fun HeaderFamilia(usuario: UsuarioEntity) {
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
                    text = "Mi Familia",
                    fontSize = 11.sp,
                    color = CompaSOSColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FamiliaCard(
    familia: FamiliaEntity,
    miembros: List<Pair<String, String>>
) {
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
            // NOMBRE FAMILIA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = CompaSOSColors.AccentBlue,
                    modifier = Modifier.size(20.dp)
                )

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

            // MIEMBROS DE LA FAMILIA
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                miembros.forEach { (nombre, rol) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CompaSOSColors.AccentBlue.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = nombre,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CompaSOSColors.TextPrimary
                            )
                            Text(
                                text = rol,
                                fontSize = 10.sp,
                                color = CompaSOSColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterFamilia(navController: NavController? = null) {
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
                    println("Agregar familia")
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