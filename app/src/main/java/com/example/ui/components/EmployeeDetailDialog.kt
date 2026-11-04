package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.ui.theme.Amber500
import com.example.ui.theme.Green500
import com.example.ui.theme.Red500

@Composable
fun EmployeeDetailDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onCheckOut: (Employee) -> Unit,
    onTransfer: (Employee) -> Unit,
    onEdit: (Employee) -> Unit,
    onDelete: (Employee) -> Unit,
    isAdmin: Boolean = true,
    onStatusChange: ((Employee, EmployeeStatus) -> Unit)? = null
) {
    val isActive = employee.status == EmployeeStatus.ACTIVE
    val isOnVacation = employee.status == EmployeeStatus.ON_VACATION
    val isCheckedOut = employee.status == EmployeeStatus.CHECKED_OUT

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .testTag("employee_detail_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = employee.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${employee.trade} • ${employee.company}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status banner with clear visual indicators
                val (statusBg, statusFg, statusLabel) = when (employee.status) {
                    EmployeeStatus.ACTIVE -> Triple(Green500.copy(alpha = 0.12f), Green500, "Active Resident in Camp")
                    EmployeeStatus.ON_VACATION -> Triple(Amber500.copy(alpha = 0.15f), Amber500, "On Vacation (Bed Reserved: Room ${employee.roomNumber}, Bed ${employee.bedNumber})")
                    EmployeeStatus.CHECKED_OUT -> Triple(Red500.copy(alpha = 0.12f), Red500, "Checked Out of Camp")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusFg.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusFg,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusLabel,
                            fontWeight = FontWeight.Bold,
                            color = statusFg
                        )
                    }
                }

                // Iqama Expiry Alert Banner (if expiring soon or expired)
                val iqamaInfo = employee.calculateIqamaExpiryInfo()
                val iqamaColor = when (iqamaInfo.status) {
                    com.example.data.model.IqamaExpiryStatus.EXPIRED -> Red500
                    com.example.data.model.IqamaExpiryStatus.EXPIRING_SOON -> Amber500
                    com.example.data.model.IqamaExpiryStatus.VALID -> Green500
                    com.example.data.model.IqamaExpiryStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iqamaColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iqamaColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = iqamaColor,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Iqama Status: ${iqamaInfo.displayText}",
                                fontWeight = FontWeight.Bold,
                                color = iqamaColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (iqamaInfo.status == com.example.data.model.IqamaExpiryStatus.EXPIRING_SOON) {
                                Text(
                                    text = "⚠️ Expiring within 30 days! Please arrange Iqama renewal to prevent camp access disruptions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (iqamaInfo.status == com.example.data.model.IqamaExpiryStatus.EXPIRED) {
                                Text(
                                    text = "⛔ Iqama is already expired! Immediate renewal action required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Info Rows
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoRow(icon = Icons.Default.Badge, label = "Iqama No.", value = employee.iqamaNumber)
                        if (employee.department.isNotBlank()) {
                            InfoRow(icon = Icons.Default.Work, label = "Department", value = employee.department)
                        }
                        InfoRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Iqama Expiry Date",
                            value = if (employee.iqamaExpiryDate.isNotBlank()) "${employee.iqamaExpiryDate} (${iqamaInfo.displayText})" else "Not Set"
                        )
                        InfoRow(icon = Icons.Default.CreditCard, label = "Camp Card No.", value = employee.cardNumber)
                        InfoRow(
                            icon = Icons.Default.MeetingRoom,
                            label = "Room & Bed",
                            value = when (employee.status) {
                                EmployeeStatus.ACTIVE -> "Room ${employee.roomNumber} (Bed ${employee.bedNumber})"
                                EmployeeStatus.ON_VACATION -> "Room ${employee.roomNumber} (Bed ${employee.bedNumber}) • Reserved"
                                EmployeeStatus.CHECKED_OUT -> "None (Checked Out • Previous: Room ${employee.roomNumber})"
                            }
                        )
                        InfoRow(icon = Icons.Default.CalendarToday, label = "Check-In Date", value = if (employee.dateIn.isNotBlank()) employee.dateIn else "Not recorded")
                        if (employee.dateOut != null) {
                            InfoRow(icon = Icons.Default.CalendarToday, label = "Check-Out Date", value = employee.dateOut)
                        }
                        if (employee.phone.isNotBlank()) {
                            InfoRow(icon = Icons.Default.Phone, label = "Phone", value = employee.phone)
                        }
                        if (employee.nationality.isNotBlank()) {
                            InfoRow(icon = Icons.Default.Public, label = "Nationality", value = employee.nationality)
                        }
                    }
                }

                if (employee.notes.isNotBlank()) {
                    Column {
                        Text("Notes:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = employee.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isAdmin) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // --- Employee Status Management Section (Admin Only) ---
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "MANUAL STATUS MANAGEMENT",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Current Status: ${employee.status.name.replace('_', ' ')}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Mark Active Button
                                    OutlinedButton(
                                        onClick = {
                                            onStatusChange?.invoke(employee, EmployeeStatus.ACTIVE)
                                        },
                                        enabled = employee.status != EmployeeStatus.ACTIVE,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Green500
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (employee.status != EmployeeStatus.ACTIVE) Green500 else Color.LightGray
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("detail_mark_active_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Active", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // Mark On Vacation Button
                                    OutlinedButton(
                                        onClick = {
                                            onStatusChange?.invoke(employee, EmployeeStatus.ON_VACATION)
                                        },
                                        enabled = employee.status != EmployeeStatus.ON_VACATION,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Amber500
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (employee.status != EmployeeStatus.ON_VACATION) Amber500 else Color.LightGray
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("detail_mark_vacation_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlightTakeoff,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("On Vacation", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Action Buttons (Edit, Transfer, Check-Out, Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onEdit(employee)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_edit_employee_button")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Details", fontSize = 12.sp)
                            }

                            if (isActive || isOnVacation) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onTransfer(employee)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_transfer_employee_button")
                                ) {
                                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Transfer Bed", fontSize = 12.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onDelete(employee)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_delete_employee_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Record", fontSize = 12.sp)
                                }
                            }
                        }

                        if (isActive || isOnVacation) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onCheckOut(employee)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_checkout_employee_button")
                                ) {
                                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Check-Out", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onDelete(employee)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Red500),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_delete_employee_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "View-Only Mode: Administrator or Camp Supervisor authorization is required to change employee status, transfer, or edit staff.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
