package com.utng.compasos_movil.data.wrapper


import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity

data class UsuarioConUbicacionesWrapper(
    val usuario: UsuarioEntity,
    val historialUbicaciones: List<HistorialUbicacionEntity> = emptyList()
)