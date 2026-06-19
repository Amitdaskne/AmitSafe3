package com.amitdas.amitsafe

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.amitdas.amitsafe.ui.screens.HomeScreen
import com.amitdas.amitsafe.ui.theme.AmitSafeTheme
import com.amitdas.amitsafe.viewmodel.VaultViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: VaultViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[VaultViewModel::class.java]

        enableEdgeToEdge()

        // Apply visual screenshot protection and recents privacy mask
        lifecycleScope.launch {
            viewModel.screenshotProtection.collectLatest { secure ->
                if (secure) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            AmitSafeTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Securely lock the app whenever context switches background
        if (viewModel.autoLockEnabled.value) {
            viewModel.lock()
        }
    }
}
