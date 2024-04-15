package com.example.todoapp

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AirplaneTicket
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ShortText
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.entities.TodoGroupEntity
import com.example.todoapp.viewmodels.SettingsViewModel
import com.example.todoapp.viewmodels.TodosViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsContent(settingsViewModel: SettingsViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner), todosViewModel: TodosViewModel = viewModel()){
    var sortExpanded by remember { mutableStateOf(false) }
    var lengthExpanded by remember { mutableStateOf(false) }
    val lengthList = listOf("1 Line", "2 Lines", "3 Lines")
    val sortList = listOf("Name", "Due Date", "Priority", "Distance")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    Column (modifier = Modifier.fillMaxWidth().padding(10.dp).verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.general_settings_label), fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Divider(thickness = 2.dp)
        GeneralSetting(name = stringResource(R.string.notifications_label), icon = Icons.Outlined.Notifications) {
            Switch(checked = settingsViewModel.notificationsEnabled, onCheckedChange = {isChecked ->
                    scope.launch { settingsViewModel.updateNotifications(isChecked) }
            })
        }
        GeneralSetting(name = stringResource(R.string.sort_by_label), icon = Icons.Outlined.Checklist) {
            Box {
                FilledTonalButton(onClick = { sortExpanded = true }) {
                    Text(text = settingsViewModel.sortByValue)
                }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    sortList.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort) },
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
        GeneralSetting(name = stringResource(R.string.description_length), icon = Icons.Outlined.ShortText) {
            Box {
                FilledTonalButton(onClick = { lengthExpanded = true }) {
                    Text(text = settingsViewModel.descriptionValue)
                }
                DropdownMenu(expanded = lengthExpanded, onDismissRequest = { lengthExpanded = false }) {
                    lengthList.forEach { length ->
                        DropdownMenuItem(
                            text = { Text(length) },
                            onClick = {
                                lengthExpanded = false
                                scope.launch { settingsViewModel.updateDescription(length) }
                            }
                        )
                    }
                }
            }
        }
        GeneralSetting(name = stringResource(R.string.dark_mode),
            icon = if(settingsViewModel.darkModeEnabled) {Icons.Default.NightlightRound} else { Icons.Default.WbSunny}) {
            Switch(
                checked = settingsViewModel.darkModeEnabled,
                onCheckedChange = {isChecked -> settingsViewModel.darkModeEnabled
                    scope.launch {
                        settingsViewModel.updateDarkMode(isChecked)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.padding(top = 20.dp))
        Text("Home Screen Custom Groups", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Divider(thickness = 2.dp)
        
        // Allows user to choose which custom groups should be shown in home page
        if (todosViewModel.todoGroupsList.isEmpty())
            Text(stringResource(R.string.no_groups_label))
        todosViewModel.todoGroupsList.forEach { group ->
            var displayedIcon = Icons.Outlined.List
            when (group.iconName){
                "Shopping" -> displayedIcon = Icons.Outlined.ShoppingCart
                "Travel" -> displayedIcon = Icons.Outlined.AirplaneTicket
                "Money" -> displayedIcon = Icons.Outlined.Money
            }
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(displayedIcon, contentDescription = group.name, modifier = Modifier.size(30.dp))
                Text(group.name, modifier = Modifier.padding(start = 10.dp), fontSize = 17.sp)
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = group.visible.toBoolean(),
                    onCheckedChange = { scope.launch { todosViewModel.updateGroupVisibility(group,it)} }
                )
            }
        }
    }
}
// Composable to display a single setting
@Composable
fun GeneralSetting(name: String, icon: ImageVector, composable: @Composable () -> Unit){
    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically
    ){
        Icon(icon, contentDescription = name, modifier = Modifier.size(30.dp))
        Text(name, modifier = Modifier.padding(start = 10.dp), fontSize = 17.sp)
        Spacer(modifier = Modifier.weight(1f))
        composable()
    }
}

/****** Previews below this line ******/

// Separate preview composables using sample data (because viewmodel can't be instantiated in preview)
@Preview (showSystemUi = true)
@Composable
fun SettingsContentPreview(){
    val maxDescriptionLength by remember { mutableStateOf("1 Line")}
    val lengthList = listOf("1 Line", "2 Lines", "3 Lines")
    val sortList = listOf("Name", "Date/Time", "Priority", "Distance")
    val groupsList = listOf(
        TodoGroupEntity(name = "Shopping", iconName = "Shopping", "true"),
        TodoGroupEntity(name = "Travel Prep", iconName = "Travel", "true"),
        TodoGroupEntity(name = "General", iconName = "default", "false"),
    )
    val sortValue by rememberSaveable { mutableStateOf("Name") }

    Column (modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
        Text("General", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Divider(thickness = 2.dp)
        GeneralSetting(name = "Notifications", icon =Icons.Outlined.Notifications ) {
            Switch(checked = false, onCheckedChange = {  })
        }
        GeneralSetting(name = "Sort By", icon = Icons.Outlined.Checklist) {
            Box {
                FilledTonalButton(onClick = { }) {
                    Text(text = sortValue)
                }
                DropdownMenu(expanded = false, onDismissRequest = {  }) {
                    sortList.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort) },
                            onClick = {}
                        )
                    }
                }
            }        }
        GeneralSetting(name = "Description Length", icon = Icons.Outlined.ShortText) {
            Box {
                FilledTonalButton(onClick = {  }) {
                    Text(text = maxDescriptionLength)
                }
                DropdownMenu(expanded = false, onDismissRequest = { }) {
                    lengthList.forEach { length ->
                        DropdownMenuItem(
                            text = { Text(length) },
                            onClick = {}
                        )
                    }
                }
            }
        }
        GeneralSetting(name = "DarkMode", icon = Icons.Default.WbSunny) {
            Switch(
                checked = false,
                onCheckedChange = {}
            )
        }
        Spacer(modifier = Modifier.padding(top = 20.dp))
        Text("Home Screen Custom Lists", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Divider(thickness = 2.dp)
        groupsList.forEach { group ->
            var displayedIcon = Icons.Outlined.List
            when (group.iconName){
                "Shopping" -> displayedIcon = Icons.Outlined.ShoppingCart
                "Travel" -> displayedIcon = Icons.Outlined.AirplaneTicket
                "Money" -> displayedIcon = Icons.Outlined.Money
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    displayedIcon,
                    contentDescription = group.name,
                    modifier = Modifier.size(30.dp)
                )
                Text(group.name, modifier = Modifier.padding(start = 10.dp), fontSize = 17.sp)
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = group.visible.toBoolean(), onCheckedChange = {})
            }
        }
    }
}