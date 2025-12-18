package com.ucb.deliveryapp.core.di

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ucb.deliveryapp.features.auth.data.repository.UserRepositoryImpl
import com.ucb.deliveryapp.features.auth.domain.repository.UserRepository
import com.ucb.deliveryapp.features.auth.domain.usecase.GetCurrentUserUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.LoginUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.LogoutUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.RegisterUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.UpdateUserUseCase
import com.ucb.deliveryapp.features.auth.presentation.UserViewModel
import com.ucb.deliveryapp.features.packages.data.repository.PackageRepositoryImpl
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import com.ucb.deliveryapp.features.packages.domain.usecase.CreatePackageUseCase
import com.ucb.deliveryapp.features.packages.domain.usecase.DeletePackageUseCase
import com.ucb.deliveryapp.features.packages.domain.usecase.GetPackageByIdUseCase
import com.ucb.deliveryapp.features.packages.domain.usecase.GetUserPackagesUseCase
import com.ucb.deliveryapp.features.packages.domain.usecase.MarkAsDeliveredUseCase
import com.ucb.deliveryapp.features.packages.domain.usecase.UpdatePackageStatusUseCase
import com.ucb.deliveryapp.features.packages.presentation.PackageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single<FirebaseAuth> { Firebase.auth }
    single<FirebaseFirestore> { Firebase.firestore }

    single<PackageRepository> { PackageRepositoryImpl(context = androidContext()) }
    single<UserRepository> { UserRepositoryImpl(context = androidContext()) }

    factory { CreatePackageUseCase(get()) }
    factory { GetUserPackagesUseCase(get()) }
    factory { GetPackageByIdUseCase(get()) }
    factory { UpdatePackageStatusUseCase(get()) }
    factory { MarkAsDeliveredUseCase(get()) }
    factory { DeletePackageUseCase(get()) }

    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { UpdateUserUseCase(get()) }

    viewModel { PackageViewModel(androidContext() as Application, repository = get()) }

    viewModel {
        UserViewModel(
            app = androidContext() as Application,
            loginUseCase = get(),
            registerUseCase = get(),
            logoutUseCase = get(),
            getCurrentUserUseCase = get(),
            updateUserUseCase = get()
        )
    }
}
