package com.ucb.deliveryapp.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.auth.data.repository.UserRepositoryImpl
import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthIntegrationTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepositoryImpl(context)

        // Limpiar sesiones previas
        auth.signOut()
    }

    @After
    fun cleanup() {
        auth.signOut()
    }

    @Test
    fun testCompleteAuthFlow() = runBlocking {
        val testEmail = "test_${UUID.randomUUID()}@test.com"
        val testPassword = "Test123!"
        val testUsername = "TestUser"

        val registerResult = userRepository.registerUser(
            UserDto(
                email = testEmail,
                username = testUsername,
                password = testPassword
            )
        )

        assert(registerResult is Result.Success)

        val loginResult = userRepository.login(testEmail, testPassword)

        assert(loginResult is Result.Success)

        val currentUser = auth.currentUser
        assert(currentUser != null)
        assert(currentUser?.email == testEmail)

        userRepository.logout()

        assert(auth.currentUser == null)
    }

    @Test
    fun testAuthWithWrongCredentials() = runBlocking {
        val loginResult = userRepository.login("nonexistent@test.com", "wrongpassword")

        assert(loginResult is Result.Error)
    }
}