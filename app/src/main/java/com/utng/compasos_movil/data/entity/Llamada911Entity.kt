package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "llamadas_911")
data class Llamada911Entity(
    @PrimaryKey
    val id: String,
    val alertaId: String,
    val numero: String?,
    val fecha: String?,
    val estado: String?
)