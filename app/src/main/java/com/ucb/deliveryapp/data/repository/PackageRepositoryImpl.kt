package com.ucb.deliveryapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class PackageRepositoryImpl(
    private val context: Context? = null
) : com.ucb.deliveryapp.domain.repository.PackageRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val packagesCollection = db.collection("packages")

    // Verificador de conexión a internet (opcional si hay contexto)
    private fun isOnline(): Boolean {
        // Si no hay contexto, asumimos que está online
        if (context == null) return true

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override suspend fun createPackage(pkg: com.ucb.deliveryapp.data.entity.Package):
            com.ucb.deliveryapp.util.Result<String> {
        return try {
            // Verificar conexión (si tenemos contexto)
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet. Guardando localmente...")
                )
            }

            val documentRef = packagesCollection.document()
            val packageWithId = pkg.copy(id = documentRef.id)
            documentRef.set(packageWithId).await()
            com.ucb.deliveryapp.util.Result.Success(documentRef.id)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun updatePackage(pkg: com.ucb.deliveryapp.data.entity.Package):
            com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet")
                )
            }

            packagesCollection.document(pkg.id).set(pkg).await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun deletePackage(packageId: String):
            com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet")
                )
            }

            packagesCollection.document(packageId).delete().await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun getAllPackages():
            com.ucb.deliveryapp.util.Result<List<com.ucb.deliveryapp.data.entity.Package>> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet. Conéctate para ver los paquetes.")
                )
            }

            val packages = packagesCollection
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(com.ucb.deliveryapp.data.entity.Package::class.java)
            com.ucb.deliveryapp.util.Result.Success(packages)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun getUserPackages(userId: String):
            com.ucb.deliveryapp.util.Result<List<com.ucb.deliveryapp.data.entity.Package>> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet. Intenta más tarde.")
                )
            }

            val packages = packagesCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt")
                .get()
                .await()
                .toObjects(com.ucb.deliveryapp.data.entity.Package::class.java)
            com.ucb.deliveryapp.util.Result.Success(packages)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun getPackageById(packageId: String):
            com.ucb.deliveryapp.util.Result<com.ucb.deliveryapp.data.entity.Package> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet. No se puede cargar el paquete.")
                )
            }

            val packageDoc = packagesCollection.document(packageId).get().await()
            val packageObj = packageDoc.toObject(com.ucb.deliveryapp.data.entity.Package::class.java)
            if (packageObj != null) com.ucb.deliveryapp.util.Result.Success(packageObj)
            else com.ucb.deliveryapp.util.Result.Error(Exception("Package not found"))
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun trackPackage(trackingNumber: String):
            com.ucb.deliveryapp.util.Result<com.ucb.deliveryapp.data.entity.Package> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet. No se puede rastrear.")
                )
            }

            val query = packagesCollection
                .whereEqualTo("trackingNumber", trackingNumber)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val packageObj = query.documents[0].toObject(com.ucb.deliveryapp.data.entity.Package::class.java)
                if (packageObj != null) com.ucb.deliveryapp.util.Result.Success(packageObj)
                else com.ucb.deliveryapp.util.Result.Error(Exception("Package not found"))
            } else {
                com.ucb.deliveryapp.util.Result.Error(Exception("Package not found"))
            }
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun updatePackageStatus(packageId: String, newStatus: String):
            com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet. No se puede actualizar el estado.")
                )
            }

            packagesCollection.document(packageId)
                .update("status", newStatus)
                .await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun markAsDelivered(packageId: String):
            com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            if (context != null && !isOnline()) {
                return com.ucb.deliveryapp.util.Result.Error(
                    Exception("Sin conexión a internet. No se puede marcar como entregado.")
                )
            }

            val updates = mapOf(
                "status" to "delivered",
                "deliveredAt" to com.google.firebase.Timestamp.now()
            )
            packagesCollection.document(packageId).update(updates).await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }
}