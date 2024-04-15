package com.example.todoapp.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (TodoGroupEntity.TABLE_NAME)
data class TodoGroupEntity (
    @PrimaryKey()
    val name: String,
    val iconName: String,
    // States whether group is visible on home screen
    val visible: String
) {
    companion object {
        const val TABLE_NAME = "todo_groups"
    }
}