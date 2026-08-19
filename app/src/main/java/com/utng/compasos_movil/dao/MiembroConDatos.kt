package com.utng.compasos_movil.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import com.utng.compasos_movil.data.entity.UsuarioEntity

/**
 * clase de datos (pojo/query result) que combina la información completa de un usuario
 * con su rol específico dentro del grupo familiar.
 *
 * @property usuario entidad [UsuarioEntity] expandida que contiene todos los datos personales del usuario.
 * @property rol función o relación desempeñada por el usuario dentro del núcleo familiar.
 */
data class MiembroConDatos(
    @Embedded val usuario: UsuarioEntity,    // expande todos los campos de la tabla usuarios
    @ColumnInfo(name = "rol") val rol: String?
)