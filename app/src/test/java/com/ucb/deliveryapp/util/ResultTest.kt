package com.ucb.deliveryapp.util

import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `resultado Success deberia contener datos`() {
        // Dado que
        val testData = "Datos de prueba"
        val successResult = Result.Success(testData)

        // Cuando/Entonces
        assertTrue(successResult is Result.Success)
        assertEquals(testData, successResult.data)
    }

    @Test
    fun `resultado Error deberia contener excepcion`() {
        // Dado que
        val testException = Exception("Error de prueba")
        val errorResult = Result.Error(testException)

        // Cuando/Entonces
        assertTrue(errorResult is Result.Error)
        assertEquals(testException, errorResult.exception)
        assertEquals("Error de prueba", errorResult.exception.message)
    }

    @Test
    fun `resultado Loading deberia ser objeto singleton`() {
        // Dado que/ Cuando/ Entonces
        val loadingResult = Result.Loading
        assertTrue(loadingResult is Result.Loading)
        assertSame(Result.Loading, loadingResult)
    }

    @Test
    fun `equals deberia funcionar para Success`() {
        // Dado que
        val result1 = Result.Success("test")
        val result2 = Result.Success("test")
        val result3 = Result.Success("diferente")

        // Cuando/Entonces
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `equals deberia funcionar para Error`() {
        // Dado que
        val exception1 = Exception("error")
        val exception2 = Exception("error")
        val result1 = Result.Error(exception1)
        val result2 = Result.Error(exception2)
        val result3 = Result.Error(Exception("diferente"))

        // Cuando/Entonces
        // Nota: Exception no implementa equals por contenido
        assertNotEquals(result1, result2) // Diferentes instancias
        assertNotEquals(result1, result3)
    }

    @Test
    fun `toString deberia mostrar informacion correcta para Success`() {
        // Dado que
        val result = Result.Success(42)

        // Cuando/Entonces
        assertTrue(result.toString().contains("Success"))
        assertTrue(result.toString().contains("42"))
    }

    @Test
    fun `toString deberia mostrar informacion correcta para Error`() {
        // Dado que
        val result = Result.Error(Exception("Test error"))

        // Cuando/Entonces
        assertTrue(result.toString().contains("Error"))
        assertTrue(result.toString().contains("Test error"))
    }

    @Test
    fun `toString deberia mostrar informacion correcta para Loading`() {
        // Dado que/ Cuando/ Entonces
        val result = Result.Loading
        assertTrue(result.toString().contains("Loading"))
    }

    @Test
    fun `when expression deberia funcionar con todos los tipos de Result`() {
        // Dado que
        val successResult: Result<String> = Result.Success("éxito")
        val errorResult: Result<String> = Result.Error(Exception("fallo"))
        val loadingResult: Result<String> = Result.Loading

        // Cuando/Entonces para Success
        when (successResult) {
            is Result.Success -> assertEquals("éxito", successResult.data)
            is Result.Error -> fail("Debería ser Success")
            Result.Loading -> fail("Debería ser Success")
        }

        // Cuando/Entonces para Error
        when (errorResult) {
            is Result.Success -> fail("Debería ser Error")
            is Result.Error -> assertEquals("fallo", errorResult.exception.message)
            Result.Loading -> fail("Debería ser Error")
        }

        // Cuando/Entonces para Loading
        when (loadingResult) {
            is Result.Success -> fail("Debería ser Loading")
            is Result.Error -> fail("Debería ser Loading")
            Result.Loading -> assertTrue(true) // Correcto
        }
    }
}