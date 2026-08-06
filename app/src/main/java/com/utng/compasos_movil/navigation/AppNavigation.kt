package com.utng.compasos_movil.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utng.compasos_movil.ui.screens.LoginScreen
import com.utng.compasos_movil.ui.screens.PerfilMedicoScreen
import com.utng.compasos_movil.ui.screens.RegistroUsuarioScreen


@Composable
    fun AppNavigation(){
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route
        ){

            composable(Screen.Login.route){
                LoginScreen(navController)
            }

            composable(Screen.Registro.route){
                RegistroUsuarioScreen(navController)
            }

            composable(Screen.PerfilMedico.route){
                PerfilMedicoScreen(navController)
            }




        }
    }
