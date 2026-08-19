package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.ContactoEmergenciaEntity

/**
 * interfaz dao (data access object) para la gestión y acceso a los datos de los contactos de emergencia
 * representados por la entidad [ContactoEmergenciaEntity].
 */
@Dao
interface ContactoEmergenciaDao {

    /**
     * inserta un nuevo contacto de emergencia en la base de datos local.
     *
     * @param contacto entidad [ContactoEmergenciaEntity] a registrar.
     */
    @Insert
    suspend fun insertar(contacto: ContactoEmergenciaEntity)

    /**
     * obtiene el listado completo de todos los contactos de emergencia almacenados en la base de datos.
     *
     * @return lista de entidades [ContactoEmergenciaEntity].
     */
    @Query("SELECT * FROM contactos_emergencia")
    suspend fun obtenerTodos(): List<ContactoEmergenciaEntity>

    /**
     * obtiene la lista de contactos de emergencia asociados a un usuario específico,
     * ordenados de forma ascendente por su nivel de prioridad.
     *
     * @param usuarioId identificador único del usuario propietario de los contactos.
     * @return lista de entidades [ContactoEmergenciaEntity] pertenecientes al usuario.
     */
    @Query("SELECT * FROM contactos_emergencia WHERE usuarioId = :usuarioId ORDER BY prioridad")
    suspend fun obtenerPorUsuario(usuarioId: String): List<ContactoEmergenciaEntity>
}