package org.insbaixcamp.lista_compra

import org.insbaixcamp.lista_compra.ListViewModel
import org.insbaixcamp.lista_compra.Lista
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onListClick: (String) -> Unit,
    onLogout: () -> Unit,
    listViewModel: ListViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Shopping Lists") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add List"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val userLists = listViewModel.userLists

            if (userLists.isEmpty()) {
                Text(
                    text = "No lists found. Click + to create one!",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(userLists) { lista ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onListClick(lista.id) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = lista.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${lista.products.size} items",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { listViewModel.deleteList(lista) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("New List") },
                    text = {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            label = { Text("List Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newListName.isNotBlank()) {
                                    listViewModel.createList(newListName.trim())
                                    newListName = ""
                                    showDialog = false
                                }
                            }
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}