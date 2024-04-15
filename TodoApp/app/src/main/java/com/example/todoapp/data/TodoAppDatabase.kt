package com.example.todoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.todoapp.entities.SettingsEntity
import com.example.todoapp.entities.SubtaskEntity
import com.example.todoapp.entities.TodoEntity
import com.example.todoapp.entities.TodoGroupEntity

@Database(entities = [TodoEntity::class, SubtaskEntity::class, TodoGroupEntity::class, SettingsEntity::class], version = 48, exportSchema = false)
abstract class TodoAppDatabase : RoomDatabase() {
    abstract fun todoDao() : TodoDAO
    abstract fun subtaskDao() : SubtaskDAO

    abstract fun groupDao(): TodoGroupDAO

    abstract fun settingsDao(): SettingsDAO

    companion object {
        private const val DB_NAME = "todoapp_db"

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context,
                TodoAppDatabase::class.java,
                DB_NAME)
                .fallbackToDestructiveMigration()
                .build()

        @Volatile private var thisDB: TodoAppDatabase? = null

        fun getDB(context: Context): TodoAppDatabase =
            thisDB ?: synchronized(this){
                thisDB ?: buildDatabase(context).also { thisDB = it}
            }
    }
}