package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CampUserRole
import com.example.ui.components.AddRoomDialog
import com.example.ui.components.AllotmentDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.CheckOutDialog
import com.example.ui.components.DailyReportDialog
import com.example.ui.components.DeleteEmployeeConfirmationDialog
import com.example.ui.components.EditEmployeeDialog
import com.example.ui.components.EmployeeDetailDialog
import com.example.ui.components.RoomDetailDialog
import com.example.ui.components.SyncStatusDialog
import com.example.ui.components.TransferDialog
import com.example.ui.components.UserManagementDialog
import com.example.ui.components.RegistrationQrDialog
import com.example.ui.components.EmployeeSelfRegistrationDialog
import com.example.ui.theme.Green500
import com.example.ui.viewmodel.CampViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CampViewModel,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.bannerMessage) {
        uiState.bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.dismissBanner()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    // Top Row: Corporate brand + Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CampTrack",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Online / Offline Status Indicator Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (uiState.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    border = BorderStroke(
                                        1.dp,
                                        if (uiState.isOnline) Color(0xFF81C784) else Color(0xFFE57373)
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.openSyncInfoDialog() }
                                        .testTag("online_offline_indicator")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = if (uiState.isOnline) "App is online" else "App is offline",
                                            tint = if (uiState.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (uiState.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.isOnline) "Online" else "Offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (uiState.isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "AWAZEL CO. • Camp Accommodation & 2-Bed Allotments",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Daily Report & Allotment Quick Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Daily Report Button
                            IconButton(
                                onClick = { viewModel.openDailyReportDialog() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("top_bar_daily_report_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Daily Occupancy Report",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            // Quick Allot action button (Admins & Supervisors)
                            if (uiState.canManageCamp) {
                                IconButton(
                                    onClick = { viewModel.openAllotmentDialog() },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("top_bar_add_allotment")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "New Allotment",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // App Update Notification Banner (Dismissible, does not show automatic popups)
                    AnimatedVisibility(visible = uiState.hasAvailableUpdate && !uiState.autoUpdateNoticeDismissed) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.openAppUpdateDialog() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "New Update Available: v${uiState.appUpdateInfo?.latestVersionName ?: ""}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (uiState.isOnline) "🟢 Online • Tap to view or download" else "Offline",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = {
                                                viewModel.startDownloadAndInstallApk(context)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            enabled = !uiState.isDownloadingApk,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = if (uiState.isDownloadingApk) "${uiState.apkDownloadProgress}%" else "Update",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.dismissUpdateBanner() },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Dismiss update notice",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                                if (uiState.isDownloadingApk) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { uiState.apkDownloadProgress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Offline Status Alert Banner
                    AnimatedVisibility(
                        visible = !uiState.isOnline,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .testTag("offline_status_banner"),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Offline indicator",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "App is Offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "Local data available. All changes queued and will auto-sync when connected.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5D4037),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { viewModel.triggerManualSync() },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Sync Now",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }

                    // Compact User & Action Controls Row (Space-efficient for mobile screens)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Profile & Role Status Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = uiState.currentUser?.displayName?.ifEmpty { uiState.currentUser?.email ?: "User" } ?: "User",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Authorization Level Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    uiState.isAdmin -> MaterialTheme.colorScheme.primary
                                    uiState.isSupervisor -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            uiState.isAdmin -> Icons.Default.Security
                                            uiState.isSupervisor -> Icons.Default.Badge
                                            else -> Icons.Default.Visibility
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            uiState.isAdmin || uiState.isSupervisor -> Color.White
                                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                                        },
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = when {
                                            uiState.isAdmin -> "Admin"
                                            uiState.isSupervisor -> "Supervisor"
                                            else -> "View Only"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when {
                                            uiState.isAdmin || uiState.isSupervisor -> Color.White
                                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                                        },
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        // Right: Admin Panel button + Logout button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isAdmin) {
                                Button(
                                    onClick = { viewModel.openUserPanel() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("admin_panel_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Users",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Prominent On-Screen Logout / Sign Out Button
                            OutlinedButton(
                                onClick = { showSignOutConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("top_bar_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Logout",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // View-Only Guidance Strip
                    if (uiState.isViewOnly) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "View-Only Mode: Contact Camp Administrator for edit rights.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Instant Search Field for Rooms & Beds Tab
                    if (uiState.selectedTab == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search rooms (e.g. Room 101, Block A)...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("main_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            )
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Navigation Bar (Tabs: Rooms, Employees, Overview, More)
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        NavigationBarItem(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == 0) Icons.Default.MeetingRoom else Icons.Outlined.MeetingRoom,
                                    contentDescription = "Rooms & Beds",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Rooms",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_rooms")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == 1) Icons.Default.Badge else Icons.Outlined.Badge,
                                    contentDescription = "Staff Roster",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Employees",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_employees")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == 2) Icons.Default.Analytics else Icons.Outlined.Analytics,
                                    contentDescription = "Camp Overview",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Overview",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_overview")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == 3,
                            onClick = { viewModel.setSelectedTab(3) },
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "More",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_more")
                        )
                    }

                    // Visible Corporate Footer (Docked at the bottom, never hidden)
                    AppFooter(
                        isOnline = uiState.isOnline,
                        occupiedBeds = uiState.stats.occupiedBeds,
                        totalBeds = uiState.stats.totalBeds,
                        activeStaff = uiState.stats.activeEmployeesCount
                    )
                }
            }
        },
        floatingActionButton = {
            // Floating Action Buttons are active for Full Authorization (Admin) and Camp Supervisor users
            if (uiState.canManageCamp) {
                when (uiState.selectedTab) {
                    0 -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openAllotmentDialog() },
                            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            text = { Text("Allot Bed", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("fab_allot_bed")
                        )
                    }
                    1 -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openAllotmentDialog() },
                            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            text = { Text("New Staff Allotment", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("fab_new_staff")
                        )
                    }
                    2 -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openAddRoomDialog() },
                            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            text = { Text("Add Room", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("fab_add_room")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> RoomsAvailabilityTab(
                    uiState = uiState,
                    onFilterChange = { viewModel.setRoomFilter(it) },
                    onBlockChange = { viewModel.setSelectedBlock(it) },
                    onRoomClick = { viewModel.openRoomDetail(it) },
                    onQuickAllotBed = { roomNumber, bedNumber ->
                        viewModel.openAllotmentDialog(roomNumber = roomNumber, bedNumber = bedNumber)
                    },
                    onAddNewRoomClick = { viewModel.openAddRoomDialog() }
                )
                1 -> EmployeesTab(
                    uiState = uiState,
                    onEmployeeClick = { viewModel.openEmployeeDetail(it) },
                    onCheckOutClick = { viewModel.openCheckoutDialog(it) },
                    onTransferClick = { viewModel.openTransferDialog(it) },
                    onEditClick = { viewModel.openEditEmployeeDialog(it) },
                    onDeleteClick = { viewModel.openDeleteEmployeeDialog(it) },
                    onNewAllotmentClick = { viewModel.openAllotmentDialog() },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onSearchScopeChange = { viewModel.setEmployeeSearchScope(it) },
                    onStatusFilterChange = { viewModel.setEmployeeStatusFilter(it) },
                    onIqamaExpiryFilterChange = { viewModel.setIqamaExpiryFilter(it) },
                    onStatusChange = { emp, status -> viewModel.changeEmployeeStatus(emp, status) },
                    onUploadEmployees = { uri, onComplete ->
                        viewModel.bulkImportEmployees(context, uri, onComplete)
                    },
                    onOpenRegistrationQrClick = { viewModel.openRegistrationQrDialog() },
                    onAllotPendingEmployee = { emp -> viewModel.openAllotmentDialog(prefillEmployee = emp) }
                )
                2 -> CampOverviewTab(
                    uiState = uiState,
                    onAddNewRoomClick = { viewModel.openAddRoomDialog() },
                    onOpenReportClick = { viewModel.openDailyReportDialog() },
                    onOpenRegistrationQrClick = { viewModel.openRegistrationQrDialog() },
                    onDownloadApkClick = { viewModel.startDownloadAndInstallApk(context) }
                )
                3 -> MoreActionsTab(
                    uiState = uiState,
                    onOpenReportClick = { viewModel.openDailyReportDialog() },
                    onOpenRegistrationQrClick = { viewModel.openRegistrationQrDialog() },
                    onDownloadApkClick = { viewModel.startDownloadAndInstallApk(context) },
                    onAddNewRoomClick = { viewModel.openAddRoomDialog() },
                    onUploadEmployees = { uri, onComplete ->
                        viewModel.bulkImportEmployees(context, uri, onComplete)
                    }
                )
            }
        }
    }

    // --- Sign Out Confirmation Dialog ---
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of CampTrack AWAZEL CO.?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        performSignOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = onSignOut,
                            scope = scope
                        )
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Admin User Management Panel Dialog ---
    if (uiState.showUserPanelDialog) {
        UserManagementDialog(
            users = uiState.allUsers,
            currentUserId = uiState.currentUser?.userId ?: "",
            onDismiss = { viewModel.closeUserPanel() },
            onCreateUser = { email, name, role ->
                viewModel.createUser(email, name, role)
            },
            onUpdateRole = { userId, newRole ->
                viewModel.updateUserRole(userId, newRole)
            },
            onDeleteUser = { userId ->
                viewModel.deleteUser(userId)
            },
            onUpdateStatus = { userId, status, remarks ->
                viewModel.updateUserApprovalStatus(userId, status, remarks)
            },
            appUpdateInfo = uiState.appUpdateInfo,
            onPublishAppUpdate = { code, name, notes, url, isMandatory ->
                viewModel.publishAppUpdate(code, name, notes, url, isMandatory)
            }
        )
    }

    // --- Allotment Dialog ---
    if (uiState.showAllotmentDialog) {
        AllotmentDialog(
            availableRooms = uiState.rooms,
            prefillRoomNumber = uiState.prefillRoomForNewAllotment,
            prefillBedNumber = uiState.prefillBedForNewAllotment,
            onDismiss = { viewModel.closeAllotmentDialog() },
            onConfirm = { name, iqama, card, room, bed, dateIn, trade, company, department, phone, nat, notes ->
                viewModel.allotEmployee(
                    name = name,
                    iqamaNumber = iqama,
                    cardNumber = card,
                    roomNumber = room,
                    bedNumber = bed,
                    dateIn = dateIn,
                    trade = trade,
                    company = company,
                    department = department,
                    phone = phone,
                    nationality = nat,
                    notes = notes
                )
            },
            errorMessage = uiState.errorMessage,
            prefillEmployee = uiState.prefillEmployeeForNewAllotment,
            pendingEmployees = uiState.pendingSelfRegisteredEmployees
        )
    }

    // --- Room Detail Dialog ---
    uiState.selectedRoomForDetail?.let { roomWithOcc ->
        RoomDetailDialog(
            roomWithOccupants = roomWithOcc,
            onDismiss = { viewModel.closeRoomDetail() },
            onAllotBed = { bedNumber ->
                viewModel.closeRoomDetail()
                viewModel.openAllotmentDialog(roomNumber = roomWithOcc.room.roomNumber, bedNumber = bedNumber)
            },
            onCheckOutEmployee = { emp ->
                viewModel.openCheckoutDialog(emp)
            },
            onTransferEmployee = { emp ->
                viewModel.openTransferDialog(emp)
            },
            onDeleteRoom = { roomNumber ->
                viewModel.deleteRoom(roomNumber)
            },
            isAdmin = uiState.canManageCamp
        )
    }

    // --- Check Out Dialog ---
    uiState.employeeForCheckout?.let { emp ->
        CheckOutDialog(
            employee = emp,
            onDismiss = { viewModel.closeCheckoutDialog() },
            onConfirm = { dateOut ->
                viewModel.confirmCheckout(emp, dateOut)
            },
            onDelete = {
                viewModel.openDeleteEmployeeDialog(emp)
            }
        )
    }

    // --- Transfer Dialog ---
    uiState.employeeForTransfer?.let { emp ->
        TransferDialog(
            employee = emp,
            availableRooms = uiState.rooms,
            onDismiss = { viewModel.closeTransferDialog() },
            onConfirm = { newRoom, newBed ->
                viewModel.confirmTransfer(emp, newRoom, newBed)
            }
        )
    }

    // --- Employee Detail Dialog ---
    uiState.employeeForDetail?.let { emp ->
        EmployeeDetailDialog(
            employee = emp,
            onDismiss = { viewModel.closeEmployeeDetail() },
            onCheckOut = { empToCheckOut ->
                viewModel.openCheckoutDialog(empToCheckOut)
            },
            onTransfer = { empToTransfer ->
                viewModel.openTransferDialog(empToTransfer)
            },
            onEdit = { empToEdit ->
                viewModel.openEditEmployeeDialog(empToEdit)
            },
            onDelete = { empToDelete ->
                viewModel.openDeleteEmployeeDialog(empToDelete)
            },
            isAdmin = uiState.canManageCamp,
            onStatusChange = { empToChange, newStatus ->
                viewModel.changeEmployeeStatus(empToChange, newStatus)
            }
        )
    }

    // --- Edit Employee Dialog ---
    uiState.employeeForEdit?.let { emp ->
        EditEmployeeDialog(
            employee = emp,
            onDismiss = { viewModel.closeEditEmployeeDialog() },
            onConfirm = { name, iqama, card, dateIn, trade, comp, dept, phone, nat, notes, iqamaExp ->
                viewModel.confirmUpdateEmployee(
                    employee = emp,
                    name = name,
                    iqamaNumber = iqama,
                    cardNumber = card,
                    dateIn = dateIn,
                    trade = trade,
                    company = comp,
                    department = dept,
                    phone = phone,
                    nationality = nat,
                    notes = notes,
                    iqamaExpiryDate = iqamaExp
                )
            }
        )
    }

    // --- Delete Employee Confirmation Dialog ---
    uiState.employeeForDelete?.let { emp ->
        DeleteEmployeeConfirmationDialog(
            employee = emp,
            onDismiss = { viewModel.closeDeleteEmployeeDialog() },
            onConfirmDelete = {
                viewModel.confirmDeleteEmployee(emp)
            }
        )
    }

    // --- Add Room Dialog ---
    if (uiState.showAddRoomDialog) {
        AddRoomDialog(
            onDismiss = { viewModel.closeAddRoomDialog() },
            onConfirm = { roomNumber, block ->
                viewModel.addNewRoom(roomNumber, block)
            }
        )
    }

    // --- Daily Occupancy & Expiry Report Dialog ---
    if (uiState.showDailyReportDialog) {
        DailyReportDialog(
            uiState = uiState,
            onDismiss = { viewModel.closeDailyReportDialog() }
        )
    }

    // --- Multi-Device Sync Status Dialog ---
    if (uiState.showSyncInfoDialog) {
        SyncStatusDialog(
            isOnline = uiState.isOnline,
            lastSyncTime = uiState.lastSyncTime,
            isSyncingNow = uiState.isSyncingNow,
            hasAvailableUpdate = uiState.hasAvailableUpdate,
            appUpdateInfo = uiState.appUpdateInfo,
            onForceSync = { viewModel.forceSync() },
            onCheckUpdate = {
                viewModel.closeSyncInfoDialog()
                viewModel.openAppUpdateDialog()
            },
            onDismiss = { viewModel.closeSyncInfoDialog() }
        )
    }

    // --- In-App App Update Dialog ---
    if (uiState.showAppUpdateDialog && uiState.appUpdateInfo != null) {
        AppUpdateDialog(
            updateInfo = uiState.appUpdateInfo!!,
            isDownloading = uiState.isDownloadingApk,
            downloadProgress = uiState.apkDownloadProgress,
            isOnline = uiState.isOnline,
            onDownloadAndInstall = {
                viewModel.startDownloadAndInstallApk(context)
            },
            onDismiss = { viewModel.closeAppUpdateDialog() }
        )
    }

    // --- Employee Registration QR Code & Link Dialog ---
    if (uiState.showRegistrationQrDialog) {
        RegistrationQrDialog(
            onDismiss = { viewModel.closeRegistrationQrDialog() },
            onOpenSelfRegistrationForm = {
                viewModel.closeRegistrationQrDialog()
                viewModel.openSelfRegistrationDialog()
            }
        )
    }

    // --- Employee Self-Registration Form Dialog ---
    if (uiState.showSelfRegistrationDialog) {
        val availableRoomNumbers = remember(uiState.rooms) {
            uiState.rooms.map { it.room.roomNumber }
        }
        EmployeeSelfRegistrationDialog(
            onDismiss = { viewModel.closeSelfRegistrationDialog() },
            onSubmit = { name, iqama, card, trade, comp, dept, phone, nat, dateIn, roomNumber, onComplete ->
                viewModel.submitEmployeeSelfRegistration(
                    name = name,
                    iqamaNumber = iqama,
                    cardNumber = card,
                    trade = trade,
                    company = comp,
                    department = dept,
                    phone = phone,
                    nationality = nat,
                    dateIn = dateIn,
                    roomNumber = roomNumber,
                    notes = "",
                    onComplete = onComplete
                )
            },
            availableRooms = availableRoomNumbers
        )
    }
}

/**
 * Persistent Application Footer Bar
 * Docked at the bottom of the screen, responsive across mobile, tablet, and desktop devices.
 * Always visible and never obscured by navigation bars, content, or buttons.
 */
@Composable
fun AppFooter(
    isOnline: Boolean,
    occupiedBeds: Int,
    totalBeds: Int,
    activeStaff: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_footer"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Live Cloud Connectivity & Corporate Identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    border = BorderStroke(
                        1.dp,
                        if (isOnline) Color(0xFF81C784) else Color(0xFFE57373)
                    ),
                    modifier = Modifier.testTag("footer_online_offline_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = if (isOnline) "App is online" else "App is offline",
                            tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.5.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828))
                        )
                        Spacer(modifier = Modifier.width(3.5.dp))
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                            fontSize = 9.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "AWAZEL CO. • CampTrack",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Real-Time Operational Counts
            Text(
                text = "Beds: $occupiedBeds/$totalBeds • Staff: $activeStaff",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
