package org.insbaixcamp.lista_compra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.insbaixcamp.lista_compra.ui.theme.Lista_compraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val startDestination = if (currentUser != null) "home" else "login"

        // Ila kan mdkhel o ma-3ndouch document f "users", creeh
        currentUser?.let { user ->
            val ref = FirebaseFirestore.getInstance().collection("users").document(user.uid)
            ref.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val email = (user.email ?: "").lowercase()
                    ref.set(
                        User(
                            uid = user.uid,
                            name = email.substringBefore("@"),
                            email = email
                        )
                    )
                }
            }
        }

        setContent {
            Lista_compraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onGoToSignUp = { navController.navigate("signup") }
                            )
                        }
                        composable("signup") {
                            SignUpScreen(
                                onSignUpSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onGoToLogin = { navController.popBackStack() }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onListClick = { listId ->
                                    navController.navigate("detail/$listId")
                                },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            route = "detail/{listId}",
                            arguments = listOf(navArgument("listId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val listId = backStackEntry.arguments?.getString("listId") ?: ""
                            ListDetailScreen(
                                listId = listId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}