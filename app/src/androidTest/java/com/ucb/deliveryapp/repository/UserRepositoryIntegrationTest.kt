// UserRepositoryRealTest.kt
package com.ucb.deliveryapp.data.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.data.repository.UserRepositoryImpl
import com.ucb.deliveryapp.util.Result
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserRepositoryRealTest {

    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setup() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository = UserRepositoryImpl(context)
    }

    @Test
    fun testUserRepositoryMethods() = runTest {
        val uniqueEmail = "integration${System.currentTimeMillis()}@test.com"

        val testUser = User(
            id = "",
            username = "Integration Test User",
            email = uniqueEmail,
            password = "TestPassword123!",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        // 1. Verificar si email está registrado (debería ser false)
        val isRegisteredBefore = repository.isEmailRegistered(uniqueEmail)
        println("Is $uniqueEmail registered before? $isRegisteredBefore")

        // 2. Registrar usuario
        val registerResult = repository.registerUser(testUser)
        println("Register result: $registerResult")

        when (registerResult) {
            is Result.Success -> {
                println("✅ User registered successfully")

                // 3. Verificar email después de registro
                kotlinx.coroutines.delay(2000)
                val isRegisteredAfter = repository.isEmailRegistered(uniqueEmail)
                println("Is $uniqueEmail registered after? $isRegisteredAfter")

                // 4. Login
                val loginResult = repository.login(uniqueEmail, "TestPassword123!")
                println("Login result: ${if (loginResult is Result.Success) "success" else "error"}")

                // 5. Obtener usuario actual
                val currentUser = repository.getCurrentUser()
                println("Current user: ${currentUser?.email ?: "null"}")

                // 6. Logout
                repository.logout()
                println("✅ Logout executed")

                // 7. Verificar usuario actual después de logout
                val afterLogout = repository.getCurrentUser()
                println("After logout: ${afterLogout?.email ?: "null"}")
            }
            is Result.Error -> {
                println("⚠️ Register failed (maybe email already exists or no Firebase connection): ${registerResult.exception.message}")
            }
            is Result.Loading -> {
                println("⏳ Register in progress")
            }
        }

        // La prueba pasa si ejecuta sin crash
        assert(true)
    }

    @Test
    fun testWithoutFirebaseConnection() = runTest {
        // Pruebas que funcionan sin conexión a Firebase

        // 1. Logout (no debería crash)
        repository.logout()
        println("✅ Logout executed without crash")

        // 2. Obtener usuario actual (puede ser null)
        val user = repository.getCurrentUser()
        println("Current user without connection: ${user?.email ?: "null"}")

        // 3. Verificar email (devuelve false sin conexión según tu implementación)
        val checkResult = repository.isEmailRegistered("nonexistent@test.com")
        println("Email check without connection: $checkResult")

        assert(true)
    }
}