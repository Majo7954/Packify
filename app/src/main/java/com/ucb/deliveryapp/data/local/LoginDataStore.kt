package com.ucb.deliveryapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first // ← AGREGAR ESTE IMPORT
import kotlinx.coroutines.flow.map

// Extension property para DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class LoginDataStore(private val context: Context) {

    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_LOGGED_IN = stringPreferencesKey("is_logged_in")
    }

    suspend fun saveUserSession(userId: String, email: String, userName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[USER_EMAIL] = email
            preferences[USER_NAME] = userName
            preferences[IS_LOGGED_IN] = "true"
        }
    }

    suspend fun getUserId(): String? {
        return context.dataStore.data
            .map { preferences ->
                preferences[USER_ID]
            }
            .first()
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    val isUserLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_LOGGED_IN] == "true"
        }

    val userId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID]
        }

    val userEmail: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_EMAIL]
        }
}