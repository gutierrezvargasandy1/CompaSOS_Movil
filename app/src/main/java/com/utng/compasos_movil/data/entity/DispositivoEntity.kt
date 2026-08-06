package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "dispositivos")
data class DispositivoEntity(
    @PrimaryKey
    val id: String,
    val usuarioId: String,
    val tipo: String?,
    val modelo: String?,
    val fabricante: String?,
    val numeroSerie: String?,
    val tokenFcm: String?,
    val bateria: Int?,
    val conectado: Boolean = true,
    val fechaVinculacion: String
)