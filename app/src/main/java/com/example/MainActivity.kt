package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.BoteAppUI
import com.example.ui.BoteCommunityViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity() {

    private var appUpdateManager: AppUpdateManager? = null
    private val UPDATE_REQUEST_CODE = 1234

    @android.annotation.SuppressLint("InvalidFragmentVersionForActivityResult")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup full-bleed layout
        enableEdgeToEdge()

        // Ask for notification permissions dynamically if on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Initialize Google Play Core AppUpdateManager with robust exception handling
        try {
            appUpdateManager = AppUpdateManagerFactory.create(this)
            checkForAppUpdates()
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize AppUpdateManager: ${e.message}")
        }
        
        // Core state manager initialization
        val viewModel = ViewModelProvider(this)[BoteCommunityViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                BoteAppUI(viewModel = viewModel)
            }
        }
    }

    private fun checkForAppUpdates() {
        val manager = appUpdateManager ?: return
        try {
            val appUpdateInfoTask = manager.appUpdateInfo
            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    // Request the update
                    try {
                        manager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to start update flow", e)
                    }
                }
            }.addOnFailureListener {
                Log.e("MainActivity", "Failed to check for app update", it)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception while calling appUpdateInfo: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        val manager = appUpdateManager ?: return
        // If an in-app update is already running, resume it.
        try {
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        manager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to resume update flow", e)
                    }
                }
            }.addOnFailureListener {
                Log.e("MainActivity", "Failed to get app update info in onResume", it)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception while getting appUpdateInfo in onResume: ${e.message}")
        }
    }
}
