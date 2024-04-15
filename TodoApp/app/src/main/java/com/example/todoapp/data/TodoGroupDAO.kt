package com.example.todoapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoapp.entities.TodoGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoGroupDAO {
    @Insert
    suspend fun insert(group: TodoGroupEntity): Long

    @Update
    suspend fun update(group: TodoGroupEntity): Int

    @Transaction
    suspend fun updateOrInsert(group: TodoGroupEntity) {
        val rowsUpdated = update(group)
        if (rowsUpdated == 0) {
            insert(group)
        }
    }
    @Query("DELETE FROM " + TodoGroupEntity.TABLE_NAME + " WHERE name = :groupName")
    suspend fun deleteGroup(groupName: String)

    @Query("SELECT * FROM " + TodoGroupEntity.TABLE_NAME)
    fun getAll() : Flow<List<TodoGroupEntity>>
}