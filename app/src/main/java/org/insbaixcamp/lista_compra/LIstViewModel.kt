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

    // State li kay-7tafeḍ b-ga3 l-listat dyal l-user f Realtime
    var userLists by mutableStateOf<List<Lista>>(emptyList())
        private set

    private var listenerRegistration: ListenerRegistration? = null

    init {
        // Mnin kay-t-crea ListViewModel, kay-bda y-qra l-listat auto
        listenToUserLists()
    }

    // 1. REALTIME LISTENER: Kat-qra ga3 l-listat li l-user member fihom f Realtime
    fun listenToUserLists() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Mssah l-listener l-qdim ila kan kayn
        listenerRegistration?.remove()

        listenerRegistration = db.collection("lists")
            .whereArrayContains("members", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to lists: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lists = snapshot.toObjects(Lista::class.java)
                    userLists = lists
                }
            }
    }

    // 2. ADD PRODUCT
    fun addProduct(list: Lista, name: String, category: String) {
        val newProduct = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            checked = false
        )

        val updatedProduct = list.products + newProduct
        val updatedList = list.copy(products = updatedProduct)

        db.collection("lists").document(list.id).set(updatedList)
    }

    // 3. REMOVE PRODUCT
    fun removeProduct(list: Lista, productId: String) {
        val updatedProduct = list.products.filter { it.id != productId }
        val updatedList = list.copy(products = updatedProduct)

        db.collection("lists").document(list.id).set(updatedList)
    }

    // 4. TOGGLE PRODUCT (CHECK / UNCHECK)
    fun toggleProduct(list: Lista, productId: String) {
        val updatedProducts = list.products.map { product ->
            if (product.id == productId) product.toggle() else product
        }
        val updatedList = list.copy(products = updatedProducts)

        db.collection("lists").document(list.id).set(updatedList)
    }

    // 5. CREATE LIST
    fun createList(name: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val listId = db.collection("lists").document().id

        val newList = Lista(
            id = listId,
            name = name,
            ownerId = currentUserId,
            members = listOf(currentUserId)
        )

        db.collection("lists").document(listId).set(newList)
    }

    // 6. DELETE LIST
    fun deleteList(list: Lista) {
        db.collection("lists").document(list.id).delete()
    }

    // 7. SHARE LIST (Zid user akhor b email aw UID f members)
    fun addMemberToList(list: Lista, newMemberUserId: String) {
        if (!list.members.contains(newMemberUserId)) {
            val updatedMembers = list.members + newMemberUserId
            val updatedList = list.copy(members = updatedMembers)
            db.collection("lists").document(list.id).set(updatedList)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Nddfo l-listener mnin l-ViewModel y-t-mssah
        listenerRegistration?.remove()
    }
}