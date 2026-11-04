package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CampStats
import com.example.data.model.Employee
import com.example.data.model.RoomWithOccupants
import com.example.ui.components.StatsOverviewCard
import com.example.ui.theme.Amber500
import com.example.ui.theme.Green500
import com.example.ui.theme.Red500
import com.example.ui.theme.Teal600
import com.example.ui.viewmodel.CampUiState
import com.example.ui.viewmodel.RoomAvailabilityFilter

@Composable
fun RoomsAvailabilityTab(
    uiState: CampUiState,
    onFilterChange: (RoomAvailabilityFilter) -> Unit,
    onBlockChange: (String) -> Unit,
    onRoomClick: (RoomWithOccupants) -> Unit,
    onQuickAllotBed: (roomNumber: String, bedNumber: Int) -> Unit,
    onAddNewRoomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("rooms_availability_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Real-Time Stats Overview Card
        item {
            StatsOverviewCard(stats = uiState.stats)
        }

        // Room Availability Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Filter Availability:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RoomAvailabilityFilter.values().forEach { filter ->
                        val isSelected = uiState.roomFilter == filter
                        val badgeCount = when (filter) {
                            RoomAvailabilityFilter.ALL -> uiState.stats.totalRooms
                            RoomAvailabilityFilter.AVAILABLE -> uiState.stats.vacantRoomsCount + uiState.stats.partialRoomsCount
                            RoomAvailabilityFilter.PARTIAL -> uiState.stats.partialRoomsCount
                            RoomAvailabilityFilter.VACANT -> uiState.stats.vacantRoomsCount
                            RoomAvailabilityFilter.FULL -> uiState.stats.fullRoomsCount
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterChange(filter) },
                            label = {
                                Text("${filter.label} ($badgeCount)")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Block Filters
                if (uiState.availableBlocks.size > 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.availableBlocks.forEach { block ->
                            FilterChip(
                                selected = uiState.selectedBlock == block,
                                onClick = { onBlockChange(block) },
                                label = { Text(block) }
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Available Rooms List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camp Rooms (${uiState.filteredRooms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Tap room to manage beds",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Empty state if no matching rooms
        if (uiState.filteredRooms.isEmpty()) {
            item {
                EmptyRoomsCard(
                    query = uiState.searchQuery,
                    onAddNewRoom = onAddNewRoomClick
                )
            }
        } else {
            // Room Cards with 2-bed status
            items(
                items = uiState.filteredRooms,
                key = { it.room.roomNumber }
            ) { roomWithOccupants ->
                RoomAvailabilityCard(
                    roomWithOccupants = roomWithOccupants,
                    isAdmin = uiState.canManageCamp,
                    onClick = { onRoomClick(roomWithOccupants) },
                    onQuickAllotBed = onQuickAllotBed
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun RoomAvailabilityCard(
    roomWithOccupants: RoomWithOccupants,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onQuickAllotBed: (roomNumber: String, bedNumber: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val room = roomWithOccupants.room
    val statusColor = when {
        roomWithOccupants.isFull -> Red500
        roomWithOccupants.isPartiallyOccupied -> Amber500
        else -> Green500
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("room_card_${room.roomNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Room Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Room ${room.roomNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = room.block,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = roomWithOccupants.occupancyStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Bed Layout: Visual representation of Bed 1 & Bed 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bed 1
                BedMiniSlot(
                    bedNumber = 1,
                    bedLabel = "Bed 1",
                    occupant = roomWithOccupants.bed1Occupant,
                    isAdmin = isAdmin,
                    onQuickAllot = { onQuickAllotBed(room.roomNumber, 1) },
                    modifier = Modifier.weight(1f)
                )

                // Bed 2
                BedMiniSlot(
                    bedNumber = 2,
                    bedLabel = "Bed 2",
                    occupant = roomWithOccupants.bed2Occupant,
                    isAdmin = isAdmin,
                    onQuickAllot = { onQuickAllotBed(room.roomNumber, 2) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BedMiniSlot(
    bedNumber: Int,
    bedLabel: String,
    occupant: Employee?,
    isAdmin: Boolean,
    onQuickAllot: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (occupant != null) {
        // Occupied Bed
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = bedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = occupant.cardNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Teal600,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = occupant.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "Iqama: ${occupant.iqamaNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Check-In: ${occupant.dateIn}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    } else {
        // Vacant Bed
        Surface(
            modifier = if (isAdmin) modifier.clickable(onClick = onQuickAllot) else modifier,
            shape = RoundedCornerShape(10.dp),
            color = Green500.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Green500.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bed,
                        contentDescription = null,
                        tint = Green500,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$bedLabel Free",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Green500
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (isAdmin) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Green500,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Tap to Allot",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Green500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "Vacant (Available)",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Green500,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRoomsCard(
    query: String,
    onAddNewRoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (query.isNotBlank()) "No rooms match '$query'" else "No rooms found for this filter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Adjust filters or add new 2-bed rooms to the camp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onAddNewRoom) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Camp Room")
            }
        }
    }
}
