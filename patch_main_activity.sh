#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/MainActivity.kt
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

    private lateinit var appUpdateManager: AppUpdateManager
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
        
        // Initialize Google Play Core AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdates()
        
        // Core state manager initialization
        val viewModel = ViewModelProvider(this)[BoteCommunityViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                BoteAppUI(viewModel = viewModel)
            }
        }
    }

    private fun checkForAppUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // Request the update
                try {
                    appUpdateManager.startUpdateFlowForResult(
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
    }

    override fun onResume() {
        super.onResume()
        // If an in-app update is already running, resume it.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to resume update flow", e)
                }
            }
        }
    }
}
INNER_EOF
