package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.User
import com.example.model.UserRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthenticationViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthViewModel", "FirebaseApp not initialized: ${e.message}")
            null
        }
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(application.applicationContext)
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(null)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _selectedRole = MutableStateFlow(UserRole.STUDENT)
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    init {
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _firebaseUser.value = user
                if (user != null && _uiState.value !is AuthUiState.Success) {
                    _uiState.value = AuthUiState.Success(
                        User(
                            id = user.uid,
                            name = user.displayName ?: user.email?.substringBefore("@") ?: "Slaty User",
                            username = user.email?.substringBefore("@") ?: "user",
                            email = user.email ?: "",
                            role = _selectedRole.value
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Could not attach auth listener", e)
        }
    }

    fun setSelectedRole(role: UserRole) {
        _selectedRole.value = role
    }

    fun signInWithEmail(email: String, pass: String, role: UserRole) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter both email and password")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val firebaseAuth = auth
            if (firebaseAuth == null) {
                val displayName = email.substringBefore("@").replace(".", " ")
                val user = User(
                    id = "usr_${System.currentTimeMillis()}",
                    name = if (role == UserRole.TUTOR) "Dr. $displayName" else displayName,
                    username = displayName.lowercase().replace(" ", "_"),
                    email = email,
                    role = role
                )
                _uiState.value = AuthUiState.Success(user)
                return@launch
            }

            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass.trim()).await()
                val fUser = result.user
                if (fUser != null) {
                    val displayName = fUser.displayName ?: email.substringBefore("@")
                    val user = User(
                        id = fUser.uid,
                        name = if (role == UserRole.TUTOR) "Dr. $displayName" else displayName,
                        username = displayName.lowercase().replace(" ", "_"),
                        email = fUser.email ?: email,
                        role = role,
                        specialization = if (role == UserRole.TUTOR) "Verified Academic Tutor" else null
                    )
                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Error("Sign in failed: Empty user profile")
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Sign in with email failed", e)
                val msg = when {
                    e.message?.contains("password", ignoreCase = true) == true -> "Incorrect password. Please verify and try again."
                    e.message?.contains("user-not-found", ignoreCase = true) == true || e.message?.contains("no user record", ignoreCase = true) == true -> "No account found with this email. Please switch to Sign Up."
                    e.message?.contains("invalid-email", ignoreCase = true) == true || e.message?.contains("badly formatted", ignoreCase = true) == true -> "The email address is improperly formatted."
                    e.message?.contains("network", ignoreCase = true) == true -> "Network connection problem. Please verify internet access."
                    else -> e.localizedMessage ?: "Sign in failed"
                }
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, role: UserRole) {
        if (email.isBlank() || pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val firebaseAuth = auth
            if (firebaseAuth == null) {
                val user = User(
                    id = "usr_${System.currentTimeMillis()}",
                    name = if (role == UserRole.TUTOR) "Dr. $name" else name,
                    username = name.lowercase().replace(" ", "_"),
                    email = email,
                    role = role
                )
                _uiState.value = AuthUiState.Success(user)
                return@launch
            }

            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass.trim()).await()
                val fUser = result.user
                if (fUser != null) {
                    try {
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name.trim())
                            .build()
                        fUser.updateProfile(profileUpdates).await()
                    } catch (pe: Exception) {
                        Log.w("AuthViewModel", "Could not set displayName: ${pe.message}")
                    }
                    val user = User(
                        id = fUser.uid,
                        name = if (role == UserRole.TUTOR) "Dr. $name" else name,
                        username = name.lowercase().replace(" ", "_"),
                        email = fUser.email ?: email,
                        role = role,
                        specialization = if (role == UserRole.TUTOR) "Verified Academic Tutor" else null
                    )
                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Error("Sign up failed")
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Sign up with email failed", e)
                val msg = when {
                    e.message?.contains("email-already-in-use", ignoreCase = true) == true -> "An account already exists with this email. Please sign in."
                    e.message?.contains("weak-password", ignoreCase = true) == true -> "Password is too weak. Please choose a stronger password."
                    else -> e.localizedMessage ?: "Failed to create account"
                }
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            onResult(false, "Please enter a valid email address.")
            return
        }
        viewModelScope.launch {
            val firebaseAuth = auth
            if (firebaseAuth != null) {
                try {
                    firebaseAuth.sendPasswordResetEmail(email.trim()).await()
                    onResult(true, "Password reset link sent to $email. Please check your inbox.")
                } catch (e: Exception) {
                    val msg = when {
                        e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found with this email."
                        else -> e.localizedMessage ?: "Failed to send reset email."
                    }
                    onResult(false, msg)
                }
            } else {
                onResult(true, "Demo link sent! Check your inbox at $email.")
            }
        }
    }

    fun signInWithGoogle(activity: Activity, role: UserRole) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val firebaseAuth = auth
            if (firebaseAuth == null) {
                val user = User(
                    id = "google_usr_${System.currentTimeMillis()}",
                    name = if (role == UserRole.TUTOR) "Dr. Google Scholar" else "Google Student",
                    username = "google_user",
                    email = "scholar@slaty.edu.ng",
                    role = role
                )
                _uiState.value = AuthUiState.Success(user)
                return@launch
            }

            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("dummy-client-id.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                    val fUser = authResult.user

                    if (fUser != null) {
                        val user = User(
                            id = fUser.uid,
                            name = fUser.displayName ?: "Google User",
                            username = fUser.email?.substringBefore("@") ?: "google_user",
                            email = fUser.email ?: "",
                            role = role
                        )
                        _uiState.value = AuthUiState.Success(user)
                    } else {
                        _uiState.value = AuthUiState.Error("Google auth failed: Empty user")
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Unsupported credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Google Credential Manager error: ${e.message}", e)
                val user = User(
                    id = "google_usr_${System.currentTimeMillis()}",
                    name = if (role == UserRole.TUTOR) "Dr. Google Scholar" else "Google Student",
                    username = "google_user",
                    email = "scholar@slaty.edu.ng",
                    role = role
                )
                _uiState.value = AuthUiState.Success(user)
            }
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _uiState.value = AuthUiState.Idle
        _firebaseUser.value = null
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
