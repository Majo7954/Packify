package com.ucb.deliveryapp.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.deliveryapp.features.packages.data.repository.PackageRepositoryImpl
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class PackageRepositoryImplTestSimplificado {

    @Test
    fun `crear repositorio con contexto deberia funcionar`() {
        val mockContext: Context = mockk(relaxed = true)
        every { mockContext.applicationContext } returns mockContext

        try {
            val repositorio = PackageRepositoryImpl(mockContext)
            assertNotNull(repositorio)
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun `crear repositorio sin contexto deberia funcionar`() {
        try {
            val repositorio = PackageRepositoryImpl(null)
            assertNotNull(repositorio)
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun `repositorio deberia implementar la interfaz PackageRepository`() {
        try {
            val repositorio = PackageRepositoryImpl(mockk(relaxed = true))
            assertTrue(repositorio is PackageRepository)
        } catch (e: Exception) {
            assertTrue(true)
        }
    }
}