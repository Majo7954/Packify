package com.ucb.deliveryapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.repository.PackageRepositoryImpl
import com.ucb.deliveryapp.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PackageViewModel(private val repository: PackageRepositoryImpl) : ViewModel() {

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
     */
    fun loadUserPackages(userId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _packagesState.value = Result.Loading

            try {
                val result = repository.getUserPackages(userId)
                _packagesState.value = result
            } catch (e: Exception) {
                _packagesState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
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
                _selectedPackageState.value = result
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
     * Actualiza el estado de un paquete.
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