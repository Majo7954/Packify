// kotlin+java/com/ucb/deliveryapp/viewmodel/UserViewModel.kt
package com.ucb.deliveryapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.data.repository.UserRepositoryImpl
import com.ucb.deliveryapp.domain.usecase.LoginUseCase
import com.ucb.deliveryapp.domain.usecase.RegisterUseCase
import com.ucb.deliveryapp.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    app: Application,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : AndroidViewModel(app) {

    private companion object {
        const val TAG = "UserViewModel"
    }

    // Estados para la UI
    private val _loginState = MutableStateFlow<Result<User>?>(null)
    val loginState: StateFlow<Result<User>?> = _loginState

    private val _profileUpdateState = MutableStateFlow<Result<Boolean>?>(null)
    val profileUpdateState: StateFlow<Result<Boolean>?> = _profileUpdateState

    private val _registrationState = MutableStateFlow<Result<Boolean>?>(null)
    val registrationState: StateFlow<Result<Boolean>?> = _registrationState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    // NUEVO: Estado para logout
    private val _logoutState = MutableStateFlow(false)
    val logoutState: StateFlow<Boolean> = _logoutState

    fun register(user: User) {
        viewModelScope.launch {
            _loadingState.value = true
            _errorState.value = null
            _registrationState.value = Result.Loading

            try {
                Log.d(TAG, "Registrando usuario: ${user.email}")
                val result = registerUseCase(user)
                _registrationState.value = result

                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "Usuario registrado exitosamente")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error en registro: ${result.exception.message}")
                        _errorState.value = "Error al registrar usuario: ${result.exception.message}"
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en registro", e)
                _registrationState.value = Result.Error(e)
                _errorState.value = "Error inesperado: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _errorState.value = null
            _loginState.value = Result.Loading

            try {
                Log.d(TAG, "Intentando login para: $email")
                val result = loginUseCase(email, password)
                _loginState.value = result

                when (result) {
                    is Result.Success -> {
                        _currentUser.value = result.data
                        Log.d(TAG, "Login exitoso para: $email")
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Login fallido para: $email - ${result.exception.message}")
                        _errorState.value = "Error en login: ${result.exception.message}"
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en login", e)
                _loginState.value = Result.Error(e)
                _errorState.value = "Error inesperado: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }

    // NUEVA FUNCIÓN: Logout completo
    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando proceso de logout")

                // Limpiar estados locales
                _currentUser.value = null
                _loginState.value = null
                _logoutState.value = true

                // Ejecutar logout en el repositorio (limpia Firebase Auth y DataStore)
                val repository = UserRepositoryImpl(getApplication())
                repository.logout()

                Log.d(TAG, "Logout completado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error durante el logout", e)
                // Aún así limpiamos los estados locales
                _currentUser.value = null
                _loginState.value = null
                _logoutState.value = true
            }
        }
    }

    // Función para limpiar errores
    fun clearError() {
        _errorState.value = null
    }

    // Función para resetear estados
    fun resetRegistrationState() {
        _registrationState.value = null
    }

    fun resetLoginState() {
        _loginState.value = null
    }

    // Función para resetear estado de logout
    fun resetLogoutState() {
        _logoutState.value = false
    }
    // FUNCIÓN PARA CARGAR USUARIO ACTUAL
    fun loadCurrentUser() {
        viewModelScope.launch {
            _loadingState.value = true
            try {
                val repository = UserRepositoryImpl(getApplication())
                val user = repository.getCurrentUser()
                _currentUser.value = user
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando usuario actual", e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    // FUNCIÓN PARA ACTUALIZAR PERFIL
    fun updateUserProfile(updatedUser: User) {
        viewModelScope.launch {
            _loadingState.value = true
            _errorState.value = null
            _profileUpdateState.value = Result.Loading

            try {
                val repository = UserRepositoryImpl(getApplication())
                val result = repository.updateUser(updatedUser.id, updatedUser)
                _profileUpdateState.value = result

                when (result) {
                    is Result.Success -> {
                        _currentUser.value = updatedUser
                        Log.d(TAG, "Perfil actualizado exitosamente")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error actualizando perfil: ${result.exception.message}")
                        _errorState.value = "Error al actualizar perfil: ${result.exception.message}"
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado actualizando perfil", e)
                _profileUpdateState.value = Result.Error(e)
                _errorState.value = "Error inesperado: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }

    // Función para resetear estado de actualización
    fun resetProfileUpdateState() {
        _profileUpdateState.value = null
    }
}