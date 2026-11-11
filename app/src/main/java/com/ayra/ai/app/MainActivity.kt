package com.ayra.ai.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.ayra.ai.app.data.ChatDatabase
import com.ayra.ai.app.ui.screens.ChatScreen
import com.ayra.ai.app.ui.theme.AyraTheme
import com.ayra.ai.app.ui.viewmodel.ChatViewModel

/**
 * Production MainActivity for Ayra AI.
 * Adheres strictly to Google Play policy and just-in-time runtime permissions.
 * Dangerous hardware permissions (camera, microphone, telephony, location)
 * are requested on-demand only when triggered by explicit user actions.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission evaluated
    }

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Just-in-time: Request notification permission on Android 13+ only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val chatDb = ChatDatabase.getDatabase(this)
        val chatDao = chatDb.chatDao()

        setContent {
            AyraTheme {
                ChatScreen(
                    chatDao = chatDao,
                    viewModel = chatViewModel
                )
            }
        }
    }
}
