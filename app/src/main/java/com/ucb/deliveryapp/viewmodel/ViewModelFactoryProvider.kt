package com.ucb.deliveryapp.viewmodel

import android.content.Context
import com.ucb.deliveryapp.data.repository.PackageRepositoryImpl

fun getPackageViewModelFactory(context: Context): PackageViewModelFactory {
    val packageRepository: com.ucb.deliveryapp.domain.repository.PackageRepository =
        PackageRepositoryImpl(context)

    return PackageViewModelFactory(packageRepository)
}

fun getUserViewModelFactory(application: android.app.Application): UserViewModelFactory {
    return UserViewModelFactory(application)
}