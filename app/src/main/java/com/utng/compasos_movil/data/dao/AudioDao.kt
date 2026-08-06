package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.AudioEntity

@Dao
interface AudioDao {

    @Insert
    suspend fun insertar(audio: AudioEntity)

    @Query("SELECT * FROM audios WHERE alertaId = :alertaId")
    suspend fun obtenerPorAlerta(alertaId: String): List<AudioEntity>
}