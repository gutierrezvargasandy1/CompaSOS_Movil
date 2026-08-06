package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "historial_ubicacion")
data class HistorialUbicacionEntity(
    @PrimaryKey
    val id: String,
    val usuarioId: String,
    val latitud: Double?,
    val longitud: Double?,
    val fecha: String
)