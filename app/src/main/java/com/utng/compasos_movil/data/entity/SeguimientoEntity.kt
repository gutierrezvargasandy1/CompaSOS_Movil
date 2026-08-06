package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "seguimiento")
data class SeguimientoEntity(
    @PrimaryKey
    val id: String,
    val alertaId: String,
    val comentario: String?,
    val estado: String?,
    val fecha: String
)