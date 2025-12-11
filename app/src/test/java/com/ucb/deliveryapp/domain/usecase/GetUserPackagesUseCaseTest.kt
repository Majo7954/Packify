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
class GetUserPackagesUseCaseTest {

    private lateinit var getUserPackagesUseCase: GetUserPackagesUseCase
    private val mockPackageRepository: PackageRepository = mockk()

    private val testUserId = "usuario_12345"
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
            estimatedDeliveryDate = Timestamp.now(),
            createdAt = Timestamp.now(),
            deliveredAt = null,
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
            estimatedDeliveryDate = Timestamp.now(),
            createdAt = Timestamp.now(),
            deliveredAt = null,
            notes = "Notas 2",
            userId = testUserId
        )
    )

    @Before
    fun setup() {
        getUserPackagesUseCase = GetUserPackagesUseCase(mockPackageRepository)
    }

    @Test
    fun `obtener paquetes del usuario deberia retornar exito con lista de paquetes`() = runTest {
        coEvery {
            mockPackageRepository.getUserPackages(testUserId)
        } returns Result.Success(testPackages)

        val resultado = getUserPackagesUseCase(testUserId)

        assertTrue(resultado is Result.Success)
        val paquetes = (resultado as Result.Success).data
        assertEquals(2, paquetes.size)
        assertEquals("pkg1", paquetes[0].id)
        assertEquals("pkg2", paquetes[1].id)
    }

    @Test
    fun `obtener paquetes deberia retornar lista vacia cuando usuario no tiene paquetes`() = runTest {
        coEvery {
            mockPackageRepository.getUserPackages(testUserId)
        } returns Result.Success(emptyList())

        val resultado = getUserPackagesUseCase(testUserId)

        assertTrue(resultado is Result.Success)
        assertTrue((resultado as Result.Success).data.isEmpty())
    }

    @Test
    fun `obtener paquetes deberia retornar error cuando el repositorio falla`() = runTest {
        val mensajeError = "Error al obtener paquetes"
        coEvery {
            mockPackageRepository.getUserPackages(testUserId)
        } returns Result.Error(Exception(mensajeError))

        val resultado = getUserPackagesUseCase(testUserId)

        assertTrue(resultado is Result.Error)
        assertEquals(mensajeError, (resultado as Result.Error).exception.message)
    }

    @Test
    fun `obtener paquetes deberia pasar el userId correcto al repositorio`() = runTest {
        val slotUserId = slot<String>()
        coEvery {
            mockPackageRepository.getUserPackages(capture(slotUserId))
        } returns Result.Success(emptyList())

        getUserPackagesUseCase(testUserId)

        assertEquals(testUserId, slotUserId.captured)
    }
}