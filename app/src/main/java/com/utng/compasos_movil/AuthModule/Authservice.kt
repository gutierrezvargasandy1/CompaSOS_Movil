package com.utng.compasos_movil.AuthModule

import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.data.repository.UsuarioRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio de Autenticación
 * Maneja login, registro, eliminación de usuario y perfil
 */
class AuthService(
    private val usuarioRepository: UsuarioRepository
) {

    /**
     * Login de usuario
     * @param correo Email del usuario
     * @param password Contraseña del usuario
     * @return Resultado del login (UsuarioEntity si es exitoso, null si falla)
     */
    suspend fun login(correo: String, password: String): UsuarioEntity? {
        // Validar que correo y password no estén vacíos
        if (correo.isBlank() || password.isBlank()) {
            return null
        }

        // Buscar usuario por correo
        val usuario = usuarioRepository.obtenerPorCorreo(correo)

        // Verificar que el usuario existe y la contraseña es correcta
        // En producción, deberías usar hashing (bcrypt, Argon2, etc)
        return if (usuario != null && usuario.password == password && usuario.activo) {
            usuario
        } else {
            null
        }
    }

    /**
     * Registra un nuevo usuario
     * @param nombre Nombre del usuario
     * @param apellidoPaterno Apellido paterno
     * @param apellidoMaterno Apellido materno
     * @param correo Email del usuario
     * @param password Contraseña
     * @param telefono Teléfono (opcional)
     * @param fechaNacimiento Fecha de nacimiento (opcional)
     * @param sexo Sexo (opcional)
     * @return Resultado del registro (UsuarioEntity si es exitoso, null si falla)
     */
    suspend fun registrar(
        nombre: String,
        apellidoPaterno: String? = null,
        apellidoMaterno: String? = null,
        correo: String,
        password: String,
        telefono: String? = null,
        fechaNacimiento: String? = null,
        sexo: String? = null
    ): UsuarioEntity? {
        // Validar datos requeridos
        if (nombre.isBlank() || correo.isBlank() || password.isBlank()) {
            return null
        }

        // Validar formato de correo básico
        if (!esCorreoValido(correo)) {
            return null
        }

        // Validar que la contraseña tenga mínimo 6 caracteres
        if (password.length < 6) {
            return null
        }

        // Verificar si el correo ya existe
        if (usuarioRepository.correoExiste(correo)) {
            return null
        }

        // Crear nuevo usuario
        val nuevoUsuario = UsuarioEntity(
            id = generarId(),
            nombre = nombre.trim(),
            apellidoPaterno = apellidoPaterno?.trim(),
            apellidoMaterno = apellidoMaterno?.trim(),
            correo = correo.trim().lowercase(),
            password = password, // En producción, hashear la contraseña
            telefono = telefono?.trim(),
            foto = null,
            fechaNacimiento = fechaNacimiento,
            sexo = sexo,
            activo = true,
            fechaRegistro = obtenerFechaActual()
        )

        // Guardar en base de datos
        usuarioRepository.registrarUsuario(nuevoUsuario)

        return nuevoUsuario
    }

    /**
     * Obtiene el perfil del usuario actual
     * @param usuarioId ID del usuario
     * @return UsuarioEntity con los datos del perfil
     */
    suspend fun obtenerPerfil(usuarioId: String): UsuarioEntity? {
        if (usuarioId.isBlank()) {
            return null
        }

        return usuarioRepository.obtenerPorId(usuarioId)
    }

    /**
     * Elimina/desactiva un usuario
     * @param usuarioId ID del usuario a eliminar
     * @return true si fue exitoso, false si no
     */
    suspend fun eliminarUsuario(usuarioId: String): Boolean {
        return try {
            if (usuarioId.isBlank()) {
                return false
            }

            usuarioRepository.eliminarUsuario(usuarioId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Actualiza el perfil del usuario
     * (Nota: El DAO actual solo soporta Insert, no Update)
     * Puedes agregar esta funcionalidad al DAO si lo necesitas
     */
    suspend fun actualizarPerfil(usuario: UsuarioEntity): Boolean {
        return try {
            // El usuario debe estar registrado previamente
            val usuarioExistente = usuarioRepository.obtenerPorId(usuario.id)
            if (usuarioExistente != null) {
                // En un DAO completo, deberías usar @Update
                usuarioRepository.registrarUsuario(usuario)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verifica si un correo es válido (formato básico)
     */
    private fun esCorreoValido(correo: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$"
        return correo.matches(emailRegex.toRegex())
    }

    /**
     * Genera un ID único para el usuario
     */
    private fun generarId(): String {
        return "user_${System.currentTimeMillis()}_${(0..9999).random()}"
    }

    /**
     * Obtiene la fecha y hora actual en formato ISO 8601
     */
    private fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formato.format(Date())
    }
}