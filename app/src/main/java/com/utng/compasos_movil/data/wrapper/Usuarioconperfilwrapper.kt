package com.utng.compasos_movil.data.wrapper

import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity

data class UsuarioConPerfilWrapper(
    val usuario: UsuarioEntity,
    val perfilMedico: PerfilMedicoEntity? = null
)