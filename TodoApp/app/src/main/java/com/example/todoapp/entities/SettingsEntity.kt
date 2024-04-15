package com.example.todoapp.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val key: String,
    val stringVal: String
) {
    companion object {
        const val TABLE_NAME = "settings"
    }
}
