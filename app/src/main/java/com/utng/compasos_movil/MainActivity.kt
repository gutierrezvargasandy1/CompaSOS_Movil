package com.utng.compasos_movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.utng.compasos_movil.ui.screens.LoginScreen
import com.utng.compasos_movil.ui.theme.CompaSOS_MovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompaSOS_MovilTheme {
                LoginScreen().LoginScreen()

            }
        }
    }
}
