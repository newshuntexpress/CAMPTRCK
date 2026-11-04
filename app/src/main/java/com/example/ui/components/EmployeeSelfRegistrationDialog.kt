package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Green500
import com.example.ui.viewmodel.CampViewModel
import com.example.util.QrCodeGenerator

@Composable
fun EmployeeSelfRegistrationDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        iqamaNumber: String,
        cardNumber: String,
        trade: String,
        company: String,
        department: String,
        phone: String,
        nationality: String,
        dateIn: String,
        notes: String,
        onComplete: (Result<String>) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var iqamaNumber by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf("Electrician") }
    var company by remember { mutableStateOf("AWAZEL CO.") }
    var department by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("Saudi Arabia") }
    var dateIn by remember { mutableStateOf(CampViewModel.getCurrentDateString()) }
    var notes by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmittedAndLocked by remember { mutableStateOf(false) }
    var generatedAllotmentLink by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var submitErrorMessage by remember { mutableStateOf<String?>(null) }

    val commonTrades = listOf(
        "Electrician", "HVAC Tech", "Pipe Fitter", "Mason", "Driver", "Welder", "Safety Officer", "Foreman", "Laborer"
    )

    val commonNationalities = listOf(
        "Saudi Arabia", "Egypt", "India", "Pakistan", "Philippines", "Bangladesh", "Nepal", "Yemen", "Jordan"
    )

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .fillMaxWidth(0.95f)
            .widthIn(max = 520.dp)
            .testTag("employee_self_registration_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSubmittedAndLocked) Icons.Default.Lock else Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isSubmittedAndLocked) "Registration Complete" else "Employee Self-Registration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "AWAZEL CO. Staff Onboarding Portal",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isSubmitting) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSubmittedAndLocked) {
                    // --- SUCCESS & LOCKED VIEW ---
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Green500.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Green500,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Your details have been successfully saved!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green500,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Employee record for $name has been registered and securely stored in the camp database.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Data Lock Notice Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Data Locked After Submission",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "To preserve audit integrity, this record is locked. Only Camp Supervisors or Administrators can modify details during staff allotment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Unique Registration / Allotment Link Card
                    if (generatedAllotmentLink.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Direct Allotment Link:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = generatedAllotmentLink,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Allotment Link", generatedAllotmentLink)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Allotment link copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Link", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Awazel Staff Allotment Link - $name")
                                                putExtra(Intent.EXTRA_TEXT, "Awazel Staff Allotment Link for $name (Iqama: $iqamaNumber):\n$generatedAllotmentLink")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Allotment Link"))
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Summary details
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Name: $name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Iqama: $iqamaNumber", fontSize = 12.sp)
                            Text("Department: $department", fontSize = 12.sp)
                            Text("Company: $company", fontSize = 12.sp)
                            Text("Mobile: $phone", fontSize = 12.sp)
                            Text("Trade: $trade", fontSize = 12.sp)
                            if (cardNumber.isNotBlank()) Text("Card No: $cardNumber", fontSize = 12.sp)
                            Text("Status: Pending Room & Bed Assignment", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                } else {
                    // --- ACTIVE SELF-REGISTRATION FORM ---
                    Text(
                        text = "Please complete your employee profile details below. Once saved, information will be submitted directly to camp management for room and bed allotment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )

                    // Error banner
                    if (validationError != null || submitErrorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError ?: submitErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // 1. Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        label = { Text("Full Name *", fontSize = 12.sp) },
                        placeholder = { Text("Enter employee full name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_name_input"),
                        enabled = !isSubmitting
                    )

                    // 2. Iqama Number (10 digits)
                    OutlinedTextField(
                        value = iqamaNumber,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 10) {
                                iqamaNumber = digitsOnly
                                validationError = null
                            }
                        },
                        label = { Text("Iqama / National ID Number (10 digits) *", fontSize = 12.sp) },
                        placeholder = { Text("10-digit Saudi Iqama") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        supportingText = {
                            Text(
                                text = "${iqamaNumber.length}/10 digits",
                                fontSize = 10.sp,
                                color = if (iqamaNumber.length == 10) Green500 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_iqama_input"),
                        enabled = !isSubmitting
                    )

                    // 3. Mobile Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            validationError = null
                        },
                        label = { Text("Mobile Number *", fontSize = 12.sp) },
                        placeholder = { Text("05xxxxxxxx or +966...") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_mobile_input"),
                        enabled = !isSubmitting
                    )

                    // 4. Department (Manual Entry)
                    OutlinedTextField(
                        value = department,
                        onValueChange = {
                            department = it
                            validationError = null
                        },
                        label = { Text("Department (Enter Manually) *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Operations, Maintenance, Bitumen Plant") },
                        leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_department_input"),
                        enabled = !isSubmitting
                    )

                    // 5. Company Name (Default: AWAZEL CO.)
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_company_input"),
                        enabled = !isSubmitting
                    )

                    // 6. Trade / Designation
                    OutlinedTextField(
                        value = trade,
                        onValueChange = { trade = it },
                        label = { Text("Trade / Designation", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_trade_input"),
                        enabled = !isSubmitting
                    )

                    // Quick Trade chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Electrician", "HVAC Tech", "Driver", "Mason", "Welder").forEach { t ->
                            FilterChip(
                                selected = trade == t,
                                onClick = { trade = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }

                    // 7. Card Number
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Employee Card No. (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. AWZ-882") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_card_input"),
                        enabled = !isSubmitting
                    )

                    // 8. Joining Date
                    OutlinedTextField(
                        value = dateIn,
                        onValueChange = { dateIn = it },
                        label = { Text("Joining Date (YYYY-MM-DD)", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_date_input"),
                        enabled = !isSubmitting
                    )

                    // 9. Nationality
                    OutlinedTextField(
                        value = nationality,
                        onValueChange = { nationality = it },
                        label = { Text("Nationality", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_nationality_input"),
                        enabled = !isSubmitting
                    )

                    // 10. Notes / Remarks
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Additional Notes (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("Special requirements, supervisor name, etc.") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_reg_notes_input"),
                        enabled = !isSubmitting
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmittedAndLocked) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("self_reg_done_button")
                ) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedIqama = iqamaNumber.trim()
                        val trimmedPhone = phone.trim()
                        val trimmedDept = department.trim()

                        if (trimmedName.isEmpty()) {
                            validationError = "Employee full name is required."
                            return@Button
                        }
                        if (trimmedIqama.length != 10) {
                            validationError = "Iqama number must be exactly 10 digits."
                            return@Button
                        }
                        if (trimmedPhone.isEmpty()) {
                            validationError = "Mobile number is required."
                            return@Button
                        }
                        if (trimmedDept.isEmpty()) {
                            validationError = "Department name is required."
                            return@Button
                        }

                        isSubmitting = true
                        validationError = null
                        submitErrorMessage = null

                        onSubmit(
                            trimmedName,
                            trimmedIqama,
                            cardNumber.trim(),
                            trade.trim().ifEmpty { "General Staff" },
                            company.trim().ifEmpty { "AWAZEL CO." },
                            trimmedDept,
                            trimmedPhone,
                            nationality.trim().ifEmpty { "Saudi Arabia" },
                            dateIn.trim().ifEmpty { CampViewModel.getCurrentDateString() },
                            notes.trim()
                        ) { result ->
                            isSubmitting = false
                            result.fold(
                                onSuccess = { empId ->
                                    isSubmittedAndLocked = true
                                    generatedAllotmentLink = QrCodeGenerator.generateEmployeeAllotmentUrl(empId, trimmedIqama)
                                },
                                onFailure = { err ->
                                    submitErrorMessage = err.message ?: "Submission failed. Please check your data and try again."
                                }
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("self_reg_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submitting...")
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!isSubmittedAndLocked && !isSubmitting) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
