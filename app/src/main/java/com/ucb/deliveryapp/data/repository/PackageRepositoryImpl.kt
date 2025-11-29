package com.ucb.deliveryapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.domain.repository.PackageRepository
import com.ucb.deliveryapp.util.Result
import kotlinx.coroutines.tasks.await

class PackageRepositoryImpl : PackageRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val packagesCollection = db.collection("packages")

    override suspend fun createPackage(pkg: Package): Result<String> {
        return try {
            // Generar ID automático de Firestore
            val documentRef = packagesCollection.document()
            val packageWithId = pkg.copy(id = documentRef.id)

            documentRef.set(packageWithId).await()
            Result.Success(documentRef.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePackage(pkg: Package): Result<Boolean> {
        return try {
            packagesCollection.document(pkg.id).set(pkg).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deletePackage(packageId: String): Result<Boolean> {
        return try {
            packagesCollection.document(packageId).delete().await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAllPackages(): Result<List<Package>> {
        return try {
            val packages = packagesCollection
                .orderBy("createdAt") // CORREGIDO: "createdAt" no "created_at"
                .get()
                .await()
                .toObjects(Package::class.java)
            Result.Success(packages)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUserPackages(userId: String): Result<List<Package>> {
        return try {
            val packages = packagesCollection
                .whereEqualTo("userId", userId) // CORREGIDO: "userId" no "user_id"
                .orderBy("createdAt") // CORREGIDO: "createdAt" no "created_at"
                .get()
                .await()
                .toObjects(Package::class.java)
            Result.Success(packages)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getPackageById(packageId: String): Result<Package> {
        return try {
            val packageDoc = packagesCollection.document(packageId).get().await()
            val packageObj = packageDoc.toObject(Package::class.java)
            if (packageObj != null) Result.Success(packageObj)
            else Result.Error(Exception("Package not found"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun trackPackage(trackingNumber: String): Result<Package> {
        return try {
            val query = packagesCollection
                .whereEqualTo("trackingNumber", trackingNumber) // CORREGIDO: "trackingNumber" no "tracking_number"
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val packageObj = query.documents[0].toObject(Package::class.java)
                if (packageObj != null) Result.Success(packageObj)
                else Result.Error(Exception("Package not found"))
            } else {
                Result.Error(Exception("Package not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePackageStatus(packageId: String, newStatus: String): Result<Boolean> {
        return try {
            packagesCollection.document(packageId)
                .update("status", newStatus)
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markAsDelivered(packageId: String): Result<Boolean> {
        return try {
            val updates = mapOf(
                "status" to "delivered",
                "deliveredAt" to com.google.firebase.Timestamp.now() // CORREGIDO: "deliveredAt" no "delivered_at"
            )
            packagesCollection.document(packageId).update(updates).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}