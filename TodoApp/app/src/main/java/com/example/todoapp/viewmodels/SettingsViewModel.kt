package com.example.todoapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.todoapp.data.TodoAppDatabase
import com.example.todoapp.entities.SettingsEntity

class SettingsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext
    private val settingsDao = TodoAppDatabase.getDB(context).settingsDao()

    var notificationsEnabled by mutableStateOf(true)
    var darkModeEnabled by mutableStateOf(false)
    var sortByValue by mutableStateOf("Name")
    var descriptionValue by mutableStateOf("1 Line")

    suspend fun updateNotifications(
        notification : Boolean
    ) {
        notificationsEnabled = notification
        settingsDao.updateOrInsert(SettingsEntity(key = NOTIFICATION, stringVal = notification.toString()))
    }

    suspend fun updateSortBy(
        sortBy : String
    ) {
        sortByValue = sortBy
        settingsDao.updateOrInsert(SettingsEntity(key = SORTBY, stringVal = sortBy))
    }

    suspend fun updateDescription(
        description : String
    ) {
        descriptionValue = description
        settingsDao.updateOrInsert(SettingsEntity(key = DESCRIPTION, stringVal = description))
    }

    suspend fun updateDarkMode(
        darkmode : Boolean
    ) {
        darkModeEnabled = darkmode
        settingsDao.updateOrInsert(SettingsEntity(key = DARKMODE, stringVal = darkmode.toString()))
    }

    // Inside SettingsViewModel
    fun updateSettings(settingsEntity: List<SettingsEntity>) {
        settingsEntity.forEach { entity ->
            when (entity.key) {
                NOTIFICATION -> {
                    notificationsEnabled = entity.stringVal.toBooleanStrictOrNull() ?: true
                }
                SORTBY -> {
                    sortByValue = entity.stringVal ?: "Name"
                }
                DESCRIPTION -> {
                    descriptionValue = entity.stringVal ?: "1 Line"
                }
                DARKMODE -> {
                    darkModeEnabled = entity.stringVal.toBooleanStrictOrNull() ?: false
                }
                else -> Log.w("settings", "unhandled setting ${entity.key}")
            }
        }
    }

    companion object{
        const val NOTIFICATION = "notification"
        const val SORTBY = "sortBy"
        const val DESCRIPTION = "description"
        const val DARKMODE = "darkMode"
    }
}
