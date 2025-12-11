package com.ucb.deliveryapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.data.local.LoginDataStore
import com.ucb.deliveryapp.domain.repository.UserRepository
import com.ucb.deliveryapp.util.Result
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class UserRepositoryImpl(
    private val context: Context
) : UserRepository {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val loginDataStore = LoginDataStore(context)
    private val usersCollection = db.collection("users")

    private companion object {
        const val STATIC_SALT = "Packify_Delivery_App_2024_Secure_Salt"
    }

    // Verificador de conexión a internet
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override suspend fun registerUser(user: User): Result<Boolean> {
        return try {
            // Verificar conexión
            if (!isOnline()) {
                return Result.Error(
                    Exception("Sin conexión a internet. No se puede registrar.")
                )
            }

            // Verificar si el email ya existe
            val emailQuery = usersCollection.whereEqualTo("email", user.email).get().await()
            if (!emailQuery.isEmpty) {
                return Result.Error(Exception("El email ya está registrado"))
            }

            // Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val userId = authResult.user?.uid ?: throw Exception("Error creating user")

            // Crear usuario en Firestore
            val userWithId = user.copy(
                id = userId,
                password = secureHash(user.password) // Guardar hash en Firestore
            )

            usersCollection.document(userId).set(userWithId).await()

            // Guardar sesión
            loginDataStore.saveUserSession(
                userId = userId,
                email = user.email,
                userName = user.username
            )

            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Verificar conexión
            if (!isOnline()) {
                return Result.Error(
                    Exception("Sin conexión a internet. No se puede iniciar sesión.")
                )
            }

            // Autenticar con Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User not found")

            // Obtener datos del usuario desde Firestore
            val userDocument = usersCollection.document(userId).get().await()
            val user = userDocument.toObject(User::class.java) ?: throw Exception("User data not found")

            // Verificar contraseña (hash comparation)
            if (user.password != secureHash(password)) {
                throw Exception("Invalid credentials")
            }

            // Guardar sesión
            loginDataStore.saveUserSession(
                userId = userId,
                email = user.email,
                userName = user.username
            )

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        // Esto puede funcionar offline si el usuario ya estaba autenticado
        val currentUser = auth.currentUser
        return currentUser?.let { user ->
            try {
                // Intentar obtener de Firestore si hay conexión
                if (isOnline()) {
                    usersCollection.document(user.uid).get().await().toObject(User::class.java)
                } else {
                    // Retornar datos básicos de auth si no hay conexión
                    User(
                        id = user.uid,
                        email = user.email ?: "",
                        username = user.displayName ?: "Usuario",
                        password = ""
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun logout() {
        auth.signOut()
        loginDataStore.clearUserSession()
    }

    override suspend fun isEmailRegistered(email: String): Boolean {
        return try {
            if (!isOnline()) {
                return false // No se puede verificar sin conexión
            }

            val query = usersCollection.whereEqualTo("email", email).get().await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            if (!isOnline()) {
                return Result.Error(
                    Exception("Sin conexión a internet. No se puede obtener información del usuario.")
                )
            }

            val user = usersCollection.document(userId).get().await().toObject(User::class.java)
            if (user != null) Result.Success(user)
            else Result.Error(Exception("User not found"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUser(userId: String, updatedUser: User): Result<Boolean> {
        return try {
            if (!isOnline()) {
                return Result.Error(
                    Exception("Sin conexión a internet. No se puede actualizar el perfil.")
                )
            }

            // Actualizar en Firestore
            usersCollection.document(userId).set(updatedUser).await()

            // Actualizar en DataStore si el nombre cambió
            loginDataStore.saveUserSession(
                userId = userId,
                email = updatedUser.email,
                userName = updatedUser.username
            )

            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun secureHash(password: String): String {
        return try {
            val bytes = (STATIC_SALT + password).toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            throw Exception("Error securing password")
        }
    }
}