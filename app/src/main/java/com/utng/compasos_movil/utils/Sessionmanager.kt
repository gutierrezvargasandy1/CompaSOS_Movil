package com.utng.compasos_movil.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SessionManager
 * Almacena y recupera los datos de sesión del usuario (ID, email, etc)
 * Usa EncryptedSharedPreferences para seguridad
 */
class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "compasos_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NOMBRE = "user_nombre"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    /**
     * Guarda los datos de la sesión cuando el usuario inicia sesión
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
     * Obtiene el ID del usuario actualmente logueado
     * @return ID del usuario o null si no hay sesión activa
     */
    fun obtenerUsuarioId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Obtiene el email del usuario actualmente logueado
     */
    fun obtenerUsuarioEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Obtiene el nombre del usuario actualmente logueado
     */
    fun obtenerUsuarioNombre(): String? {
        return sharedPreferences.getString(KEY_USER_NOMBRE, null)
    }

    /**
     * Verifica si hay una sesión activa
     */
    fun estaSesionActiva(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Cierra la sesión (elimina todos los datos guardados)
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
     * Obtiene todos los datos de la sesión en un diccionario
     */
    fun obtenerDatosSesion(): Map<String, Any> {
        return mapOf(
            "usuarioId" to (obtenerUsuarioId() ?: ""),
            "email" to (obtenerUsuarioEmail() ?: ""),
            "nombre" to (obtenerUsuarioNombre() ?: "")
        )
    }
}