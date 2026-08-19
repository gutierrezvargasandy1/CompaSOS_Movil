package com.utng.compasos_movil.AuthModule

import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.data.repository.UsuarioRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * servicio de autenticación de la aplicación.
 * maneja el inicio de sesión, el registro de nuevos usuarios, la consulta y actualización de perfiles,
 * la eliminación/desactivación de cuentas y la validación de datos de credenciales.
 *
 * @property usuarioRepository repositorio encargado de la gestión de persistencia de datos de usuario.
 */
class AuthService(
    private val usuarioRepository: UsuarioRepository
) {

    /**
     * realiza la autenticación de un usuario mediante su correo electrónico y contraseña.
     * valida que los campos no estén vacíos, busca el usuario en el repositorio y verifica
     * que la contraseña coincida y la cuenta se encuentre activa.
     *
     * @param correo email del usuario a autenticar.
     * @param password contraseña ingresada por el usuario.
     * @return la entidad [UsuarioEntity] si el login es exitoso, o null si falla la validación o credenciales.
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
     * registra un nuevo usuario en la base de datos tras validar los campos obligatorios,
     * el formato del correo, la longitud de la contraseña y la inexistencia previa del correo.
     *
     * @param nombre nombre del usuario (obligatorio).
     * @param apellidoPaterno apellido paterno del usuario (opcional).
     * @param apellidoMaterno apellido materno del usuario (opcional).
     * @param correo dirección de correo electrónico única.
     * @param password contraseña (mínimo 6 caracteres).
     * @param telefono número telefónico (opcional).
     * @param fechaNacimiento fecha de nacimiento en formato texto (opcional).
     * @param sexo género o sexo del usuario (opcional).
     * @return la entidad [UsuarioEntity] creada e insertada, o null si alguna validación falla.
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
     * obtiene la información de perfil de un usuario a partir de su identificador único.
     *
     * @param usuarioId id único del usuario a consultar.
     * @return la entidad [UsuarioEntity] con los datos del perfil, o null si el id está en blanco o no existe.
     */
    suspend fun obtenerPerfil(usuarioId: String): UsuarioEntity? {
        if (usuarioId.isBlank()) {
            return null
        }

        return usuarioRepository.obtenerPorId(usuarioId)
    }

    /**
     * elimina o desactiva la cuenta de un usuario en la base de datos a partir de su id.
     *
     * @param usuarioId id del usuario a eliminar.
     * @return true si la eliminación se realizó con éxito, false si el id es inválido o ocurre un error.
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
     * actualiza la información del perfil de un usuario previamente registrado.
     *
     * @param usuario entidad [UsuarioEntity] con los datos modificados.
     * @return true si el usuario existía y se procesó la actualización, false en caso contrario.
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
     * verifica si una cadena cumple con la estructura básica de una dirección de correo electrónico válida.
     *
     * @param correo texto del correo a evaluar.
     * @return true si coincide con la expresión regular del formato correo, false de lo contrario.
     */
    private fun esCorreoValido(correo: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$"
        return correo.matches(emailRegex.toRegex())
    }

    /**
     * genera un identificador único alfanumérico para registrar a un nuevo usuario.
     *
     * @return cadena con formato "user_{timestamp}_{random}".
     */
    private fun generarId(): String {
        return "user_${System.currentTimeMillis()}_${(0..9999).random()}"
    }

    /**
     * obtiene la fecha y hora actual del sistema formateada en el estándar "yyyy-MM-dd HH:mm:ss".
     *
     * @return cadena de texto con la fecha y hora formateada.
     */
    private fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formato.format(Date())
    }
}