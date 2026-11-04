package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled. UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.IqamaExpiryStatus
import com.example.ui.viewmodel.CampUiState
import com.example.ui.viewmodel.EmployeeSearchScope
import com.example.ui.viewmodel.EmployeeStatusFilter
import com.example.ui.viewmodel.IqamaExpiryFilter
import com.example.util.BulkEmployeeImporter

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.verticalScroll
import com.example.util.DownloadedFileInfo
import com.example.util.TemplateFormat

import androidx.compose.material.icons.filled.QrCode

@Composable
fun EmployeesTab(
    uiState: CampUiState,
    onEmployeeClick: (Employee) -> Unit,
    onCheckOutClick: (Employee) -> Unit,
    onTransferClick: (Employee) -> Unit,
    onEditClick: (Employee) -> Unit,
    onDeleteClick: (Employee) -> Unit,
    onNewAllotmentClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchScopeChange: (EmployeeSearchScope) -> Unit,
    onStatusFilterChange: (EmployeeStatusFilter) -> Unit,
    onIqamaExpiryFilterChange: (IqamaExpiryFilter) -> Unit,
    onStatusChange: (Employee, EmployeeStatus) -> Unit,
    onUploadEmployees: (Uri, onComplete: (Int, List<String>) -> Unit) -> Unit,
    onOpenRegistrationQrClick: () -> Unit = {},
    onAllotPendingEmployee: (Employee) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTemplateChoiceDialog by remember { mutableStateOf(false) }
    var downloadedFileInfo by remember { mutableStateOf<DownloadedFileInfo?>(null) }
    var showDownloadSuccessDialog by remember { mutableStateOf(false) }
    var downloadErrorMessage by remember { mutableStateOf<String?>(null) }

    var showUploadResultDialog by remember { mutableStateOf(false) }
    var uploadSuccessCount by remember { mutableStateOf(0) }
    var uploadErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            isUploading = true
            onUploadEmployees(selectedUri) { count, errors ->
                isUploading = false
                uploadSuccessCount = count
                uploadErrors = errors
                showUploadResultDialog = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // --- Header Section: Title, Stats & Bulk Import Actions ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Staff Roster & Employees",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.filteredEmployees.size} staff records found",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bulk Import, Sample Template & QR Buttons (Admins & Supervisors)
            if (uiState.canManageCamp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code for Employee Self-Registration
                    OutlinedButton(
                        onClick = onOpenRegistrationQrClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).testTag("employee_qr_code_button")
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Self-Reg QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Download Sample Template Button
                    OutlinedButton(
                        onClick = {
                            showTemplateChoiceDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).testTag("download_template_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Template", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Upload Employees Button (replaces Bulk Import)
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/plain",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "*/*"
                                )
                            )
                        },
                        enabled = !isUploading && !uiState.isLoading,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).testTag("upload_employees_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            if (isUploading) "Uploading..." else "Upload Employees",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Search & Filter Bar ---
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name, iqama, card or room...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("employee_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // --- Status Filters Row (Horizontal Scroll) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmployeeStatusFilter.values().forEach { filter ->
                FilterChip(
                    selected = uiState.employeeStatusFilter == filter,
                    onClick = { onStatusFilterChange(filter) },
                    label = { Text(filter.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IqamaExpiryFilter.values().forEach { expiryFilter ->
                FilterChip(
                    selected = uiState.iqamaExpiryFilter == expiryFilter,
                    onClick = { onIqamaExpiryFilterChange(expiryFilter) },
                    label = { 
                        Text(
                            when(expiryFilter) {
                                IqamaExpiryFilter.ALL -> "All Iqamas"
                                IqamaExpiryFilter.EXPIRING_30 -> "Expiring ≤30d (${uiState.expiringIqamaCount})"
                                IqamaExpiryFilter.EXPIRED -> "Expired (${uiState.expiredIqamaCount})"
                                IqamaExpiryFilter.VALID -> "Valid (${uiState.validIqamaCount})"
                            },
                            fontSize = 11.sp
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (expiryFilter == IqamaExpiryFilter.EXPIRED && uiState.expiredIqamaCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = if (expiryFilter == IqamaExpiryFilter.EXPIRED && uiState.expiredIqamaCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_iqama_${expiryFilter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- Highlight Banner for Pending Self-Registered Staff ---
        if (uiState.pendingSelfRegisteredEmployees.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("pending_self_registered_banner"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                border = BorderStroke(1.dp, Color(0xFFFFB74D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${uiState.pendingSelfRegisteredEmployees.size} Self-Registered Staff Awaiting Allotment",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Personal details locked. Assign room & bed to complete onboarding.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.5.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                    if (uiState.canManageCamp) {
                        Button(
                            onClick = {
                                val firstPending = uiState.pendingSelfRegisteredEmployees.firstOrNull()
                                if (firstPending != null) {
                                    onAllotPendingEmployee(firstPending)
                                } else {
                                    onNewAllotmentClick()
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                        ) {
                            Text("Allot Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Employee Cards List ---
        if (uiState.filteredEmployees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No employees found matching criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(uiState.filteredEmployees, key = { it.uniqueKey }) { employee ->
                    EmployeeCard(
                        employee = employee,
                        canManage = uiState.canManageCamp,
                        onClick = { onEmployeeClick(employee) },
                        onCheckOutClick = { onCheckOutClick(employee) },
                        onTransferClick = { onTransferClick(employee) },
                        onEditClick = { onEditClick(employee) },
                        onDeleteClick = { onDeleteClick(employee) },
                        onStatusChange = { newStatus -> onStatusChange(employee, newStatus) },
                        onAllotClick = { onAllotPendingEmployee(employee) }
                    )
                }
            }
        }
    }

    // --- Dialog: Choose Template Format to Download ---
    if (showTemplateChoiceDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTemplateChoiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download Employee Template",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose the template format you want to download. Use the CSV format to fill in Excel/Google Sheets and upload directly into the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Excel / CSV Spreadsheet (Recommended for editing & uploading)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val res = BulkEmployeeImporter.saveTemplateToDownloads(context, TemplateFormat.CSV)
                                if (res.isSuccess) {
                                    downloadedFileInfo = res.getOrNull()
                                    showTemplateChoiceDialog = false
                                    showDownloadSuccessDialog = true
                                } else {
                                    downloadErrorMessage = res.exceptionOrNull()?.message ?: "Failed to download CSV"
                                    showTemplateChoiceDialog = false
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Excel / CSV Spreadsheet (.csv)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Recommended. Open in Excel, fill staff data, and upload back.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: PDF Reference Guide
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val res = BulkEmployeeImporter.saveTemplateToDownloads(context, TemplateFormat.PDF)
                                if (res.isSuccess) {
                                    downloadedFileInfo = res.getOrNull()
                                    showTemplateChoiceDialog = false
                                    showDownloadSuccessDialog = true
                                } else {
                                    downloadErrorMessage = res.exceptionOrNull()?.message ?: "Failed to download PDF"
                                    showTemplateChoiceDialog = false
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PDF Format Guide (.pdf)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Printable reference guide with field rules and sample layout.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showTemplateChoiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog: Template Downloaded Successfully ---
    if (showDownloadSuccessDialog && downloadedFileInfo != null) {
        val info = downloadedFileInfo!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDownloadSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Template Downloaded",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "The template has been downloaded successfully to your device's Downloads folder.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = info.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text(
                        text = "You can open it now or share it to another app or computer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { BulkEmployeeImporter.shareDownloadedFile(context, info) }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = {
                            BulkEmployeeImporter.openDownloadedFile(context, info)
                            showDownloadSuccessDialog = false
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDownloadSuccessDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- Dialog: Download Error Message ---
    if (downloadErrorMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { downloadErrorMessage = null },
            title = { Text("Download Failed") },
            text = { Text(downloadErrorMessage ?: "An unexpected error occurred while saving the template.") },
            confirmButton = {
                Button(onClick = { downloadErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // --- Dialog: Upload Employees Result Summary ---
    if (showUploadResultDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUploadResultDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uploadSuccessCount > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (uploadSuccessCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Employee Import Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uploadSuccessCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Successfully imported $uploadSuccessCount employees into camp roster.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else if (uploadErrors.isEmpty()) {
                        Text(
                            text = "No employees were imported.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uploadErrors.isNotEmpty()) {
                        Text(
                            text = "${uploadErrors.size} issue(s) detected during import:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(minOf(160.dp, (uploadErrors.size * 32).dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                uploadErrors.forEach { err ->
                                    Text(
                                        text = "• $err",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showUploadResultDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun EmployeeCard(
    employee: Employee,
    canManage: Boolean,
    onClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onTransferClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusChange: (EmployeeStatus) -> Unit,
    onAllotClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val iqamaInfo = remember(employee.iqamaExpiryDate) { employee.calculateIqamaExpiryInfo() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("employee_card_${employee.uniqueKey}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Name on single straight line (strict requirement) + Status Badge + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Employee Information Hierarchy:
                // 1. Employee Name: Display the full employee name on a single straight line. Do not allow to wrap.
                // 2. Trade / Designation directly below name.
                // 3. Company Name below trade/designation.
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.name.ifEmpty { "Unnamed Employee" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("employee_name_${employee.uniqueKey}")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = employee.trade.ifEmpty { "General Staff" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    val compText = if (employee.department.isNotBlank()) {
                        "${employee.company.ifEmpty { "AWAZEL CO." }} • ${employee.department}"
                    } else {
                        employee.company.ifEmpty { "AWAZEL CO." }
                    }
                    Text(
                        text = compText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: Status badge + action menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            employee.isPendingAllotment -> Color(0xFFFFF3E0)
                            employee.status == EmployeeStatus.ACTIVE -> Color(0xFFE8F5E9)
                            employee.status == EmployeeStatus.ON_VACATION -> Color(0xFFFFF8E1)
                            else -> Color(0xFFFFEBEE)
                        }
                    ) {
                        Text(
                            text = when {
                                employee.isPendingAllotment -> "Pending Allotment"
                                employee.status == EmployeeStatus.ACTIVE -> "Active"
                                employee.status == EmployeeStatus.ON_VACATION -> "Vacation"
                                else -> "Checked Out"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = when {
                                employee.isPendingAllotment -> Color(0xFFE65100)
                                employee.status == EmployeeStatus.ACTIVE -> Color(0xFF2E7D32)
                                employee.status == EmployeeStatus.ON_VACATION -> Color(0xFFF57C00)
                                else -> Color(0xFFC62828)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (canManage) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(28.dp).testTag("employee_menu_${employee.uniqueKey}")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Actions", modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View Details") },
                                    onClick = { showMenu = false; onClick() }
                                )
                                if (employee.status == EmployeeStatus.ACTIVE) {
                                    DropdownMenuItem(
                                        text = { Text("Transfer Bed/Room") },
                                        onClick = { showMenu = false; onTransferClick() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Check Out") },
                                        onClick = { showMenu = false; onCheckOutClick() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Set On Vacation") },
                                        onClick = { showMenu = false; onStatusChange(EmployeeStatus.ON_VACATION) }
                                    )
                                } else if (employee.status == EmployeeStatus.ON_VACATION) {
                                    DropdownMenuItem(
                                        text = { Text("Set Active") },
                                        onClick = { showMenu = false; onStatusChange(EmployeeStatus.ACTIVE) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Check Out") },
                                        onClick = { showMenu = false; onCheckOutClick() }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Edit Record") },
                                    onClick = { showMenu = false; onEditClick() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; onDeleteClick() }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details info chips (Room / Bed, Iqama No, Card No, Phone)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Room & Bed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (!employee.roomNumber.isNullOrEmpty()) "Room ${employee.roomNumber} (Bed ${employee.bedNumber ?: 1})" else "Unallotted",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Iqama & Mobile Number Column
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Iqama: ${employee.iqamaNumber.ifEmpty { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Mobile: ${employee.phone.ifEmpty { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Card Number
                        Text(
                            text = "Card: ${employee.cardNumber.ifEmpty { "N/A" }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Iqama Expiry Badge / Text
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (iqamaInfo.status) {
                                IqamaExpiryStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
                                IqamaExpiryStatus.EXPIRING_SOON -> Color(0xFFFFE0B2)
                                else -> Color.Transparent
                            }
                        ) {
                            Text(
                                text = iqamaInfo.displayText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (iqamaInfo.isExpiringWithin30Days) FontWeight.Bold else FontWeight.Normal,
                                color = when (iqamaInfo.status) {
                                    IqamaExpiryStatus.EXPIRED -> MaterialTheme.colorScheme.onErrorContainer
                                    IqamaExpiryStatus.EXPIRING_SOON -> Color(0xFFE65100)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (employee.isPendingAllotment && canManage) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onAllotClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .testTag("allot_pending_button_${employee.uniqueKey}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Allot Room & Bed Now", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
