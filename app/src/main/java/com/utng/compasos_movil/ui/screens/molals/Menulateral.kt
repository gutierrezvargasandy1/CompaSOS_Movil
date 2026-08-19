package com.utng.compasos_movil.ui.screens.molals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.ui.theme.CompaSOSFieldShapeRadius

/**
 * representa una opción del menú lateral de navegación.
 *
 * @property titulo texto descriptivo o nombre de la opción.
 * @property icono vector de imagen representativo de la opción.
 * @property badge indicador numérico opcional para contadores o notificaciones.
 */
data class MenuOpcion(
    val titulo: String,
    val icono: ImageVector,
    val badge: Int? = null
)

/**
 * representa la información del usuario desplegada en la cabecera del menú lateral.
 *
 * @property nombreCompleto nombre y apellidos del usuario.
 * @property correo correo electrónico de la cuenta activa.
 * @property fotoUri uri de la imagen de perfil del usuario (opcional).
 */
data class MenuUsuario(
    val nombreCompleto: String,
    val correo: String,
    val fotoUri: String? = null
)

/**
 * lista de opciones principales que componen el círculo familiar y las secciones de actividad.
 */
private val opcionesPrincipales = listOf(
    MenuOpcion("Contactos de emergencia", Icons.Filled.ContactEmergency),
    MenuOpcion("Familia", Icons.Filled.FamilyRestroom),
    MenuOpcion("Dispositivos", Icons.Filled.PhoneAndroid),
    MenuOpcion("Historial de ubicaciones", Icons.Filled.History),
    MenuOpcion("Notificaciones", Icons.Filled.Notifications),
    MenuOpcion("Alertas de familiares",   Icons.Filled.NotificationImportant)
)

/**
 * lista de opciones secundarias destinadas a la configuración general.
 */
private val opcionesSecundarias = listOf(
    MenuOpcion("Configuración", Icons.Filled.Settings)
)

/**
 * componente composable que renderiza la estructura completa y las secciones del menú lateral de navegación.
 *
 * @param usuario datos del usuario a mostrar en la cabecera.
 * @param opcionSeleccionada título de la opción actualmente seleccionada en la aplicación.
 * @param onOpcionSeleccionada evento emitido al presionar una opción del menú.
 * @param onCerrarSesion evento emitido al pulsar la opción de cierre de sesión.
 * @param onEditarPerfil evento emitido al hacer clic en la cabecera del usuario para editar el perfil.
 */
@Composable
fun MenuLateralContent(
    usuario: MenuUsuario,
    opcionSeleccionada: String? = null,
    onOpcionSeleccionada: (String) -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    onEditarPerfil: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(CompaSOSColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp)
    ) {
        MenuCabeceraUsuario(
            usuario = usuario,
            onClick = onEditarPerfil,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(24.dp))
        MenuDivider()
        Spacer(Modifier.height(12.dp))

        MenuSeccionTitulo("Tu círculo")
        opcionesPrincipales.take(2).forEach { opcion ->
            MenuOpcionItem(
                opcion = opcion,
                seleccionada = opcion.titulo == opcionSeleccionada,
                onClick = { onOpcionSeleccionada(opcion.titulo) }
            )
        }

        Spacer(Modifier.height(16.dp))
        MenuSeccionTitulo("Actividad")
        opcionesPrincipales.drop(2).forEach { opcion ->
            MenuOpcionItem(
                opcion = opcion,
                seleccionada = opcion.titulo == opcionSeleccionada,
                onClick = { onOpcionSeleccionada(opcion.titulo) }
            )
        }

        Spacer(Modifier.height(16.dp))
        MenuDivider()
        Spacer(Modifier.height(12.dp))

        opcionesSecundarias.forEach { opcion ->
            MenuOpcionItem(
                opcion = opcion,
                seleccionada = opcion.titulo == opcionSeleccionada,
                onClick = { onOpcionSeleccionada(opcion.titulo) }
            )
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        MenuDivider()

        MenuOpcionItem(
            opcion = MenuOpcion("Cerrar sesión", Icons.AutoMirrored.Filled.Logout),
            seleccionada = false,
            colorTexto = Color(0xFFEF5350),
            colorIcono = Color(0xFFEF5350),
            onClick = onCerrarSesion
        )
    }
}

/**
 * componente composable interno que renderiza la información del perfil del usuario en la parte superior del menú.
 *
 * @param usuario información del usuario a presentar.
 * @param onClick acción a realizar cuando se presiona la cabecera.
 * @param modifier modificador para personalizar la maquetación.
 */
@Composable
private fun MenuCabeceraUsuario(
    usuario: MenuUsuario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CompaSOSColors.FieldBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Foto de perfil",
                tint = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = usuario.nombreCompleto,
                color = CompaSOSColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1
            )
            Text(
                text = usuario.correo,
                color = CompaSOSColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Editar perfil",
            tint = CompaSOSColors.IconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * componente composable interno que dibuja el título o encabezado de un grupo de opciones en el menú.
 *
 * @param texto cadena de texto para el título de la sección.
 */
@Composable
private fun MenuSeccionTitulo(texto: String) {
    Text(
        text = texto.uppercase(),
        color = CompaSOSColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

/**
 * componente composable interno que dibuja una línea divisoria horizontal.
 */
@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = CompaSOSColors.FieldBackground,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

/**
 * componente composable interno para renderizar cada una de las opciones clicables del menú lateral.
 *
 * @param opcion datos de la opción a dibujar.
 * @param seleccionada indica si el ítem actual se encuentra activo.
 * @param onClick evento al seleccionar la opción.
 * @param colorTexto color con el que se pintará el texto de la opción.
 * @param colorIcono color aplicado al ícono del elemento.
 */
@Composable
private fun MenuOpcionItem(
    opcion: MenuOpcion,
    seleccionada: Boolean,
    onClick: () -> Unit,
    colorTexto: Color = CompaSOSColors.TextPrimary,
    colorIcono: Color = CompaSOSColors.IconTint
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(CompaSOSFieldShapeRadius))
            .background(if (seleccionada) CompaSOSColors.AccentBlue.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = opcion.icono,
            contentDescription = null,
            tint = if (seleccionada) CompaSOSColors.AccentBlue else colorIcono,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = opcion.titulo,
            color = if (seleccionada) CompaSOSColors.AccentBlue else colorTexto,
            fontSize = 14.sp,
            fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (opcion.badge != null && opcion.badge > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFEF5350))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (opcion.badge > 9) "9+" else opcion.badge.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}