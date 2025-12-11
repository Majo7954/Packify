package com.ucb.deliveryapp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ucb.deliveryapp.data.repository.UserRepositoryImpl
import com.ucb.deliveryapp.domain.usecase.LoginUseCase
import com.ucb.deliveryapp.domain.usecase.RegisterUseCase

class UserViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            // Crear repositorio con contexto de aplicación
            val repository = UserRepositoryImpl(application.applicationContext)
            val loginUseCase = LoginUseCase(repository)
            val registerUseCase = RegisterUseCase(repository)
            return UserViewModel(application, loginUseCase, registerUseCase) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}