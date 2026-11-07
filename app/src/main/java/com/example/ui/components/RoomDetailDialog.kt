package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Employee
import com.example.data.model.RoomWithOccupants
import com.example.ui.theme.Amber500
import com.example.ui.theme.Green500
import com.example.ui.theme.Red500
import com.example.ui.theme.Teal600

@Composable
fun RoomDetailDialog(
    roomWithOccupants: RoomWithOccupants,
    onDismiss: () -> Unit,
    onAllotBed: (bedNumber: Int) -> Unit,
    onCheckOutEmployee: (Employee) -> Unit,
    onTransferEmployee: (Employee) -> Unit,
    onDeleteRoom: (String) -> Unit,
    isAdmin: Boolean = true
) {
    val room = roomWithOccupants.room

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .testTag("room_detail_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Room ${room.roomNumber}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                roomWithOccupants.isFull -> Red500.copy(alpha = 0.15f)
                                roomWithOccupants.isPartiallyOccupied -> Amber500.copy(alpha = 0.15f)
                                else -> Green500.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = roomWithOccupants.occupancyStatusText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    roomWithOccupants.isFull -> Red500
                                    roomWithOccupants.isPartiallyOccupied -> Amber500
                                    else -> Green500
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "${room.block} • 2 Bed Capacity",
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
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bed 1 Section
                BedOccupancyCard(
                    bedNumber = 1,
                    bedLabel = "Bed 1 (Bed A)",
                    occupant = roomWithOccupants.bed1Occupant,
                    onAllot = { onAllotBed(1) },
                    onCheckOut = { occ -> onCheckOutEmployee(occ) },
                    onTransfer = { occ -> onTransferEmployee(occ) },
                    isAdmin = isAdmin
                )

                // Bed 2 Section
                BedOccupancyCard(
                    bedNumber = 2,
                    bedLabel = "Bed 2 (Bed B)",
                    occupant = roomWithOccupants.bed2Occupant,
                    onAllot = { onAllotBed(2) },
                    onCheckOut = { occ -> onCheckOutEmployee(occ) },
                    onTransfer = { occ -> onTransferEmployee(occ) },
                    isAdmin = isAdmin
                )

                // If completely vacant, option to delete room (Admins only)
                if (roomWithOccupants.isVacant && isAdmin) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { onDeleteRoom(room.roomNumber) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Empty Room")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun BedOccupancyCard(
    bedNumber: Int,
    bedLabel: String,
    occupant: Employee?,
    onAllot: () -> Unit,
    onCheckOut: (Employee) -> Unit,
    onTransfer: (Employee) -> Unit,
    isAdmin: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (occupant != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else Green500.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            if (occupant != null) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            else Green500.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bed,
                        contentDescription = null,
                        tint = if (occupant != null) MaterialTheme.colorScheme.primary else Green500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bedLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (occupant != null) Red500.copy(alpha = 0.15f) else Green500.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (occupant != null) "BOOKED" else "AVAILABLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (occupant != null) Red500 else Green500,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (occupant != null) {
                // Occupant information
                Text(
                    text = occupant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Iqama badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Iqama: ${occupant.iqamaNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Card No badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Teal600
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Card: ${occupant.cardNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${occupant.trade} • ${occupant.company}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "In: ${occupant.dateIn}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons: Check-Out and Transfer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onCheckOut(occupant) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("checkout_button_bed_$bedNumber"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Red500
                            ),
                            border = BorderStroke(1.dp, Red500.copy(alpha = 0.4f))
                        ) {
                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check-Out", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onTransfer(occupant) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transfer_button_bed_$bedNumber")
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Transfer Bed", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Empty bed notice & Allot action
                Text(
                    text = if (isAdmin) "This bed is currently unassigned and ready for allotment." else "This bed is empty (View-Only).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onAllot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("allot_bed_${bedNumber}_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green500
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Allot Employee to Bed $bedNumber")
                    }
                }
            }
        }
    }
}
