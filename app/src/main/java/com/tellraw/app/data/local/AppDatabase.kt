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

@Database(
    entities = [CommandHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao
}