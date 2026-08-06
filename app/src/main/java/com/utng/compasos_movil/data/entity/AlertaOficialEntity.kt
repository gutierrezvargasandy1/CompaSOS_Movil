package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "alertas_oficiales")
data class AlertaOficialEntity(
    @PrimaryKey
    val id: String,
    val titulo: String?,
    val descripcion: String?,
    val tipo: String?,
    val estado: String?,
    val municipio: String?,
    val estadoRepublica: String?,
    val fechaInicio: String?,
    val fechaFin: String?
)