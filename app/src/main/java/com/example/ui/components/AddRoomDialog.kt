package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (roomNumber: String, block: String) -> Unit
) {
    var roomNumber by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("Block A") }
    var blockExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val blocks = listOf("Block A", "Block B", "Block C", "Block D")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_room_dialog"),
        icon = {
            Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(
                text = "Add New Camp Room",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Standard camp room specifications: 2 Bed Capacity (Bed 1 & Bed 2). Select from Blocks A, B, C, D or enter manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(all = 10.dp)
                    )
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = {
                        roomNumber = it
                        errorMsg = null
                    },
                    label = { Text("Room Number *") },
                    placeholder = { Text("e.g. A-1, D-1") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_room_number_input"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = blockExpanded,
                    onExpandedChange = { blockExpanded = !blockExpanded }
                ) {
                    OutlinedTextField(
                        value = block,
                        onValueChange = {
                            block = it
                            errorMsg = null
                        },
                        label = { Text("Camp Block *") },
                        placeholder = { Text("Select or type block name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("new_room_block_input"),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = blockExpanded,
                        onDismissRequest = { blockExpanded = false }
                    ) {
                        blocks.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    block = b
                                    blockExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomNumber.isBlank()) {
                        errorMsg = "Please enter room number"
                        return@Button
                    }
                    if (block.isBlank()) {
                        errorMsg = "Please enter camp block"
                        return@Button
                    }
                    onConfirm(roomNumber.trim(), block.trim())
                },
                modifier = Modifier.testTag("confirm_add_room_button")
            ) {
                Text("Add Room")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
