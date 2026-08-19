package com.utng.compasos_movil.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * gestor de sesiones encargado de almacenar, recuperar y eliminar la información de autenticación del usuario.
 * utiliza [EncryptedSharedPreferences] para garantizar el cifrado de datos sensibles en el almacenamiento local.
 *
 * @param context contexto de la aplicación necesario para inicializar el cifrado y las preferencias compartidas.
 */
class SessionManager(context: Context) {

    /** clave maestra utilizada para el cifrado y descifrado de las preferencias. */
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /** instancia de preferencias compartidas cifradas con esquemas aes256. */
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "compasos_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** objeto de compañía que contiene las constantes de llaves para el almacenamiento de datos. */
    companion object {
        /** llave para guardar y consultar el identificador único del usuario. */
        private const val KEY_USER_ID = "user_id"

        /** llave para guardar y consultar el correo electrónico del usuario. */
        private const val KEY_USER_EMAIL = "user_email"

        /** llave para guardar y consultar el nombre del usuario. */
        private const val KEY_USER_NOMBRE = "user_nombre"

        /** llave para guardar y consultar el estado de la sesión del usuario. */
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    /**
     * guarda la información del usuario y marca la sesión como activa en las preferencias cifradas.
     *
     * @param usuarioId identificador único del usuario autenticado.
     * @param email correo electrónico asociado a la cuenta del usuario.
     * @param nombre nombre del usuario autenticado.
     */
    fun guardarSesion(
        usuarioId: String,
        email: String,
        nombre: String
    ) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, usuarioId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NOMBRE, nombre)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * recupera el identificador único del usuario en sesión.
     *
     * @return cadena con el id del usuario o null si no existe una sesión activa.
     */
    fun obtenerUsuarioId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * recupera el correo electrónico del usuario en sesión.
     *
     * @return cadena con el correo del usuario o null si no se encuentra registrado.
     */
    fun obtenerUsuarioEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * recupera el nombre del usuario en sesión.
     *
     * @return cadena con el nombre del usuario o null si no se encuentra registrado.
     */
    fun obtenerUsuarioNombre(): String? {
        return sharedPreferences.getString(KEY_USER_NOMBRE, null)
    }

    /**
     * verifica si existe una sesión de usuario actualmente activa.
     *
     * @return true si la sesión está activa, false de lo contrario.
     */
    fun estaSesionActiva(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * remueve todas las llaves asociadas a la sesión del usuario en las preferencias.
     */
    fun cerrarSesion() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NOMBRE)
            remove(KEY_IS_LOGGED_IN)
            apply()
        }
    }

    /**
     * recopila los datos principales de la sesión en una estructura de mapa clave-valor.
     *
     * @return un mapa [Map] que contiene el identificador, correo y nombre del usuario.
     */
    fun obtenerDatosSesion(): Map<String, Any> {
        return mapOf(
            "usuarioId" to (obtenerUsuarioId() ?: ""),
            "email" to (obtenerUsuarioEmail() ?: ""),
            "nombre" to (obtenerUsuarioNombre() ?: "")
        )
    }
}