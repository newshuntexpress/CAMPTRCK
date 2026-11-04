package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CampStats
import com.example.ui.theme.Amber500
import com.example.ui.theme.Green500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Red500
import com.example.ui.theme.Teal600

import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun StatsOverviewCard(
    stats: CampStats,
    modifier: Modifier = Modifier
) {
    var showVisualChart by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_overview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Live Occupancy + View Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Green500)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REAL-TIME OCCUPANCY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Toggle Chip for Pie Chart / Progress Indicator
                    FilterChip(
                        selected = showVisualChart,
                        onClick = { showVisualChart = !showVisualChart },
                        label = {
                            Text(
                                if (showVisualChart) "Hide Chart" else "Pie Chart",
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showVisualChart) Icons.Default.ViewAgenda else Icons.Default.PieChart,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            stats.occupancyPercentage >= 90 -> Red500.copy(alpha = 0.15f)
                            stats.occupancyPercentage >= 70 -> Amber500.copy(alpha = 0.15f)
                            else -> Green500.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = "${stats.occupancyPercentage}% Full",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                stats.occupancyPercentage >= 90 -> Red500
                                stats.occupancyPercentage >= 70 -> Amber500
                                else -> Green500
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showVisualChart) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    RoomOccupancyChart(stats = stats)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (stats.occupancyPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    stats.occupancyPercentage >= 90 -> Red500
                    stats.occupancyPercentage >= 70 -> Amber500
                    else -> Teal600
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Key Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniItem(
                    label = "Total Rooms",
                    value = "${stats.totalRooms}",
                    icon = Icons.Default.MeetingRoom,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniItem(
                    label = "Occupied Beds",
                    value = "${stats.occupiedBeds}/${stats.totalBeds}",
                    icon = Icons.Default.Bed,
                    color = Amber500,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniItem(
                    label = "Available Beds",
                    value = "${stats.availableBeds}",
                    icon = Icons.Default.CheckCircle,
                    color = Green500,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniItem(
                    label = "Active Staff",
                    value = "${stats.activeEmployeesCount}",
                    icon = Icons.Default.People,
                    color = Teal600,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Room Status Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoomStatusChip(
                    count = stats.vacantRoomsCount,
                    label = "Empty Rooms",
                    color = Green500,
                    modifier = Modifier.weight(1f)
                )
                RoomStatusChip(
                    count = stats.partialRoomsCount,
                    label = "1 Bed Free",
                    color = Amber500,
                    modifier = Modifier.weight(1f)
                )
                RoomStatusChip(
                    count = stats.fullRoomsCount,
                    label = "Full Rooms",
                    color = Red500,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Live Employee Status & Vacation Summary (Automatically updates on status changes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EMPLOYEE STATUS SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Green500.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "🟢 Live Status Count",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Green500,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Total Active Employees
                EmployeeStatusSummaryBox(
                    label = "Active Employees",
                    count = stats.activeEmployeesCount,
                    icon = Icons.Default.CheckCircle,
                    color = Green500,
                    modifier = Modifier.weight(1f)
                )

                // 2. Total Employees on Vacation
                EmployeeStatusSummaryBox(
                    label = "On Vacation",
                    count = stats.onVacationEmployeesCount,
                    icon = Icons.Default.FlightTakeoff,
                    color = Amber500,
                    modifier = Modifier.weight(1f)
                )

                // 3. Total Employees
                EmployeeStatusSummaryBox(
                    label = "Total Employees",
                    count = stats.totalEmployeesCount,
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EmployeeStatusSummaryBox(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricMiniItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RoomStatusChip(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1
            )
        }
    }
}
