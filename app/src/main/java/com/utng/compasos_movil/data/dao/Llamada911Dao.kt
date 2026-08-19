package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.Llamada911Entity

/**
 * interfaz dao (data access object) para la gestión y registro de llamadas al servicio 911
 * asociadas a la entidad [Llamada911Entity].
 */
@Dao
interface Llamada911Dao {

    /**
     * inserta un nuevo registro de llamada al 911 en la base de datos local.
     *
     * @param llamada entidad [Llamada911Entity] a registrar.
     */
    @Insert
    suspend fun insertar(llamada: Llamada911Entity)

    /**
     * obtiene el listado de llamadas al 911 vinculadas a una alerta específica.
     *
     * @param alertaId identificador único de la alerta consultada.
     * @return lista de entidades [Llamada911Entity] asociadas a la alerta.
     */
    @Query("SELECT * FROM llamadas_911 WHERE alertaId = :alertaId")
    suspend fun obtenerPorAlerta(alertaId: String): List<Llamada911Entity>
}