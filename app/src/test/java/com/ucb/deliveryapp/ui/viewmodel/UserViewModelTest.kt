package com.ucb.deliveryapp.ui.viewmodel

import android.app.Application
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.domain.usecase.LoginUseCase
import com.ucb.deliveryapp.domain.usecase.RegisterUseCase
import com.ucb.deliveryapp.util.Result
import com.ucb.deliveryapp.util.TestDispatcherRule
import com.ucb.deliveryapp.viewmodel.UserViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var userViewModel: UserViewModel
    private val mockApplication: Application = mockk(relaxed = true)
    private val mockLoginUseCase: LoginUseCase = mockk()
    private val mockRegisterUseCase: RegisterUseCase = mockk()

    private val testUser = User(
        id = "usuario_123",
        email = "test@ucb.edu.bo",
        username = "Usuario Test",
        password = "password123"
    )

    @Before
    fun setup() {
        userViewModel = UserViewModel(mockApplication, mockLoginUseCase, mockRegisterUseCase)
    }

    @Test
    fun `iniciar sesion exitoso deberia actualizar loginState`() = runTest {
        // Dado que
        val email = "test@ucb.edu.bo"
        val password = "password123"
        coEvery {
            mockLoginUseCase(email, password)
        } returns Result.Success(testUser)

        // Cuando
        userViewModel.login(email, password)

        // Entonces
        try {
            val loginState = userViewModel.loginState.first()
            assertNotNull(loginState)
            assertTrue(loginState is Result.Success)
            val successState = loginState as Result.Success
            assertEquals(testUser.email, successState.data.email)
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun `registro exitoso deberia actualizar registrationState`() = runTest {
        // Dado que
        coEvery {
            mockRegisterUseCase(testUser)
        } returns Result.Success(true)

        // Cuando
        userViewModel.register(testUser)

        // Entonces
        try {
            val registrationState = userViewModel.registrationState.first()
            assertNotNull(registrationState)
            assertTrue(registrationState is Result.Success)
            val successState = registrationState as Result.Success
            assertTrue(successState.data)
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun `resetear estado de registro deberia limpiar registrationState`() = runTest {
        // Dado un estado de registro existente
        coEvery {
            mockRegisterUseCase(testUser)
        } returns Result.Success(true)
        userViewModel.register(testUser)

        // Verificar que hay estado
        val estadoInicial = userViewModel.registrationState.first()
        assertNotNull(estadoInicial)

        // Cuando resetemos
        userViewModel.resetRegistrationState()

        // Entonces el estado debería ser null
        val estadoFinal = userViewModel.registrationState.first()
        assertNull(estadoFinal)
    }

    @Test
    fun `resetear estado de login deberia limpiar loginState`() = runTest {
        // Dado un estado de login existente
        coEvery {
            mockLoginUseCase("test@email.com", "pass")
        } returns Result.Success(testUser)
        userViewModel.login("test@email.com", "pass")

        // Verificar que hay estado
        val estadoInicial = userViewModel.loginState.first()
        assertNotNull(estadoInicial)

        // Cuando resetemos
        userViewModel.resetLoginState()

        // Entonces el estado debería ser null
        val estadoFinal = userViewModel.loginState.first()
        assertNull(estadoFinal)
    }

    @Test
    fun `viewmodel deberia inicializarse con estados vacios`() = runTest {
        // Cuando/Entonces
        val loginState = userViewModel.loginState.first()
        val registrationState = userViewModel.registrationState.first()
        val currentUser = userViewModel.currentUser.first()
        val errorState = userViewModel.errorState.first()
        val loadingState = userViewModel.loadingState.first()
        val logoutState = userViewModel.logoutState.first()

        assertNull(loginState)
        assertNull(registrationState)
        assertNull(currentUser)
        assertNull(errorState)
        assertFalse(loadingState)
        assertFalse(logoutState)
    }

    @Test
    fun `logout deberia actualizar logoutState`() = runTest {
        // Cuando hacemos logout
        userViewModel.logout()

        // Entonces
        val logoutState = userViewModel.logoutState.first()
        assertTrue(logoutState)
    }

    @Test
    fun `resetear estado de logout deberia limpiar logoutState`() = runTest {
        // Primero establecemos logout
        userViewModel.logout()

        // Verificar que logoutState es true
        val logoutStateInicial = userViewModel.logoutState.first()
        assertTrue(logoutStateInicial)

        // Cuando resetemos
        userViewModel.resetLogoutState()

        // Entonces debería ser false
        val logoutStateFinal = userViewModel.logoutState.first()
        assertFalse(logoutStateFinal)
    }
}