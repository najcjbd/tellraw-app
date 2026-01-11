package com.tellraw.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val selector: String,
    val message: String,
    val javaCommand: String,
    val bedrockCommand: String,
    val timestamp: Long
)

@Dao
interface CommandHistoryDao {
    
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CommandHistory>>
    
    @Query("SELECT * FROM command_history WHERE id = :id")
    suspend fun getById(id: Long): CommandHistory?
    
    @Insert
    suspend fun insert(history: CommandHistory)
    
    @Update
    suspend fun update(history: CommandHistory)
    
    @Delete
    suspend fun delete(history: CommandHistory)
    
    @Query("DELETE FROM command_history")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM command_history WHERE selector LIKE :query OR message LIKE :query ORDER BY timestamp DESC")
    fun searchByQuery(query: String): Flow<List<CommandHistory>>
}

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val key: String,
    val value: String
)

@Dao
interface AppSettingsDao {
    
    @Query("SELECT * FROM app_settings WHERE key = :key")
    suspend fun getByKey(key: String): AppSettings?
    
    @Query("SELECT * FROM app_settings")
    fun getAll(): Flow<List<AppSettings>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: AppSettings)
    
    @Update
    suspend fun update(settings: AppSettings)
    
    @Delete
    suspend fun delete(settings: AppSettings)
    
    @Query("DELETE FROM app_settings WHERE key = :key")
    suspend fun deleteByKey(key: String)
    
    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}

@Database(
    entities = [CommandHistory::class, AppSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun appSettingsDao(): AppSettingsDao
}