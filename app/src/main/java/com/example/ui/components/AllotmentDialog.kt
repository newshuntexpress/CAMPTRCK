package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.RoomWithOccupants
import com.example.ui.theme.Green500
import com.example.ui.viewmodel.CampViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.example.data.model.Employee
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllotmentDialog(
    availableRooms: List<RoomWithOccupants>,
    prefillRoomNumber: String?,
    prefillBedNumber: Int?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        iqamaNumber: String,
        cardNumber: String,
        roomNumber: String,
        bedNumber: Int,
        dateIn: String,
        trade: String,
        company: String,
        department: String,
        phone: String,
        nationality: String,
        notes: String
    ) -> Unit,
    errorMessage: String? = null,
    prefillEmployee: Employee? = null,
    pendingEmployees: List<Employee> = emptyList()
) {
    // Only rooms that have at least 1 bed available
    val roomsWithFreeBeds = remember(availableRooms) {
        availableRooms.filter { it.availableBeds > 0 }
    }

    var activeLinkedEmployee by remember(prefillEmployee) {
        mutableStateOf(prefillEmployee)
    }

    var selectedRoomNumber by remember {
        mutableStateOf(
            prefillRoomNumber ?: roomsWithFreeBeds.firstOrNull()?.room?.roomNumber ?: ""
        )
    }

    val selectedRoomObj = remember(selectedRoomNumber, availableRooms) {
        availableRooms.firstOrNull { it.room.roomNumber == selectedRoomNumber }
    }

    // Available beds in selected room
    val bed1Available = selectedRoomObj?.bed1Occupant == null
    val bed2Available = selectedRoomObj?.bed2Occupant == null

    var selectedBedNumber by remember(selectedRoomNumber) {
        val initialBed = prefillBedNumber ?: when {
            bed1Available -> 1
            bed2Available -> 2
            else -> 1
        }
        mutableIntStateOf(initialBed)
    }

    var name by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.name ?: "")
    }
    var iqamaNumber by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.iqamaNumber ?: "")
    }
    var cardNumber by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.cardNumber ?: "")
    }
    var dateIn by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.dateIn?.ifEmpty { null } ?: CampViewModel.getCurrentDateString())
    }
    var trade by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.trade?.ifEmpty { null } ?: "")
    }
    var company by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.company?.ifEmpty { null } ?: "AWAZEL CO.")
    }
    var department by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.department ?: "")
    }
    var phone by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.phone ?: "")
    }
    var nationality by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.nationality ?: "")
    }
    var notes by remember(activeLinkedEmployee) {
        mutableStateOf(activeLinkedEmployee?.notes ?: "")
    }

    val isEmployeePrefilled = activeLinkedEmployee != null

    var roomDropdownExpanded by remember { mutableStateOf(false) }

    // Validation
    var validationError by remember { mutableStateOf<String?>(null) }

    val commonTrades = listOf(
        "Electrician", "HVAC Tech", "Pipe Fitter", "Mason", "Driver", "Welder", "Safety Officer", "Foreman"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .widthIn(max = 500.dp)
            .testTag("allotment_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Room Allotment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Assign employee to 2-bed room",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (errorMessage != null || validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = (errorMessage ?: validationError)!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Self-Registration Connection Banner / Chip Selection
                if (activeLinkedEmployee != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
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
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Auto-Connected to Self-Registered Staff",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${activeLinkedEmployee?.name} (Iqama: ${activeLinkedEmployee?.iqamaNumber})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            IconButton(
                                onClick = { activeLinkedEmployee = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear link",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                } else if (pendingEmployees.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Auto-fill from Self-Registered Staff (${pendingEmployees.size} pending):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pendingEmployees.forEach { pendingEmp ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { activeLinkedEmployee = pendingEmp },
                                        label = {
                                            Text(
                                                text = "${pendingEmp.name} (${pendingEmp.department.ifEmpty { pendingEmp.trade }})",
                                                fontSize = 10.5.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Employee Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    readOnly = isEmployeePrefilled,
                    label = { Text("Employee Full Name *" + if (isEmployeePrefilled) " (Locked)" else "") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    trailingIcon = if (isEmployeePrefilled) {
                        { Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_input"),
                    singleLine = true
                )

                // 2. Iqama Number & Card Number in a Row
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
                        readOnly = isEmployeePrefilled,
                        label = { Text("Iqama No. *" + if (isEmployeePrefilled) " (Locked)" else "") },
                        placeholder = { Text("e.g. 2489102341") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("iqama_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = {
                            cardNumber = it
                            validationError = null
                        },
                        label = { Text("Card No. *") },
                        placeholder = { Text("CRD-1021") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_input"),
                        singleLine = true
                    )
                }


                // 3. Room Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = roomDropdownExpanded,
                    onExpandedChange = { roomDropdownExpanded = !roomDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedRoomObj != null) {
                            "Room ${selectedRoomObj.room.roomNumber} - ${selectedRoomObj.room.block} (${selectedRoomObj.availableBeds} bed free)"
                        } else "No empty room available",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Room No. Allotment *") },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("room_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = roomDropdownExpanded,
                        onDismissRequest = { roomDropdownExpanded = false }
                    ) {
                        if (roomsWithFreeBeds.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No rooms with free beds available") },
                                onClick = { roomDropdownExpanded = false }
                            )
                        } else {
                            roomsWithFreeBeds.forEach { r ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Room ${r.room.roomNumber} (${r.room.block})",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${r.availableBeds} bed free • Bed 1: ${if (r.bed1Occupant == null) "Open" else "Booked"} • Bed 2: ${if (r.bed2Occupant == null) "Open" else "Booked"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Green500
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedRoomNumber = r.room.roomNumber
                                        if (r.bed1Occupant == null) selectedBedNumber = 1
                                        else if (r.bed2Occupant == null) selectedBedNumber = 2
                                        roomDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Bed Selection (Bed 1 vs Bed 2)
                Column {
                    Text(
                        text = "Bed Allotment (2 Bed Room):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = bed1Available) { selectedBedNumber = 1 },
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                !bed1Available -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                selectedBedNumber == 1 -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedBedNumber == 1,
                                    onClick = { if (bed1Available) selectedBedNumber = 1 },
                                    enabled = bed1Available
                                )
                                Column {
                                    Text(
                                        text = "Bed 1 (Bed A)",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (bed1Available) "Available" else "Booked (${selectedRoomObj?.bed1Occupant?.name?.take(8)}...)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (bed1Available) Green500 else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = bed2Available) { selectedBedNumber = 2 },
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                !bed2Available -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                selectedBedNumber == 2 -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedBedNumber == 2,
                                    onClick = { if (bed2Available) selectedBedNumber = 2 },
                                    enabled = bed2Available
                                )
                                Column {
                                    Text(
                                        text = "Bed 2 (Bed B)",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (bed2Available) "Available" else "Booked (${selectedRoomObj?.bed2Occupant?.name?.take(8)}...)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (bed2Available) Green500 else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Check-In Date (Prominent with Today preset)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Check-In Date *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        FilterChip(
                            selected = dateIn == CampViewModel.getCurrentDateString(),
                            onClick = { dateIn = CampViewModel.getCurrentDateString() },
                            label = { Text("Today (${CampViewModel.getCurrentDateString()})", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = dateIn,
                        onValueChange = { dateIn = it },
                        label = { Text("Check-In Date (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("date_in_input"),
                        singleLine = true
                    )
                }

                // 6. Trade / Role with quick chips
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
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // 7. Company & Department
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company") },
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

                // 8. Phone & Nationality
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
                        label = { Text("Nationality (Optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
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
                    if (selectedRoomNumber.isBlank()) {
                        validationError = "Please select a room."
                        return@Button
                    }
                    val isBedTaken = if (selectedBedNumber == 1) !bed1Available else !bed2Available
                    if (isBedTaken) {
                        validationError = "Bed $selectedBedNumber is already booked. Please select an available bed."
                        return@Button
                    }

                    onConfirm(
                        name,
                        iqamaNumber,
                        cardNumber,
                        selectedRoomNumber,
                        selectedBedNumber,
                        dateIn,
                        trade,
                        company,
                        department,
                        phone,
                        nationality,
                        notes
                    )
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("confirm_allotment_button")
            ) {
                Text("Confirm Allotment", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text("Cancel", fontSize = 13.sp)
            }
        }
    )
}
