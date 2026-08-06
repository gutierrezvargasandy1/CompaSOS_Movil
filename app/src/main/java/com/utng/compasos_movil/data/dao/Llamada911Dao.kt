package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.Llamada911Entity

@Dao
interface Llamada911Dao {

    @Insert
    suspend fun insertar(llamada: Llamada911Entity)

    @Query("SELECT * FROM llamadas_911 WHERE alertaId = :alertaId")
    suspend fun obtenerPorAlerta(alertaId: String): List<Llamada911Entity>
}