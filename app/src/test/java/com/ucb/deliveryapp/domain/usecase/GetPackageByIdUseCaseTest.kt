package com.ucb.deliveryapp.domain.usecase

import com.google.firebase.Timestamp
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackagePriority
import com.ucb.deliveryapp.data.entity.PackageStatus
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
class GetPackageByIdUseCaseTest {

    private lateinit var getPackageByIdUseCase: GetPackageByIdUseCase
    private val mockPackageRepository: PackageRepository = mockk()

    private val testPackageId = "paquete_12345"
    private val testPackage = Package(
        id = testPackageId,
        trackingNumber = "UCB987654321",
        senderName = "Remitente Test",
        recipientName = "Destinatario Test",
        recipientAddress = "Dirección Test",
        recipientPhone = "9876543210",
        weight = 3.5,
        status = PackageStatus.IN_TRANSIT,
        priority = PackagePriority.EXPRESS,
        estimatedDeliveryDate = Timestamp.now(),
        createdAt = Timestamp.now(),
        deliveredAt = null,
        notes = "Paquete test",
        userId = "usuario123"
    )

    @Before
    fun setup() {
        getPackageByIdUseCase = GetPackageByIdUseCase(mockPackageRepository)
    }

    @Test
    fun `obtener paquete por ID deberia retornar exito con el paquete`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.getPackageById(testPackageId)
        } returns Result.Success(testPackage)

        // Cuando
        val resultado = getPackageByIdUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Success)
        val paquete = (resultado as Result.Success).data
        assertEquals(testPackageId, paquete.id)
        assertEquals("UCB987654321", paquete.trackingNumber)
        assertEquals("Remitente Test", paquete.senderName)
    }

    @Test
    fun `obtener paquete por ID deberia retornar error cuando el paquete no existe`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.getPackageById(testPackageId)
        } returns Result.Error(Exception("Paquete no encontrado"))

        // Cuando
        val resultado = getPackageByIdUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertEquals("Paquete no encontrado", (resultado as Result.Error).exception.message)
    }

    @Test
    fun `obtener paquete por ID deberia pasar el packageId correcto al repositorio`() = runTest {
        // Dado que
        val slotPackageId = slot<String>()
        coEvery {
            mockPackageRepository.getPackageById(capture(slotPackageId))
        } returns Result.Success(testPackage)

        // Cuando
        getPackageByIdUseCase(testPackageId)

        // Entonces
        assertEquals(testPackageId, slotPackageId.captured)
    }

    @Test
    fun `obtener paquete por ID deberia manejar errores de red`() = runTest {
        // Dado que
        coEvery {
            mockPackageRepository.getPackageById(testPackageId)
        } returns Result.Error(Exception("Timeout de red"))

        // Cuando
        val resultado = getPackageByIdUseCase(testPackageId)

        // Entonces
        assertTrue(resultado is Result.Error)
        assertTrue((resultado as Result.Error).exception.message!!.contains("Timeout"))
    }
}