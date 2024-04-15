package com.example.todoapp.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity (TodoEntity.TABLE_NAME)
@TypeConverters(Converters::class)
data class TodoEntity(
    val name: String,
    val description: String,
    val priority: String,
    // Date/Time recorded on creation of a _Todo
    val onCreateDateTime: String,
    // Date/Time specified by the user
    val userDateTime: String,
    // Location recorded on creation of a _Todo
    val onCreateLocation: String,
    // Location specified by the user
    val userLocation: String,
    // Distance (in meters) from which a notification should be shown
    val notificationDistance: Float,
    val imageUriList: String,
    val completed: String,
    val favorite: String,
    val group: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
){
    companion object {
        const val TABLE_NAME = "todos"
    }
}