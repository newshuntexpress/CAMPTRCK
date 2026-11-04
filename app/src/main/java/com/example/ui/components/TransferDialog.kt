package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Employee
import com.example.data.model.RoomWithOccupants
import com.example.ui.theme.Green500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    employee: Employee,
    availableRooms: List<RoomWithOccupants>,
    onDismiss: () -> Unit,
    onConfirm: (newRoomNumber: String, newBedNumber: Int) -> Unit
) {
    // Rooms that have at least one vacant bed
    val roomsWithSpace = remember(availableRooms) {
        availableRooms.filter { it.availableBeds > 0 }
    }

    var selectedRoomNumber by remember {
        mutableStateOf(roomsWithSpace.firstOrNull()?.room?.roomNumber ?: "")
    }

    val selectedRoomObj = remember(selectedRoomNumber, availableRooms) {
        availableRooms.firstOrNull { it.room.roomNumber == selectedRoomNumber }
    }

    val bed1Available = selectedRoomObj?.bed1Occupant == null || selectedRoomObj?.bed1Occupant?.id == employee.id
    val bed2Available = selectedRoomObj?.bed2Occupant == null || selectedRoomObj?.bed2Occupant?.id == employee.id

    var selectedBedNumber by remember(selectedRoomNumber) {
        mutableIntStateOf(if (bed1Available) 1 else 2)
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .testTag("transfer_dialog"),
        icon = {
            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transfer Room / Bed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = employee.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Current: Room ${employee.roomNumber} • Bed ${employee.bedNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Iqama: ${employee.iqamaNumber} • Card: ${employee.cardNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Destination Room Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedRoomObj != null) {
                            "Room ${selectedRoomObj.room.roomNumber} - ${selectedRoomObj.room.block} (${selectedRoomObj.availableBeds} bed free)"
                        } else "No vacant room available",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination Room") },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        roomsWithSpace.forEach { r ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "Room ${r.room.roomNumber} (${r.room.block})",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${r.availableBeds} free bed(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Green500
                                        )
                                    }
                                },
                                onClick = {
                                    selectedRoomNumber = r.room.roomNumber
                                    if (r.bed1Occupant == null) selectedBedNumber = 1
                                    else if (r.bed2Occupant == null) selectedBedNumber = 2
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Destination Bed
                Column {
                    Text(
                        text = "Select Destination Bed:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedBedNumber == 1,
                                    onClick = { if (bed1Available) selectedBedNumber = 1 },
                                    enabled = bed1Available
                                )
                                Column {
                                    Text("Bed 1", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (bed1Available) "Available" else "Occupied",
                                        style = MaterialTheme.typography.labelSmall,
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
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedBedNumber == 2,
                                    onClick = { if (bed2Available) selectedBedNumber = 2 },
                                    enabled = bed2Available
                                )
                                Column {
                                    Text("Bed 2", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (bed2Available) "Available" else "Occupied",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bed2Available) Green500 else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRoomNumber.isBlank()) {
                        errorMsg = "Please select destination room"
                        return@Button
                    }
                    onConfirm(selectedRoomNumber, selectedBedNumber)
                },
                modifier = Modifier.testTag("confirm_transfer_button")
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
