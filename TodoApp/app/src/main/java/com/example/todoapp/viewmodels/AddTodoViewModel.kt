package com.example.todoapp.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.todoapp.entities.Converters
import com.example.todoapp.entities.SubtaskEntity
import com.example.todoapp.entities.TodoEntity

class AddTodoViewModel(app: Application) : AndroidViewModel(app) {

    // Temporary values to be stored while user is filling form to add _Todo
    var taskName by mutableStateOf("")
    var description by mutableStateOf("")
    var userLocation by mutableStateOf("")
    var notificationDistance by mutableFloatStateOf(0f)
    var imageUriList = mutableStateListOf<Uri>()
    var selectedPriority by mutableStateOf("NONE")
    var selectedGroup by  mutableStateOf("Select Group")
    var selectedDate  by mutableStateOf("")
    var selectedTime by mutableStateOf("")
    var recordDateTimeOnCreate by mutableStateOf(false)
    var recordLocationOnCreate by mutableStateOf(false)
    var subtaskNumber by mutableStateOf(1)
    var tempSubtaskList = mutableStateListOf<SubtaskEntity>()
        private set

    // _Todo to be edited
    var toEditTodo: TodoEntity? by mutableStateOf(null)

    // Determines if dialog for adding/editing _Todo is shown
    var showDialog by mutableStateOf(false)

    fun incrementSubtaskNumber() {
        subtaskNumber++
    }

    fun updateShowDialog (shown: Boolean) {
        showDialog = shown
    }

    fun insertTempSubtask(subtask: SubtaskEntity) {
        tempSubtaskList.add(subtask)
    }

    fun updateTempSubtaskName(subtask: SubtaskEntity, name: String) {
        val index = tempSubtaskList.indexOf(subtask)
        if (index != -1){
            tempSubtaskList[index] = SubtaskEntity(
                subtask.todoId, name, subtask.completed, subtask.id
            )
            Log.i("persistence-logs", "temporary subtasks after update ${tempSubtaskList.toList()}")
        }
        else {
            Log.i("persistence-logs", "could not find temporary subtask $subtask to update name")
        }
    }

    fun updateTaskName (updatedName: String) {
        taskName = updatedName
    }

    fun updateDescription (updatedDescription: String) {
        description = updatedDescription
    }

    fun updateUserLocation (updatedUserLocation: String) {
        userLocation = updatedUserLocation
    }

    fun updateNotificationDistance (updatedNotificationDistance: Float) {
        notificationDistance = updatedNotificationDistance
    }

    fun updateSelectedPriority(updatedPriority: String) {
        selectedPriority = updatedPriority
    }

    fun updateSelectedGroup(updatedGroup: String) {
        selectedGroup = updatedGroup
    }

    fun updateSelectedDate(updatedDate: String) {
        selectedDate = updatedDate
    }

    fun updatedSelectedTime(updatedTime: String) {
        selectedTime = updatedTime
    }

    fun updateRecordDateTimeOnCreate(record: Boolean) {
        recordDateTimeOnCreate = record
    }

    fun updateRecordLocationOnCreate(record: Boolean) {
        recordLocationOnCreate = record
    }

    // Use the values of the _Todo to be edited
    fun setToEditTodo(todo: TodoEntity, todoViewModel: TodosViewModel) {
        toEditTodo = todo
        taskName = todo.name
        description = todo.description
        userLocation = todo.userLocation
        notificationDistance = todo.notificationDistance
        selectedPriority = todo.priority
        if (todo.group != "none")
            selectedGroup = todo.group
        if (todo.userDateTime != "T") {
            selectedTime = todo.userDateTime.split("T")[1]
            selectedDate = todo.userDateTime.split("T")[0]
        }
        val converter = Converters()
        val uriList = converter.stringToList(todo.imageUriList)
        uriList.forEach { uri ->
            imageUriList.add(uri)
        }
        if (todo.onCreateDateTime != "T")
            recordDateTimeOnCreate = true
        if (todo.onCreateLocation.isNotEmpty())
            recordLocationOnCreate = true

        // Add any subtasks of the _Todo to be edited
        todoViewModel.subtasksList.filter { it.todoId == todo.id.toString() }.forEach { subtask ->
            tempSubtaskList.add(subtask)
        }
    }

    // Reset values
    fun clear() {
        taskName = ""
        description = ""
        userLocation = ""
        notificationDistance = 0f
        selectedPriority = "NONE"
        selectedGroup = "Select Group"
        selectedDate = ""
        selectedTime = ""
        recordLocationOnCreate = false
        recordDateTimeOnCreate = false
        tempSubtaskList.clear()
        imageUriList.clear()
        toEditTodo = null
    }
}