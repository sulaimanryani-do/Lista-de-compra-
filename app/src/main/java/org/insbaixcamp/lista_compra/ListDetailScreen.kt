package org.insbaixcamp.lista_compra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: String,
    onBack: () -> Unit,
    listViewModel: ListViewModel = viewModel()
) {
    val lista = listViewModel.userLists.find { it.id == listId }

    var showAddDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var shareEmail by remember { mutableStateOf("") }
    var shareMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lista?.name ?: "List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (lista == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (lista.products.isEmpty()) {
                Text(
                    text = "No products yet. Click + to add one!",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lista.products, key = { it.id }) { product ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = product.checked,
                                    onCheckedChange = {
                                        listViewModel.toggleProduct(lista, product.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (product.checked)
                                            TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (product.category.isNotBlank()) {
                                        Text(
                                            text = product.category,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    listViewModel.removeProduct(lista, product.id)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
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

    // ---- Dialog: zid product (category = dropdown b search) ----
    if (showAddDialog && lista != null) {
        val filteredCategories = CATEGORIES.filter {
            it.contains(productCategory.trim(), ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Product") },
            text = {
                Column {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = productCategory,
                            onValueChange = {
                                productCategory = it
                                categoryExpanded = true
                            },
                            label = { Text("Category") },
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )

                        if (filteredCategories.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                filteredCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            productCategory = category
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Khassha tkon categorie mn l-list
                    val validCategory = CATEGORIES.firstOrNull {
                        it.equals(productCategory.trim(), ignoreCase = true)
                    }
                    if (productName.isNotBlank() && validCategory != null) {
                        listViewModel.addProduct(lista, productName.trim(), validCategory)
                        productName = ""
                        productCategory = ""
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---- Dialog: compartir b email ----
    if (showShareDialog && lista != null) {
        AlertDialog(
            onDismissRequest = {
                showShareDialog = false
                shareMessage = null
            },
            title = { Text("Share list") },
            text = {
                Column {
                    OutlinedTextField(
                        value = shareEmail,
                        onValueChange = { shareEmail = it },
                        label = { Text("User email") },
                        singleLine = true
                    )
                    shareMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val wanted = shareEmail.trim().lowercase()
                    if (wanted.isNotBlank()) {
                        FirebaseFirestore.getInstance().collection("users")
                            .get()
                            .addOnSuccessListener { result ->
                                val userDoc = result.documents.firstOrNull {
                                    (it.getString("email") ?: "").trim().lowercase() == wanted
                                }
                                if (userDoc == null) {
                                    shareMessage = "No user found with this email"
                                } else {
                                    listViewModel.addMemberToList(lista, userDoc.id)
                                    shareMessage = "Shared successfully ✅"
                                    shareEmail = ""
                                }
                            }
                            .addOnFailureListener {
                                shareMessage = it.localizedMessage
                            }
                    }
                }) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShareDialog = false
                    shareMessage = null
                }) { Text("Close") }
            }
        )
    }
}