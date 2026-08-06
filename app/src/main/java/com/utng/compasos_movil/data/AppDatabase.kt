package com.utng.compasos_movil.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.AlertaOficialDao
import com.utng.compasos_movil.data.dao.AudioDao
import com.utng.compasos_movil.data.dao.ContactoEmergenciaDao
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.FamiliaDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.HistorialUbicacionDao
import com.utng.compasos_movil.data.dao.Llamada911Dao
import com.utng.compasos_movil.data.dao.NotificacionDao
import com.utng.compasos_movil.data.dao.PerfilMedicoDao
import com.utng.compasos_movil.data.dao.SeguimientoDao
import com.utng.compasos_movil.data.dao.SensorDao
import com.utng.compasos_movil.data.dao.UbicacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.AlertaOficialEntity
import com.utng.compasos_movil.data.entity.AudioEntity
import com.utng.compasos_movil.data.entity.ContactoEmergenciaEntity
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.data.entity.FamiliaEntity
import com.utng.compasos_movil.data.entity.FamiliaUsuarioEntity
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.Llamada911Entity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity
import com.utng.compasos_movil.data.entity.SeguimientoEntity
import com.utng.compasos_movil.data.entity.SensorEntity
import com.utng.compasos_movil.data.entity.UbicacionEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        PerfilMedicoEntity::class,
        ContactoEmergenciaEntity::class,
        DispositivoEntity::class,
        AlertaEntity::class,
        UbicacionEntity::class,
        AudioEntity::class,
        SensorEntity::class,
        NotificacionEntity::class,
        SeguimientoEntity::class,
        AlertaOficialEntity::class,
        HistorialUbicacionEntity::class,
        FamiliaEntity::class,
        FamiliaUsuarioEntity::class,
        Llamada911Entity::class
               ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

    abstract fun perfilMedicoDao(): PerfilMedicoDao

    abstract fun contactoEmergenciaDao(): ContactoEmergenciaDao

    abstract fun dispositivoDao(): DispositivoDao

    abstract fun alertaDao(): AlertaDao

    abstract fun ubicacionDao(): UbicacionDao

    abstract fun audioDao(): AudioDao

    abstract fun sensorDao(): SensorDao

    abstract fun notificacionDao(): NotificacionDao

    abstract fun seguimientoDao(): SeguimientoDao

    abstract fun alertaOficialDao(): AlertaOficialDao

    abstract fun historialUbicacionDao(): HistorialUbicacionDao

    abstract fun familiaDao(): FamiliaDao

    abstract fun familiaUsuarioDao(): FamiliaUsuarioDao

    abstract fun llamada911Dao(): Llamada911Dao

}