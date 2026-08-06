package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.ContactoEmergenciaEntity

@Dao
interface ContactoEmergenciaDao {

    @Insert
    suspend fun insertar(contacto: ContactoEmergenciaEntity)

    @Query("SELECT * FROM contactos_emergencia")
    suspend fun obtenerTodos(): List<ContactoEmergenciaEntity>

    @Query("SELECT * FROM contactos_emergencia WHERE usuarioId = :usuarioId ORDER BY prioridad")
    suspend fun obtenerPorUsuario(usuarioId: String): List<ContactoEmergenciaEntity>
}