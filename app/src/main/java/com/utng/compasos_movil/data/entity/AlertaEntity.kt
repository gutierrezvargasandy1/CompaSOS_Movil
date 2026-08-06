package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "alertas")
data class AlertaEntity(
    @PrimaryKey
    val id: String,
    val usuarioId: String,
    val dispositivoId: String?,
    val tipoAlerta: String?,
    val descripcion: String?,
    val estado: String?,
    val fecha: String
)