package com.example.todoapp.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (SubtaskEntity.TABLE_NAME)
data class SubtaskEntity(
    // id of _Todo which has the subtask
    val todoId: String,
    val name: String,
    val completed: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) {
    companion object {
        const val TABLE_NAME = "subtasks"
    }
}