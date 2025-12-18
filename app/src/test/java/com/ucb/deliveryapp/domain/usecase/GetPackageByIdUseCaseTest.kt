package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.model.PackagePriority
import com.ucb.deliveryapp.features.packages.domain.model.PackageStatus
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import com.ucb.deliveryapp.features.packages.domain.usecase.GetPackageByIdUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetPackageByIdUseCaseTest {

    private lateinit var useCase: GetPackageByIdUseCase
    private val repo: PackageRepository = mockk()

    private val testPackageId = "paquete_12345"

    private val now = 1_700_000_000_000L
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
        estimatedDeliveryAtMillis = now + 172_800_000L,
        createdAtMillis = now,
        deliveredAtMillis = null,
        notes = "Paquete test",
        userId = "usuario123"
    )

    @Before
    fun setup() {
        useCase = GetPackageByIdUseCase(repo)
    }

    @Test
    fun `obtener paquete por ID deberia retornar exito con el paquete`() = runTest {
        coEvery { repo.getPackageById(testPackageId) } returns Result.Success(testPackage)

        val result = useCase(testPackageId)

        assertTrue(result is Result.Success)
        val paquete = (result as Result.Success).data
        assertEquals(testPackageId, paquete.id)
        assertEquals("UCB987654321", paquete.trackingNumber)
        assertEquals("Remitente Test", paquete.senderName)

        coVerify(exactly = 1) { repo.getPackageById(testPackageId) }
    }

    @Test
    fun `obtener paquete por ID deberia retornar error cuando el paquete no existe`() = runTest {
        coEvery { repo.getPackageById(testPackageId) } returns Result.Error(Exception("Paquete no encontrado"))

        val result = useCase(testPackageId)

        assertTrue(result is Result.Error)
        assertEquals("Paquete no encontrado", (result as Result.Error).exception.message)

        coVerify(exactly = 1) { repo.getPackageById(testPackageId) }
    }

    @Test
    fun `obtener paquete por ID deberia pasar el packageId correcto al repositorio`() = runTest {
        val slotPackageId = slot<String>()
        coEvery { repo.getPackageById(capture(slotPackageId)) } returns Result.Success(testPackage)

        useCase(testPackageId)

        assertEquals(testPackageId, slotPackageId.captured)
        coVerify(exactly = 1) { repo.getPackageById(any()) }
    }

    @Test
    fun `obtener paquete por ID deberia manejar errores de red`() = runTest {
        coEvery { repo.getPackageById(testPackageId) } returns Result.Error(Exception("Timeout de red"))

        val result = useCase(testPackageId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception.message!!.contains("Timeout"))

        coVerify(exactly = 1) { repo.getPackageById(testPackageId) }
    }
}