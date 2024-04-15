package com.example.todoapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoapp.entities.TodoEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface TodoDAO {
    @Insert suspend fun insert(todo:TodoEntity): Long

    @Delete suspend fun delete(todo: TodoEntity)

    @Update suspend fun update(todo: TodoEntity): Int

    @Transaction
    suspend fun updateOrInsert(todo: TodoEntity) {
        val rowsUpdated = update(todo)
        if (rowsUpdated == 0) {
            insert(todo)
        }
    }

    @Query ("SELECT id FROM " + TodoEntity.TABLE_NAME + " WHERE rowid = :rowId")
    suspend fun getTodoId(rowId: Long) : Int


    @Query("SELECT * FROM " + TodoEntity.TABLE_NAME)
    fun getAll() : Flow<List<TodoEntity>>
}