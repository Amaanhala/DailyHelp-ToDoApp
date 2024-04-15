package com.example.todoapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.AirplaneTicket
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.data.TodoAppDatabase
import com.example.todoapp.entities.TodoEntity
import com.example.todoapp.entities.TodoGroupEntity
import com.example.todoapp.services.GeoLocationService
import com.example.todoapp.ui.theme.TodoAppTheme
import com.example.todoapp.viewmodels.AddTodoViewModel
import com.example.todoapp.viewmodels.SettingsViewModel
import com.example.todoapp.viewmodels.TodosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermission()) {
            Log.i("location-logs", "no permission, requesting")

            requestFineLocationPermission()
        }
        setContent {
            // Get viewmodels and DAOs
            val todosViewModel = viewModel<TodosViewModel>()
            val settingsViewModel = viewModel<SettingsViewModel>()
            val addTodoViewModel = viewModel<AddTodoViewModel>()
            val todoDao = TodoAppDatabase.getDB(LocalContext.current).todoDao()
            val subtaskDao = TodoAppDatabase.getDB(LocalContext.current).subtaskDao()
            val groupDao = TodoAppDatabase.getDB(LocalContext.current).groupDao()
            val settingsDAO = TodoAppDatabase.getDB(LocalContext.current).settingsDao()
            GeoLocationService.locationViewModel = todosViewModel

            // Load data from database
            LaunchedEffect("GET_ALL_TODOS") {
                todoDao.getAll().distinctUntilChanged().collectLatest { todoEntities ->
                    todosViewModel.updateTodos(todoEntities)
                }
            }
            LaunchedEffect("GET_ALL_SUBTASKS") {
                subtaskDao.getAll().distinctUntilChanged().collectLatest { subtaskEntities ->
                    todosViewModel.updateSubtasks(subtaskEntities)
                }
            }
            LaunchedEffect("GET_ALL_GROUPS") {
                groupDao.getAll().distinctUntilChanged().collectLatest { groupEntities ->
                    todosViewModel.updateTodoGroups(groupEntities)
                }
            }
            LaunchedEffect("GET_ALL_SETTINGS") {
                settingsDAO.getAllSettings().distinctUntilChanged().collectLatest { settingsEntities ->
                    settingsViewModel.updateSettings(settingsEntities)
                }
            }
            TodoAppTheme {
                Surface {
                    TodoScaffold(todosViewModel, addTodoViewModel, settingsViewModel, ::openMap)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        @SuppressLint("MissingPermission")
        if (hasPermission()) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            Log.i("location-logs", "last known loc: $location")
            if (location != null) {
                GeoLocationService.updateLatestLocation(location)
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, GeoLocationService)
        }
    }

    override fun onPause() {
        super.onPause()
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(GeoLocationService)
    }

    private val GPS_LOCATION_PERMISSION_REQUEST = 1
    private fun requestFineLocationPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION),
            GPS_LOCATION_PERMISSION_REQUEST)
    }

    private fun hasPermission(): Boolean {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun openMap(lat: String, lon: String) {
        val intentUri = Uri.parse("geo:$lat,$lon")
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }
}

@Composable
fun TodoScaffold(todosViewModel: TodosViewModel, addTodoViewModel: AddTodoViewModel, settingsViewModel: SettingsViewModel, openMap: (String, String)->Unit) {
    val context = LocalContext.current

    if (settingsViewModel.notificationsEnabled) {
        Log.i("notification-logs", "enabled notifications")
        todosViewModel.onReminderDue = { _, message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
        todosViewModel.onNearbyTodo = { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    } else {
        Log.i("notification-logs", "disabled notifications")
        todosViewModel.onReminderDue = { _, _ ->}
        todosViewModel.onNearbyTodo = { _ -> }
    }

    var selectedScreenID by rememberSaveable { mutableStateOf(ScreenID.HOME) }

    // Will be used to determine the group from which _Todos will be displayed
    var selectedGroupName by rememberSaveable { mutableStateOf("") }


    // When a group is clicked, display the list of todos that belong to that group
    val onGroupClick: (String) -> Unit = { groupName ->
        selectedGroupName = groupName
        selectedScreenID = ScreenID.TODOLIST
    }

    Scaffold(
        topBar = { TitleBar(selectedScreenID, selectedGroupName, settingsViewModel, todosViewModel) },
        bottomBar = {
            NavigationComponents(selectedScreenID, updateSelected = { selectedScreenID = it })
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            when (selectedScreenID) {
                ScreenID.HOME -> MainScreen(todosViewModel, onGroupClick)
                ScreenID.TODOLIST -> {
                    ToDoListContent(todosViewModel, addTodoViewModel, settingsViewModel, selectedGroupName, openMap)
                }
                ScreenID.ALLTASKS -> {
                    selectedGroupName = "All Tasks"
                    ToDoListContent(todosViewModel, addTodoViewModel, settingsViewModel, selectedGroupName, openMap)
                }
                ScreenID.FAVORITES -> {
                    selectedGroupName = "Favorites"
                    ToDoListContent(todosViewModel, addTodoViewModel, settingsViewModel, selectedGroupName, openMap)
                }
                ScreenID.COMPLETED -> {
                    selectedGroupName = "Completed"
                    ToDoListContent(todosViewModel, addTodoViewModel, settingsViewModel, selectedGroupName, openMap)
                }
                ScreenID.SETTINGS -> SettingsContent(settingsViewModel)
            }
            AddTodoFloat(todosViewModel, addTodoViewModel, openMap)
        }
    }
}

@Composable
fun MainScreen(todosViewModel: TodosViewModel, onGroupClick: (String) -> Unit) {
    var showGroupDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Display groups for all tasks, favorite tasks, and completed tasks
            Text(stringResource(R.string.default_groups_label), style = MaterialTheme.typography.titleLarge)
            GroupCard(todosViewModel, "All Tasks", onGroupClick)
            Spacer(modifier = Modifier.height(16.dp))
            GroupCard(todosViewModel, "Favorites", onGroupClick)
            Spacer(modifier = Modifier.height(16.dp))
            GroupCard(todosViewModel, "Completed", onGroupClick)

            // Display custom groups header with add button on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.custom_groups_label), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showGroupDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Group")
                }
            }

            // Display custom groups
            todosViewModel.todoGroupsList.forEach { group ->
                if (group.visible == "true") {
                    GroupCard(
                        todosViewModel = todosViewModel,
                        groupName = group.name,
                        onGroupClick = onGroupClick,
                        onDeleteGroup = { groupName ->
                            CoroutineScope(Dispatchers.Main).launch {
                                todosViewModel.deleteGroup(groupName)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showGroupDialog) {
        AddGroupDialog(todosViewModel = todosViewModel, onDismiss = { showGroupDialog = false })
    }
}

@Composable
fun GroupCard(todosViewModel: TodosViewModel, groupName: String, onGroupClick: (String) -> Unit, onDeleteGroup: ((String) -> Unit)? = null) {


    // Count number of tasks belonging to the group
    val taskNumber = when (groupName) {

        // Count all non-completed tasks
        "All Tasks" -> {
            // Ensure at least one element matches the predicate, otherwise exception is thrown
            if (todosViewModel.todosList.firstOrNull { it.completed == "false" } != null)
                todosViewModel.todosList.count { it.completed == "false" }
            else
                0
        }

        // Count all non-completed favorite tasks
        "Favorites" -> {
            if (todosViewModel.todosList.firstOrNull { it.completed == "false" && it.favorite == "true" } != null)
                todosViewModel.todosList.count { it.completed == "false" && it.favorite == "true" }
            else
                0
        }

        // Count all completed tasks
        "Completed" -> {
            if (todosViewModel.todosList.firstOrNull { it.completed == "true" } != null)
                todosViewModel.todosList.count { it.completed == "true" }
            else
                0
        }

        // Count custom group tasks
        else -> {
            if (todosViewModel.todosList.firstOrNull { it.completed == "false" && it.group == groupName } != null)
                todosViewModel.todosList.count { it.completed == "false" && it.group == groupName }
            else
                0
        }
    }

    // Determine the group icon
    var icon = Icons.Outlined.List
    if (groupName == "All Tasks") {
        icon = Icons.Outlined.List
    } else if (groupName == "Favorites") {
        icon = Icons.Outlined.FavoriteBorder
    } else if (groupName == "Completed") {
        icon = Icons.Outlined.Check
    }
    // Custom group
    else {
        val iconName = todosViewModel.todoGroupsList.first { it.name == groupName }.iconName
        if (iconName == "Shopping")
            icon = Icons.Outlined.ShoppingCart
        else if (iconName == "Travel")
            icon = Icons.Outlined.AirplaneTicket
        else if (iconName == "Money")
            icon = Icons.Outlined.Money
    }

    val isCustomGroup = groupName !in listOf("All Tasks", "Favorites", "Completed")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onGroupClick(groupName) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                imageVector = icon,
                contentDescription = "Group Icon",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = groupName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = taskNumber.toString(),
                style = MaterialTheme.typography.bodySmall
            )
            var showDeleteDialog by remember { mutableStateOf(false) }

            // Delete button for custom groups
            if (isCustomGroup) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Group")
                }
            }

            // Delete confirmation dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Confirm Deletion") },
                    text = { Text("Are you sure you want to delete the group $groupName? All Todos included in the group will also be deleted permanently!") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDeleteGroup?.invoke(groupName)
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AddGroupDialog(todosViewModel: TodosViewModel, onDismiss: () -> Unit) {
    var groupName by rememberSaveable { mutableStateOf("") }

    // List of default group names
    val recommendedGroupNames = listOf("Sights", "Vacation", "Work")

    // Icons choices
    val iconChoices = listOf(
        GroupIcon("List", Icons.Outlined.List),
        GroupIcon("Shopping", Icons.Outlined.ShoppingCart),
        GroupIcon("Travel", Icons.Outlined.AirplaneTicket),
        GroupIcon("Money", Icons.Filled.Money),
    )
    val (selectedIcon, onSelected) = remember { mutableStateOf(iconChoices[0]) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Scaffold(
            modifier = Modifier.height(330.dp),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.new_group_label), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (groupName.isNotBlank() &&
                                    todosViewModel.todoGroupsList.none { it.name == groupName }) {
                                    todosViewModel.insertTodoGroup(
                                        TodoGroupEntity(
                                            groupName, selectedIcon.name, "true"
                                        )
                                    )
                                    onDismiss()
                                } else {
                                    snackbarHostState.showSnackbar("Please enter a unique group name!")
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save_button), fontSize = 15.sp)
                    }
                }

                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 5.dp))

                Text(stringResource(R.string.recommended_group_names_label), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    recommendedGroupNames.forEach { name ->
                        FilledTonalButton(onClick = { groupName = name }) {
                            Text(name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Group Name input
                OutlinedTextField(
                    label = { Text(stringResource(R.string.group_name_label)) },
                    value = groupName,
                    onValueChange = { groupName = it }
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(stringResource(R.string.group_icon_label), fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    iconChoices.forEach {
                        Icon(it.icon, it.name, modifier = Modifier.padding(start = 5.dp))
                        RadioButton(
                            selected = (it == selectedIcon),
                            onClick = { onSelected(it) }
                        )
                    }
                }
            }
        }
    }
}

// Data class to be used for group icon selection
data class GroupIcon(
    val name: String,
    val icon: ImageVector
)


// Enum for screen state
enum class ScreenID {
    HOME, TODOLIST, ALLTASKS, FAVORITES, COMPLETED, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(screen: ScreenID, selectedGroupName: String, settingsViewModel: SettingsViewModel, todosViewModel: TodosViewModel) {
    var sortExpanded by remember { mutableStateOf(false) }
    val sortList = listOf("Name", "Due Date", "Priority", "Distance")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = {
            Text(
                when (screen) {
                    ScreenID.HOME -> "Home"
                    ScreenID.TODOLIST -> selectedGroupName
                    ScreenID.ALLTASKS -> "All Tasks"
                    ScreenID.FAVORITES -> "Favorites"
                    ScreenID.COMPLETED -> "Completed"
                    ScreenID.SETTINGS -> "Settings"
                }
            )
        },
        actions = {
            if (screen != ScreenID.HOME && screen != ScreenID.SETTINGS)
                Box {
                    IconButton(onClick = { sortExpanded = true }) {
                        Icon(Icons.Filled.Checklist, contentDescription = "Sorting Option")
                    }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        sortList.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text("Sort By $sort") },
                                onClick = {
                                    settingsViewModel.sortByValue = sort
                                    sortExpanded = false
                                    scope.launch { settingsViewModel.updateSortBy(sort) }
                                    if (sort == "Distance" && todosViewModel.currentLocation == null)
                                        Toast.makeText(context, "Can't get current location!!",
                                            Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }
        }
    )
}

// Data class for navigation bar components
data class NavigationComponent(
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val screenID: ScreenID
) {
    @Composable
    fun IconStyled(selectedScreenID: ScreenID) {
        if (selectedScreenID == screenID)
            Icon(filledIcon, contentDescription = title)
        else
            Icon(outlinedIcon, contentDescription = title)
    }
}

val navigationList = listOf(
    NavigationComponent("Home", Icons.Filled.Home, Icons.Outlined.Home, ScreenID.HOME),
    NavigationComponent(
        "All",
        Icons.Filled.ViewList,
        Icons.Outlined.ViewList,
        ScreenID.ALLTASKS
    ),
    NavigationComponent(
        "Favorites",
        Icons.Filled.Favorite,
        Icons.Outlined.FavoriteBorder,
        ScreenID.FAVORITES
    ),
    NavigationComponent(
        "Done",
        Icons.Filled.CheckCircle,
        Icons.Outlined.CheckCircle,
        ScreenID.COMPLETED
    ),
    NavigationComponent(
        "Settings",
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        ScreenID.SETTINGS
    )
)

@Composable
fun NavigationComponents(
    selectedScreenID: ScreenID,
    updateSelected: (screenID: ScreenID) -> Unit
) {
    NavigationBar {
        navigationList.forEach { navigation ->
            NavigationBarItem(
                label = { Text(navigation.title) },
                icon = { navigation.IconStyled(selectedScreenID) },
                selected = (selectedScreenID == navigation.screenID),
                onClick = { updateSelected(navigation.screenID) }
            )
        }
    }
}

/****** Previews below this line ******/

// Separate preview composables using sample data (because viewmodel can't be instantiated in preview)
@Preview(showSystemUi = true)
@Composable
fun PreviewMainScreen() {
    // Mock data for preview
    val defaultGroups = listOf("All Tasks", "Favorites", "Completed")
    val customGroups = listOf("Bookings", "Locations")
    val sampleTodos = listOf(
        TodoEntity("Task 1", "Description 1", "High", "2023-11-19", "2023-11-20", "", "", 10f, "", "false", "true", "Bookings", 1),
        TodoEntity("Task 2", "Description 2", "Medium", "2023-11-20", "2023-11-21", "", "", 15f, "", "true", "false", "Locations", 2),
        TodoEntity("Sample Task 3", "Description 3", "Low", "T", "T", "", "UserLocation", 0f, "", "true", "false", "Group 2", 3)
    )


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Default Groups Section
            Text("Default Groups", style = MaterialTheme.typography.titleLarge)
            defaultGroups.forEach { groupName ->
                val filteredTasks = sampleTodos.filter {
                    when (groupName) {
                        "All Tasks" -> it.completed == "false"
                        "Favorites" -> it.favorite == "true" && it.completed == "false"
                        "Completed" -> it.completed == "true"
                        else -> false
                    }
                }
                GroupCardMock(groupName, filteredTasks) {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Groups Section with Add button on the right
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Custom Groups", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Group")
                }
            }
            customGroups.forEach { groupName ->
                GroupCardMock(groupName, sampleTodos.filter { it.group == groupName }) {}
            }
        }
    }
}

@Composable
fun GroupCardMock(groupName: String, tasks: List<TodoEntity>, onClick: () -> Unit) {
    val taskCount = tasks.size
    val icon = when (groupName) {
        "All Tasks" -> Icons.Outlined.List
        "Favorites" -> Icons.Outlined.FavoriteBorder
        "Completed" -> Icons.Outlined.Check
        "Locations" -> Icons.Outlined.AirplaneTicket // Example custom icon
        "Bookings" -> Icons.Outlined.Money // Example custom icon
        else -> Icons.Outlined.List
    }
    val isCustomGroup = groupName !in listOf("All Tasks", "Favorites", "Completed")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
            .background(color = Color.LightGray)

    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(icon, contentDescription = "Group Icon", modifier = Modifier
                .size(40.dp)
                .clip(CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = groupName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(text = "$taskCount", style = MaterialTheme.typography.bodySmall)
            if (isCustomGroup) {
                IconButton(onClick = {  }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Group")
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview (showSystemUi = true)
@Composable
fun AddGroupDialogPreview() {

    // List of default group names
    val recommendedGroupNames = listOf("Adventure", "Vacation", "Work")

    // Icons choices
    val iconChoices = listOf(
        GroupIcon("List", Icons.Outlined.List),
        GroupIcon("Shopping", Icons.Outlined.ShoppingCart),
        GroupIcon("Travel", Icons.Outlined.AirplaneTicket),
        GroupIcon("Money", Icons.Filled.Money),
    )

    Dialog(
        onDismissRequest = {  },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Scaffold(
            modifier = Modifier.height(330.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Group", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {}
                    ) {
                        Text("Save", fontSize = 15.sp)
                    }
                }

                Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 5.dp))

                // Recommended Group Names
                Text("Recommended Group Names", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    recommendedGroupNames.forEach { name ->
                        FilledTonalButton(onClick = {  }) {
                            Text(name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Group Name input
                OutlinedTextField(
                    label = { Text("Group Name") },
                    value = "",
                    onValueChange = { }
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Group Icon
                Text("Group Icon", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    iconChoices.forEach {
                        Icon(it.icon, it.name, modifier = Modifier.padding(start = 5.dp))
                        RadioButton(
                            selected = false,
                            onClick = {  }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showSystemUi = true)
@Composable
fun NavigationBarPreview() {
    Scaffold(
        topBar = { TitleBarPreview(ScreenID.ALLTASKS, "") },
        bottomBar = {
            NavigationComponents(ScreenID.ALLTASKS, updateSelected = {  })
        }
    ) {}
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBarPreview(screen: ScreenID = ScreenID.ALLTASKS, selectedGroupName: String = "") {
    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = {
            Text(
                when (screen) {
                    ScreenID.HOME -> "Home"
                    ScreenID.TODOLIST -> selectedGroupName
                    ScreenID.ALLTASKS -> "All Tasks"
                    ScreenID.FAVORITES -> "Favorites"
                    ScreenID.COMPLETED -> "Completed"
                    ScreenID.SETTINGS -> "Settings"
                }
            )
        },
        actions = {
            if (screen != ScreenID.HOME && screen != ScreenID.SETTINGS)
                Box {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Checklist, contentDescription = "Sorting Option")
                    }
                }
        }
    )
}