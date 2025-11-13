// PackageViewModelFactory.kt
package com.ucb.deliveryapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ucb.deliveryapp.repository.PackageRepository
import com.ucb.deliveryapp.viewmodel.PackageViewModel

/**
 * Factory class para crear instancias de PackageViewModel.
 *
 * Esta clase es necesaria porque PackageViewModel tiene un parámetro en su constructor
 * (PackageRepository), y ViewModelProvider no puede crear ViewModels con parámetros
 * por sí solo.
 */
class PackageViewModelFactory(
    private val packageRepository: PackageRepository
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass La clase del ViewModel a crear
     * @return Una instancia del ViewModel con el repository inyectado
     * @throws IllegalArgumentException si la clase del ViewModel no es PackageViewModel
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackageViewModel::class.java)) {
            return PackageViewModel(packageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}