package com.utng.compasos_movil.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val apellidoPaterno: String?,
    val apellidoMaterno: String?,
    val correo: String,
    val password: String,
    val telefono: String?,
    val foto: String?,
    val fechaNacimiento: String?,
    val sexo: String?,
    val activo: Boolean = true,
    val fechaRegistro: String
)