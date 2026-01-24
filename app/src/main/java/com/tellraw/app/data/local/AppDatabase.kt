package com.tellraw.app.data.local

import androidx.room.*

@Database(
    entities = [],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // 数据库现在为空，所有数据都存储在文件中
}