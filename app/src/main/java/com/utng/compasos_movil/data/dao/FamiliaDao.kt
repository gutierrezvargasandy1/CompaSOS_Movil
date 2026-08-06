package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.FamiliaEntity

@Dao
interface FamiliaDao {

    @Insert
    suspend fun insertar(familia: FamiliaEntity)

    @Query("SELECT * FROM familias")
    suspend fun obtenerTodas(): List<FamiliaEntity>
}