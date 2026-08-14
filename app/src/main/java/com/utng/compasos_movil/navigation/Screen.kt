package com.utng.compasos_movil.navigation

sealed class Screen(val route: String) {
    object Login                : Screen("login")
    object Registro             : Screen("registro")
    object PerfilMedico         : Screen("perfil_medico")
    object Dashboard            : Screen("dashboard")
    object ContactosEmergencia  : Screen("contactos_emergencia")
    object Familia              : Screen("familia")
    object Dispositivos         : Screen("dispositivos")
    object HistorialUbicaciones : Screen("historial_ubicaciones")
    object Notificaciones       : Screen("notificaciones")
    object Configuracion        : Screen("configuracion")
    object Perfil               : Screen("perfil")
    object EditarPerfil         : Screen("editar_perfil")

    object AlertasRecibidas : Screen("alertas_recibidas")
    object VincularTv : Screen("vincular_tv")

    // ← nuevo: pantalla detalle de alerta (desde notificación o lista)
    object AlertaDetalle : Screen("alertaDetalle/{alertaId}") {
        fun crearRuta(alertaId: String) = "alertaDetalle/$alertaId"
    }

}