package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.model.PackagePriority
import com.ucb.deliveryapp.features.packages.domain.model.PackageStatus
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import com.ucb.deliveryapp.features.packages.domain.usecase.GetUserPackagesUseCase
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
class GetUserPackagesUseCaseTest {

    private lateinit var useCase: GetUserPackagesUseCase
    private val repo: PackageRepository = mockk()

    private val testUserId = "usuario_12345"

    private val now = 1_700_000_000_000L
    private val testPackages = listOf(
        Package(
            id = "pkg1",
            trackingNumber = "UCB111111",
            senderName = "Remitente 1",
            recipientName = "Destinatario 1",
            recipientAddress = "Dirección 1",
            recipientPhone = "1111111111",
            weight = 2.5,
            status = PackageStatus.PENDING,
            priority = PackagePriority.NORMAL,
            estimatedDeliveryAtMillis = now + 86_400_000L,
            createdAtMillis = now,
            deliveredAtMillis = null,
            notes = "Notas 1",
            userId = testUserId
        ),
        Package(
            id = "pkg2",
            trackingNumber = "UCB222222",
            senderName = "Remitente 2",
            recipientName = "Destinatario 2",
            recipientAddress = "Dirección 2",
            recipientPhone = "2222222222",
            weight = 7.5,
            status = PackageStatus.IN_TRANSIT,
            priority = PackagePriority.EXPRESS,
            estimatedDeliveryAtMillis = now + 172_800_000L,
            createdAtMillis = now,
            deliveredAtMillis = null,
            notes = "Notas 2",
            userId = testUserId
        )
    )

    @Before
    fun setup() {
        useCase = GetUserPackagesUseCase(repo)
    }

    @Test
    fun `obtener paquetes del usuario deberia retornar exito con lista de paquetes`() = runTest {
        coEvery { repo.getUserPackages(testUserId) } returns Result.Success(testPackages)

        val result = useCase(testUserId)

        assertTrue(result is Result.Success)
        val paquetes = (result as Result.Success).data
        assertEquals(2, paquetes.size)
        assertEquals("pkg1", paquetes[0].id)
        assertEquals("pkg2", paquetes[1].id)

        coVerify(exactly = 1) { repo.getUserPackages(testUserId) }
    }

    @Test
    fun `obtener paquetes deberia retornar lista vacia cuando usuario no tiene paquetes`() = runTest {
        coEvery { repo.getUserPackages(testUserId) } returns Result.Success(emptyList())

        val result = useCase(testUserId)

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())

        coVerify(exactly = 1) { repo.getUserPackages(testUserId) }
    }

    @Test
    fun `obtener paquetes deberia retornar error cuando el repositorio falla`() = runTest {
        val msg = "Error al obtener paquetes"
        coEvery { repo.getUserPackages(testUserId) } returns Result.Error(Exception(msg))

        val result = useCase(testUserId)

        assertTrue(result is Result.Error)
        assertEquals(msg, (result as Result.Error).exception.message)

        coVerify(exactly = 1) { repo.getUserPackages(testUserId) }
    }

    @Test
    fun `obtener paquetes deberia pasar el userId correcto al repositorio`() = runTest {
        val slotUserId = slot<String>()
        coEvery { repo.getUserPackages(capture(slotUserId)) } returns Result.Success(emptyList())

        useCase(testUserId)

        assertEquals(testUserId, slotUserId.captured)
        coVerify(exactly = 1) { repo.getUserPackages(any()) }
    }
}