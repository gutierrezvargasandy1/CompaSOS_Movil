package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "familias")
data class FamiliaEntity(
    @PrimaryKey
    val id: String,
    val nombre: String?
)