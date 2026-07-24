package com.zacksimpson.reminders.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Signed-out vs signed-in, mirroring [DataState]'s Loading/Ready/Corrupt shape but for
 *  auth rather than task data. */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val uid: String, val email: String) : AuthState
}

/**
 * Stores the signed-in account's tokens for phone<->desktop sync in the same shared
 * DataStore instance [RemindersRepository] already uses, under its own preference keys.
 * [AuthClient] does the actual network calls; this only persists what comes back and
 * decides when a stored ID token needs refreshing.
 */
class AuthRepository(private val dataStore: DataStore<Preferences>) {
    private val client = AuthClient()

    val state: Flow<AuthState> = dataStore.data.map { p ->
        val uid = p[UID_KEY]
        val email = p[EMAIL_KEY]
        if (uid != null && email != null) AuthState.SignedIn(uid, email) else AuthState.SignedOut
    }

    suspend fun signIn(email: String, password: String) {
        val tokens = client.signInWithPassword(email, password)
        persist(tokens, email)
    }

    fun close() = client.close()

    suspend fun signOut() {
        dataStore.edit { p ->
            p.remove(UID_KEY)
            p.remove(EMAIL_KEY)
            p.remove(ID_TOKEN_KEY)
            p.remove(REFRESH_TOKEN_KEY)
            p.remove(EXPIRES_AT_KEY)
        }
    }

    /** A valid ID token for authenticated Firestore REST calls (Phase 3), refreshing
     *  first if the stored one is expired or about to be. Null if signed out. */
    suspend fun validIdToken(): String? {
        val prefs = dataStore.data.first()
        val refreshToken = prefs[REFRESH_TOKEN_KEY] ?: return null
        val idToken = prefs[ID_TOKEN_KEY]
        val expiresAt = prefs[EXPIRES_AT_KEY] ?: 0L
        if (idToken != null && System.currentTimeMillis() < expiresAt - REFRESH_MARGIN_MS) {
            return idToken
        }
        val email = prefs[EMAIL_KEY] ?: return null
        val tokens = client.refresh(refreshToken)
        persist(tokens, email)
        return tokens.idToken
    }

    private suspend fun persist(tokens: AuthTokens, email: String) {
        dataStore.edit { p ->
            p[UID_KEY] = tokens.uid
            p[EMAIL_KEY] = email
            p[ID_TOKEN_KEY] = tokens.idToken
            p[REFRESH_TOKEN_KEY] = tokens.refreshToken
            p[EXPIRES_AT_KEY] = tokens.expiresAt
        }
    }

    private companion object {
        val UID_KEY = stringPreferencesKey("auth:uid")
        val EMAIL_KEY = stringPreferencesKey("auth:email")
        val ID_TOKEN_KEY = stringPreferencesKey("auth:idToken")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("auth:refreshToken")
        val EXPIRES_AT_KEY = longPreferencesKey("auth:expiresAt")

        // Refresh a bit before actual expiry so a call made right at the boundary
        // doesn't get rejected by Firestore for using a token that expired mid-flight.
        const val REFRESH_MARGIN_MS = 60_000L
    }
}

/** Same WhileSubscribed/initial-value/catch convention RemindersRepository.dataStateIn uses —
 *  an unreadable DataStore falls back to SignedOut rather than crashing the collector. */
fun AuthRepository.authStateIn(scope: CoroutineScope): StateFlow<AuthState> =
    state
        .catch { emit(AuthState.SignedOut) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)
