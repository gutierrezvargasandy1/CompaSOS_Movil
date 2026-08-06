package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "perfil_medico")
data class PerfilMedicoEntity(
    @PrimaryKey
    val id: String,
    val usuarioId: String,
    val tipoSangre: String?,
    val alergias: String?,
    val padecimientos: String?,
    val medicamentos: String?,
    val peso: Double?,
    val altura: Double?,
    val observaciones: String?
)