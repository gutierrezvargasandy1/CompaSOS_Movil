package com.utng.compasos_movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room3.Room
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.navigation.AppNavigation
import com.utng.compasos_movil.ui.theme.CompaSOS_MovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "compasos.db"
        ).build()

        setContent {
            CompaSOS_MovilTheme {
                AppNavigation()

            }
        }
    }
}
