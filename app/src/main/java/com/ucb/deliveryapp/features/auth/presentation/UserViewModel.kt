package com.ucb.deliveryapp.features.auth.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import com.ucb.deliveryapp.features.auth.domain.usecase.GetCurrentUserUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.LoginUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.LogoutUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.RegisterUseCase
import com.ucb.deliveryapp.features.auth.domain.usecase.UpdateUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    app: Application,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase
) : AndroidViewModel(app) {

    private companion object {
        const val TAG = "UserViewModel"
    }

    private fun appString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    private val _loginState = MutableStateFlow<Result<UserDto>?>(null)
    val loginState: StateFlow<Result<UserDto>?> = _loginState

    private val _profileUpdateState = MutableStateFlow<Result<Boolean>?>(null)
    val profileUpdateState: StateFlow<Result<Boolean>?> = _profileUpdateState

    private val _registrationState = MutableStateFlow<Result<Boolean>?>(null)
    val registrationState: StateFlow<Result<Boolean>?> = _registrationState

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    private val _logoutState = MutableStateFlow(false)
    val logoutState: StateFlow<Boolean> = _logoutState

    fun register(user: UserDto) {
        viewModelScope.launch {
            _loadingState.value = true
            _errorState.value = null
            _registrationState.value = Result.Loading

            try {
                Log.d(TAG, "Registrando usuario: ${user.email}")
                val result = registerUseCase(user)
                _registrationState.value = result

                when (result) {
                    is Result.Success -> Log.d(TAG, appString(R.string.usuario_registrado_exitosamente))
                    is Result.Error -> {
                        Log.e(TAG, appString(R.string.error_en_registro_log, result.exception.message ?: ""))
                        _errorState.value = appString(
                            R.string.error_al_registrar_usuario,
                            result.exception.message ?: appString(R.string.error_desconocido)
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, appString(R.string.error_inesperado_en_registro_log), e)
                _registrationState.value = Result.Error(e)
                _errorState.value = appString(
                    R.string.error_inesperado,
                    e.message ?: appString(R.string.error_desconocido)
                )
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
                Log.d(TAG, appString(R.string.intentando_login_para, email))
                val result = loginUseCase(email, password)
                _loginState.value = result

                when (result) {
                    is Result.Success -> {
                        _currentUser.value = result.data
                        Log.d(TAG, appString(R.string.login_exitoso_para, email))
                    }
                    is Result.Error -> {
                        Log.w(
                            TAG,
                            appString(R.string.login_fallido_para_log, email, result.exception.message ?: "")
                        )
                        _errorState.value = appString(
                            R.string.error_en_login,
                            result.exception.message ?: appString(R.string.error_desconocido)
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, appString(R.string.error_inesperado_en_login_log), e)
                _loginState.value = Result.Error(e)
                _errorState.value = appString(
                    R.string.error_inesperado,
                    e.message ?: appString(R.string.error_desconocido)
                )
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, appString(R.string.iniciando_proceso_logout))
                logoutUseCase()

                _currentUser.value = null
                _loginState.value = null
                _logoutState.value = true

                Log.d(TAG, appString(R.string.logout_completado_exitosamente))
            } catch (e: Exception) {
                Log.e(TAG, appString(R.string.error_durante_logout_log), e)
                _currentUser.value = null
                _loginState.value = null
                _logoutState.value = true
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _loadingState.value = true
            try {
                val user = getCurrentUserUseCase()
                _currentUser.value = user
            } catch (e: Exception) {
                Log.e(TAG, appString(R.string.error_cargando_usuario_actual_log), e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun updateUserProfile(updatedUser: UserDto) {
        viewModelScope.launch {
            _loadingState.value = true
            _errorState.value = null
            _profileUpdateState.value = Result.Loading

            try {
                val result = updateUserUseCase(updatedUser.id, updatedUser)
                _profileUpdateState.value = result

                when (result) {
                    is Result.Success -> {
                        _currentUser.value = updatedUser
                        Log.d(TAG, appString(R.string.perfil_actualizado_exitosamente))
                    }
                    is Result.Error -> {
                        Log.e(
                            TAG,
                            appString(R.string.error_actualizando_perfil_log, result.exception.message ?: "")
                        )
                        _errorState.value = appString(
                            R.string.error_al_actualizar_perfil,
                            result.exception.message ?: appString(R.string.error_desconocido)
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, appString(R.string.error_inesperado_actualizando_perfil_log), e)
                _profileUpdateState.value = Result.Error(e)
                _errorState.value = appString(
                    R.string.error_inesperado,
                    e.message ?: appString(R.string.error_desconocido)
                )
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun clearError() { _errorState.value = null }

    fun resetRegistrationState() { _registrationState.value = null }
    fun resetLoginState() { _loginState.value = null }
    fun resetLogoutState() { _logoutState.value = false }
    fun resetProfileUpdateState() { _profileUpdateState.value = null }
}