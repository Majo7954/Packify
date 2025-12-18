package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.usecase.UpdatePackageStatusUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatePackageStatusUseCaseTest {

    private lateinit var updatePackageStatusUseCase: UpdatePackageStatusUseCase
    private val mockPackageRepository: PackageRepository = mockk()

    private val testPackageId = "paquete_12345"
    private val testNewStatus = "en_transito"

    @Before
    fun setup() {
        updatePackageStatusUseCase = UpdatePackageStatusUseCase(mockPackageRepository)
    }

    @Test
    fun `actualizar estado del paquete deberia retornar exito cuando la actualizacion es exitosa`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.updatePackageStatus(testPackageId, testNewStatus)
        } returns Result.Success(true)

        // Cuando
        val resultado = updatePackageStatusUseCase(testPackageId, testNewStatus)

        // Entonces
        assertTrue(resultado is Result.Success)
        assertTrue((resultado as Result.Success).data)
    }

    @Test
    fun `actualizar estado deberia retornar error cuando el paquete no existe`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.updatePackageStatus(testPackageId, testNewStatus)
        } returns Result.Error(Exception("Paquete no encontrado"))

        // Cuando
        val resultado = updatePackageStatusUseCase(testPackageId, testNewStatus)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertEquals("Paquete no encontrado", (resultado as Result.Error).exception.message)
    }

    @Test
    fun `actualizar estado deberia pasar los parametros correctos al repositorio`() = runTest {
        // Dado que
        val slotPackageId = slot<String>()
        val slotStatus = slot<String>()
        coEvery {
            mockPackageRepository.updatePackageStatus(
                capture(slotPackageId),
                capture(slotStatus)
            )
        } returns Result.Success(true)

        // Cuando
        updatePackageStatusUseCase(testPackageId, testNewStatus)

        // Entonces
        assertEquals(testPackageId, slotPackageId.captured)
        assertEquals(testNewStatus, slotStatus.captured)
    }

    @Test
    fun `actualizar estado deberia manejar estados invalidos`() = runTest {
        // Dado que
        val estadoInvalido = "estado_invalido"
        coEvery {
            mockPackageRepository.updatePackageStatus(testPackageId, estadoInvalido)
        } returns Result.Error(Exception("Estado inválido: $estadoInvalido"))

        // Cuando
        val resultado = updatePackageStatusUseCase(testPackageId, estadoInvalido)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertTrue((resultado as Result.Error).exception.message!!.contains("inválido"))
    }
}