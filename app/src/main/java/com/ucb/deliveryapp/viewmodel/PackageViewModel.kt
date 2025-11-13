package com.ucb.deliveryapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucb.deliveryapp.repository.PackageRepository
import com.ucb.deliveryapp.data.entity.Package // Asegúrate de importar tu clase Package
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar toda la lógica de negocio relacionada con los paquetes.
 *
 * @param repository El repositorio que proporciona acceso a los datos de los paquetes.
 */
class PackageViewModel(private val repository: PackageRepository) : ViewModel() {

    // LiveData para la lista de paquetes de un usuario. La UI observará esto.
    private val _packages = MutableLiveData<List<Package>>()
    val packages: LiveData<List<Package>> = _packages

    // LiveData para un solo paquete, usado en la pantalla de detalles.
    private val _selectedPackage = MutableLiveData<Package?>()
    val selectedPackage: LiveData<Package?> = _selectedPackage

    /**
     * Obtiene todos los paquetes asociados a un ID de usuario específico.
     * Actualiza el LiveData `_packages`.
     *
     * @param userId El ID del usuario cuyos paquetes se quieren obtener.
     */
    fun loadUserPackages(userId: Int) {
        viewModelScope.launch {
            try {
                // Llama a la función suspendida del repositorio
                val userPackages = repository.getUserPackages(userId)
                _packages.postValue(userPackages)
            } catch (e: Exception) {
                // Manejar el error, por ejemplo, mostrando un mensaje en la UI
                // a través de otro LiveData.
                _packages.postValue(emptyList()) // Informa a la UI que la lista está vacía
            }
        }
    }

    /**
     * Obtiene un paquete específico por su ID.
     * Actualiza el LiveData `_selectedPackage`.
     *
     * @param packageId El ID del paquete a obtener.
     */
    fun loadPackageById(packageId: Int) {
        viewModelScope.launch {
            try {
                val pkg = repository.getPackageById(packageId)
                _selectedPackage.postValue(pkg)
            } catch (e: Exception) {
                _selectedPackage.postValue(null)
            }
        }
    }

    /**
     * Crea un nuevo paquete en la base de datos.
     *
     * @param pkg El objeto Package a insertar.
     */
    fun createPackage(pkg: Package) {
        viewModelScope.launch {
            repository.createPackage(pkg)
            // Opcional: Recargar la lista de paquetes después de crear uno nuevo.
            // loadUserPackages(pkg.userId)
        }
    }

    /**
     * Actualiza el estado de un paquete.
     *
     * @param packageId El ID del paquete a actualizar.
     * @param newStatus El nuevo estado para el paquete.
     */
    fun updatePackageStatus(packageId: Int, newStatus: String) {
        viewModelScope.launch {
            repository.updatePackageStatus(packageId, newStatus)
            // Recarga los detalles del paquete para que la UI se actualice
            loadPackageById(packageId)
        }
    }

    /**
     * Marca un paquete como entregado.
     *
     * @param packageId El ID del paquete a marcar como entregado.
     */
    fun markAsDelivered(packageId: Int) {
        viewModelScope.launch {
            repository.markAsDelivered(packageId)
            loadPackageById(packageId)
        }
    }

    /**
     * Elimina un paquete de la base de datos.
     *
     * @param pkg El objeto Package a eliminar.
     */
    fun deletePackage(pkg: Package) {
        viewModelScope.launch {
            repository.deletePackage(pkg)
        }
    }
}
