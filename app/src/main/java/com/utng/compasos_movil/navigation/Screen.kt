package com.utng.compasos_movil.navigation

sealed class Screen (val route: String ){
    object Login: Screen("login")
    object Registro: Screen("registro")
    object PerfilMedico: Screen("perfil_medico")

}