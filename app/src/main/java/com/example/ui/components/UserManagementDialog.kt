package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import com.example.data.model.AppUpdateInfo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CampUser
import com.example.data.model.CampUserRole
import com.example.ui.theme.Green500
import com.example.ui.theme.Red500
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun UserManagementDialog(
    users: List<CampUser>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onCreateUser: (email: String, name: String, role: CampUserRole) -> Unit,
    onUpdateRole: (userId: String, newRole: CampUserRole) -> Unit,
    onDeleteUser: (userId: String) -> Unit,
    onUpdateStatus: (userId: String, status: String, remarks: String) -> Unit = { _, _, _ -> },
    appUpdateInfo: AppUpdateInfo? = null,
    onPublishAppUpdate: ((versionCode: Long, versionName: String, notes: String, downloadUrl: String, isMandatory: Boolean) -> Unit)? = null
) {
    var selectedAdminTab by remember { mutableStateOf(0) } // 0 = Users, 1 = Device Updates & Sync
    var updateVersionName by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.latestVersionName ?: "1.1") }
    var updateVersionCode by remember(appUpdateInfo) { mutableStateOf((appUpdateInfo?.latestVersionCode?.plus(1) ?: 2L).toString()) }
    var updateNotes by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.releaseNotes ?: "Live multi-device automatic sync and camp room updates.") }
    var updateDownloadUrl by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.downloadUrl.orEmpty()) }
    var updateIsMandatory by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.isMandatory ?: false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createInitialRole by remember { mutableStateOf(CampUserRole.SUPERVISOR) }
    var userForRoleEdit by remember { mutableStateOf<CampUser?>(null) }
    var userForDelete by remember { mutableStateOf<CampUser?>(null) }
    var userForReject by remember { mutableStateOf<CampUser?>(null) }

    // Search, Filter & Pagination states
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "APPROVED", "PENDING", "REJECTED"
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 5

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(680.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Admin Panel: User Management",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Authorized registry & approval workflow",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Admin Tab Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { selectedAdminTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedAdminTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Users & Approvals (${users.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAdminTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedAdminTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedAdminTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedAdminTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Device Updates & Sync",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedAdminTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedAdminTab == 0) {
                    // Summary Cards at Top
                    val totalUsers = users.size
                    val approvedCount = users.count { it.status.uppercase() == "APPROVED" }
                    val pendingCount = users.count { it.status.uppercase() == "PENDING" }
                    val rejectedCount = users.count { it.status.uppercase() == "REJECTED" || it.status.uppercase() == "NOT_APPROVED" }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserMetricCard(
                            title = "Total",
                            count = totalUsers,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        UserMetricCard(
                            title = "Approved",
                            count = approvedCount,
                            color = Green500,
                            modifier = Modifier.weight(1f)
                        )
                        UserMetricCard(
                            title = "Pending",
                            count = pendingCount,
                            color = Color(0xFFF57C00),
                            modifier = Modifier.weight(1f)
                        )
                        UserMetricCard(
                            title = "Rejected",
                            count = rejectedCount,
                            color = Red500,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search & Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                currentPage = 0
                            },
                            placeholder = { Text("Search by name or email...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                createInitialRole = CampUserRole.SUPERVISOR
                                showCreateDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(50.dp).testTag("admin_create_supervisor_button")
                        ) {
                            Icon(imageVector = Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("ALL", "APPROVED", "PENDING", "REJECTED").forEach { status ->
                            Surface(
                                onClick = {
                                    statusFilter = status
                                    currentPage = 0
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (statusFilter == status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = status.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (statusFilter == status) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtered & Paged List
                    val filteredUsers = remember(users, searchQuery, statusFilter) {
                        users.filter { u ->
                            val matchesSearch = u.displayName.contains(searchQuery, ignoreCase = true) || u.email.contains(searchQuery, ignoreCase = true)
                            val matchesStatus = when (statusFilter) {
                                "APPROVED" -> u.status.uppercase() == "APPROVED"
                                "PENDING" -> u.status.uppercase() == "PENDING"
                                "REJECTED" -> u.status.uppercase() == "REJECTED" || u.status.uppercase() == "NOT_APPROVED"
                                else -> true
                            }
                            matchesSearch && matchesStatus
                        }.sortedBy { it.displayName }
                    }

                    val totalPages = maxOf(1, (filteredUsers.size + pageSize - 1) / pageSize)
                    if (currentPage >= totalPages) currentPage = totalPages - 1
                    val pagedUsers = filteredUsers.drop(currentPage * pageSize).take(pageSize)

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users found matching filter criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pagedUsers, key = { it.id.ifEmpty { it.email } }) { user ->
                                UserItemCard(
                                    user = user,
                                    isCurrentUser = user.userId == currentUserId,
                                    onEditRole = { userForRoleEdit = user },
                                    onDelete = { userForDelete = user },
                                    onApprove = { onUpdateStatus(user.id, "APPROVED", "") },
                                    onReject = { userForReject = user }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pagination Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${currentPage * pageSize + 1}–${minOf((currentPage + 1) * pageSize, filteredUsers.size)} of ${filteredUsers.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { if (currentPage > 0) currentPage-- },
                                    enabled = currentPage > 0,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Prev", fontSize = 11.sp)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Text(
                                            text = "${currentPage + 1} / $totalPages",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                    enabled = currentPage < totalPages - 1,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Next", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Tab 1: Device Updates & Sync
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Live Multi-Device Auto-Sync Active",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "All rooms, bed allotments, transfers, and employee records instantly update across all staff devices.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Broadcast App Update to All Staff Devices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = updateVersionName,
                            onValueChange = { updateVersionName = it },
                            label = { Text("Version Name (e.g. 1.2)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = updateVersionCode,
                            onValueChange = { updateVersionCode = it.filter { char -> char.isDigit() } },
                            label = { Text("Version Code (Numeric, e.g. 3)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = updateNotes,
                            onValueChange = { updateNotes = it },
                            label = { Text("Release Notes / Improvements") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        OutlinedTextField(
                            value = updateDownloadUrl,
                            onValueChange = { updateDownloadUrl = it },
                            label = { Text("APK Download URL (e.g. https://... or Google Drive)") },
                            placeholder = { Text("https://example.com/app-release.apk") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mandatory Update",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prompts users immediately when connected online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = updateIsMandatory,
                                onCheckedChange = { updateIsMandatory = it }
                            )
                        }

                        Button(
                            onClick = {
                                val code = updateVersionCode.toLongOrNull() ?: 2L
                                onPublishAppUpdate?.invoke(code, updateVersionName, updateNotes, updateDownloadUrl, updateIsMandatory)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publish Update to All Devices", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Sub Dialogs
    if (showCreateDialog) {
        CreateUserSubDialog(
            initialRole = createInitialRole,
            onDismiss = { showCreateDialog = false },
            onConfirm = { email, name, role ->
                onCreateUser(email, name, role)
                showCreateDialog = false
            }
        )
    }

    if (userForRoleEdit != null) {
        EditRoleSubDialog(
            user = userForRoleEdit!!,
            onDismiss = { userForRoleEdit = null },
            onConfirm = { newRole ->
                onUpdateRole(userForRoleEdit!!.id, newRole)
                userForRoleEdit = null
            }
        )
    }

    if (userForDelete != null) {
        AlertDialog(
            onDismissRequest = { userForDelete = null },
            title = { Text("Delete User Authorization") },
            text = {
                Text("Are you sure you want to revoke and delete user '${userForDelete!!.email}' (${userForDelete!!.displayName})? They will lose access to the system.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(userForDelete!!.id)
                        userForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userForDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (userForReject != null) {
        RejectUserReasonDialog(
            user = userForReject!!,
            onDismiss = { userForReject = null },
            onConfirm = { remarks ->
                onUpdateStatus(userForReject!!.id, "REJECTED", remarks)
                userForReject = null
            }
        )
    }
}

@Composable
fun UserMetricCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UserItemCard(
    user: CampUser,
    isCurrentUser: Boolean,
    onEditRole: () -> Unit,
    onDelete: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusUpper = user.status.uppercase()
    val statusColor = when (statusUpper) {
        "APPROVED" -> Green500
        "PENDING" -> Color(0xFFF57C00)
        else -> Red500
    }

    val lastLoginStr = user.lastLoginAt?.let {
        SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(it.toDate())
    } ?: "Never"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            user.isAdmin -> MaterialTheme.colorScheme.primary
                            user.isSupervisor -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    user.isAdmin -> Icons.Default.AdminPanelSettings
                                    user.isSupervisor -> Icons.Default.Security
                                    else -> Icons.Default.Visibility
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.displayName.ifEmpty { "User" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCurrentUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "YOU",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusUpper,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Role: ${user.roleEnum.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last Login: $lastLoginStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (statusUpper != "APPROVED") {
                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier.size(32.dp).testTag("approve_user_${user.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Approve User",
                                tint = Green500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (statusUpper != "REJECTED" && statusUpper != "NOT_APPROVED") {
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier.size(32.dp).testTag("reject_user_${user.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Reject User",
                                tint = Red500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditRole,
                        modifier = Modifier.size(32.dp).testTag("edit_user_role_${user.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Role",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!isCurrentUser) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).testTag("delete_user_${user.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete User",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RejectUserReasonDialog(
    user: CampUser,
    onDismiss: () -> Unit,
    onConfirm: (remarks: String) -> Unit
) {
    var remarks by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Not Approve / Reject User Access") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Please provide reason / remarks for rejecting ${user.displayName.ifEmpty { user.email }}:")
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Rejection Remarks") },
                    placeholder = { Text("e.g. Unauthorized account / Not part of camp staff") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(remarks.trim().ifEmpty { "Not approved by Administrator" }) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm Rejection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateUserSubDialog(
    initialRole: CampUserRole = CampUserRole.SUPERVISOR,
    onDismiss: () -> Unit,
    onConfirm: (email: String, name: String, role: CampUserRole) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Add New User Authorization",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "New users are automatically approved and ready for login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = null
                    },
                    label = { Text("User Email *") },
                    placeholder = { Text("e.g. supervisor@awazel.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_user_email_input")
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display / Full Name") },
                    placeholder = { Text("e.g. Camp Supervisor Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_user_name_input")
                )

                Text(
                    text = "Authorization Level:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = CampUserRole.SUPERVISOR }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == CampUserRole.SUPERVISOR,
                                onClick = { selectedRole = CampUserRole.SUPERVISOR }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Camp Supervisor (CRUD All Sections)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "CRUD access for rooms, beds, staff, check-in, transfer. No user deletion rights.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = CampUserRole.ADMIN }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == CampUserRole.ADMIN,
                                onClick = { selectedRole = CampUserRole.ADMIN }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Full Authorization (Admin)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Full privileges to create, edit, delete, and manage user approvals.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = CampUserRole.VIEW_ONLY }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == CampUserRole.VIEW_ONLY,
                                onClick = { selectedRole = CampUserRole.VIEW_ONLY }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "View Only",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Read-only access to dashboards and occupancy reports.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank() || !email.contains("@")) {
                        error = "Please enter a valid email address."
                        return@Button
                    }
                    onConfirm(email.trim(), displayName.trim(), selectedRole)
                },
                modifier = Modifier.testTag("submit_create_user")
            ) {
                Text("Add & Approve User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditRoleSubDialog(
    user: CampUser,
    onDismiss: () -> Unit,
    onConfirm: (CampUserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.roleEnum) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change User Authorization",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Updating authorization for ${user.displayName.ifEmpty { user.email }}:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = CampUserRole.SUPERVISOR }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == CampUserRole.SUPERVISOR,
                                onClick = { selectedRole = CampUserRole.SUPERVISOR }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Camp Supervisor (CRUD All Sections)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "CRUD access for rooms, beds, staff, check-in, transfer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = CampUserRole.ADMIN }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == CampUserRole.ADMIN,
                                onClick = { selectedRole = CampUserRole.ADMIN }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Full Authorization (Admin)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Full privileges to create, delete, change records, manage users.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = CampUserRole.VIEW_ONLY }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == CampUserRole.VIEW_ONLY,
                                onClick = { selectedRole = CampUserRole.VIEW_ONLY }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "View Only",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Read-only access (no modification or deletion allowed)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRole) },
                modifier = Modifier.testTag("confirm_role_change")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
