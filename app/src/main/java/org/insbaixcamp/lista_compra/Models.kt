package org.insbaixcamp.lista_compra

data class Category(
    val id: String = "",
    val name: String = ""
)

data class Product(
    val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val checked: Boolean = false
) {
    fun toggle(): Product = copy(checked = !checked)
}

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = ""
)

data class Lista(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList()
)