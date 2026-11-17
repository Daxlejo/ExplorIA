package com.daxlejo.datoscerca.data.historial

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entradas", indices = [Index("generado_en_ms"), Index("lugar_id")])
data class EntradaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lugar_id") val lugarId: String,
    @ColumnInfo(name = "lugar_nombre") val lugarNombre: String,
    @ColumnInfo(name = "lugar_fuente") val lugarFuente: String,
    val latitud: Double,
    val longitud: Double,
    @ColumnInfo(name = "distancia_metros") val distanciaMetros: Int,
    val texto: String,
    val tipo: String,
    val intensidad: String,
    @ColumnInfo(name = "generado_en_ms") val generadoEnMs: Long,
    val favorito: Boolean,
)

@Dao
interface EntradaDao {
    @Query("SELECT * FROM entradas ORDER BY generado_en_ms DESC, id DESC")
    fun observar(): Flow<List<EntradaEntity>>

    @Query("SELECT * FROM entradas WHERE id = :id")
    fun observarPorId(id: Long): Flow<EntradaEntity?>

    @Query("SELECT * FROM entradas ORDER BY generado_en_ms DESC, id DESC LIMIT :limite")
    suspend fun recientes(limite: Int): List<EntradaEntity>

    @Insert
    suspend fun insertar(entrada: EntradaEntity): Long

    @Query("UPDATE entradas SET favorito = NOT favorito WHERE id = :id")
    suspend fun alternarFavorito(id: Long)

    @Query("DELETE FROM entradas WHERE id = :id")
    suspend fun borrar(id: Long)
}

@Database(entities = [EntradaEntity::class], version = 1, exportSchema = false)
abstract class BaseDatosApp : RoomDatabase() {
    abstract fun entradaDao(): EntradaDao

    companion object {
        fun crear(context: Context): BaseDatosApp =
            Room.databaseBuilder(context, BaseDatosApp::class.java, "datos_cerca.db").build()
    }
}
