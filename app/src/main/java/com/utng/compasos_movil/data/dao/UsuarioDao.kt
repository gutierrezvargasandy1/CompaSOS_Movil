package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.utng.compasos_movil.data.entity.UsuarioEntity

/**
 * interfaz dao (data access object) para la gestión y acceso a los datos de los usuarios
 * representados por la entidad [UsuarioEntity].
 */
@Dao
interface UsuarioDao {

    /**
     * inserta un nuevo usuario en la base de datos local.
     *
     * @param usuario entidad [UsuarioEntity] a registrar.
     */
    @Insert
    suspend fun insertar(usuario: UsuarioEntity)

    /**
     * actualiza la información de un usuario existente en la base de datos local.
     *
     * @param usuario entidad [UsuarioEntity] con los datos modificados.
     */
    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    /**
     * obtiene el listado completo de todos los usuarios registrados en la base de datos.
     *
     * @return lista de entidades [UsuarioEntity].
     */
    @Query("SELECT * FROM usuarios")
    suspend fun obtenerTodos(): List<UsuarioEntity>

    /**
     * consulta y obtiene un usuario específico a partir de su identificador único.
     *
     * @param id identificador único del usuario.
     * @return la entidad [UsuarioEntity] encontrada o null si no existe.
     */
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun obtenerPorId(id: String): UsuarioEntity?

    /**
     * busca usuarios activos cuyo nombre, apellidos o correo contengan el texto especificado.
     *
     * @param texto término de búsqueda para filtrar usuarios.
     * @return lista de hasta 20 entidades [UsuarioEntity] coincidentes.
     */
    @Query("""
    SELECT * FROM usuarios
    WHERE activo = 1
      AND (nombre LIKE '%' || :texto || '%'
           OR apellidoPaterno LIKE '%' || :texto || '%'
           OR apellidoMaterno LIKE '%' || :texto || '%'
           OR correo LIKE '%' || :texto || '%')
    LIMIT 20
""")
    suspend fun buscar(texto: String): List<UsuarioEntity>

    /**
     * obtiene un usuario registrado a partir de su correo electrónico.
     *
     * @param correo correo electrónico del usuario a buscar.
     * @return la entidad [UsuarioEntity] correspondiente o null si no se encuentra.
     */
    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    suspend fun obtenerPorCorreo(correo: String): UsuarioEntity?
}