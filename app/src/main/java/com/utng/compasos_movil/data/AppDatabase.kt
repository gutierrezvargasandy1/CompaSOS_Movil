package com.utng.compasos_movil.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
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

/**
 * base de datos principal de room para la aplicación compasos.
 * define el esquema de la base de datos, las entidades registradas y los objetos de acceso a datos (dao).
 */
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

    /**
     * provee el objeto de acceso a datos para la entidad de usuarios.
     *
     * @return instancia de [UsuarioDao].
     */
    abstract fun usuarioDao():            UsuarioDao

    /**
     * provee el objeto de acceso a datos para los perfiles médicos.
     *
     * @return instancia de [PerfilMedicoDao].
     */
    abstract fun perfilMedicoDao():       PerfilMedicoDao

    /**
     * provee el objeto de acceso a datos para los contactos de emergencia.
     *
     * @return instancia de [ContactoEmergenciaDao].
     */
    abstract fun contactoEmergenciaDao(): ContactoEmergenciaDao

    /**
     * provee el objeto de acceso a datos para los dispositivos vinculados.
     *
     * @return instancia de [DispositivoDao].
     */
    abstract fun dispositivoDao():        DispositivoDao

    /**
     * provee el objeto de acceso a datos para la gestión de alertas.
     *
     * @return instancia de [AlertaDao].
     */
    abstract fun alertaDao():             AlertaDao

    /**
     * provee el objeto de acceso a datos para las ubicaciones registradas en alertas.
     *
     * @return instancia de [UbicacionDao].
     */
    abstract fun ubicacionDao():          UbicacionDao

    /**
     * provee el objeto de acceso a datos para los archivos de audio grabados.
     *
     * @return instancia de [AudioDao].
     */
    abstract fun audioDao():              AudioDao

    /**
     * provee el objeto de acceso a datos para las lecturas de los sensores.
     *
     * @return instancia de [SensorDao].
     */
    abstract fun sensorDao():             SensorDao

    /**
     * provee el objeto de acceso a datos para el historial de notificaciones.
     *
     * @return instancia de [NotificacionDao].
     */
    abstract fun notificacionDao():       NotificacionDao

    /**
     * provee el objeto de acceso a datos para los eventos de seguimiento de alertas.
     *
     * @return instancia de [SeguimientoDao].
     */
    abstract fun seguimientoDao():        SeguimientoDao

    /**
     * provee el objeto de acceso a datos para las alertas oficiales recibidas.
     *
     * @return instancia de [AlertaOficialDao].
     */
    abstract fun alertaOficialDao():      AlertaOficialDao

    /**
     * provee el objeto de acceso a datos para el historial general de ubicación.
     *
     * @return instancia de [HistorialUbicacionDao].
     */
    abstract fun historialUbicacionDao(): HistorialUbicacionDao

    /**
     * provee el objeto de acceso a datos para la gestión de grupos familiares.
     *
     * @return instancia de [FamiliaDao].
     */
    abstract fun familiaDao():            FamiliaDao

    /**
     * provee el objeto de acceso a datos para las relaciones entre miembros y grupos familiares.
     *
     * @return instancia de [FamiliaUsuarioDao].
     */
    abstract fun familiaUsuarioDao():     FamiliaUsuarioDao

    /**
     * provee el objeto de acceso a datos para el registro de llamadas al 911.
     *
     * @return instancia de [Llamada911Dao].
     */
    abstract fun llamada911Dao():         Llamada911Dao

    // ── Singleton ─────────────────────────────────────────────────────────────
    companion object {
        /**
         * instancia única almacenada en memoria de la base de datos [AppDatabase].
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * obtiene la instancia singleton existente o crea una nueva utilizando el contexto de la aplicación.
         *
         * @param context contexto de la aplicación para inicializar la base de datos de room.
         * @return la instancia única de [AppDatabase].
         */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "compasos.db"
                )
                    .fallbackToDestructiveMigration() // ← evita crash si sube version sin Migration
                    .build()
                    .also { INSTANCE = it }
            }
    }
}