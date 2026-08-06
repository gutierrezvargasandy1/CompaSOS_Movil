package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "ubicaciones")
data class UbicacionEntity(
    @PrimaryKey
    val id: String,
    val alertaId: String,
    val latitud: Double?,
    val longitud: Double?,
    val precision: Double?,
    val velocidad: Double?,
    val fecha: String
)