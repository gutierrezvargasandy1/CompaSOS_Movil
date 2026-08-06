package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "contactos_emergencia")
data class ContactoEmergenciaEntity(
    @PrimaryKey
    val id: String,
    val usuarioId: String,
    val nombre: String?,
    val telefono: String?,
    val correo: String?,
    val parentesco: String?,
    val prioridad: Int?
)