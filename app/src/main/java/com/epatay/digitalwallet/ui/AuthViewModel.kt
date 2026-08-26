package com.epatay.digitalwallet.ui

import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.sync.FirebaseSyncManager
import com.epatay.digitalwallet.sync.FirebaseSyncWorker
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val uid: String, val name: String?, val email: String?, val photoUrl: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val syncManager = FirebaseSyncManager(application)
    private val workManager = WorkManager.getInstance(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated(user.uid, user.displayName, user.email, user.photoUrl?.toString())
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun signInWithGoogle(context: android.content.Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val webClientId = "488008293284-dv4epl69lh4ni2vu8lec7jukrg0p6a4q.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    firebaseAuthWithGoogle(idToken)
                } else {
                    _authState.value = AuthState.Error("Geçersiz kimlik doğrulama türü.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign In Failed", e)
                _authState.value = AuthState.Error("Giriş iptal edildi veya başarısız oldu: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                // Giriş başarılı, yerel kayıtları Firebase kullanıcısına bağla
                syncManager.assignGuestDataToUser()
                syncManager.pullDataFromFirebase(user.uid)

                // Firebase Sync Worker'ı OneTimeWorkRequest ile tetikle
                val syncRequest = OneTimeWorkRequestBuilder<FirebaseSyncWorker>().build()
                workManager.enqueue(syncRequest)

                _authState.value = AuthState.Authenticated(user.uid, user.displayName, user.email, user.photoUrl?.toString())
            } else {
                _authState.value = AuthState.Error("Firebase kimlik doğrulama hatası.")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Firebase Auth Failed", e)
            _authState.value = AuthState.Error("Kimlik doğrulama başarısız oldu: ${e.localizedMessage}")
        }
    }

    fun signOut(context: android.content.Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        syncManager.pushDataToFirebase(user.uid)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error pushing data before logout", e)
                }
            }

            try {
                auth.signOut()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Firebase Logout Failed", e)
            }
            
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Credential Manager Clear Failed", e)
            }
            
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    syncManager.clearDatabaseOnLogout()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Database Clear Failed", e)
            }
            
            _authState.value = AuthState.Idle
        }
    }
}
