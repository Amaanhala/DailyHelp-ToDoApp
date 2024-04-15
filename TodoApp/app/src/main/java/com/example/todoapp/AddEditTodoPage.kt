package com.example.todoapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.todoapp.entities.Converters
import com.example.todoapp.entities.SubtaskEntity
import com.example.todoapp.entities.TodoEntity
import com.example.todoapp.viewmodels.AddTodoViewModel
import com.example.todoapp.viewmodels.TodosViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Constant padding values
object Dimensions {
    val PADDING = 25.dp
    val TEXT_PADDING = 10.dp
}



@Composable
fun AddTodoFloat(todosViewModel: TodosViewModel, addTodoViewModel: AddTodoViewModel, openMaps: (String, String)->Unit) {
    if (addTodoViewModel.showDialog) {
        Dialog(onDismissRequest = {
            addTodoViewModel.updateShowDialog(false)
            addTodoViewModel.clear()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ), content = {
                AddTodoDialog(todosViewModel, addTodoViewModel, openMaps) {
                    addTodoViewModel.updateShowDialog(false)
                    addTodoViewModel.clear()
                }
            }
        )
    }
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        FloatingActionButton(onClick = { addTodoViewModel.updateShowDialog(true) },
            modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd)
        ) { Icon(imageVector = Icons.Default.Add, contentDescription = null) }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AddTodoDialog(todosViewModel: TodosViewModel, addTodoViewModel: AddTodoViewModel, openMap: (String, String)->Unit, closeDialog: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var templatesExpanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf("Templates") }

    val templatesList = listOf("Air Travel Bookings", "Winter Trip Packing",
            "City Exploration Plan",
            "Museum Tour",
            "Nature Trails and Parks",
            "Culinary Adventures",
            "Relaxation Retreat",
            "Historical Journey",
            "Art and Culture Immersion",
        "Outdoor Expedition")
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        Column(
            modifier = Modifier
                .padding(Dimensions.PADDING)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = when (addTodoViewModel.toEditTodo) {
                            null -> stringResource(R.string.new_task_label)
                            else -> stringResource(R.string.edit_task_label) },
                    modifier = Modifier.padding(0.dp, 10.dp, 0.dp, 0.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    scope.launch {
                        val coordinatesPattern = """^[-+]?([1-8]?\d(\.\d+)?|90(\.0+)?),\s*[-+]?((1[0-7]\d(\.\d+)?)|([1-9]?\d(\.\d+)?)|180(\.0+)?)$""".toRegex()

                        if (addTodoViewModel.taskName.isBlank()) {
                            snackbarHostState.showSnackbar("Please enter a task name!")
                        } else if (!coordinatesPattern.matches(addTodoViewModel.userLocation) && addTodoViewModel.userLocation.isNotEmpty()) {
                            snackbarHostState.showSnackbar("Invalid coordinates format for Location.")
                        } else if ((addTodoViewModel.selectedDate.isEmpty() && addTodoViewModel.selectedTime.isNotEmpty()) ||
                            (addTodoViewModel.selectedDate.isNotEmpty() && addTodoViewModel.selectedTime.isEmpty())) {
                            snackbarHostState.showSnackbar("Please set both Reminder date and time.")
                        } else {
                            addOrEditTodo(todosViewModel, addTodoViewModel)
                            closeDialog()
                        }
                    }
                }) {
                    Text("Save", fontSize = 15.sp)
                }
            }

            // If creating new _Todo, display templates
            // Templates are hard coded _Todos for the user to copy and fill in further
            // Recommended to fold the below if statement, to improve readability
            if (addTodoViewModel.toEditTodo == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        // DropdownMenu toggle button
                        FilledTonalButton(
                            onClick = { templatesExpanded = true }
                        ) {
                            Text(selectedTemplate)
                        }
                        DropdownMenu(expanded = templatesExpanded, onDismissRequest = { templatesExpanded = false }
                        ) {
                            templatesList.forEach { template ->
                                DropdownMenuItem(
                                    text = { Text(template) },
                                    onClick = {
                                        selectedTemplate = template

                                        // Create empty _Todo that will be filled with template values
                                        var templateTodo = TodoEntity(
                                            "temp", "", "", "", "", "",
                                            "", 0f, "", "false", "false", ""
                                        )
                                        val templateSubtasks = mutableListOf<SubtaskEntity>()

                                        if (template == "Air Travel Bookings") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "Important tasks for air travelling!",
                                                priority = "MEDIUM",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(

                                                    // id of parent _Todo is irrelevant as
                                                    // subtasks are temporary
                                                    todoId = "0",
                                                    name = "Book Airplane Tickets",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Book Hotel",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Rent Car",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Winter Trip Packing") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "What clothes to pack for trips made during the winter" +
                                                        " season (Dec-Mar) ",
                                                priority = "LOW",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Warm Clothes (e.g. jacket, pants, thermal tops)",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Thermos Flask",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Max-Sized Backpack",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "City Exploration Plan") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "A day-by-day itinerary to explore the top sights and hidden gems of the city.",
                                                priority = "HIGH",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Visit historical landmarks",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Try local cuisine",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Explore local markets",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Attend a cultural event",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Museum Tour") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "Plan visits to renowned museums and galleries for a dose of art and history.",
                                                priority = "MEDIUM",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Research must-see exhibits",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Check for free entry days",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Plan visit timings and breaks",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Nature Trails and Parks") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "A checklist for outdoor enthusiasts to explore nature trails and parks.",
                                                priority = "HIGH",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Pack hiking essentials",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Download trail maps",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Check weather conditions",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Culinary Adventures") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "Discover and savor the unique flavors of the city's cuisine.",
                                                priority = "MEDIUM",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Reserve tables at top-rated restaurants",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Attend a cooking class",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Visit a local farmer's market",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Relaxation Retreat") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "Plan a relaxing retreat to rejuvenate with spa visits and serene settings.",
                                                priority = "LOW",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Book a spa day",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Find a quiet beach or park",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Schedule a meditation session",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Historical Journey") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "Embark on a journey through the city's rich history with visits to significant landmarks.",
                                                priority = "HIGH",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Explore ancient ruins",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Take a guided historical tour",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Visit war memorials and museums",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Art and Culture Immersion") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "Immerse yourself in the local art scene and cultural heritage.",
                                                priority = "MEDIUM",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Visit contemporary art galleries",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Attend a cultural festival",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Book a night at the opera or ballet",
                                                    completed = "false"
                                                )
                                            )
                                        } else if (template == "Outdoor Expedition") {
                                            templateTodo = templateTodo.copy(
                                                name = template,
                                                description = "Plan an exhilarating outdoor adventure across the city's natural landscapes.",
                                                priority = "HIGH",
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Hike local trails",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Schedule a river rafting trip",
                                                    completed = "false"
                                                )
                                            )
                                            templateSubtasks.add(
                                                SubtaskEntity(
                                                    todoId = "0",
                                                    name = "Organize a mountain biking day",
                                                    completed = "false"
                                                )
                                            )
                                        }


                                        applyTemplate(
                                            templateTodo,
                                            templateSubtasks,
                                            addTodoViewModel
                                        )
                                        templatesExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = addTodoViewModel.taskName,
                onValueChange = { addTodoViewModel.taskName = it },
                label = { Text(stringResource(R.string.task_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = addTodoViewModel.description,
                onValueChange = { addTodoViewModel.description = it },
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier.fillMaxWidth()) // Field for _Todo description
            Divider(modifier = Modifier.fillMaxWidth().padding(8.dp))
            SubTasks(addTodoViewModel) // Allows creation of _Todo subtasks
            Images(addTodoViewModel) // Allows images to be added to _Todo
            Priorities(addTodoViewModel) // Allows setting of _Todo priority
            Groups(todosViewModel, addTodoViewModel) // Allows adding _Todo to group
            Divider(modifier = Modifier.fillMaxWidth().padding(8.dp))
            UserLocation(addTodoViewModel, todosViewModel, openMap)// Allow user to select location for _Todo by entering latitude and longitude

            Divider(modifier = Modifier.fillMaxWidth().padding(8.dp))

            // Provide option to record location and date/time of _Todo creation
            OnCreateDateTime(addTodoViewModel)
            OnCreateLocation(addTodoViewModel)

            // Allow user to specify reminder date and time for _Todo
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserDate(LocalContext.current, addTodoViewModel)
                Spacer(modifier = Modifier.weight(1f))
                UserTime(LocalContext.current, addTodoViewModel)
            }
        }
    }
}

// Adds new _Todo, or edits existing one
suspend fun addOrEditTodo(todosViewModel: TodosViewModel, addTodoViewModel: AddTodoViewModel) {
    var onCreateDateTime = "T"
    if (addTodoViewModel.recordDateTimeOnCreate) {
        // Get current date/time in ISO_LOCAL_DATE_TIME format
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR).toString()
        val currentMonth =
            calendar.get(Calendar.MONTH).toString().padStart(2, '0')
        val currentDay =
            calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val currentHour =
            calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val currentMinute =
            calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        val currentSecond =
            calendar.get(Calendar.SECOND).toString().padStart(2, '0')
        onCreateDateTime =
            "$currentYear-$currentMonth-$currentDay" + "T" + "$currentHour:$currentMinute:$currentSecond"
    }

    // Date/time specified by user
    var userDateTime = "T"

    // Check if both date and time are set or none are set
    if (addTodoViewModel.selectedDate.isNotEmpty() && addTodoViewModel.selectedTime.isNotEmpty()) {
        userDateTime = addTodoViewModel.selectedDate + "T" + addTodoViewModel.selectedTime
    } else if (addTodoViewModel.selectedDate.isEmpty() && addTodoViewModel.selectedTime.isEmpty()) {
        userDateTime = "T" // Default value if both date and time are not set
    }

    val selectedGroup = when (addTodoViewModel.selectedGroup) {
        "" -> "none"
        "Select Group" -> "none"
        else -> addTodoViewModel.selectedGroup
    }

    // Check if user has entered valid coordinates
    val coordinatesRegex = """^[-+]?([1-8]?\d(\.\d+)?|90(\.0+)?),\s*[-+]?(180(\.0+)?|((1[0-7]\d)|([1-9]?\d))(\.\d+)?)${'$'}""".toRegex()
    if (!coordinatesRegex.matches(addTodoViewModel.userLocation))
        addTodoViewModel.userLocation = ""

    var onCreateLocation = ""
    if (addTodoViewModel.recordLocationOnCreate && todosViewModel.currentLocation != null)
        onCreateLocation = "${todosViewModel.currentLatitude}, ${todosViewModel.currentLongitude}"


    // To convert uriList to String
    val converter = Converters()

    // Retrieve details from form to insert or edit _Todo
    var todo = TodoEntity(
        name = addTodoViewModel.taskName.trim(),
        description = addTodoViewModel.description,
        priority = addTodoViewModel.selectedPriority,
        onCreateDateTime = onCreateDateTime,
        userDateTime = userDateTime,
        onCreateLocation = onCreateLocation,
        userLocation = addTodoViewModel.userLocation,
        notificationDistance = addTodoViewModel.notificationDistance,
        imageUriList = converter.listToString(addTodoViewModel.imageUriList),
        completed = "false",
        favorite = "false",
        group = selectedGroup
    )

    // To be used when adding subtasks
    val todoId: Int

    // Adding new _Todo
    if (addTodoViewModel.toEditTodo == null) {

        // Insert the _Todo and get its newly inserted id
        todoId = todosViewModel.insertTodo(todo)
    } // Editing existing _Todo
    else {
        todo = todo.copy(
            id = addTodoViewModel.toEditTodo!!.id,
            favorite = addTodoViewModel.toEditTodo!!.favorite
        )
        todosViewModel.updateTodo(todo)
        todoId = addTodoViewModel.toEditTodo!!.id
    }

    // Use id of inserted _Todo to insert subtasks
    addTodoViewModel.tempSubtaskList.forEach { subtask ->

        // Add new subtask
        if (subtask.todoId == "0") {
            todosViewModel.insertSubtask(
                SubtaskEntity(
                    todoId.toString(),
                    subtask.name,
                    subtask.completed
                )
            )
        }
        // Editing existing subtask
        else {
            todosViewModel.updateSubtask(
                SubtaskEntity(
                    todoId.toString(),
                    subtask.name,
                    subtask.completed,
                    subtask.id
                )
            )
        }
    }
}

// Helper function to apply templates
fun applyTemplate(templateTodo: TodoEntity, templateSubtasks: List<SubtaskEntity>, addTodoViewModel: AddTodoViewModel) {
    addTodoViewModel.clear()
    addTodoViewModel.updateTaskName(templateTodo.name)
    addTodoViewModel.updateDescription(templateTodo.description)
    addTodoViewModel.updateSelectedPriority(templateTodo.priority)

    // Insert template subtasks, if any exist
    templateSubtasks.forEach { templateSubtask ->
        addTodoViewModel.insertTempSubtask(templateSubtask)
    }
}

// Allows creation of _Todo subtasks
@Composable
fun SubTasks(addTodoViewModel: AddTodoViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.subtasks_label),
            modifier = Modifier.padding(Dimensions.TEXT_PADDING)
        )
        IconButton(onClick = {
            addTodoViewModel.insertTempSubtask(
                SubtaskEntity(
                    // id of parent _Todo is irrelevant
                    // as subtasks are temporary
                    "0", "Subtask ${addTodoViewModel.subtaskNumber}", "false"
                )
            )
            addTodoViewModel.incrementSubtaskNumber()
        }
        ) {
            Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "")
        }
    }
    addTodoViewModel.tempSubtaskList.forEach { subtask ->
        OutlinedTextField(
            value = subtask.name,
            onValueChange = { addTodoViewModel.updateTempSubtaskName(subtask, it) },
            label = { Text(stringResource(R.string.subtask_name_label)) },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

// Allows images to be added to _Todo
@Composable
fun Images(addTodoViewModel: AddTodoViewModel){
    val context= LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.images_label),
            modifier = Modifier.padding(Dimensions.TEXT_PADDING)
        )
        ImageBottomSheet{imageUri ->
            addTodoViewModel.imageUriList.add(imageUri)
        }
    }
    if (!addTodoViewModel.imageUriList.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagesRow(imageUriList = addTodoViewModel.imageUriList, context)
        }
    }
}

@Composable
fun ImagesRow(imageUriList:List<Uri>,context: Context){
//    var context= LocalContext.current
    LazyRow{
        items(imageUriList.size){index ->
            Column (
                modifier = Modifier.padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = getBitmap(imageUriList[index], context)!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RectangleShape)
                        .size(120.dp)
                )
            }
        }
    }
}

fun getBitmap(uri: Uri?, context: Context): Bitmap? {
    var result: Bitmap? = null
    if (uri != null) {
        val contentResolver: ContentResolver = context.contentResolver
        result = ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri)
        )
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageBottomSheet(onImageSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    //request for permission
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(false)
    }
    if (checkCameraPermission())
        hasCameraPermission = true
    else {
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted)
                hasCameraPermission = true
            else {
                Toast.makeText(context, "Camera Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        SideEffect {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    //pickImgFromCamera
    var cameraImageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var cameraImageBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    val imageFromCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captured ->
        if (!captured) {
            cameraImageUri = null
            cameraImageBitmap = null
        } else {
            cameraImageUri?.let { onImageSelected(it) }
//                Log.i("aaa","get image from camera")
        }
        Log.i("cameraImageUri", "（$captured -> $cameraImageUri）")
        Log.i("cameraImageBitmap", cameraImageBitmap.toString())
    }

    //pickImgFromGallery
    var galleryImageUri by remember { mutableStateOf<Uri?>(null) }
    var pickedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val imageFromGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null){
            galleryImageUri=null
            pickedImageBitmap = null
        } else {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
            onImageSelected(uri)
        }
        Log.i("galleryImageUri", uri.toString())
    }


    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    IconButton(onClick = { showBottomSheet = true }) {
        Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "")
    }
    if (showBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp, 16.dp, 25.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Add image",
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 20.dp)
                    )
                }
                TextButton(
                    onClick = {
                        cameraImageBitmap = null
                        cameraImageUri = FileProvider.getUriForFile(
                            context, context.packageName + ".provider", newImageFile(context)
                        )
                        imageFromCameraLauncher.launch(cameraImageUri)
                        showBottomSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "Camera")
                }
                TextButton(
                    onClick = {
                        imageFromGalleryLauncher.launch(
                            PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        showBottomSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "Gallery")
                }
            }
        }
    }
}

@Composable
fun checkCameraPermission(): Boolean {
    return PackageManager.PERMISSION_GRANTED ==
            ActivityCompat.checkSelfPermission(
                LocalContext.current,
                android.Manifest.permission.CAMERA
            )
}

fun newImageFile(context: Context): File {
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val time = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    return File.createTempFile("SNAP-$time", ".jpg", storageDir)
}

enum class Priority (val value: Int) {
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    NONE(0)
}

// Allows setting of _Todo priority
@Composable
fun Priorities(addTodoViewModel: AddTodoViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(R.string.priority_label), modifier = Modifier.padding(Dimensions.TEXT_PADDING))
        Box {
            // DropdownMenu toggle button
            FilledTonalButton(onClick = { expanded = true }
            ) {
                Text(addTodoViewModel.selectedPriority)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }
            ) {
                Priority.values().forEach { priority ->
                    DropdownMenuItem(
                        text = { Text(priority.name) },
                        onClick = {
                            addTodoViewModel.updateSelectedPriority(priority.name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Allows adding _Todo to group
@Composable
fun Groups(todosViewModel: TodosViewModel, addTodoViewModel: AddTodoViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(R.string.group_label), modifier = Modifier.padding(Dimensions.TEXT_PADDING))
        Box {
            // DropdownMenu toggle button
            FilledTonalButton(onClick = { expanded = true }) {
                Text(addTodoViewModel.selectedGroup)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }
            ) {
                todosViewModel.todoGroupsList.forEach { group ->
                    DropdownMenuItem(text = { Text(group.name) },
                        onClick = {
                            addTodoViewModel.updateSelectedGroup(group.name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Allow user to select location by entering latitude and longitude
@Composable
fun UserLocation(addTodoViewModel: AddTodoViewModel, todosViewModel: TodosViewModel, openMap: (String, String) -> Unit) {
    val context = LocalContext.current
    Column (horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = addTodoViewModel.userLocation,
            onValueChange = { addTodoViewModel.updateUserLocation(it)},
            label = { Text(text = stringResource(R.string.user_location_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(
                onClick = {
                    if (todosViewModel.currentLocation != null)
                        openMap(
                            todosViewModel.currentLatitude.toString(),
                            todosViewModel.currentLongitude.toString()
                        )
                    else
                        Toast.makeText(context,"Error: Can't get current location!",Toast.LENGTH_LONG).show()
                }
            ) {
                Text("Open Map")
            }
        }
        Text("Notification Distance: %.2fm".format(addTodoViewModel.notificationDistance))
        Slider(
            value = addTodoViewModel.notificationDistance,
            onValueChange = { addTodoViewModel.updateNotificationDistance(it)},
            valueRange = 0f..1000f
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "0")
            Text(text = "1km")
        }
    }
}

// Provide option to record date/time of _Todo creation
@Composable
fun OnCreateDateTime(addTodoViewModel: AddTodoViewModel) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("Remember Current Date/Time", modifier = Modifier.padding(Dimensions.TEXT_PADDING))
        Switch(checked = addTodoViewModel.recordDateTimeOnCreate,
            onCheckedChange = { addTodoViewModel.updateRecordDateTimeOnCreate(it) })
    }
}

// Provide option to record location of _Todo creation
@Composable
fun OnCreateLocation(addTodoViewModel: AddTodoViewModel) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("Remember Current Location", modifier = Modifier.padding(Dimensions.TEXT_PADDING))
        Switch(checked = addTodoViewModel.recordLocationOnCreate,
            onCheckedChange = { addTodoViewModel.updateRecordLocationOnCreate(it) })
    }
}

// Allow user to specify reminder date for _Todo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDate(context: Context, addTodoViewModel: AddTodoViewModel) {
    var selected by rememberSaveable { mutableStateOf(false) }

    // Get current date
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            addTodoViewModel.updateSelectedDate("$selectedYear-${(selectedMonth + 1).toString().padStart(2,'0')}-${selectedDay.toString().padStart(2,'0')}")
        }, year, month, day
    )
    // Disable past dates
    datePickerDialog.datePicker.minDate = calendar.timeInMillis
    FilterChip(onClick = {
            datePickerDialog.show()
            selected = true
        },
        label = {
            if (!selected && addTodoViewModel.selectedDate == "")
                Text("Reminder Date")
            else
                Text(addTodoViewModel.selectedDate) },
        selected = selected,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = "Open Date Picker"
            )
        }
    )
}

// // Allow user to specify reminder time for _Todo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTime(context: Context, addTodoViewModel: AddTodoViewModel) {
    var selected by rememberSaveable { mutableStateOf(false) }

    // Get current date
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour: Int, selectedMinute: Int ->
            addTodoViewModel.updatedSelectedTime("${selectedHour.toString().padStart(2,'0')}:${selectedMinute.toString().padStart(2,'0')}:00")
        }, hour, minute, false
    )

    FilterChip(
        onClick = {
            timePickerDialog.show()
            selected = true
        },
        label = {
            if (!selected && addTodoViewModel.selectedTime == "")
                Text("Reminder Time")
            else
                Text(addTodoViewModel.selectedTime)},
        selected = selected,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = "Open Time Picker"
            )
        }
    )
}


/****** Preview below this line ******/

// Separate preview composables using sample data (because viewmodel can't be instantiated in preview)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview (showSystemUi = true)
@Composable
fun AddTodoDialogPreview() {
    Scaffold {
        Column(
            modifier = Modifier
                .padding(Dimensions.PADDING)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "New Task",
                    modifier = Modifier.padding(0.dp, 10.dp, 0.dp, 0.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {}
                ) {
                    Text("Save", fontSize = 15.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // DropdownMenu toggle button
                FilledTonalButton(
                    onClick = {  }
                ) {
                    Text("Templates")
                }
            }
            OutlinedTextField(
                value = "",
                onValueChange = {  },
                label = { Text("Task Name") },
                modifier = Modifier
                    .fillMaxWidth()
            )
            OutlinedTextField(
                value = "",
                onValueChange = {  },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SubTasks",
                    modifier = Modifier.padding(Dimensions.TEXT_PADDING)
                )
                IconButton(onClick = {}
                ) {
                    Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "")
                }
            }
            OutlinedTextField(
                value = "",
                onValueChange = {  },
                label = { Text("Subtask Name") },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Images", modifier = Modifier.padding(Dimensions.TEXT_PADDING))
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "Images")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority",
                    modifier = Modifier.padding(Dimensions.TEXT_PADDING),
                )
                // DropdownMenu toggle button
                FilledTonalButton(
                    onClick = {  }
                ) {
                    Text("Select Priority")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Group",
                    modifier = Modifier.padding(Dimensions.TEXT_PADDING),
                )
                // DropdownMenu toggle button
                FilledTonalButton(
                    onClick = {  }
                ) {
                    Text("Select Group")
                }
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // Allow user to select location by entering latitude and longitude
            OutlinedTextField(
                value = "",
                onValueChange = {  },
                label = { Text(text = "Location (Copy from map pin)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = {}
                ) {
                    Text("Open Map")
                }
            }
            Column (horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Notification Distance: %.2fm".format(500F))
                Slider(
                    value = 500F,
                    onValueChange = { },
                    valueRange = 0f..1000f
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "0")
                    Text(text = "1km")
                }
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // Provide option to record location and date/time of _Todo creation
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Remember Current Date/Time", modifier = Modifier.padding(Dimensions.TEXT_PADDING))
                Switch(checked = false,
                    onCheckedChange = {  })
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Remember Current Location", modifier = Modifier.padding(Dimensions.TEXT_PADDING))
                Switch(checked = true,
                    onCheckedChange = {  })
            }

            // Show date picker and time picker in a new row
            // get initial value by current context
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    onClick = {},
                    label = { Text("Reminder Date") },
                    selected = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = "Open Date Picker"
                        )
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                FilterChip(
                    onClick = {},
                    label = { Text("Reminder Time") },
                    selected = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = "Open Time Picker"
                        )
                    }
                )
            }
        }
    }
}