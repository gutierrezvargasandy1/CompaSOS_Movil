package com.utng.compasos_movil.data.wrapper


import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity

/**
 * clase de datos envoltorio (wrapper) que combina la información de un usuario
 * con la lista de su historial de ubicaciones geográficas registradas.
 *
 * @property usuario entidad [UsuarioEntity] que contiene los datos personales del usuario.
 * @property historialUbicaciones lista de entidades [HistorialUbicacionEntity] pertenecientes al usuario, por defecto vacía.
 */
data class UsuarioConUbicacionesWrapper(
    val usuario: UsuarioEntity,
    val historialUbicaciones: List<HistorialUbicacionEntity> = emptyList()
)