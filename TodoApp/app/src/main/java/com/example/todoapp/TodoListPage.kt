package com.example.todoapp

import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.entities.Converters
import com.example.todoapp.entities.SubtaskEntity
import com.example.todoapp.entities.TodoEntity
import com.example.todoapp.viewmodels.AddTodoViewModel
import com.example.todoapp.viewmodels.SettingsViewModel
import com.example.todoapp.viewmodels.TodosViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun ToDoListContent(todosViewModel: TodosViewModel, addTodoViewModel: AddTodoViewModel, settingsViewModel: SettingsViewModel, groupName: String, openMap: (String, String)->Unit) {
    val scope = rememberCoroutineScope()
    val context= LocalContext.current
    val displayedTodos = when (groupName) {
        "All Tasks" -> todosViewModel.todosList.filter { it.completed == "false" }
        "Favorites" -> todosViewModel.todosList.filter { it.completed == "false" && it.favorite == "true" }
        "Completed" -> todosViewModel.todosList.filter { it.completed == "true" }
        else -> todosViewModel.todosList.filter { it.completed == "false" && it.group == groupName }
    }
    // Sort _Todos using custom comparators, based on settings
    var comparator = nameComparator()
    if (settingsViewModel.sortByValue == "Due Date")
        comparator = dateComparator()
    else if (settingsViewModel.sortByValue == "Priority")
        comparator = priorityComparator().reversed() // Show high-priority tasks first
    // Sort by distance only when current location is available
    else if (settingsViewModel.sortByValue == "Distance" && todosViewModel.currentLatitude != null && todosViewModel.currentLongitude != null)
        comparator = distanceComparator(todosViewModel.currentLatitude!!, todosViewModel.currentLongitude!!)

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Delete all completed tasks button
        if (groupName == "Completed") {
            item {
                val completedTasksCount = todosViewModel.todosList.count { it.completed == "true" }

                Button(
                    onClick = {
                        if (completedTasksCount > 0) {
                            showDeleteAllDialog = true
                        } else {
                            // Show a toast indicating there are no completed tasks to delete
                            Toast.makeText(context, "No completed tasks to delete.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("Delete All Completed Tasks")
                }

                // Show the confirmation dialog when requested
                if (showDeleteAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAllDialog = false },
                        title = { Text("Confirm Deletion") },
                        text = { Text("Are you sure you want to delete all completed tasks?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteAllDialog = false
                                    scope.launch {
                                        todosViewModel.deleteAllCompletedTodos()
                                    }
                                }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDeleteAllDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
        items(displayedTodos.sortedWith(comparator)) { todo ->
            var isExpanded by rememberSaveable { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Radio button for completion status
                        RadioButton(selected = todo.completed.toBoolean(),
                            onClick = { scope.launch { todosViewModel.updateCompletedTodo(todo, !todo.completed.toBoolean()) } }
                        )
                        // Display priority as exclamation marks in front of _Todo name
                        val priority = when (Priority.valueOf(todo.priority)) {
                            Priority.HIGH-> "!!!"
                            Priority.MEDIUM -> "!!"
                            Priority.LOW -> "!"
                            Priority.NONE -> ""
                        }
                        Text(text = "$priority ${todo.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.weight(1f).clickable {
                                    addTodoViewModel.updateShowDialog(true)
                                    addTodoViewModel.setToEditTodo(todo, todosViewModel)
                        })
                        Column(horizontalAlignment = Alignment.End) {
                            // IconButton for expanding/collapsing the details
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand or Collapse"
                                )
                            }
                            if (groupName != "Completed") {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            todosViewModel.updateFavoriteTodo(
                                                todo,
                                                !todo.favorite.toBoolean()
                                            )
                                        }
                                    }
                                ) {
                                    if (todo.favorite.toBoolean()) {
                                        Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                                    } else {
                                        Icon(
                                            Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Favorite"
                                        )
                                    }
                                }
                            } else {
                                // If completed tasks are listed, provide option to delete _Todo
                                DeleteTodoIcon {
                                    // Delete _Todo and associated subtasks
                                    todosViewModel.subtasksList.forEach { subtask ->
                                        if (subtask.todoId == todo.id.toString()) {
                                            scope.launch {
                                                todosViewModel.deleteSubtask(subtask)
                                            }
                                            Log.i("persistence-logs", "deleting subtask $subtask")
                                        }
                                    }
                                    Log.i("persistence-logs", "deleting todo $todo")
                                    scope.launch {
                                        todosViewModel.deleteTodo(todo)
                                    }
                                }
                            }
                        }
                    }
                }
                // Display _Todo details, if any
                if (todo.description.isNotEmpty()) {
                    Text(
                        text = todo.description,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Light,
                        maxLines = when (settingsViewModel.descriptionValue) {
                            "1 Line" -> 1
                            "2 Lines" -> 2
                            "3 Lines" -> 3
                            else -> 1
                        },
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 'T' signifies empty date
                if (todo.userDateTime != "T") {
                    Text(
                        text = "Due Date: ${todo.userDateTime.replace("T", ", ")}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Light
                    )
                }
                if (todo.onCreateDateTime != "T") {
                    Text(
                        text = "Created on: ${todo.onCreateDateTime.replace("T", ", ")}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Light
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                // Further _Todo details can be hidden and displayed on demand
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Priority: ${todo.priority}", fontWeight = FontWeight.Light)
                        // Two buttons for opening map on userLocation and onCreateLocation
                        Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                            if (todo.userLocation != "") { MapButton("Specified Location", todo.userLocation , openMap) }
                            if (todo.onCreateLocation != "") { MapButton("On-Create Location", todo.onCreateLocation, openMap) }
                        }
                        // Use type converter to convert from String to list of uri
                        val converter = Converters()
                        ImagesRow(imageUriList = converter.stringToList(todo.imageUriList), context)
                        todosViewModel.subtasksList.filter { it.todoId == todo.id.toString() }.forEach { subtask ->
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = subtask.completed.toBoolean(),
                                    onCheckedChange = { isChecked ->
                                        scope.launch { todosViewModel.updateCompletedSubtask(subtask, isChecked) }
                                    })
                                Text(subtask.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider(modifier = Modifier.padding(start = 32.dp, top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteTodoIcon(deleteTodo: () -> Unit) {
    var openDialog by remember { mutableStateOf(false) }
    if (openDialog) {
        DeleteTodoDialog(onDismissRequest = { openDialog = false},
            onConfirmation = {
                openDialog = false
                deleteTodo()
            })
    }

    IconButton(onClick = { openDialog = true }) {
        Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Delete")
    }
}

@Composable
fun DeleteTodoDialog(onDismissRequest: () -> Unit, onConfirmation: () -> Unit) {
    AlertDialog(
        icon = {
            Icon(Icons.Filled.Warning, contentDescription = "Warning")
        },
        title = {
            Text(text = stringResource(R.string.delete_todo_label))
        },
        text = {
            Text(text = stringResource(R.string.permanent_action_label))
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(stringResource(R.string.confirm_label))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.dismiss_label))
            }
        }
    )
}


@Composable
fun MapButton(buttonText: String, location: String, openMap: (String, String)->Unit) {
    val latitude = location.split(",")[0]
    val longitude = location.split(",")[1]
    ElevatedButton(
        onClick = {
            openMap(latitude,longitude)
        }
    ) {
        Text(buttonText)
    }
}

fun nameComparator() = Comparator<TodoEntity> { todo1, todo2 ->
    todo1.name.compareTo(todo2.name, ignoreCase = true)
}

fun dateComparator() = Comparator<TodoEntity> { todo1, todo2 ->
    var todo1DateTime: LocalDateTime = LocalDateTime.MIN
    var todo2DateTime = LocalDateTime.MIN

    // Parse non-empty dates using ISO_LOCAL_DATE_TIME format
    if (todo1.userDateTime != "T")
        todo1DateTime = LocalDateTime.parse(todo1.userDateTime)
    if (todo2.userDateTime != "T")
        todo2DateTime = LocalDateTime.parse(todo2.userDateTime)

    Log.i("datelog", "todo1DateTime: $todo1DateTime")
    Log.i("datelog", "todo2DateTime: $todo2DateTime")

    // 'T' means no date or time was specified
    when {
        (todo1.userDateTime == "T" && todo2.userDateTime != "T") -> 1
        (todo1.userDateTime != "T" && todo2.userDateTime == "T") -> -1
        (todo1.userDateTime == "T" && todo2.userDateTime == "T") -> 0
        (todo1DateTime.isBefore(todo2DateTime)) -> -1
        (todo1DateTime.isAfter(todo2DateTime)) -> 1
        else -> 0
    }
}

fun priorityComparator() = Comparator<TodoEntity> { todo1, todo2 ->
    val todo1Priority = Priority.valueOf(todo1.priority).value
    val todo2Priority = Priority.valueOf(todo2.priority).value

    todo1Priority.compareTo(todo2Priority)
}

fun distanceComparator(currentLatitude: Double, currentLongitude: Double) = Comparator<TodoEntity> { todo1, todo2 ->
    val todo1Distance = FloatArray(3)
    val todo2Distance = FloatArray(3)

    // Calculate distance to location specified in first _Todo
    if (todo1.userLocation.isNotEmpty()) {
        val userLatitude = todo1.userLocation.split(',')[0].toDouble()
        val userLongitude = todo1.userLocation.split(',')[1].toDouble()
        Location.distanceBetween(currentLatitude,
            currentLongitude,userLatitude,userLongitude, todo1Distance)
    }

    // Repeat for second _Todo
    if (todo2.userLocation.isNotEmpty()) {
        val userLatitude = todo2.userLocation.split(',')[0].toDouble()
        val userLongitude = todo2.userLocation.split(',')[1].toDouble()
        Location.distanceBetween(currentLatitude,
            currentLongitude,userLatitude,userLongitude, todo2Distance)
    }

    when {
        (todo1.userLocation.isEmpty() && todo2.userLocation.isNotEmpty()) -> 1
        (todo1.userLocation.isNotEmpty() && todo2.userLocation.isEmpty()) -> -1
        (todo1.userLocation.isEmpty() && todo2.userLocation.isEmpty()) -> 0
        (todo1Distance[0] < todo2Distance[0]) -> -1
        (todo1Distance[0] > todo2Distance[0]) -> 1
        else -> 0
    }
}


/****** Previews below this line ******/

// Separate preview composables using sample data (because viewmodel can't be instantiated in preview)
@Preview(showSystemUi = true)
@Composable
fun ToDoListContentPreview(
    todosList: List<TodoEntity> = listOf(
//        TodoEntity("Task 1", "Description for task 1", "High", "2023-11-19", "2023-11-20", "Office", "Location 1", 10, androidx.core.R.drawable.notification_bg, "false", "true", "Work", 1),
        TodoEntity(
            "Task 1",
            "Description for task 1",
            "HIGH",
            "2023-11-19, 09:52:00",
            "2023-11-20, 23:10:23",
            "Office",
            "Location 1",
            10f,
            "",
            "false",
            "true",
            "Work",
            1
        ),
//        TodoEntity("Task 2", "Description for task 2", "Medium", "2023-11-20", "2023-11-21", "Home", "Location 2", 15, androidx.core.R.drawable.notification_bg, "true", "false", "Personal", 2)
        TodoEntity(
            "Task 2",
            "Description for task 2",
            "LOW",
            "2023-11-20, 6:23:33",
            "2023-11-21, 15:44:00",
            "Home",
            "Location 2",
            15f,
            "",
            "true",
            "false",
            "Personal",
            2
        )
    ),
    subtasksList: List<SubtaskEntity> = listOf(
        SubtaskEntity("1", "Subtask 1", "true"),
        SubtaskEntity("1", "Subtask 2", "false"),
        SubtaskEntity("2", "Subtask 1", "true")
    )
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(todosList) { todo ->
            var isExpanded by rememberSaveable { mutableStateOf(true) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = todo.completed.toBoolean(),
                            onClick = { /* Mock onClick */ }
                        )
                        // Display priority as exclamation marks in front of _Todo name
                        val priority = when (todo.priority) {
                            "HIGH" -> "!!!"
                            "MEDIUM" -> "!!"
                            "LOW" -> "!"
                            else -> ""
                        }
                        Text(
                            text = "$priority ${todo.name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand or Collapse"
                                )
                            }
                            IconButton(
                                onClick = { /* Mock onClick */ }
                            ) {
                                if (todo.favorite.toBoolean()) {
                                    Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                                } else {
                                    Icon(
                                        Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite"
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = todo.description,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Light,
                        maxLines = 1
                    )
                    Text(
                        text = "Due Date: ${todo.userDateTime}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = "Created on: ${todo.onCreateDateTime}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Light
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding()){
                            Text(text = "Priority: ${todo.priority}", fontWeight = FontWeight.Light, modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp))
                            Row (modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween){
                                ElevatedButton(onClick = {}) {
                                    Text("Specified Location")
                                }
                                ElevatedButton(onClick = {}) {
                                    Text("On-Create Location")
                                }
                            }
                            subtasksList.forEach { subtask ->
                                if (subtask.todoId == todo.id.toString()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = subtask.completed.toBoolean(),
                                            onCheckedChange = { /* Mock onCheckedChange */ }
                                        )
                                        Text(
                                            subtask.name,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Divider(modifier = Modifier.padding(start = 32.dp, top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun DeleteTodoDialogPreview() {
    AlertDialog(
        icon = {
            Icon(Icons.Filled.Warning, contentDescription = "Warning")
        },
        title = {
            Text(text = "Delete Todo?")
        },
        text = {
            Text(text = "This action will be permanent")
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = {}
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {}
            ) {
                Text("Dismiss")
            }
        }
    )
}

