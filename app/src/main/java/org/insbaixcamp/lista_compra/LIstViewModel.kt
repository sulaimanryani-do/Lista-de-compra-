package org.insbaixcamp.lista_compra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

class ListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Llistes de l'usuari (accés propi o compartit), actualitzades en temps real
    var userLists by mutableStateOf<List<Lista>>(emptyList())
        private set

    // Passa a true quan arriba el primer snapshot (així sabem si una llista
    // ha estat eliminada o si encara s'està carregant)
    var loaded by mutableStateOf(false)
        private set

    private var listenerRegistration: ListenerRegistration? = null

    init {
        listenToUserLists()
    }

    // ---------- ESCOLTA EN TEMPS REAL ----------
    fun listenToUserLists() {
        val currentUserId = auth.currentUser?.uid ?: return
        // Eliminem l'escoltador anterior si n'hi havia
        listenerRegistration?.remove()

        listenerRegistration = db.collection("lists")
            .whereArrayContains("members", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to lists: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    userLists = snapshot.toObjects(Lista::class.java)
                        .sortedBy { it.name.lowercase() }
                    loaded = true
                }
            }
    }

    // ---------- FUNCIÓ AUXILIAR: actualització amb transacció ----------
    // Llegeix la llista actual de Firestore, hi aplica el canvi i la desa.
    // Així evitem que dos usuaris es sobreescriguin els canvis mútuament.
    private fun updateList(listId: String, transform: (Lista) -> Lista) {
        val ref = db.collection("lists").document(listId)
        db.runTransaction<Any?> { tx ->
            val current = tx.get(ref).toObject(Lista::class.java)
                ?: return@runTransaction null
            tx.set(ref, transform(current))
            null
        }
    }

    // ---------- LLISTES ----------
    fun createList(name: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val listId = db.collection("lists").document().id

        val newList = Lista(
            id = listId,
            name = name,
            ownerId = currentUserId,
            members = listOf(currentUserId),
            categories = DEFAULT_CATEGORIES.map {
                Category(id = UUID.randomUUID().toString(), name = it)
            }
        )
        db.collection("lists").document(listId).set(newList)
    }

    fun renameList(listId: String, newName: String) =
        updateList(listId) { it.copy(name = newName) }

    // Només el propietari pot eliminar la llista
    fun deleteList(list: Lista) {
        val uid = auth.currentUser?.uid ?: return
        if (list.ownerId != uid) return
        db.collection("lists").document(list.id).delete()
    }

    // Compartir: afegeix l'UID d'un altre usuari als membres de la llista
    fun addMemberToList(listId: String, newMemberUserId: String) =
        updateList(listId) { l ->
            if (l.members.contains(newMemberUserId)) l
            else l.copy(members = l.members + newMemberUserId)
        }

    // ---------- CATEGORIES ----------
    fun addCategory(listId: String, name: String) {
        if (name.isBlank()) return
        updateList(listId) { l ->
            l.copy(categories = l.categories + Category(UUID.randomUUID().toString(), name.trim()))
        }
    }

    fun renameCategory(listId: String, categoryId: String, newName: String) {
        if (newName.isBlank()) return
        updateList(listId) { l ->
            l.copy(categories = l.categories.map {
                if (it.id == categoryId) it.copy(name = newName.trim()) else it
            })
        }
    }

    // Només s'elimina si la categoria no conté cap producte
    fun deleteCategory(listId: String, categoryId: String) =
        updateList(listId) { l ->
            if (l.products.any { it.categoryId == categoryId }) l
            else l.copy(categories = l.categories.filter { it.id != categoryId })
        }

    // ---------- PRODUCTES ----------
    fun addProduct(listId: String, name: String, categoryId: String) {
        if (name.isBlank()) return
        val newProduct = Product(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            categoryId = categoryId,
            checked = false
        )
        updateList(listId) { l -> l.copy(products = l.products + newProduct) }
    }

    fun editProduct(listId: String, productId: String, name: String, categoryId: String) {
        if (name.isBlank()) return
        updateList(listId) { l ->
            l.copy(products = l.products.map {
                if (it.id == productId) it.copy(name = name.trim(), categoryId = categoryId) else it
            })
        }
    }

    // Marca o desmarca un producte com a completat (sense eliminar-lo)
    fun toggleProduct(listId: String, productId: String) =
        updateList(listId) { l ->
            l.copy(products = l.products.map {
                if (it.id == productId) it.toggle() else it
            })
        }

    fun removeProduct(listId: String, productId: String) =
        updateList(listId) { l ->
            l.copy(products = l.products.filter { it.id != productId })
        }

    override fun onCleared() {
        super.onCleared()
        // Alliberem l'escoltador quan el ViewModel es destrueix
        listenerRegistration?.remove()
    }
}