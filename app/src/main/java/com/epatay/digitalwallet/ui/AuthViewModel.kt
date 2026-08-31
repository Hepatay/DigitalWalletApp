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
    data class Loading(val message: String = "Lütfen bekleyin...") : AuthState()
    object SignedOut : AuthState()
    data class Authenticated(val uid: String, val name: String?, val email: String?, val photoUrl: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
    data class ActionSuccess(val message: String) : AuthState()
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

    fun checkCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated(user.uid, user.displayName, user.email, user.photoUrl?.toString())
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun deleteAccountAndData(context: android.content.Context) {
        _authState.value = AuthState.Loading("Hesabınız ve tüm verileriniz siliniyor...")
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                val uid = user.uid
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        syncManager.deleteAllUserDataFromFirebase(uid)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error deleting cloud data", e)
                }

                try {
                    user.delete().await()
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Firebase User Delete Failed (fallback to signOut)", e)
                    try {
                        auth.signOut()
                    } catch (ex: Exception) {
                        Log.e("AuthViewModel", "Firebase signOut fallback failed", ex)
                    }
                }
            } else {
                try {
                    auth.signOut()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Firebase signOut failed", e)
                }
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
                Log.e("AuthViewModel", "Local DB Clear Failed", e)
            }

            val prefs = context.getSharedPreferences("wallet_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_guest_mode", false).apply()

            _authState.value = AuthState.ActionSuccess(context.getString(R.string.delete_account_success))
        }
    }

    fun signInWithGoogle(context: android.content.Context) {
        _authState.value = AuthState.Loading("Google ile giriş yapılıyor...")
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                val webClientId = if (resId != 0) {
                    context.getString(resId)
                } else {
                    "430423683284-3o47lpmpj6fmfgvptut8u580crve4qe9.apps.googleusercontent.com"
                }

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
            _authState.value = AuthState.Loading("Kimlik doğrulanıyor...")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                _authState.value = AuthState.Loading("Verileriniz hesabınıza aktarılıyor ve eşitleniyor...")
                
                // 1. Assign any guest records in Room to this user
                syncManager.assignGuestDataToUser()

                // 2. Pull any existing remote records from Firebase cloud
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        syncManager.pullDataFromFirebase(user.uid)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Initial Pull Failed", e)
                }

                // 3. Synchronously push any existing local data to Firebase cloud immediately
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        syncManager.pushDataToFirebase(user.uid)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Initial Push Failed", e)
                }

                FirebaseSyncWorker.trigger(getApplication())

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
        _authState.value = AuthState.Loading("Verileriniz buluta yedekleniyor...")
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

            _authState.value = AuthState.Loading("Güvenli çıkış yapılıyor...")

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
            
            val prefs = context.getSharedPreferences("wallet_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_guest_mode", false).apply()

            _authState.value = AuthState.SignedOut
        }
    }
}
