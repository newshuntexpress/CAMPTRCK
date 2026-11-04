package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Employee
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EditEmployeeDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        iqamaNumber: String,
        cardNumber: String,
        dateIn: String,
        trade: String,
        company: String,
        department: String,
        phone: String,
        nationality: String,
        notes: String,
        iqamaExpiryDate: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(employee.name) }
    var iqamaNumber by remember { mutableStateOf(employee.iqamaNumber) }
    var iqamaExpiryDate by remember { mutableStateOf(employee.iqamaExpiryDate) }
    var cardNumber by remember { mutableStateOf(employee.cardNumber) }
    var dateIn by remember { mutableStateOf(employee.dateIn) }
    var trade by remember { mutableStateOf(employee.trade) }
    var company by remember { mutableStateOf(employee.company) }
    var department by remember { mutableStateOf(employee.department) }
    var phone by remember { mutableStateOf(employee.phone) }
    var nationality by remember { mutableStateOf(employee.nationality) }
    var notes by remember { mutableStateOf(employee.notes) }

    var validationError by remember { mutableStateOf<String?>(null) }

    val commonTrades = listOf(
        "Electrician", "HVAC Tech", "Pipe Fitter", "Mason", "Driver", "Welder", "Foreman"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .testTag("edit_employee_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Employee Data",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Validation Error Banner
                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Resident Room Info Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (employee.roomNumber != null) "Current Allotment: Room ${employee.roomNumber} (Bed ${employee.bedNumber})"
                            else "Status: Checked Out",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Employee Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_employee_name_input"),
                    singleLine = true
                )

                // Iqama & Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = iqamaNumber,
                        onValueChange = {
                            iqamaNumber = it
                            validationError = null
                        },
                        label = { Text("Iqama No. *") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_iqama_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = {
                            cardNumber = it
                            validationError = null
                        },
                        label = { Text("Card No. *") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_card_input"),
                        singleLine = true
                    )
                }

                // Check-In Date (Date In)
                Column {
                    OutlinedTextField(
                        value = dateIn,
                        onValueChange = {
                            dateIn = it
                            validationError = null
                        },
                        label = { Text("Check-In Date (YYYY-MM-DD) *") },
                        placeholder = { Text("e.g. 2026-10-26") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_date_in_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                        FilterChip(
                            selected = dateIn == todayStr,
                            onClick = {
                                dateIn = todayStr
                                validationError = null
                            },
                            label = { Text("Today ($todayStr)", fontSize = 10.sp) }
                        )
                    }
                }

                // Iqama Expiry Date
                Column {
                    OutlinedTextField(
                        value = iqamaExpiryDate,
                        onValueChange = {
                            iqamaExpiryDate = it
                            validationError = null
                        },
                        label = { Text("Iqama Expiry Date (YYYY-MM-DD)") },
                        placeholder = { Text("e.g. 2026-11-20") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_iqama_expiry_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        val presetOptions = remember {
                            listOf(
                                "+15d (Soon)" to 15,
                                "+28d (Soon)" to 28,
                                "+6 Months" to 180,
                                "+1 Year" to 365
                            )
                        }
                        presetOptions.forEach { (label, days) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, days)
                                    iqamaExpiryDate = sdf.format(cal.time)
                                    validationError = null
                                },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Trade with Chips
                Column {
                    OutlinedTextField(
                        value = trade,
                        onValueChange = { trade = it },
                        label = { Text("Trade / Designation") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        commonTrades.take(4).forEach { t ->
                            FilterChip(
                                selected = trade == t,
                                onClick = { trade = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Company & Department
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        placeholder = { Text("e.g. Operations") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Phone & Nationality
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nationality,
                        onValueChange = { nationality = it },
                        label = { Text("Nationality") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Please enter employee name."
                        return@Button
                    }
                    if (iqamaNumber.isBlank()) {
                        validationError = "Please enter Iqama number."
                        return@Button
                    }
                    if (cardNumber.isBlank()) {
                        validationError = "Please enter card / badge number."
                        return@Button
                    }
                    if (dateIn.isBlank()) {
                        validationError = "Please enter Check-In date."
                        return@Button
                    }
                    onConfirm(
                        name,
                        iqamaNumber,
                        cardNumber,
                        dateIn,
                        trade,
                        company,
                        department,
                        phone,
                        nationality,
                        notes,
                        iqamaExpiryDate
                    )
                },
                modifier = Modifier.testTag("confirm_edit_employee_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
