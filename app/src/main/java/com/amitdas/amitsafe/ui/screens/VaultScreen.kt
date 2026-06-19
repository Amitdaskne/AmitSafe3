package com.amitdas.amitsafe.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amitdas.amitsafe.database.VaultItem
import com.amitdas.amitsafe.ui.theme.*
import com.amitdas.amitsafe.viewmodel.VaultViewModel
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onViewItem: (VaultItem) -> Unit
) {
    val items by viewModel.vaultItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val activeSort by viewModel.activeSort.collectAsState()

    val selectedItems = remember { mutableStateListOf<VaultItem>() }
    val isMultiSelectMode = selectedItems.isNotEmpty()

    var showSortMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        // Multi-Select Visual Header Context Bar
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedItems.size} Selected",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                selectedItems.forEach { viewModel.toggleFavorite(it) }
                                selectedItems.clear()
                            },
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favorite All", tint = CyberPink)
                        }
                        IconButton(
                            onClick = {
                                selectedItems.forEach { viewModel.shareSecureItem(context, it) }
                                selectedItems.clear()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share Selected", tint = CyberCyan)
                        }
                        IconButton(
                            onClick = {
                                selectedItems.forEach { viewModel.moveToBin(it) }
                                selectedItems.clear()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Trash Selected", tint = CyberPink)
                        }
                        IconButton(
                            onClick = { selectedItems.clear() }
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel selection", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Standard Row for Search & Filters
        if (!isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Bar Field
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search Vault...", color = TextSecondary) },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search icon", tint = CyberCyan) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardDark,
                        unfocusedContainerColor = CardDark,
                        focusedIndicatorColor = CyberCyan,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("search_text_input")
                )

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark)
                            .testTag("sort_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SortByAlpha,
                            contentDescription = "Sort Options",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(CardDark)
                    ) {
                        val sortOptions = listOf("Newest", "Oldest", "Larger", "Name")
                        sortOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = Color.White) },
                                onClick = {
                                    viewModel.activeSort.value = opt
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (activeSort == opt) {
                                        Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = CyberCyan)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Quick Category Filters
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                val filters = listOf("All", "Images", "Videos")
                filters.forEach { filterItem ->
                    val selected = activeFilter == filterItem
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.activeFilter.value = filterItem },
                        label = { Text(filterItem, color = if (selected) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            containerColor = CardDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = BorderDark,
                            selectedBorderColor = CyberCyan
                        )
                    )
                }
            }
        }

        // Empty State or Staggered Vault Grid
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderZip,
                        contentDescription = "Empty Folder",
                        tint = TextMuted,
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "Your safe is empty",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Import private snapshots and recordings from your local picker to protect files securely on this device.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("gallery_items_grid")
            ) {
                items(items, key = { it.id }) { item ->
                    val isSelected = selectedItems.contains(item)
                    val scaleBy = if (isSelected) 0.92f else 1.0f

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CyberCyan else BorderDark,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(CardDark)
                            .combinedClickable(
                                onClick = {
                                    if (isMultiSelectMode) {
                                        if (isSelected) selectedItems.remove(item) else selectedItems.add(item)
                                    } else {
                                        onViewItem(item)
                                    }
                                },
                                onLongClick = {
                                    if (!isMultiSelectMode) {
                                        selectedItems.add(item)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Display Thumbnail
                        val mediaFile = File(item.vaultPath)
                        if (mediaFile.exists()) {
                            AsyncImage(
                                model = mediaFile,
                                contentDescription = item.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback file image indicator
                            Icon(
                                imageVector = if (item.fileType == "video") Icons.Filled.Videocam else Icons.Filled.Image,
                                contentDescription = "Media thumbnail missing",
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Video Play Icon Indicator
                        if (item.fileType == "video") {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Video",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Favorite Star Overlay bottom right
                        if (item.isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Starred",
                                tint = CyberPink,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }

                        // Check indicator overlay during Multi-Select Mode
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CyberCyan.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Checked",
                                    tint = CyberCyan,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
