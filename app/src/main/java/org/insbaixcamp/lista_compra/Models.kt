package org.insbaixcamp.lista_compra

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
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
    val products: List<Product> = emptyList(),
    val members: List<String> = emptyList()
) {
    fun addProduct(product: Product): Lista = copy(products = products + product)
}