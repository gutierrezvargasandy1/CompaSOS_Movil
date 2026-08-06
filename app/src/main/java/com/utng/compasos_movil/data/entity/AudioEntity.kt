package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "audios")
data class AudioEntity(
    @PrimaryKey
    val id: String,
    val alertaId: String,
    val urlAudio: String?,
    val duracion: Int?,
    val fecha: String
)