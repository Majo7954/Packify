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
class CreatePackageUseCaseTest {

    private lateinit var createPackageUseCase: CreatePackageUseCase
    private val mockPackageRepository: PackageRepository = mockk()

    private val testPackage = Package(
        id = "",
        trackingNumber = "UCB123456789",
        senderName = "Remitente Test",
        recipientName = "Destinatario Test",
        recipientAddress = "Dirección Test 123",
        recipientPhone = "1234567890",
        weight = 5.0,
        status = PackageStatus.PENDING,
        priority = PackagePriority.NORMAL,
        estimatedDeliveryDate = Timestamp.now(),
        createdAt = Timestamp.now(),
        deliveredAt = null,
        notes = "Notas test",
        userId = "user123"
    )

    @Before
    fun setup() {
        createPackageUseCase = CreatePackageUseCase(mockPackageRepository)
    }

    @Test
    fun `crear paquete deberia retornar exito con ID del paquete`() = runTest {
        val idEsperado = "paquete_12345"
        coEvery {
            mockPackageRepository.createPackage(testPackage)
        } returns Result.Success(idEsperado)

        val resultado = createPackageUseCase(testPackage)

        assertTrue(resultado is Result.Success)
        assertEquals(idEsperado, (resultado as Result.Success).data)
    }

    @Test
    fun `crear paquete deberia retornar error cuando el repositorio falla`() = runTest {
        val mensajeError = "Error al crear paquete"
        coEvery {
            mockPackageRepository.createPackage(testPackage)
        } returns Result.Error(Exception(mensajeError))

        val resultado = createPackageUseCase(testPackage)

        assertTrue(resultado is Result.Error)
        assertEquals(mensajeError, (resultado as Result.Error).exception.message)
    }

    @Test
    fun `crear paquete deberia manejar errores de red`() = runTest {
        coEvery {
            mockPackageRepository.createPackage(testPackage)
        } returns Result.Error(Exception("Red no disponible"))

        val resultado = createPackageUseCase(testPackage)

        assertTrue(resultado is Result.Error)
        assertTrue((resultado as Result.Error).exception.message!!.contains("Red"))
    }

    @Test
    fun `crear paquete deberia pasar el paquete correcto al repositorio`() = runTest {
        val slotPackage = slot<Package>()
        coEvery {
            mockPackageRepository.createPackage(capture(slotPackage))
        } returns Result.Success("test_id")

        createPackageUseCase(testPackage)

        assertNotNull(slotPackage.captured)
        assertEquals(testPackage.trackingNumber, slotPackage.captured.trackingNumber)
        assertEquals(testPackage.userId, slotPackage.captured.userId)
        assertEquals(testPackage.status, slotPackage.captured.status)
    }
}