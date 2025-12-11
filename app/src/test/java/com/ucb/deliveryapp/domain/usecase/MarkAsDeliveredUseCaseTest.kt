package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.domain.repository.PackageRepository
import com.ucb.deliveryapp.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarkAsDeliveredUseCaseTest {

    private lateinit var markAsDeliveredUseCase: MarkAsDeliveredUseCase
    private val mockPackageRepository: PackageRepository = mockk()

    private val testPackageId = "paquete_12345"

    @Before
    fun setup() {
        markAsDeliveredUseCase = MarkAsDeliveredUseCase(mockPackageRepository)
    }

    @Test
    fun `marcar como entregado deberia retornar exito cuando la operacion es exitosa`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.markAsDelivered(testPackageId)
        } returns Result.Success(true)

        // Cuando
        val resultado = markAsDeliveredUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Success)
        assertTrue((resultado as Result.Success).data)
    }

    @Test
    fun `marcar como entregado deberia retornar error cuando el paquete no existe`() = runTest {
        // Dado que
        val mensajeError = "Paquete no encontrado"
        coEvery {
            mockPackageRepository.markAsDelivered(testPackageId)
        } returns Result.Error(Exception(mensajeError))

        // Cuando
        val resultado = markAsDeliveredUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertEquals(mensajeError, (resultado as Result.Error).exception.message)
    }

    @Test
    fun `marcar como entregado deberia pasar el packageId correcto al repositorio`() = runTest {
        // Dado que
        val slotPackageId = slot<String>()
        coEvery {
            mockPackageRepository.markAsDelivered(capture(slotPackageId))
        } returns Result.Success(true)

        // Cuando
        markAsDeliveredUseCase(testPackageId)

        // Entonces
        assertEquals(testPackageId, slotPackageId.captured)
    }

    @Test
    fun `marcar como entregado deberia manejar errores de base de datos`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.markAsDelivered(testPackageId)
        } returns Result.Error(Exception("Conexión a base de datos fallida"))

        // Cuando
        val resultado = markAsDeliveredUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertTrue((resultado as Result.Error).exception.message!!.contains("base de datos"))
    }
}