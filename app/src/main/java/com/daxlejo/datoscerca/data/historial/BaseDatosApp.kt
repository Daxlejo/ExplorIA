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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entradas", indices = [Index("generado_en_ms"), Index("lugar_id")])
data class EntradaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lugar_id") val lugarId: String,
    @ColumnInfo(name = "lugar_nombre") val lugarNombre: String,
    @ColumnInfo(name = "lugar_fuente") val lugarFuente: String,
    @ColumnInfo(name = "lugar_relevancia", defaultValue = "NINGUNA") val lugarRelevancia: String,
    val latitud: Double,
    val longitud: Double,
    @ColumnInfo(name = "distancia_metros") val distanciaMetros: Int,
    val texto: String,
    val tipo: String,
    val intensidad: String,
    @ColumnInfo(name = "generado_en_ms") val generadoEnMs: Long,
    val favorito: Boolean,
    @ColumnInfo(defaultValue = "MANUAL") val origen: String,
)

@Dao
interface EntradaDao {
    @Query("SELECT * FROM entradas ORDER BY generado_en_ms DESC, id DESC")
    fun observar(): Flow<List<EntradaEntity>>

    @Query("SELECT * FROM entradas WHERE id = :id")
    fun observarPorId(id: Long): Flow<EntradaEntity?>

    @Query("SELECT * FROM entradas ORDER BY generado_en_ms DESC, id DESC LIMIT :limite")
    suspend fun recientes(limite: Int): List<EntradaEntity>

    @Query("SELECT COUNT(*) FROM entradas WHERE origen = :origen AND generado_en_ms >= :desdeMs")
    suspend fun contarDesde(origen: String, desdeMs: Long): Int

    @Insert
    suspend fun insertar(entrada: EntradaEntity): Long

    @Query("UPDATE entradas SET favorito = NOT favorito WHERE id = :id")
    suspend fun alternarFavorito(id: Long)

    @Query("DELETE FROM entradas WHERE id = :id")
    suspend fun borrar(id: Long)
}

@Database(entities = [EntradaEntity::class], version = 2, exportSchema = false)
abstract class BaseDatosApp : RoomDatabase() {
    abstract fun entradaDao(): EntradaDao

    companion object {
        fun crear(context: Context): BaseDatosApp =
            Room.databaseBuilder(context, BaseDatosApp::class.java, "datos_cerca.db")
                .addMigrations(MIGRACION_1_2)
                .build()
    }
}

// v2: relevancia del lugar y origen del dato. Lo guardado antes queda como manual (no gasta el cupo por hora).
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entradas ADD COLUMN lugar_relevancia TEXT NOT NULL DEFAULT 'NINGUNA'")
        db.execSQL("ALTER TABLE entradas ADD COLUMN origen TEXT NOT NULL DEFAULT 'MANUAL'")
    }
}
