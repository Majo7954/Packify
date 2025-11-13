package com.ucb.deliveryapp.viewmodel

import android.content.Context
import com.ucb.deliveryapp.data.db.AppDatabase
import com.ucb.deliveryapp.repository.PackageRepository

/**
 * Función de utilidad para crear y obtener una instancia de PackageViewModelFactory.
 *
 * Esta función centraliza la creación de las dependencias (Database y Repository)
 * necesarias para construir el PackageViewModel. De esta forma, la UI (Activity o Composable)
 * no necesita conocer los detalles de implementación.
 *
 * @param context El contexto de la aplicación, necesario para obtener la base de datos.
 * @return Una instancia de [PackageViewModelFactory] lista para ser usada.
 */
fun getViewModelFactory(context: Context): PackageViewModelFactory {
    // 1. Obtiene la instancia de la base de datos de Room.
    val database = AppDatabase.getDatabase(context.applicationContext)

    // 2. Crea la instancia del repositorio, pasándole el DAO necesario.
    val packageRepository = PackageRepository(database.packageDao())

    // 3. Crea y devuelve la fábrica del ViewModel, inyectando el repositorio.
    return PackageViewModelFactory(packageRepository)
}
