package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import com.ucb.deliveryapp.features.packages.domain.usecase.DeletePackageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeletePackageUseCaseTest {

    private lateinit var useCase: DeletePackageUseCase
    private val repo: PackageRepository = mockk()

    private val packageId = "paquete_12345"

    @Before
    fun setup() {
        useCase = DeletePackageUseCase(repo)
    }

    @Test
    fun `eliminar paquete retorna exito`() = runTest {
        coEvery { repo.deletePackage(packageId) } returns Result.Success(true)

        val result = useCase(packageId)

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
        coVerify(exactly = 1) { repo.deletePackage(packageId) }
    }

    @Test
    fun `eliminar paquete retorna error si no existe`() = runTest {
        coEvery { repo.deletePackage(packageId) } returns Result.Error(Exception("Paquete no encontrado"))

        val result = useCase(packageId)

        assertTrue(result is Result.Error)
        assertEquals("Paquete no encontrado", (result as Result.Error).exception.message)
        coVerify(exactly = 1) { repo.deletePackage(packageId) }
    }
}
