package com.utng.compasos_movil.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.UsuarioEntity

@Database(
    entities = [UsuarioEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

}