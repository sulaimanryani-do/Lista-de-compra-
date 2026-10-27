package org.insbaixcamp.lista_compra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var showCreateDialog by remember { mutableStateOf(false) }
    var listToRename by remember { mutableStateOf<Lista?>(null) }
    var listToDelete by remember { mutableStateOf<Lista?>(null) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add List")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val userLists = listViewModel.userLists

            if (!listViewModel.loaded) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (userLists.isEmpty()) {
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
                    items(userLists, key = { it.id }) { lista ->
                        val isOwner = lista.ownerId == currentUserId
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lista.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${lista.products.size} products · ${lista.categories.size} categories",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    // Indica si la llista és compartida
                                    if (!isOwner) {
                                        Text(
                                            text = "Shared with you",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    } else if (lista.members.size > 1) {
                                        Text(
                                            text = "Shared with ${lista.members.size - 1} person(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                // Canviar el nom: qualsevol membre. Eliminar: només el propietari
                                IconButton(onClick = { listToRename = lista }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                                }
                                if (isOwner) {
                                    IconButton(onClick = { listToDelete = lista }) {
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
            }
        }
    }

    // ---- Diàleg: crear llista ----
    if (showCreateDialog) {
        NameDialog(
            title = "New List",
            label = "List Name",
            confirmText = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                listViewModel.createList(it)
                showCreateDialog = false
            }
        )
    }

    // ---- Diàleg: canviar el nom de la llista ----
    listToRename?.let { l ->
        NameDialog(
            title = "Rename List",
            label = "List Name",
            initial = l.name,
            onDismiss = { listToRename = null },
            onConfirm = {
                listViewModel.renameList(l.id, it)
                listToRename = null
            }
        )
    }

    // ---- Diàleg: confirmar l'eliminació ----
    listToDelete?.let { l ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Delete list") },
            text = { Text("Delete \"${l.name}\" for everyone? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    listViewModel.deleteList(l)
                    listToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) { Text("Cancel") }
            }
        )
    }
}