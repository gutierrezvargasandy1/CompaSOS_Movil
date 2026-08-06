package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "notificaciones")
data class NotificacionEntity(
    @PrimaryKey
    val id: String,
    val alertaId: String,
    val destinatario: String?,
    val tipo: String?,
    val estado: String?,
    val fecha: String
)