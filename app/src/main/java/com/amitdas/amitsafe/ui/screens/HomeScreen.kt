package com.amitdas.amitsafe.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amitdas.amitsafe.database.VaultItem
import com.amitdas.amitsafe.ui.theme.BackgroundAmiled
import com.amitdas.amitsafe.ui.theme.CardDark
import com.amitdas.amitsafe.ui.theme.CyberCyan
import com.amitdas.amitsafe.ui.theme.TextMuted
import com.amitdas.amitsafe.viewmodel.VaultViewModel

@Composable
fun HomeScreen(viewModel: VaultViewModel) {
    val isLocked by viewModel.isLocked.collectAsState()
    var currentTab by remember { mutableStateOf(0) } // 0: Vault, 1: Import, 2: Favorites, 3: Settings
    var activeViewerItem by remember { mutableStateOf<VaultItem?>(null) }

    // Unified wrapper that respects lock screen priority
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLocked) {
            LockScreen(
                viewModel = viewModel,
                onUnlockSuccess = { viewModel.unlockExternal() }
            )
        } else {
            // Main workspace with bottom bar navigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = CardDark,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("app_navigation_bar")
                    ) {
                        // 1. Vault Tab
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 0) Icons.Filled.FolderOpen else Icons.Outlined.Folder,
                                    contentDescription = "Vault",
                                    tint = if (currentTab == 0) CyberCyan else Color.White
                                )
                            },
                            label = { Text("Vault", color = Color.White, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CyberCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_vault")
                        )

                        // 2. Import Tab
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 1) Icons.Filled.AddPhotoAlternate else Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = "Import",
                                    tint = if (currentTab == 1) CyberCyan else Color.White
                                )
                            },
                            label = { Text("Import", color = Color.White, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CyberCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_import")
                        )

                        // 3. Favorites Tab
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 2) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Favorites",
                                    tint = if (currentTab == 2) CyberCyan else Color.White
                                )
                            },
                            label = { Text("Favorites", color = Color.White, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CyberCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_favorites")
                        )

                        // 4. Settings Tab
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = if (currentTab == 3) CyberCyan else Color.White
                                )
                            },
                            label = { Text("Settings", color = Color.White, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CyberCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(BackgroundAmiled)
                ) {
                    // Swapping Tabs with fading transition effects
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "TabSwitcher"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> VaultScreen(
                                viewModel = viewModel,
                                onViewItem = { item -> activeViewerItem = item }
                            )
                            1 -> ImportScreen(viewModel = viewModel)
                            2 -> FavoritesScreen(
                                viewModel = viewModel,
                                onViewItem = { item -> activeViewerItem = item }
                            )
                            3 -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }

            // High priority ViewerScreen overlay
            activeViewerItem?.let { item ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    ViewerScreen(
                        item = item,
                        viewModel = viewModel,
                        onBack = { activeViewerItem = null }
                    )
                }
            }
        }
    }
}
