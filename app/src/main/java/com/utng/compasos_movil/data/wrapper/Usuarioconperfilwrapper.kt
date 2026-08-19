package com.utng.compasos_movil.data.wrapper

import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity

/**
 * clase de datos envoltorio (wrapper) que agrupa la información de un usuario
 * junto con su perfil médico asociado de forma opcional.
 *
 * @property usuario entidad [UsuarioEntity] que contiene los datos del usuario.
 * @property perfilMedico entidad [PerfilMedicoEntity] opcional con la información médica de emergencia.
 */
data class UsuarioConPerfilWrapper(
    val usuario: UsuarioEntity,
    val perfilMedico: PerfilMedicoEntity? = null
)