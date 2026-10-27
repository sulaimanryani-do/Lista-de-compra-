package org.insbaixcamp.lista_compra

// Categories per defecte que es creen automàticament en crear una llista nova
val DEFAULT_CATEGORIES = listOf(
    "Fruits",
    "Vegetables",
    "Meat",
    "Fish",
    "Dairy",
    "Bakery",
    "Drinks",
    "Snacks",
    "Frozen",
    "Cleaning",
    "Hygiene",
    "Other"
)

// Paraules clau (català / castellà / anglès) que determinen la categoria d'un producte
private val KEYWORDS: Map<String, List<String>> = mapOf(
    "Dairy" to listOf(
        "llet", "leche", "milk", "formatge", "queso", "cheese",
        "iogurt", "yogur", "yogurt", "mantega", "mantequilla", "nata", "reques"
    ),
    "Fruits" to listOf(
        "poma", "manzana", "apple", "platan", "plátano", "banana", "taronja",
        "naranja", "orange", "pera", "raim", "uva", "maduixa", "fresa",
        "llimona", "limón", "limon", "fruita", "fruta", "meló", "sandia", "síndria"
    ),
    "Vegetables" to listOf(
        "tomaquet", "tomate", "ceba", "cebolla", "patata", "pastanaga",
        "zanahoria", "enciam", "lechuga", "pebrot", "pimiento", "carbassó",
        "calabacín", "bròquil", "brocoli", "espinac", "all", "verdura", "hortalissa"
    ),
    "Meat" to listOf(
        "pollastre", "pollo", "chicken", "vedella", "ternera", "beef", "porc",
        "cerdo", "botifarra", "salsitxa", "salchicha", "carn", "carne", "hamburguesa", "pernil", "jamón"
    ),
    "Fish" to listOf(
        "peix", "pescado", "fish", "salmó", "salmón", "tonyina", "atún",
        "bacallà", "bacalao", "gamba", "musclo", "sardina", "lluç", "merluza"
    ),
    "Bakery" to listOf(
        "pa", "pan", "bread", "croissant", "galeta", "galleta", "magdalena",
        "farina", "harina", "brioix", "baguet", "ensaïmada"
    ),
    "Drinks" to listOf(
        "aigua", "agua", "water", "suc", "zumo", "juice", "cervesa", "cerveza",
        "beer", "vi", "vino", "wine", "cola", "refresc", "refresco", "cafè", "café", "te"
    ),
    "Snacks" to listOf(
        "xips", "chips", "patatas fritas", "avellana", "ametlla", "almendra",
        "cacauet", "cacahuete", "xocolata", "chocolate", "snack", "caramel", "fruits secs"
    ),
    "Frozen" to listOf(
        "gelat", "helado", "ice", "congelat", "congelado", "pizza", "croqueta", "nuggets"
    ),
    "Cleaning" to listOf(
        "lleixiu", "lejía", "lejia", "detergent", "detergente", "fregona",
        "netejador", "limpiador", "suavitzant", "suavizante", "bossa", "bolsa", "rentavaixelles", "lavavajillas"
    ),
    "Hygiene" to listOf(
        "xampú", "xampu", "champú", "champu", "shampoo", "sabó", "jabón", "jabon",
        "dentifrici", "dentífrico", "desodorant", "desodorante", "paper higiènic",
        "papel higiénico", "compresa", "raspall", "cepillo"
    )
)

/**
 * Funcionalitat extra: classificació automàtica.
 * Analitza les paraules del nom del producte i retorna la categoria
 * corresponent d'aquesta llista (cercant-la pel nom).
 * Si no hi ha cap coincidència, retorna null.
 */
fun guessCategory(productName: String, categories: List<Category>): Category? {
    val words = productName
        .lowercase()
        .split(Regex("[^\\p{L}]+"))
        .filter { it.isNotBlank() }
    if (words.isEmpty()) return null

    for ((categoryName, keys) in KEYWORDS) {
        val hit = words.any { w ->
            keys.any { k -> w == k || (k.length >= 4 && w.startsWith(k)) }
        }
        if (hit) {
            categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
                ?.let { return it }
        }
    }
    return null
}