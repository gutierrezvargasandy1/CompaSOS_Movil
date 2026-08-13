package com.utng.compasos_movil.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import com.utng.compasos_movil.data.entity.UsuarioEntity

data class MiembroConDatos(
    @Embedded val usuario: UsuarioEntity,    // expande todos los campos de la tabla usuarios
    @ColumnInfo(name = "rol") val rol: String?
)