package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import com.ucb.deliveryapp.features.auth.domain.repository.UserRepository
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.auth.domain.usecase.LoginUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginUseCaseTest {

    private lateinit var loginUseCase: LoginUseCase
    private val mockUserRepository: UserRepository = mockk()

    @Before
    fun setup() {
        loginUseCase = LoginUseCase(mockUserRepository)
    }

    @Test
    fun `iniciar sesion deberia retornar exito cuando las credenciales son validas`() = runTest {
        val email = "test@ucb.edu.bo"
        val password = "password123"
        val usuarioEsperado = UserDto(
            id = "user123",
            email = email,
            username = "Usuario Test",
            password = "hashed_password"
        )

        coEvery {
            mockUserRepository.login(email, password)
        } returns Result.Success(usuarioEsperado)

        val resultado = loginUseCase(email, password)

        assertTrue(resultado is Result.Success)
        val exito = resultado as Result.Success
        assertEquals(usuarioEsperado.id, exito.data.id)
        assertEquals(usuarioEsperado.email, exito.data.email)
    }

    @Test
    fun `iniciar sesion deberia retornar error cuando las credenciales son invalidas`() = runTest {
        val email = "incorrecto@email.com"
        val password = "contrasena_incorrecta"

        coEvery {
            mockUserRepository.login(email, password)
        } returns Result.Error(Exception("Credenciales inválidas"))

        val resultado = loginUseCase(email, password)

        assertTrue(resultado is Result.Error)
        val error = resultado as Result.Error
        assertTrue(error.exception.message!!.contains("inválidas"))
    }

    @Test
    fun `iniciar sesion deberia manejar email vacio`() = runTest {
        val email = ""
        val password = "password123"

        coEvery {
            mockUserRepository.login(email, password)
        } returns Result.Error(Exception("Email no puede estar vacío"))

        val resultado = loginUseCase(email, password)

        assertTrue(resultado is Result.Error)
    }

    @Test
    fun `iniciar sesion deberia manejar contraseña vacia`() = runTest {
        val email = "test@ucb.edu.bo"
        val password = ""

        coEvery {
            mockUserRepository.login(email, password)
        } returns Result.Error(Exception("Contraseña no puede estar vacía"))

        val resultado = loginUseCase(email, password)

        assertTrue(resultado is Result.Error)
    }

    @Test
    fun `iniciar sesion deberia pasar las credenciales correctas al repositorio`() = runTest {
        val email = "test@ucb.edu.bo"
        val password = "password123"
        val slotEmail = slot<String>()
        val slotPassword = slot<String>()

        coEvery {
            mockUserRepository.login(
                capture(slotEmail),
                capture(slotPassword)
            )
        } returns Result.Success(
            UserDto(id = "1", email = email, username = "test", password = "hash")
        )

        loginUseCase(email, password)

        assertEquals(email, slotEmail.captured)
        assertEquals(password, slotPassword.captured)
    }
}