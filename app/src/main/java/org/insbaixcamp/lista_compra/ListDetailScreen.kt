package org.insbaixcamp.lista_compra

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current
    val lista = listViewModel.userLists.find { it.id == listId }

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareEmail by remember { mutableStateOf("") }
    var shareMessage by remember { mutableStateOf<String?>(null) }

    // Si la llista ha estat eliminada (o ja no hi tenim accés), tornem enrere
    LaunchedEffect(listViewModel.loaded, lista) {
        if (listViewModel.loaded && lista == null) onBack()
    }

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
            FloatingActionButton(onClick = {
                if (lista == null) return@FloatingActionButton
                if (lista.categories.isEmpty()) {
                    Toast.makeText(context, "Create a category first", Toast.LENGTH_SHORT).show()
                } else {
                    editingProduct = null
                    showProductDialog = true
                }
            }) {
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
            } else {
                // Productes la categoria dels quals ja no existeix
                val knownIds = lista.categories.map { it.id }.toSet()
                val orphans = lista.products.filter { it.categoryId !in knownIds }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mostrem cada categoria amb els seus productes
                    lista.categories.forEach { category ->
                        val products = lista.products.filter { it.categoryId == category.id }

                        item(key = "cat_${category.id}") {
                            CategoryHeader(
                                name = category.name,
                                count = products.size,
                                onEdit = {
                                    editingCategory = category
                                    showCategoryDialog = true
                                },
                                onDelete = {
                                    if (products.isNotEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "Remove or move its products first",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        listViewModel.deleteCategory(lista.id, category.id)
                                    }
                                }
                            )
                        }

                        if (products.isEmpty()) {
                            item(key = "empty_${category.id}") {
                                Text(
                                    text = "No products",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        } else {
                            items(products, key = { it.id }) { product ->
                                ProductRow(
                                    product = product,
                                    onToggle = { listViewModel.toggleProduct(lista.id, product.id) },
                                    onEdit = {
                                        editingProduct = product
                                        showProductDialog = true
                                    },
                                    onDelete = { listViewModel.removeProduct(lista.id, product.id) }
                                )
                            }
                        }
                    }

                    // Secció per als productes sense categoria vàlida
                    if (orphans.isNotEmpty()) {
                        item(key = "orphans_header") {
                            CategoryHeader(name = "Uncategorized", count = orphans.size, onEdit = null, onDelete = null)
                        }
                        items(orphans, key = { it.id }) { product ->
                            ProductRow(
                                product = product,
                                onToggle = { listViewModel.toggleProduct(lista.id, product.id) },
                                onEdit = {
                                    editingProduct = product
                                    showProductDialog = true
                                },
                                onDelete = { listViewModel.removeProduct(lista.id, product.id) }
                            )
                        }
                    }

                    item(key = "add_category") {
                        OutlinedButton(
                            onClick = {
                                editingCategory = null
                                showCategoryDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 72.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add category")
                        }
                    }
                }
            }
        }
    }

    // ---- Diàleg: producte (afegir / editar) ----
    if (showProductDialog && lista != null) {
        ProductDialog(
            categories = lista.categories,
            initial = editingProduct,
            onDismiss = { showProductDialog = false },
            onConfirm = { name, categoryId ->
                val p = editingProduct
                if (p == null) listViewModel.addProduct(lista.id, name, categoryId)
                else listViewModel.editProduct(lista.id, p.id, name, categoryId)
                showProductDialog = false
            }
        )
    }

    // ---- Diàleg: categoria (afegir / canviar el nom) ----
    if (showCategoryDialog && lista != null) {
        val c = editingCategory
        NameDialog(
            title = if (c == null) "New Category" else "Rename Category",
            label = "Category name",
            initial = c?.name ?: "",
            onDismiss = { showCategoryDialog = false },
            onConfirm = { name ->
                if (c == null) listViewModel.addCategory(lista.id, name)
                else listViewModel.renameCategory(lista.id, c.id, name)
                showCategoryDialog = false
            }
        )
    }

    // ---- Diàleg: compartir la llista per correu electrònic ----
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
                    if (wanted.isBlank()) {
                        shareMessage = "Enter an email"
                    } else {
                        // Cerquem l'usuari pel correu electrònic a la col·lecció "users"
                        FirebaseFirestore.getInstance().collection("users")
                            .whereEqualTo("email", wanted)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { result ->
                                val userDoc = result.documents.firstOrNull()
                                if (userDoc == null) {
                                    shareMessage = "No user found with this email"
                                } else {
                                    listViewModel.addMemberToList(lista.id, userDoc.id)
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

// ================== COMPONENTS ==================

// Capçalera d'una categoria (nom, nombre de productes i botons d'editar/eliminar)
@Composable
private fun CategoryHeader(
    name: String,
    count: Int,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$name ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Rename category")
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete category",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Fila d'un producte: checkbox (completat), nom, editar i eliminar
@Composable
private fun ProductRow(
    product: Product,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = product.checked, onCheckedChange = { onToggle() })
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (product.checked) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Diàleg senzill amb un sol camp (nom). L'utilitzen HomeScreen i les categories. */
@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmText: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                label = { Text(label) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { msg -> @Composable { Text(msg) } }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                // El nom és obligatori
                if (text.isBlank()) error = "Name is required"
                else onConfirm(text.trim())
            }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Diàleg per afegir/editar un producte, amb classificació automàtica de la categoria. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDialog(
    categories: List<Category>,
    initial: Product?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, categoryId: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    // Si l'usuari tria la categoria manualment, no la canviem automàticament
    var pickedManually by remember { mutableStateOf(initial != null) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selected = categories.find { it.id == categoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Product" else "Edit Product") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { value ->
                        name = value
                        error = null
                        // Funcionalitat extra: classifiquem el producte mentre s'escriu
                        if (!pickedManually) {
                            guessCategory(value, categories)?.let { categoryId = it.id }
                        }
                    },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selected?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    categoryId = c.id
                                    pickedManually = true
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Validacions: el nom i la categoria són obligatoris
                when {
                    name.isBlank() -> error = "Product name is required"
                    selected == null -> error = "Choose a category"
                    else -> onConfirm(name.trim(), selected.id)
                }
            }) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}