package com.ucb.deliveryapp.features.packages.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucb.deliveryapp.core.notification.NotificationHelper
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.model.PackageStatus
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PackageViewModel(
    private val app: Application,
    private val repository: PackageRepository
) : AndroidViewModel(app) {

    private val _packagesState = MutableStateFlow<Result<List<Package>>?>(null)
    val packagesState: StateFlow<Result<List<Package>>?> = _packagesState

    private val _selectedPackageState = MutableStateFlow<Result<Package>?>(null)
    val selectedPackageState: StateFlow<Result<Package>?> = _selectedPackageState

    private val _createPackageState = MutableStateFlow<Result<String>?>(null)
    val createPackageState: StateFlow<Result<String>?> = _createPackageState

    private val _updatePackageState = MutableStateFlow<Result<Boolean>?>(null)
    val updatePackageState: StateFlow<Result<Boolean>?> = _updatePackageState

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    fun loadUserPackages(userId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _packagesState.value = Result.Loading

            try {
                val result = repository.getUserPackages(userId)
                when (result) {
                    is Result.Success -> {
                        val updatedPackages = result.data.map { pkg ->
                            applyAutoStatusTransition(pkg)
                        }

                        updatedPackages.forEach { updatedPkg ->
                            val originalStatus = getOriginalStatus(result.data, updatedPkg.id)
                            if (updatedPkg.status != originalStatus) {
                                updatePackageStatusInBackground(updatedPkg.id, updatedPkg.status)

                                NotificationHelper.showPackageStatusChanged(
                                    context = app,
                                    packageId = updatedPkg.id,
                                    trackingNumber = updatedPkg.trackingNumber,
                                    newStatusText = toStatusText(updatedPkg.status)
                                )
                            }
                        }

                        _packagesState.value = Result.Success(updatedPackages)
                    }

                    is Result.Error -> _packagesState.value = result
                    else -> _packagesState.value = result
                }
            } catch (e: Exception) {
                _packagesState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun loadPackageById(packageId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _selectedPackageState.value = Result.Loading

            try {
                val result = repository.getPackageById(packageId)
                if (result is Result.Success) {
                    val updated = applyAutoStatusTransition(result.data)

                    if (updated.status != result.data.status) {
                        updatePackageStatusInBackground(packageId, updated.status)

                        NotificationHelper.showPackageStatusChanged(
                            context = app,
                            packageId = packageId,
                            trackingNumber = updated.trackingNumber,
                            newStatusText = toStatusText(updated.status)
                        )

                        _selectedPackageState.value = Result.Success(updated)
                    } else {
                        _selectedPackageState.value = result
                    }
                } else {
                    _selectedPackageState.value = result
                }
            } catch (e: Exception) {
                _selectedPackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun createPackage(pkg: Package) {
        viewModelScope.launch {
            _loadingState.value = true
            _createPackageState.value = Result.Loading

            try {
                val result = repository.createPackage(pkg)
                _createPackageState.value = result

                if (result is Result.Success) {
                    loadUserPackages(pkg.userId)
                }
            } catch (e: Exception) {
                _createPackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun updatePackageStatus(packageId: String, newStatus: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _updatePackageState.value = Result.Loading

            try {
                val pre = repository.getPackageById(packageId)
                val tracking = (pre as? Result.Success)?.data?.trackingNumber ?: packageId

                val result = repository.updatePackageStatus(packageId, newStatus)
                _updatePackageState.value = result

                if (result is Result.Success && result.data) {
                    NotificationHelper.showPackageStatusChanged(
                        context = app,
                        packageId = packageId,
                        trackingNumber = tracking,
                        newStatusText = toStatusText(newStatus)
                    )

                    loadPackageById(packageId)
                }
            } catch (e: Exception) {
                _updatePackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun markAsDelivered(packageId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _updatePackageState.value = Result.Loading

            try {
                val pre = repository.getPackageById(packageId)
                val tracking = (pre as? Result.Success)?.data?.trackingNumber ?: packageId

                val result = repository.markAsDelivered(packageId)
                _updatePackageState.value = result

                if (result is Result.Success && result.data) {
                    NotificationHelper.showPackageStatusChanged(
                        context = app,
                        packageId = packageId,
                        trackingNumber = tracking,
                        newStatusText = toStatusText(PackageStatus.DELIVERED)
                    )

                    loadPackageById(packageId)
                }
            } catch (e: Exception) {
                _updatePackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun deletePackage(packageId: String, userId: String) {
        viewModelScope.launch {
            _loadingState.value = true
            try {
                val result = repository.deletePackage(packageId)
                if (result is Result.Success && result.data) {
                    loadUserPackages(userId)
                }
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun trackPackage(trackingNumber: String) {
        viewModelScope.launch {
            _loadingState.value = true
            _selectedPackageState.value = Result.Loading

            try {
                val result = repository.trackPackage(trackingNumber)
                _selectedPackageState.value = result
            } catch (e: Exception) {
                _selectedPackageState.value = Result.Error(e)
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun resetCreatePackageState() { _createPackageState.value = null }
    fun resetUpdatePackageState() { _updatePackageState.value = null }
    fun resetSelectedPackage() { _selectedPackageState.value = null }

    private fun updatePackageStatusInBackground(packageId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                repository.updatePackageStatus(packageId, newStatus)
            } catch (_: Exception) {
            }
        }
    }

    private fun getOriginalStatus(originalPackages: List<Package>, packageId: String): String {
        return originalPackages.find { it.id == packageId }?.status ?: ""
    }

    private fun applyAutoStatusTransition(packageItem: Package): Package {
        if (packageItem.status == PackageStatus.DELIVERED ||
            packageItem.status == PackageStatus.CANCELLED
        ) return packageItem

        val nowMillis = System.currentTimeMillis()
        val createdAtMillis = packageItem.createdAtMillis
        if (createdAtMillis <= 0L) return packageItem

        val hoursSinceCreation = TimeUnit.MILLISECONDS.toHours(nowMillis - createdAtMillis)

        val newStatus = when (packageItem.status) {
            PackageStatus.PENDING -> {
                if (hoursSinceCreation >= 4) PackageStatus.IN_TRANSIT else PackageStatus.PENDING
            }
            PackageStatus.IN_TRANSIT -> {
                val daysSinceCreation = TimeUnit.MILLISECONDS.toDays(nowMillis - createdAtMillis)
                if (daysSinceCreation >= 2) PackageStatus.DELIVERED else PackageStatus.IN_TRANSIT
            }
            else -> packageItem.status
        }

        return if (newStatus != packageItem.status) packageItem.copy(status = newStatus) else packageItem
    }

    private fun toStatusText(status: String): String {
        return when (status) {
            PackageStatus.PENDING -> "Pendiente"
            PackageStatus.IN_TRANSIT -> "En tránsito"
            PackageStatus.DELIVERED -> "Entregado"
            PackageStatus.CANCELLED -> "Cancelado"
            else -> status
        }
    }
}