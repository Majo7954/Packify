package com.ucb.deliveryapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ucb.deliveryapp.repository.PackageRepository
import com.ucb.deliveryapp.data.entity.Package

class PackageViewModel(private val repository: PackageRepository) : ViewModel() {

  // Cambiar de LiveData a StateFlow para Compose
  private val _packages = MutableStateFlow<List<Package>>(emptyList())
  val packages: StateFlow<List<Package>> = _packages.asStateFlow()

  private val _selectedPackage = MutableStateFlow<Package?>(null)
  val selectedPackage: StateFlow<Package?> = _selectedPackage.asStateFlow()

  /**
   * Obtiene todos los paquetes asociados a un ID de usuario específico.
   */
  fun loadUserPackages(userId: Int) {
    viewModelScope.launch {
      try {
        val userPackages = repository.getUserPackages(userId)
        _packages.value = userPackages
      } catch (e: Exception) {
        _packages.value = emptyList()
      }
    }
  }

  /**
   * Obtiene un paquete específico por su ID.
   */
  fun loadPackageById(packageId: Int) {
    viewModelScope.launch {
      try {
        val pkg = repository.getPackageById(packageId)
        _selectedPackage.value = pkg
      } catch (e: Exception) {
        _selectedPackage.value = null
      }
    }
  }

  /**
   * Crea un nuevo paquete en la base de datos.
   */
  fun createPackage(pkg: Package) {
    viewModelScope.launch {
      try {
        repository.createPackage(pkg)
        // Recargar la lista después de crear
        loadUserPackages(pkg.userId)
      } catch (e: Exception) {
        // Manejar error
      }
    }
  }

  /**
   * Actualiza el estado de un paquete.
   */
  fun updatePackageStatus(packageId: Int, newStatus: String) {
    viewModelScope.launch {
      try {
        repository.updatePackageStatus(packageId, newStatus)
        loadPackageById(packageId)
      } catch (e: Exception) {
        // Manejar error
      }
    }
  }

  /**
   * Marca un paquete como entregado.
   */
  fun markAsDelivered(packageId: Int) {
    viewModelScope.launch {
      try {
        repository.markAsDelivered(packageId)
        loadPackageById(packageId)
      } catch (e: Exception) {
        // Manejar error
      }
    }
  }

  /**
   * Elimina un paquete de la base de datos.
   */
  fun deletePackage(pkg: Package) {
    viewModelScope.launch {
      try {
        repository.deletePackage(pkg)
        // Recargar la lista después de eliminar
        loadUserPackages(pkg.userId)
      } catch (e: Exception) {
        // Manejar error
      }
    }
  }
}
