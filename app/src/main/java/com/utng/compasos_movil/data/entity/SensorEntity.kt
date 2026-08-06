package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sensores")
data class SensorEntity(
    @PrimaryKey
    val id: String,
    val alertaId: String,
    val frecuenciaCardiaca: Int?,
    val aceleracion: Double?,
    val detectoCaida: Boolean?,
    val fecha: String
)