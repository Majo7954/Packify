// kotlin+java/com/ucb/deliveryapp/data/repository/PackageRepositoryImpl.kt
package com.ucb.deliveryapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class PackageRepositoryImpl : com.ucb.deliveryapp.domain.repository.PackageRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val packagesCollection = db.collection("packages")

    override suspend fun createPackage(pkg: com.ucb.deliveryapp.data.entity.Package): com.ucb.deliveryapp.util.Result<String> {
        return try {
            val documentRef = packagesCollection.document()
            val packageWithId = pkg.copy(id = documentRef.id)
            documentRef.set(packageWithId).await()
            com.ucb.deliveryapp.util.Result.Success(documentRef.id)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun updatePackage(pkg: com.ucb.deliveryapp.data.entity.Package): com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            packagesCollection.document(pkg.id).set(pkg).await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun deletePackage(packageId: String): com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            packagesCollection.document(packageId).delete().await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun getAllPackages(): com.ucb.deliveryapp.util.Result<List<com.ucb.deliveryapp.data.entity.Package>> {
        return try {
            val packages = packagesCollection
                .orderBy("createdAt") // camelCase
                .get()
                .await()
                .toObjects(com.ucb.deliveryapp.data.entity.Package::class.java)
            com.ucb.deliveryapp.util.Result.Success(packages)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun getUserPackages(userId: String): com.ucb.deliveryapp.util.Result<List<com.ucb.deliveryapp.data.entity.Package>> {
        return try {
            val packages = packagesCollection
                .whereEqualTo("userId", userId) // camelCase
                .orderBy("createdAt") // camelCase
                .get()
                .await()
                .toObjects(com.ucb.deliveryapp.data.entity.Package::class.java)
            com.ucb.deliveryapp.util.Result.Success(packages)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun getPackageById(packageId: String): com.ucb.deliveryapp.util.Result<com.ucb.deliveryapp.data.entity.Package> {
        return try {
            val packageDoc = packagesCollection.document(packageId).get().await()
            val packageObj = packageDoc.toObject(com.ucb.deliveryapp.data.entity.Package::class.java)
            if (packageObj != null) com.ucb.deliveryapp.util.Result.Success(packageObj)
            else com.ucb.deliveryapp.util.Result.Error(Exception("Package not found"))
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun trackPackage(trackingNumber: String): com.ucb.deliveryapp.util.Result<com.ucb.deliveryapp.data.entity.Package> {
        return try {
            val query = packagesCollection
                .whereEqualTo("trackingNumber", trackingNumber) // camelCase
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

    override suspend fun updatePackageStatus(packageId: String, newStatus: String): com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            packagesCollection.document(packageId)
                .update("status", newStatus)
                .await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }

    override suspend fun markAsDelivered(packageId: String): com.ucb.deliveryapp.util.Result<Boolean> {
        return try {
            val updates = mapOf(
                "status" to "delivered",
                "deliveredAt" to com.google.firebase.Timestamp.now() // camelCase
            )
            packagesCollection.document(packageId).update(updates).await()
            com.ucb.deliveryapp.util.Result.Success(true)
        } catch (e: Exception) {
            com.ucb.deliveryapp.util.Result.Error(e)
        }
    }
}