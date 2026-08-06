package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.UsuarioEntity

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios")
    suspend fun obtenerTodos(): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE id = :usuarioId")
    suspend fun obtenerPorId(usuarioId: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    suspend fun obtenerPorCorreo(correo: String): UsuarioEntity?
}