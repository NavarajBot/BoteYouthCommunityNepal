package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val email: String,
    val fullName: String,
    val role: String, // "standard" or "admin"
    val uid: String,
    val isFirebaseUser: Boolean
)

suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Task failed"))
        }
    }
}

object BoteAuthManager {
    private const val TAG = "BoteAuthManager"
    
    private var firebaseAuth: FirebaseAuth? = null
    private var isFirebaseAvailable = false

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    fun initialize(context: Context) {
        try {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val projectId = BuildConfig.FIREBASE_PROJECT_ID
            val appId = BuildConfig.FIREBASE_APPLICATION_ID

            Log.d(TAG, "Initializing Firebase... ProjectID: $projectId")

            if (!apiKey.isNullOrBlank() && apiKey != "placeholder_api_key" &&
                !projectId.isNullOrBlank() && projectId != "placeholder_project_id" &&
                !appId.isNullOrBlank() && appId != "placeholder_app_id") {
                
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setProjectId(projectId)
                        .setApplicationId(appId)
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
                firebaseAuth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                Log.d(TAG, "Firebase Auth successfully initialized.")
                
                // Set initial user if already signed in via Firebase
                val firebaseUser = firebaseAuth?.currentUser
                if (firebaseUser != null) {
                    val email = firebaseUser.email ?: ""
                    val isUserAdmin = checkIfAdmin(email)
                    _currentUser.value = AuthUser(
                        email = email,
                        fullName = firebaseUser.displayName ?: email.substringBefore("@"),
                        role = if (isUserAdmin) "admin" else "standard",
                        uid = firebaseUser.uid,
                        isFirebaseUser = true
                    )
                }
            } else {
                Log.d(TAG, "Firebase credentials not provided. Operating in secure Local Fallback mode.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Auth: ${e.message}. Using secure Local Fallback mode.")
        } finally {
            _isInitializing.value = false
        }
    }

    fun isFirebaseActive(): Boolean = isFirebaseAvailable

    private fun checkIfAdmin(email: String): Boolean {
        val lower = email.lowercase()
        return lower.contains("admin") || lower.endsWith("@bote.org")
    }

    suspend fun signUp(
        context: Context,
        email: String,
        password: String,
        fullName: String,
        adminPasscode: String
    ): Result<AuthUser> {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            return Result.failure(Exception("All fields are required"))
        }

        val isAdminRequested = adminPasscode.trim() == "BOTE_ADMIN"
        val role = if (isAdminRequested || checkIfAdmin(email)) "admin" else "standard"

        if (isFirebaseAvailable) {
            return try {
                val auth = firebaseAuth ?: throw Exception("Firebase Auth not initialized")
                val authResult = auth.createUserWithEmailAndPassword(email, password).awaitTask()
                val user = authResult.user ?: throw Exception("User creation failed")
                
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()
                user.updateProfile(profileUpdates).awaitTask()

                val loggedUser = AuthUser(
                    email = email,
                    fullName = fullName,
                    role = role,
                    uid = user.uid,
                    isFirebaseUser = true
                )
                
                // Cache in local Room database
                val db = BoteDatabase.getDatabase(context)
                db.boteDao().insertUser(
                    BoteUser(
                        email = email,
                        passwordHash = password,
                        fullName = fullName,
                        role = role,
                        uid = user.uid
                    )
                )

                _currentUser.value = loggedUser
                Result.success(loggedUser)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Local fallback signup
            return try {
                val db = BoteDatabase.getDatabase(context)
                val existing = db.boteDao().getUserByEmail(email)
                if (existing != null) {
                    return Result.failure(Exception("User with this email already exists"))
                }

                val newUser = BoteUser(
                    email = email,
                    passwordHash = password,
                    fullName = fullName,
                    role = role
                )
                db.boteDao().insertUser(newUser)

                val loggedUser = AuthUser(
                    email = email,
                    fullName = fullName,
                    role = role,
                    uid = newUser.uid,
                    isFirebaseUser = false
                )
                _currentUser.value = loggedUser
                Result.success(loggedUser)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun login(
        context: Context,
        email: String,
        password: String
    ): Result<AuthUser> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("All fields are required"))
        }

        if (isFirebaseAvailable) {
            return try {
                val auth = firebaseAuth ?: throw Exception("Firebase Auth not initialized")
                val authResult = auth.signInWithEmailAndPassword(email, password).awaitTask()
                val user = authResult.user ?: throw Exception("Sign in failed")

                // Try to get fullName and role from local DB cache
                val db = BoteDatabase.getDatabase(context)
                val cached = db.boteDao().getUserByEmail(email)
                val fullName = cached?.fullName ?: user.displayName ?: email.substringBefore("@")
                val role = cached?.role ?: if (checkIfAdmin(email)) "admin" else "standard"

                val loggedUser = AuthUser(
                    email = email,
                    fullName = fullName,
                    role = role,
                    uid = user.uid,
                    isFirebaseUser = true
                )
                _currentUser.value = loggedUser
                Result.success(loggedUser)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Local fallback login
            return try {
                val db = BoteDatabase.getDatabase(context)
                val user = db.boteDao().getUserByEmail(email)
                if (user == null || user.passwordHash != password) {
                    return Result.failure(Exception("Invalid email or password"))
                }

                val loggedUser = AuthUser(
                    email = email,
                    fullName = user.fullName,
                    role = user.role,
                    uid = user.uid,
                    isFirebaseUser = false
                )
                _currentUser.value = loggedUser
                Result.success(loggedUser)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun loginWithGoogle(
        context: Context,
        email: String,
        fullName: String,
        uid: String
    ): Result<AuthUser> {
        if (email.isBlank()) {
            return Result.failure(Exception("Google Account email is required"))
        }
        val role = if (checkIfAdmin(email)) "admin" else "standard"
        val loggedUser = AuthUser(
            email = email,
            fullName = fullName,
            role = role,
            uid = uid,
            isFirebaseUser = false
        )
        return try {
            val db = BoteDatabase.getDatabase(context)
            val existing = db.boteDao().getUserByEmail(email)
            if (existing == null) {
                db.boteDao().insertUser(
                    BoteUser(
                        email = email,
                        passwordHash = "google_auth_linked",
                        fullName = fullName,
                        role = role,
                        uid = uid
                    )
                )
            }
            _currentUser.value = loggedUser
            Result.success(loggedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        if (isFirebaseAvailable) {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Error logging out from Firebase: ${e.message}")
            }
        }
        _currentUser.value = null
    }

    suspend fun deleteAccount(): Result<Boolean> {
        return try {
            if (isFirebaseAvailable) {
                val user = firebaseAuth?.currentUser
                user?.delete()?.awaitTask()
            }
            _currentUser.value = null
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
