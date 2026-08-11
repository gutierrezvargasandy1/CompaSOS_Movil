package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.utng.compasos_movil.data.entity.UsuarioEntity

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios")
    suspend fun obtenerTodos(): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun obtenerPorId(id: String): UsuarioEntity?

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
    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    suspend fun obtenerPorCorreo(correo: String): UsuarioEntity?


}