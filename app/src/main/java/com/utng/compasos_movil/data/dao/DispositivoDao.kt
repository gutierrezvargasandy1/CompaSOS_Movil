package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.DispositivoEntity

/**
 * interfaz dao (data access object) para la gestión y acceso a los datos de dispositivos vinculados
 * representados por la entidad [DispositivoEntity].
 */
@Dao
interface DispositivoDao {

    /**
     * inserta o reemplaza un dispositivo en la base de datos local.
     * utiliza la estrategia [OnConflictStrategy.REPLACE] para evitar conflictos de clave primaria al revincular.
     *
     * @param dispositivo entidad [DispositivoEntity] a registrar o actualizar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(dispositivo: DispositivoEntity)

    /**
     * obtiene el listado completo de todos los dispositivos registrados en la base de datos.
     *
     * @return lista de entidades [DispositivoEntity].
     */
    @Query("SELECT * FROM dispositivos")
    suspend fun obtenerTodos(): List<DispositivoEntity>

    /**
     * obtiene la lista de dispositivos pertenecientes a un usuario específico.
     *
     * @param usuarioId identificador único del usuario propietario.
     * @return lista de entidades [DispositivoEntity] asociadas al usuario.
     */
    @Query("SELECT * FROM dispositivos WHERE usuarioId = :usuarioId")
    suspend fun obtenerPorUsuario(usuarioId: String): List<DispositivoEntity>

    /**
     * consulta y obtiene un dispositivo específico a partir de su identificador único.
     *
     * @param id identificador único del dispositivo.
     * @return la entidad [DispositivoEntity] encontrada o null si no existe.
     */
    @Query("SELECT * FROM dispositivos WHERE id = :id")
    suspend fun obtenerPorId(id: String): DispositivoEntity?

    /**
     * actualiza el nivel de batería y el estado de conexión de un dispositivo registrado cuando se recibe un reporte.
     *
     * @param id identificador del dispositivo a actualizar.
     * @param bateria porcentaje o nivel de batería (opcional).
     * @param conectado indica si el dispositivo se encuentra conectado actualmente.
     */
    @Query("UPDATE dispositivos SET bateria = :bateria, conectado = :conectado WHERE id = :id")
    suspend fun actualizarEstado(id: String, bateria: Int?, conectado: Boolean)

    /**
     * obtiene la lista de dispositivos de tipo 'tv' vinculados a un usuario que se encuentran en estado conectado.
     *
     * @param userId identificador del usuario propietario.
     * @return lista de entidades [DispositivoEntity] correspondientes a pantallas tv activas.
     */
    @Query("""
        SELECT * FROM dispositivos
        WHERE tipo = 'tv'
          AND usuarioId = :userId
          AND conectado = 1
    """)
    suspend fun obtenerTvsVinculados(userId: String): List<DispositivoEntity>

    /**
     * marca un dispositivo como desconectado actualizando el estado de conexión a falso.
     *
     * @param id identificador único del dispositivo a desconectar.
     */
    @Query("UPDATE dispositivos SET conectado = 0 WHERE id = :id")
    suspend fun desconectar(id: String)
}