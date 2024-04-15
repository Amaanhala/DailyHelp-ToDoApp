package com.example.todoapp.viewmodels

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.TodoAppDatabase
import com.example.todoapp.entities.SubtaskEntity
import com.example.todoapp.entities.TodoEntity
import com.example.todoapp.entities.TodoGroupEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class TodosViewModel(app: Application) : AndroidViewModel(app) {
    private val context = getApplication<Application>().applicationContext
    private val todoDao = TodoAppDatabase.getDB(context).todoDao()
    private val subtaskDao = TodoAppDatabase.getDB(context).subtaskDao()
    private val groupDao = TodoAppDatabase.getDB(context).groupDao()

    var todosList = mutableStateListOf<TodoEntity>()
        private set
    var subtasksList = mutableStateListOf<SubtaskEntity>()
        private set

    var todoGroupsList = mutableStateListOf<TodoGroupEntity>()
        private set


    suspend fun updateCompletedTodo(todo: TodoEntity, completed: Boolean) {
        val index = todosList.indexOf(todo)
        if (index != -1) {
            // Todo: Refactor this and other methods by using todo.copy()
            val updatedTodo = TodoEntity(
                todo.name,
                todo.description,
                todo.priority,
                todo.onCreateDateTime,
                todo.userDateTime,
                todo.onCreateLocation,
                todo.userLocation,
                todo.notificationDistance,
                todo.imageUriList,
                completed.toString(),
                todo.favorite,
                todo.group,
                todo.id
            )
            todosList[index] = updatedTodo
            todoDao.update(updatedTodo)

            // If the main task is marked as completed, also mark all its subtasks as completed
            if (completed) {
                val relatedSubtasks = subtasksList.filter { it.todoId == todo.id.toString() }
                for (subtask in relatedSubtasks) {
                    val updatedSubtask = SubtaskEntity(
                        subtask.todoId,
                        subtask.name,
                        "true",
                        subtask.id
                    )
                    subtasksList[subtasksList.indexOf(subtask)] = updatedSubtask
                    subtaskDao.update(updatedSubtask)
                }
            }
        } else {
            Log.i("persistence-logs", "could not find todo $todo to update completed")
        }
    }
    suspend fun updateFavoriteTodo(todo: TodoEntity, favorite: Boolean) {
        val index = todosList.indexOf(todo)
        if (index != -1) {
            val updatedTodo = TodoEntity(
                todo.name,
                todo.description,
                todo.priority,
                todo.onCreateDateTime,
                todo.userDateTime,
                todo.onCreateLocation,
                todo.userLocation,
                todo.notificationDistance,
                todo.imageUriList,
                todo.completed,
                favorite.toString(),
                todo.group,
                todo.id
            )
            todosList[index] = updatedTodo
            todoDao.update(updatedTodo)
        } else {
            Log.i("persistence-logs", "could not find todo $todo to update favorite")
        }
    }
    suspend fun updateTodo(todo: TodoEntity) {
        val toUpdateTodo = todosList.firstOrNull { it.id == todo.id }
        val index = todosList.indexOf(toUpdateTodo)
        if (index != -1) {
            val updatedTodo = todo.copy()
            todosList[index] = updatedTodo
            todoDao.update(updatedTodo)

            // Remove from notified _Todos, so that notifications are shown again
            notifiedLocationTodos.remove(todo.id)
            notifiedReminderTodos.remove(todo.id)
        } else {
            Log.i("persistence-logs", "could not find todo $todo to update ")
        }
    }

    suspend fun insertTodo(todo: TodoEntity): Int {

        // Insert _Todo and get the rowId of the inserted _Todo
        val rowId = todoDao.insert(todo)

        // Get the id (primary key) of the inserted _Todo
        val id = todoDao.getTodoId(rowId)

        // Update list
        todosList.add(
            TodoEntity(
                todo.name,
                todo.description,
                todo.priority,
                todo.onCreateDateTime,
                todo.userDateTime,
                todo.onCreateLocation,
                todo.userLocation,
                todo.notificationDistance,
                todo.imageUriList,
                todo.completed,
                todo.favorite,
                todo.group,
                id
            )
        )
        //Log.i("persistence-logs", "after _Todo insert: " + todosList.toList())

        return id
    }

    suspend fun deleteTodo(todo: TodoEntity) {
        todoDao.delete(todo)
        todosList.remove(todo)
    }

    suspend fun updateTodos(todoEntities: List<TodoEntity>) {
        todoEntities.forEach { entity ->
            todoDao.updateOrInsert(entity)

            // refresh list
            if (!todosList.contains(entity))
                todosList.add(entity)
        }
        Log.i("persistence-logs", "todos in viewmodel " + todosList.toList())
    }

    suspend fun updateCompletedSubtask(subtask: SubtaskEntity, completed: Boolean) {
        val index = subtasksList.indexOf(subtask)
        if (index != -1) {
            val updatedSubtask = SubtaskEntity(
                subtask.todoId, subtask.name, completed.toString(), subtask.id
            )
            subtasksList[index] = updatedSubtask
            subtaskDao.update(updatedSubtask)

            // Determine if all subtasks of the related task are completed
            val allSubtasksCompleted = subtasksList.filter { it.todoId == subtask.todoId }
                .all { it.completed.toBoolean() }

            val mainTaskIndex = todosList.indexOfFirst { it.id.toString() == subtask.todoId }
            if (mainTaskIndex != -1) {
                val mainTask = todosList[mainTaskIndex]

                // Update the main task's completion status based on the subtasks' status
                val updatedMainTask = TodoEntity(
                    mainTask.name,
                    mainTask.description,
                    mainTask.priority,
                    mainTask.onCreateDateTime,
                    mainTask.userDateTime,
                    mainTask.onCreateLocation,
                    mainTask.userLocation,
                    mainTask.notificationDistance,
                    mainTask.imageUriList,
                    allSubtasksCompleted.toString(), // Update based on subtasks' completion
                    mainTask.favorite,
                    mainTask.group,
                    mainTask.id
                )
                todosList[mainTaskIndex] = updatedMainTask
                todoDao.update(updatedMainTask)
            }
        } else {
            Log.i("persistence-logs", "could not find subtask $subtask to update completed")
        }
    }



    suspend fun updateSubtask(subtask: SubtaskEntity) {
        val toUpdateSubtask = subtasksList.firstOrNull { it.id == subtask.id }
        val index = subtasksList.indexOf(toUpdateSubtask)
        if (index != -1) {
            val updatedSubtask = SubtaskEntity(
                subtask.todoId, subtask.name, subtask.completed, subtask.id
            )
            subtasksList[index] = updatedSubtask
            subtaskDao.update(updatedSubtask)
        } else {
            Log.i("persistence-logs", "could not find subtask $subtask to update")
        }
    }

    suspend fun insertSubtask(subtask: SubtaskEntity) {

        // Insert Subtask and get the rowId of the inserted Subtask
        val rowId = subtaskDao.insert(subtask)

        // Get the id (primary key) of the inserted Subtask
        val id = subtaskDao.getSubtaskId(rowId)

        // Update list
        subtasksList.add(
            SubtaskEntity(
                subtask.todoId, subtask.name, subtask.completed, id
            )
        )
        //Log.i("persistence-logs", "after subtask insert: " + subtasksList.toList())
    }

    suspend fun deleteSubtask(subtask: SubtaskEntity) {
        subtaskDao.delete(subtask)
        subtasksList.remove(subtask)
    }

   // suspend fun requestDeleteAllCompletedTodos() {
     //   onDeleteAllCompletedRequest?.invoke()
   // }

    suspend fun deleteAllCompletedTodos() {
        val completedTodos = todosList.filter { it.completed == "true" }
        completedTodos.forEach { completedTodo ->
            // Delete associated subtasks for each completed Todo
            val relatedSubtasks = subtasksList.filter { it.todoId == completedTodo.id.toString() }
            relatedSubtasks.forEach { subtask ->
                deleteSubtask(subtask)
            }
            // Delete the completed Todo
            deleteTodo(completedTodo)
        }
    }

    suspend fun updateSubtasks(subtaskEntities: List<SubtaskEntity>) {
        //Log.i("persistence-logs", "after clearing " + subtasksList.toList())
        subtaskEntities.forEach { entity ->
            subtaskDao.updateOrInsert(entity)

            // Refresh list
            if (!subtasksList.contains(entity))
                subtasksList.add(entity)
        }
        Log.i("persistence-logs", "subtasks in viewmodel " + subtasksList.toList())
    }


    suspend fun updateGroupVisibility(group: TodoGroupEntity, visible: Boolean) {
        val index = todoGroupsList.indexOf(group)
        if (index != -1) {
            val updatedGroup = TodoGroupEntity(
                group.name, group.iconName, visible.toString()
            )
            todoGroupsList[index] = updatedGroup
            groupDao.update(updatedGroup)
        } else {
            Log.i("persistence-logs", "could not find group $group to update visibility!")
        }
    }
    suspend fun deleteGroup(groupName: String) {

        // Delete all _Todos in the group, and their subtasks
        val todosInGroup = todosList.filter { it.group == groupName }
        todosInGroup.forEach {todo ->
            deleteTodo(todo)
            subtasksList.filter { it.todoId == todo.id.toString() }.forEach {
                deleteSubtask(it)
            }
        }

        // Delete the group from the database
        groupDao.deleteGroup(groupName)
        // Update the in-memory list
        todoGroupsList.removeIf { it.name == groupName }
    }

    suspend fun insertTodoGroup(group: TodoGroupEntity) {

        // Insert Group
        groupDao.insert(group)

        // Update list
        todoGroupsList.add(group)
        Log.i("persistence-logs", "after group insert: " + todoGroupsList.toList())
    }

    suspend fun updateTodoGroups(groupEntities: List<TodoGroupEntity>) {
        groupEntities.forEach { entity ->
            groupDao.updateOrInsert(entity)

            // Refresh list
            if (!todoGroupsList.contains(entity))
                todoGroupsList.add(entity)
        }
        Log.i("persistence-logs", "groups in viewmodel " + todoGroupsList.toList())

    }

    // The following code is provided in week 9
    var currentLocation: Location? by mutableStateOf<Location?>(null)
        private set
    var currentLatitude by mutableStateOf<Double?>(null)
        private set
    var currentLongitude by mutableStateOf<Double?>(null)
        private set

    private fun _setLocation(newLocation: Location?) {
        currentLocation = newLocation
        currentLatitude = currentLocation?.latitude ?: null
        currentLongitude = currentLocation?.longitude ?: null
    }

    fun updateLocation(newLocation: Location) {
        _setLocation(newLocation)
    }

    // Checking for due reminders and nearby _Todos every minute
    init {
        viewModelScope.launch {
            while (isActive) {
                Log.i("notification-logs", "checking...")
                checkForDueReminders()
                checkForNearbyTodos()
                delay(60000) // Checks every minute
            }
        }
    }
    // Maintain IDs of due _Todos that have been notified,
    // so as not to repeat notification
    private val notifiedReminderTodos = mutableSetOf<Int>()
    private val toleranceInMillis: Long = 60000 // Tolerance of 1 minute in milliseconds for reminder
    private val oneDayInMillis = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
    private val twoDaysInMillis = 2 * oneDayInMillis // 2 days in milliseconds

    private fun checkForDueReminders() {
        val currentDateTimeString = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(currentDateTimeString)

        todosList.forEach { todo ->
            if (todo.completed != "true" && !notifiedReminderTodos.contains(todo.id) && todo.userDateTime != "T" && currentDateTime != null) {
                val reminderTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(todo.userDateTime)
                if (reminderTime != null) {
                    val timeDifference = abs(reminderTime.time - currentDateTime.time)
                    when {
                        timeDifference in 0..toleranceInMillis -> {
                            // Immediate reminder
                            val reminderMessage = "${todo.name}: Due ${getFormattedReminderTime(reminderTime, currentDateTime)}"
                            onReminderDue(todo.id, reminderMessage)
                            notifiedReminderTodos.add(todo.id)
                        }
                        timeDifference in oneDayInMillis..(oneDayInMillis + toleranceInMillis) -> {
                            // Reminder for 24 hours before
                            onReminderDue(todo.id, "${todo.name}: Reminder in 24 hours")
                        }
                        timeDifference in twoDaysInMillis..(twoDaysInMillis + toleranceInMillis) -> {
                            // Reminder for 2 days before
                            onReminderDue(todo.id, "${todo.name}: Reminder in 2 days")
                        }
                    }
                }
            }
        }
    }

    private fun getFormattedReminderTime(reminderTime: Date, currentDateTime: Date): String {
        val duration = Duration.between(currentDateTime.toInstant(), reminderTime.toInstant())

        return when {
            duration.toDays() >= 1 -> SimpleDateFormat("d MMM 'at' HH:mm", Locale.getDefault()).format(reminderTime)
            duration.toHours() > 0 -> "in ${duration.toHours()} hours"
            duration.toMinutes() > 0 -> "in ${duration.toMinutes()} minutes"
            else -> "Now"
        }
    }

    // Will be set during onCreate
    var onReminderDue: (Int, String) -> Unit = { _, _ -> }

    // Maintain IDs of nearby _Todos that have been notified,
    // so as not to repeat notification
    private val notifiedLocationTodos = mutableSetOf<Int>()
    private fun checkForNearbyTodos() {

        // Ensure current location is accessible
        if (currentLocation != null && currentLongitude != null && currentLatitude != null) {

            todosList.forEach {todo ->

                // If user has specified a location, check if notification should be shown
                // Don't show notifications if _Todo is completed or if notification has been sent before
                if (todo.userLocation.isNotEmpty() && todo.completed != "true" && !notifiedLocationTodos.contains(todo.id)) {

                    // Get latitude and longitude of user selected location
                    val userLatitude = todo.userLocation.split(',')[0].toDouble()
                    val userLongitude = todo.userLocation.split(',')[1].toDouble()

                    Log.i("location-notification", "current latitude of $currentLatitude current longitude $currentLongitude")
                    Log.i("location-notification", "user latitude of $userLatitude user longitude $userLongitude")


                    // Calculate distance between current and user selected location
                    val results = FloatArray(3)
                    Location.distanceBetween(currentLatitude!!,
                        currentLongitude!!,userLatitude,userLongitude, results)
                    Log.i("location-notification", "distance to ${todo.name} is ${results[0]} | notification distance ${todo.notificationDistance}")

                    if (results[0] <= todo.notificationDistance) {
                        onNearbyTodo("Todo nearby: ${todo.name}!!")
                        notifiedLocationTodos.add(todo.id)
                    }
                }
            }
        }
    }

    // Will be set during onCreate
    var onNearbyTodo: (String) -> Unit = { _ -> }
}