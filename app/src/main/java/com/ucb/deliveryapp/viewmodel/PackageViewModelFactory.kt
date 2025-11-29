package com.ucb.deliveryapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ucb.deliveryapp.data.repository.PackageRepositoryImpl

class PackageViewModelFactory(
    private val packageRepository: PackageRepositoryImpl
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackageViewModel::class.java)) {
            return PackageViewModel(packageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}