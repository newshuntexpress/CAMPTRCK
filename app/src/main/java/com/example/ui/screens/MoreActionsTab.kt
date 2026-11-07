package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CampUiState
import com.example.util.BulkEmployeeImporter
import com.example.util.DownloadedFileInfo
import com.example.util.TemplateFormat

@Composable
fun MoreActionsTab(
    uiState: CampUiState,
    onOpenReportClick: () -> Unit,
    onOpenRegistrationQrClick: () -> Unit,
    onDownloadApkClick: () -> Unit,
    onAddNewRoomClick: () -> Unit,
    onOpenWebPortalClick: () -> Unit = {},
    onUploadEmployees: (Uri, onComplete: (Int, List<String>) -> Unit) -> Unit = { _, _ -> },
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_actions_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "More Options & Management Tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Access Self-Registration QR, bulk employee import, reports, and system tools",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- Staff Onboarding & Bulk Operations Section ---
        item {
            Text(
                text = "Employee Onboarding & Import",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Self-REG QR Code Button
                    Button(
                        onClick = onOpenRegistrationQrClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("more_qr_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Self-REG QR", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Self-REG QR Code (Staff Registration)", fontWeight = FontWeight.Bold)
                    }

                    // Web Portal Access Button (All Facilities)
                    OutlinedButton(
                        onClick = onOpenWebPortalClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("more_open_web_browser_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = "Open Web Portal",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Open Web Portal (awazelcamp.online • All Facilities)", fontWeight = FontWeight.SemiBold)
                    }

                    // 2. Upload Employees Button
                    if (uiState.canManageCamp) {
                        OutlinedButton(
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("more_upload_employees_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Uploading Employees...")
                            } else {
                                Icon(Icons.Default.UploadFile, contentDescription = "Upload Employees", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Upload Employees (CSV / Excel)", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // 3. Download Sample Template Button
                        OutlinedButton(
                            onClick = { showTemplateChoiceDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("more_download_template_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Template", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Download Employee Sample Template", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // --- Camp Operations & Reports Section ---
        item {
            Text(
                text = "Camp Operations & Utilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenReportClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("more_export_report_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Daily Occupancy Report (PDF / Print)")
                    }

                    OutlinedButton(
                        onClick = onDownloadApkClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("more_download_apk_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Android APK")
                    }

                    if (uiState.canManageCamp) {
                        OutlinedButton(
                            onClick = onAddNewRoomClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("more_add_room_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New 2-Bed Camp Room")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val stats = uiState.stats
                            val summary = "AWAZEL CO. CAMP SUMMARY:\n• Rooms: ${stats.totalRooms}\n• Beds: ${stats.totalBeds}\n• Booked Beds: ${stats.occupiedBeds}\n• Available Beds: ${stats.availableBeds}\n• Active Staff: ${stats.activeEmployeesCount}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Camp Summary", summary))
                            Toast.makeText(context, "Camp summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("more_copy_summary_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Camp Summary to Clipboard")
                    }
                }
            }
        }
    }

    // --- Dialog: Choose Template Format to Download ---
    if (showTemplateChoiceDialog) {
        AlertDialog(
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
                        text = "Choose the template format you want to download. Use CSV to edit in Excel or Google Sheets and upload into the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Excel / CSV Spreadsheet
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
        AlertDialog(
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
                        text = "Template Saved",
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
                    Text(
                        text = "File successfully saved to your device Downloads folder:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = info.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "Location: Downloads/${info.fileName}\nYou can share this template to WhatsApp/Email or open it in Excel.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            BulkEmployeeImporter.shareDownloadedFile(context, info)
                        }
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
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
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
        AlertDialog(
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
        AlertDialog(
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
                                text = "Successfully imported $uploadSuccessCount employees into the system.",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (uploadErrors.isNotEmpty()) {
                        Text(
                            text = "${uploadErrors.size} rows could not be imported:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                uploadErrors.forEach { err ->
                                    Text(
                                        text = "• $err",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showUploadResultDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
