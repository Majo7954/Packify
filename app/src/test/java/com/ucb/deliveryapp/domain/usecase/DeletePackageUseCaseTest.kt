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
class DeletePackageUseCaseTest {

    private lateinit var deletePackageUseCase: DeletePackageUseCase
    private val mockPackageRepository: PackageRepository = mockk()

    private val testPackageId = "paquete_12345"

    @Before
    fun setup() {
        deletePackageUseCase = DeletePackageUseCase(mockPackageRepository)
    }

    @Test
    fun `eliminar paquete deberia retornar exito cuando la eliminacion es exitosa`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.deletePackage(testPackageId)
        } returns Result.Success(true)

        // Cuando
        val resultado = deletePackageUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Success)
        assertTrue((resultado as Result.Success).data)
    }

    @Test
    fun `eliminar paquete deberia retornar error cuando el paquete no existe`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.deletePackage(testPackageId)
        } returns Result.Error(Exception("Paquete no encontrado"))

        // Cuando
        val resultado = deletePackageUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertEquals("Paquete no encontrado", (resultado as Result.Error).exception.message)
    }

    @Test
    fun `eliminar paquete deberia pasar el packageId correcto al repositorio`() = runTest {
        // Dado que
        val slotPackageId = slot<String>()
        coEvery {
            mockPackageRepository.deletePackage(capture(slotPackageId))
        } returns Result.Success(true)

        // Cuando
        deletePackageUseCase(testPackageId)

        // Entonces
        assertEquals(testPackageId, slotPackageId.captured)
    }

    @Test
    fun `eliminar paquete deberia manejar errores de permisos`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.deletePackage(testPackageId)
        } returns Result.Error(Exception("Permiso denegado para eliminar"))

        // Cuando
        val resultado = deletePackageUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertTrue((resultado as Result.Error).exception.message!!.contains("Permiso"))
    }
}