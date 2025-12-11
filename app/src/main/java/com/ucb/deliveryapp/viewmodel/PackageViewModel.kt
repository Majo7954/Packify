// PackageViewModel.kt - VERSIÓN CON TRANSICIONES AUTOMÁTICAS REALES
package com.ucb.deliveryapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.domain.repository.PackageRepository  // <-- CAMBIAR AQUÍ
import com.ucb.deliveryapp.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PackageViewModel(private val repository: PackageRepository) : ViewModel() {  // <-- CAMBIAR AQUÍ

    // Estados usando StateFlow (más moderno que LiveData para Compose)
    private val _packagesState = MutableStateFlow<Result<List<Package>>?>(null)
    val packagesState: StateFlow<Result<List<Package>>?> = _packagesState

    private val _selectedPackageState = MutableStateFlow<Result<Package>?>(null)
    val selectedPackageState: StateFlow<Result<Package>?> = _selectedPackageState

    private val _createPackageState = MutableStateFlow<Result<String>?>(null)
    val createPackageState: StateFlow<Result<String>?> = _createPackageState

    private val _updatePackageState = MutableStateFlow<Result<Boolean>?>(null)
    val updatePackageState: StateFlow<Result<Boolean>?> = _updatePackageState

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    /**
     * Obtiene todos los paquetes asociados a un ID de usuario específico.
     * Aplica transiciones automáticas de estado durante la carga.
     */
    fun loadUserPackages(userId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _packagesState.value = Result.Loading

            try {
                val result = repository.getUserPackages(userId)

                when (result) {
                    is Result.Success -> {
                        // ✅ APLICAR TRANSICIONES AUTOMÁTICAS A LOS PAQUETES
                        val updatedPackages = result.data.map { pkg ->
                            applyAutoStatusTransition(pkg)
                        }

                        // ✅ ACTUALIZAR EN BASE DE DATOS SI HAY CAMBIOS
                        updatedPackages.forEach { updatedPkg ->
                            if (updatedPkg.status != getOriginalStatus(result.data, updatedPkg.id)) {
                                updatePackageStatusInBackground(updatedPkg.id, updatedPkg.status)
                            }
                        }

                        _packagesState.value = Result.Success(updatedPackages)
                    }
                    is Result.Error -> {
                        _packagesState.value = result
                    }
                    else -> {
                        _packagesState.value = result
                    }
                }
            } catch (e: Exception) {
                _packagesState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Aplica transición automática de estado basado en el tiempo
     */
    private fun applyAutoStatusTransition(packageItem: Package): Package {
        // Si ya está entregado o cancelado, no cambiar
        if (packageItem.status == PackageStatus.DELIVERED ||
            packageItem.status == PackageStatus.CANCELLED) {
            return packageItem
        }

        val currentTime = Timestamp.now().seconds
        val createdAt = packageItem.createdAt.seconds
        val hoursSinceCreation = TimeUnit.SECONDS.toHours(currentTime - createdAt)

        val newStatus = when (packageItem.status) {
            PackageStatus.PENDING -> {
                // Después de 4 horas, pasa a EN TRÁNSITO
                if (hoursSinceCreation >= 4) {
                    PackageStatus.IN_TRANSIT
                } else {
                    PackageStatus.PENDING
                }
            }
            PackageStatus.IN_TRANSIT -> {
                // Después de 2 días (48 horas), pasa a ENTREGADO
                val daysSinceCreation = TimeUnit.SECONDS.toDays(currentTime - createdAt)
                if (daysSinceCreation >= 2) {
                    PackageStatus.DELIVERED
                } else {
                    PackageStatus.IN_TRANSIT
                }
            }
            else -> packageItem.status
        }

        // Solo retornar paquete modificado si cambió el estado
        return if (newStatus != packageItem.status) {
            packageItem.copy(status = newStatus)
        } else {
            packageItem
        }
    }

    /**
     * Obtiene el estado original de un paquete desde la lista original
     */
    private fun getOriginalStatus(originalPackages: List<Package>, packageId: String): String {
        return originalPackages.find { it.id == packageId }?.status ?: ""
    }

    /**
     * Actualiza el estado de un paquete en segundo plano
     */
    private fun updatePackageStatusInBackground(packageId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                repository.updatePackageStatus(packageId, newStatus)
                println("✅ Estado automático actualizado: $packageId → $newStatus")
            } catch (e: Exception) {
                println("❌ Error en actualización automática: ${e.message}")
            }
        }
    }

    /**
     * Obtiene un paquete específico por su ID.
     */
    fun loadPackageById(packageId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _selectedPackageState.value = Result.Loading

            try {
                val result = repository.getPackageById(packageId)

                // Aplicar transición automática también al cargar individualmente
                if (result is Result.Success) {
                    val updatedPackage = applyAutoStatusTransition(result.data)
                    if (updatedPackage.status != result.data.status) {
                        // Actualizar en base de datos si cambió
                        updatePackageStatusInBackground(packageId, updatedPackage.status)
                        _selectedPackageState.value = Result.Success(updatedPackage)
                    } else {
                        _selectedPackageState.value = result
                    }
                } else {
                    _selectedPackageState.value = result
                }
            } catch (e: Exception) {
                _selectedPackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Crea un nuevo paquete en Firestore.
     */
    fun createPackage(pkg: Package) {
        viewModelScope.launch {
            _loadingState.value = true
            _createPackageState.value = Result.Loading

            try {
                val result = repository.createPackage(pkg)
                _createPackageState.value = result

                // Si se creó exitosamente, recargar la lista
                if (result is Result.Success) {
                    loadUserPackages(pkg.userId)
                }
            } catch (e: Exception) {
                _createPackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Actualiza el estado de un paquete (manual).
     */
    fun updatePackageStatus(packageId: String, newStatus: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _updatePackageState.value = Result.Loading

            try {
                val result = repository.updatePackageStatus(packageId, newStatus)
                _updatePackageState.value = result

                // Recargar el paquete actualizado
                if (result is Result.Success && result.data) {
                    loadPackageById(packageId)
                }
            } catch (e: Exception) {
                _updatePackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Marca un paquete como entregado.
     */
    fun markAsDelivered(packageId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _updatePackageState.value = Result.Loading

            try {
                val result = repository.markAsDelivered(packageId)
                _updatePackageState.value = result

                // Recargar el paquete actualizado
                if (result is Result.Success && result.data) {
                    loadPackageById(packageId)
                }
            } catch (e: Exception) {
                _updatePackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Elimina un paquete de Firestore.
     */
    fun deletePackage(packageId: String, userId: String) {
        viewModelScope.launch {
            _loadingState.value = true

            try {
                val result = repository.deletePackage(packageId)
                if (result is Result.Success && result.data) {
                    // Recargar la lista después de eliminar
                    loadUserPackages(userId)
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Rastrea un paquete por número de tracking.
     */
    fun trackPackage(trackingNumber: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _selectedPackageState.value = Result.Loading

            try {
                val result = repository.trackPackage(trackingNumber)
                _selectedPackageState.value = result
            } catch (e: Exception) {
                _selectedPackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    // Funciones para resetear estados
    fun resetCreatePackageState() {
        _createPackageState.value = null
    }

    fun resetUpdatePackageState() {
        _updatePackageState.value = null
    }

    fun resetSelectedPackage() {
        _selectedPackageState.value = null
    }
}