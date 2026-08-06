package com.utng.compasos_movil.data.entity

import androidx.room3.Entity

@Entity(tableName = "familia_usuario", primaryKeys = ["familiaId", "usuarioId"])
data class FamiliaUsuarioEntity(
    val familiaId: String,
    val usuarioId: String,
    val rol: String?
)