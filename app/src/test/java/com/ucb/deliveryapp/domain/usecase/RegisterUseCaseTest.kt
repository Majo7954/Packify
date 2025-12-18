package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import com.ucb.deliveryapp.features.auth.domain.repository.UserRepository
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.auth.domain.usecase.RegisterUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterUseCaseTest {

    private lateinit var registerUseCase: RegisterUseCase
    private val mockUserRepository: UserRepository = mockk()

    private val testUser = UserDto(
        id = "",
        email = "test@ucb.edu.bo",
        username = "testuser",
        password = "password123"
    )

    @Before
    fun setup() {
        registerUseCase = RegisterUseCase(mockUserRepository)
    }

    @Test
    fun `registrar usuario deberia retornar exito cuando el registro es exitoso`() = runTest {
        // Dado que
        coEvery {
            mockUserRepository.registerUser(testUser)
        } returns Result.Success(true)

        // Cuando
        val resultado = registerUseCase(testUser)

        // Entonces
        assertTrue(resultado is Result.Success)
        assertTrue((resultado as Result.Success).data)
    }

    @Test
    fun `registrar usuario deberia retornar error cuando el email ya existe`() = runTest {
        // Dado que
        val mensajeError = "El email ya está registrado"
        coEvery {
            mockUserRepository.registerUser(testUser)
        } returns Result.Error(Exception(mensajeError))

        // Cuando
        val resultado = registerUseCase(testUser)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertEquals(mensajeError, (resultado as Result.Error).exception.message)
    }

    @Test
    fun `registrar usuario deberia manejar contraseñas debiles`() = runTest {
        // Dado que
        val usuarioContrasenaDebil = testUser.copy(password = "123")
        coEvery {
            mockUserRepository.registerUser(usuarioContrasenaDebil)
        } returns Result.Error(Exception("Contraseña demasiado débil"))

        // Cuando
        val resultado = registerUseCase(usuarioContrasenaDebil)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertTrue((resultado as Result.Error).exception.message!!.contains("débil"))
    }

    @Test
    fun `registrar usuario deberia pasar el usuario correcto al repositorio`() = runTest {
        // Dado que
        val slotUsuario = slot<UserDto>()
        coEvery {
            mockUserRepository.registerUser(capture(slotUsuario))
        } returns Result.Success(true)

        // Cuando
        registerUseCase(testUser)

        // Entonces
        assertNotNull(slotUsuario.captured)
        assertEquals(testUser.email, slotUsuario.captured.email)
        assertEquals(testUser.username, slotUsuario.captured.username)
    }
}