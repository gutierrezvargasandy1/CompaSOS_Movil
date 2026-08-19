package com.utng.compasos_movil.navigation

/**
 * clase sellada que representa las diferentes pantallas y rutas de navegación de la aplicación.
 *
 * @property route ruta en formato de cadena utilizada por el navhost para identificar la pantalla.
 */
sealed class Screen(val route: String) {
    /**
     * pantalla de inicio de sesión de usuario.
     */
    object Login                : Screen("login")

    /**
     * pantalla de registro de nuevo usuario.
     */
    object Registro             : Screen("registro")

    /**
     * pantalla para la configuración inicial del perfil médico del usuario.
     */
    object PerfilMedico         : Screen("perfil_medico")

    /**
     * pantalla principal del panel de control (dashboard).
     */
    object Dashboard            : Screen("dashboard")

    /**
     * pantalla para la gestión de contactos de emergencia.
     */
    object ContactosEmergencia  : Screen("contactos_emergencia")

    /**
     * pantalla para la administración del grupo familiar.
     */
    object Familia              : Screen("familia")

    /**
     * pantalla para la gestión y vinculación de dispositivos.
     */
    object Dispositivos         : Screen("dispositivos")

    /**
     * pantalla para la visualización del historial de ubicaciones geográficas.
     */
    object HistorialUbicaciones : Screen("historial_ubicaciones")

    /**
     * pantalla para la consulta del historial de notificaciones.
     */
    object Notificaciones       : Screen("notificaciones")

    /**
     * pantalla de configuración general de la aplicación.
     */
    object Configuracion        : Screen("configuracion")

    /**
     * pantalla para la visualización del perfil del usuario.
     */
    object Perfil               : Screen("perfil")

    /**
     * pantalla para la edición de los datos del perfil de usuario.
     */
    object EditarPerfil         : Screen("editar_perfil")

    /**
     * pantalla para la lista de alertas de emergencia recibidas.
     */
    object AlertasRecibidas : Screen("alertas_recibidas")

    /**
     * pantalla para la vinculación con dispositivos tv.
     */
    object VincularTv : Screen("vincular_tv")

    // ← nuevo: pantalla detalle de alerta (desde notificación o lista)
    /**
     * pantalla de detalle de una alerta específica.
     */
    object AlertaDetalle : Screen("alertaDetalle/{alertaId}") {
        /**
         * construye la ruta navegable parametrizada con el id de la alerta.
         *
         * @param alertaId identificador único de la alerta a consultar.
         * @return cadena de texto con la ruta formateada para la navegación.
         */
        fun crearRuta(alertaId: String) = "alertaDetalle/$alertaId"
    }

}