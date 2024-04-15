package com.example.todoapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoapp.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface SettingsDAO {
    @Insert
    suspend fun insert(settingsEntity: SettingsEntity)

    @Update
    suspend fun update(settingsEntity: SettingsEntity) : Int

    @Transaction
    suspend fun updateOrInsert(settingsEntity: SettingsEntity){
        val rowUpdated = update(settingsEntity)
        if(rowUpdated == 0)
        {
            insert(settingsEntity)
        }
    }

    @Query("SELECT * FROM ${SettingsEntity.TABLE_NAME}")
    fun getAllSettings(): Flow<List<SettingsEntity>>
}