package com.example.todoapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoapp.entities.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDAO {
    @Insert suspend fun insert(subtask: SubtaskEntity): Long

    @Update suspend fun update(subtask: SubtaskEntity): Int

    @Delete suspend fun delete(subtask: SubtaskEntity)

    @Transaction
    suspend fun updateOrInsert(subtask: SubtaskEntity) {
        val rowsUpdated = update(subtask)
        if (rowsUpdated == 0) {
            insert(subtask)
        }
    }

    @Query ("SELECT id FROM " + SubtaskEntity.TABLE_NAME + " WHERE rowid = :rowId")
    suspend fun getSubtaskId(rowId: Long) : Int

    @Query("SELECT * FROM " + SubtaskEntity.TABLE_NAME)
    fun getAll() : Flow<List<SubtaskEntity>>
}