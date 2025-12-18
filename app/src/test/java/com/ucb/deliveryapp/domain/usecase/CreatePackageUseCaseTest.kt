package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.model.PackagePriority
import com.ucb.deliveryapp.features.packages.domain.model.PackageStatus
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import com.ucb.deliveryapp.features.packages.domain.usecase.CreatePackageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePackageUseCaseTest {

    private lateinit var useCase: CreatePackageUseCase
    private val repo: PackageRepository = mockk()

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
        // AJUSTA ESTO A TU MODELO REAL:
        estimatedDeliveryAtMillis = 0L,
        createdAtMillis = 0L,
        deliveredAtMillis = null,
        notes = "Notas test",
        userId = "user123"
    )

    @Before
    fun setup() {
        useCase = CreatePackageUseCase(repo)
    }

    @Test
    fun `crear paquete retorna exito con ID`() = runTest {
        val expectedId = "paquete_12345"
        coEvery { repo.createPackage(testPackage) } returns Result.Success(expectedId)

        val result = useCase(testPackage)

        assertTrue(result is Result.Success)
        assertEquals(expectedId, (result as Result.Success).data)
        coVerify(exactly = 1) { repo.createPackage(testPackage) }
    }

    @Test
    fun `crear paquete retorna error cuando repo falla`() = runTest {
        val msg = "Error al crear paquete"
        coEvery { repo.createPackage(testPackage) } returns Result.Error(Exception(msg))

        val result = useCase(testPackage)

        assertTrue(result is Result.Error)
        assertEquals(msg, (result as Result.Error).exception.message)
        coVerify(exactly = 1) { repo.createPackage(testPackage) }
    }
}
