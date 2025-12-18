package com.ucb.deliveryapp.features.packages.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.data.mapper.toDomain
import com.ucb.deliveryapp.features.packages.data.mapper.toDto
import com.ucb.deliveryapp.features.packages.data.remote.dto.PackageDto
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository
import kotlinx.coroutines.tasks.await

class PackageRepositoryImpl(
    private val context: Context? = null
) : PackageRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val packagesCollection = db.collection("packages")

    private fun isOnline(): Boolean {
        if (context == null) return true
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override suspend fun createPackage(pkg: Package): Result<String> {
        return try {
            if (context != null && !isOnline()) {
                return Result.Error(Exception("Sin conexión a internet. Guardando localmente..."))
            }

            val docRef = packagesCollection.document()
            val dto = pkg.copy(id = docRef.id).toDto()
            docRef.set(dto).await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePackage(pkg: Package): Result<Boolean> {
        return try {
            if (context != null && !isOnline()) return Result.Error(Exception("Sin conexión a internet"))
            packagesCollection.document(pkg.id).set(pkg.toDto()).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deletePackage(packageId: String): Result<Boolean> {
        return try {
            if (context != null && !isOnline()) return Result.Error(Exception("Sin conexión a internet"))
            packagesCollection.document(packageId).delete().await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAllPackages(): Result<List<Package>> {
        return try {
            if (context != null && !isOnline()) {
                return Result.Error(Exception("Sin conexión a internet. Conéctate para ver los paquetes."))
            }

            val dtos = packagesCollection.orderBy("createdAt").get().await()
                .toObjects(PackageDto::class.java)

            Result.Success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUserPackages(userId: String): Result<List<Package>> {
        return try {
            if (context != null && !isOnline()) {
                return Result.Error(Exception("Sin conexión a internet. Intenta más tarde."))
            }

            val dtos = packagesCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(PackageDto::class.java)

            Result.Success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getPackageById(packageId: String): Result<Package> {
        return try {
            if (context != null && !isOnline()) {
                return Result.Error(Exception("Sin conexión a internet. No se puede cargar el paquete."))
            }

            val doc = packagesCollection.document(packageId).get().await()
            val dto = doc.toObject(PackageDto::class.java)
            if (dto != null) Result.Success(dto.toDomain())
            else Result.Error(Exception("Package not found"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun trackPackage(trackingNumber: String): Result<Package> {
        return try {
            if (context != null && !isOnline()) {
                return Result.Error(Exception("Sin conexión a internet. No se puede rastrear."))
            }

            val query = packagesCollection.whereEqualTo("trackingNumber", trackingNumber).get().await()
            val dto = query.documents.firstOrNull()?.toObject(PackageDto::class.java)

            if (dto != null) Result.Success(dto.toDomain())
            else Result.Error(Exception("Package not found"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePackageStatus(packageId: String, newStatus: String): Result<Boolean> {
        return try {
            if (context != null && !isOnline()) {
                return Result.Error(Exception("Sin conexión a internet. No se puede actualizar el estado."))
            }
            packagesCollection.document(packageId).update("status", newStatus).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markAsDelivered(packageId: String): Result<Boolean> {
        return try {
            if (context != null && !isOnline()) {
                return Result.Error(Exception("Sin conexión a internet. No se puede marcar como entregado."))
            }

            val updates = mapOf(
                "status" to "delivered",
                "deliveredAt" to Timestamp.now()
            )
            packagesCollection.document(packageId).update(updates).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
