package org.insbaixcamp.lista_compra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        errorMessage = null
    }

    // ---------- LOGIN ----------
    fun loginUser(email: String, pass: String, onLoginSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }

        isLoading = true
        errorMessage = null

        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    ensureUserDocument()
                    isLoading = false
                    onLoginSuccess()
                } else {
                    isLoading = false
                    errorMessage = task.exception?.localizedMessage ?: "Login failed"
                }
            }
    }

    // Ila l-user ma-3ndouch document f "users", kan-creeh
    private fun ensureUserDocument() {
        val user = auth.currentUser ?: return
        val ref = firestore.collection("users").document(user.uid)
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

    // ---------- SIGN UP ----------
    fun signUpUser(name: String, email: String, pass: String, onSignUpSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }
        if (pass.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        isLoading = true
        errorMessage = null

        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""

                    val newUser = User(
                        uid = uid,
                        name = name.trim(),
                        email = email.trim().lowercase()
                    )

                    firestore.collection("users").document(uid)
                        .set(newUser)
                        .addOnSuccessListener {
                            isLoading = false
                            onSignUpSuccess()
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            errorMessage = e.localizedMessage ?: "Failed to save user profile"
                        }
                } else {
                    isLoading = false
                    errorMessage = task.exception?.localizedMessage ?: "Sign up failed"
                }
            }
    }
}