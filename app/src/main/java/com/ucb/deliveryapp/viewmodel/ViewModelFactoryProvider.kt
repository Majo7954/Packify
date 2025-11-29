package com.ucb.deliveryapp.viewmodel

import android.content.Context
import com.ucb.deliveryapp.data.repository.PackageRepositoryImpl

/**
 * Función de utilidad para crear y obtener una instancia de PackageViewModelFactory.
 * Ahora usa Firebase Firestore en lugar de Room.
 */
fun getPackageViewModelFactory(context: Context): PackageViewModelFactory {
    // Crear la instancia del repositorio para Firebase (sin necesidad de Room)
    val packageRepository = PackageRepositoryImpl()

    // Crear y devolver la fábrica del ViewModel
    return PackageViewModelFactory(packageRepository)
}

/**
 * Función de utilidad para crear y obtener una instancia de UserViewModelFactory.
 */
fun getUserViewModelFactory(context: Context): UserViewModelFactory {
    return UserViewModelFactory(context.applicationContext as android.app.Application)
}