package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.ProxySourceEntity
import com.example.data.model.V2RayConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxySourceDao {
    @Query("SELECT * FROM proxy_sources ORDER BY id ASC")
    fun getAllSourcesFlow(): Flow<List<ProxySourceEntity>>

    @Query("SELECT * FROM proxy_sources ORDER BY id ASC")
    suspend fun getAllSources(): List<ProxySourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: ProxySourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<ProxySourceEntity>)

    @Update
    suspend fun updateSource(source: ProxySourceEntity)

    @Query("DELETE FROM proxy_sources WHERE id = :id")
    suspend fun deleteSource(id: Int)

    @Query("SELECT COUNT(*) FROM proxy_sources")
    suspend fun countSources(): Int
}

@Dao
interface V2RayConfigDao {
    @Query("SELECT * FROM v2ray_configs ORDER BY pingMs ASC, lastChecked DESC")
    fun getAllConfigsFlow(): Flow<List<V2RayConfigEntity>>

    @Query("SELECT * FROM v2ray_configs")
    suspend fun getAllConfigs(): List<V2RayConfigEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfigs(configs: List<V2RayConfigEntity>)

    @Update
    suspend fun updateConfig(config: V2RayConfigEntity)

    @Query("UPDATE v2ray_configs SET pingMs = :ping, lastChecked = :timestamp WHERE configHash = :hash")
    suspend fun updatePing(hash: String, ping: Long, timestamp: Long)

    @Query("DELETE FROM v2ray_configs WHERE configHash = :hash")
    suspend fun deleteConfig(hash: String)

    @Query("DELETE FROM v2ray_configs")
    suspend fun clearAllConfigs()

    @Query("DELETE FROM v2ray_configs WHERE isFavorite = 0")
    suspend fun clearNonFavorites()

    @Query("SELECT * FROM v2ray_configs WHERE isFavorite = 1")
    fun getFavoriteConfigsFlow(): Flow<List<V2RayConfigEntity>>
}

@Database(entities = [ProxySourceEntity::class, V2RayConfigEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun proxySourceDao(): ProxySourceDao
    abstract fun v2RayConfigDao(): V2RayConfigDao
}
