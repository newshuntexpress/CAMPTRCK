package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CampStats
import com.example.ui.theme.Amber500
import com.example.ui.theme.Green500
import com.example.ui.theme.Red500
import com.example.ui.theme.Teal600

enum class ChartDisplayMode {
    ROOMS, // Shows Total Rooms, Occupied Rooms, Vacant Rooms
    BEDS   // Shows Total Beds, Occupied Beds, Available Beds
}

@Composable
fun RoomOccupancyChart(
    stats: CampStats,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(ChartDisplayMode.ROOMS) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(stats, displayMode) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // Room-level calculation
    val totalRooms = stats.totalRooms.coerceAtLeast(1)
    val occupiedRoomsTotal = stats.fullRoomsCount + stats.partialRoomsCount
    val vacantRoomsCount = stats.vacantRoomsCount

    val fullRoomsAngle = if (totalRooms > 0) (stats.fullRoomsCount.toFloat() / totalRooms) * 360f else 0f
    val partialRoomsAngle = if (totalRooms > 0) (stats.partialRoomsCount.toFloat() / totalRooms) * 360f else 0f
    val vacantRoomsAngle = if (totalRooms > 0) (vacantRoomsCount.toFloat() / totalRooms) * 360f else 0f

    // Bed-level calculation
    val totalBeds = stats.totalBeds.coerceAtLeast(1)
    val occupiedBeds = stats.occupiedBeds
    val availableBeds = stats.availableBeds

    val occupiedBedsAngle = if (totalBeds > 0) (occupiedBeds.toFloat() / totalBeds) * 360f else 0f
    val availableBedsAngle = if (totalBeds > 0) (availableBeds.toFloat() / totalBeds) * 360f else 0f

    val occupiedPercent = if (stats.totalRooms > 0) ((occupiedRoomsTotal.toDouble() / stats.totalRooms) * 100).toInt() else 0
    val vacantPercent = if (stats.totalRooms > 0) ((vacantRoomsCount.toDouble() / stats.totalRooms) * 100).toInt() else 0

    val occBedsPercent = if (stats.totalBeds > 0) ((occupiedBeds.toDouble() / stats.totalBeds) * 100).toInt() else 0
    val availBedsPercent = if (stats.totalBeds > 0) ((availableBeds.toDouble() / stats.totalBeds) * 100).toInt() else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_dashboard_summary_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with toggle Mode (Rooms vs Beds)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Occupancy Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (displayMode == ChartDisplayMode.ROOMS) "Room status summary" else "Bed capacity summary",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = displayMode == ChartDisplayMode.ROOMS,
                        onClick = { displayMode = ChartDisplayMode.ROOMS },
                        label = { Text("Rooms", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = displayMode == ChartDisplayMode.BEDS,
                        onClick = { displayMode = ChartDisplayMode.BEDS },
                        label = { Text("Beds", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Room Information Layout - FIRST, in compact format:
            // 1. Occupied Rooms in a single horizontal row
            // 2. Vacant Rooms in a separate single horizontal row
            if (displayMode == ChartDisplayMode.ROOMS) {
                // Row 1: Booked Rooms (Clean single row without duplicate room count or percentage badge)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (stats.fullRoomsCount > stats.partialRoomsCount) Red500 else Amber500).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, (if (stats.fullRoomsCount > stats.partialRoomsCount) Red500 else Amber500).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (stats.fullRoomsCount > stats.partialRoomsCount) Red500 else Amber500)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Booked Rooms",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${stats.fullRoomsCount} Full • ${stats.partialRoomsCount} 1-Bed Free)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 2: Empty Rooms (Separate single horizontal row)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Green500.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Green500.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Green500)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Empty Rooms",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${vacantRoomsCount * 2} Free Beds Available)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Beds Mode:
                // Row 1: Occupied Beds (Single horizontal row)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Amber500.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Amber500.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Amber500)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Booked Beds",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${stats.activeEmployeesCount} Active Residents)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Amber500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$occupiedBeds beds ($occBedsPercent%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Amber500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Row 2: Available Beds (Separate single horizontal row)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Green500.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Green500.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Green500)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Available Beds",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(Ready for Immediate Check-In)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Green500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$availableBeds beds ($availBedsPercent%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Green500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Chart Section: Rooms icon on LEFT, Beds icon on RIGHT, and Chart centrally positioned between them
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Rooms Icon & Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { displayMode = ChartDisplayMode.ROOMS }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (displayMode == ChartDisplayMode.ROOMS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (displayMode == ChartDisplayMode.ROOMS) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = "Rooms",
                                tint = if (displayMode == ChartDisplayMode.ROOMS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rooms",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (displayMode == ChartDisplayMode.ROOMS) FontWeight.Bold else FontWeight.Medium,
                        color = if (displayMode == ChartDisplayMode.ROOMS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stats.totalRooms} Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Center: Donut Chart Canvas
                Box(
                    modifier = Modifier.size(125.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(115.dp)) {
                        val strokeWidth = 16.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2,
                            (size.height - diameter) / 2
                        )
                        val arcSize = Size(diameter, diameter)

                        // Background track
                        drawArc(
                            color = Color(0xFFE2E8F0),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )

                        var currentStartAngle = -90f
                        val progress = animationProgress.value

                        if (displayMode == ChartDisplayMode.ROOMS) {
                            // 1. Full Rooms (Red)
                            if (fullRoomsAngle > 0f) {
                                val sweep = fullRoomsAngle * progress
                                drawArc(
                                    color = Red500,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                currentStartAngle += sweep
                            }

                            // 2. Partial Rooms (Amber)
                            if (partialRoomsAngle > 0f) {
                                val sweep = partialRoomsAngle * progress
                                drawArc(
                                    color = Amber500,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                currentStartAngle += sweep
                            }

                            // 3. Vacant Rooms (Green)
                            if (vacantRoomsAngle > 0f) {
                                val sweep = vacantRoomsAngle * progress
                                drawArc(
                                    color = Green500,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                            }
                        } else {
                            // Beds mode
                            if (occupiedBedsAngle > 0f) {
                                val sweep = occupiedBedsAngle * progress
                                drawArc(
                                    color = Amber500,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                currentStartAngle += sweep
                            }

                            if (availableBedsAngle > 0f) {
                                val sweep = availableBedsAngle * progress
                                drawArc(
                                    color = Green500,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                            }
                        }
                    }

                    // Center Labels inside Donut Hole
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (displayMode == ChartDisplayMode.ROOMS) "${stats.totalRooms}" else "${stats.totalBeds}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (displayMode == ChartDisplayMode.ROOMS) "TOTAL ROOMS" else "TOTAL BEDS",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right Side: Beds Icon & Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { displayMode = ChartDisplayMode.BEDS }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (displayMode == ChartDisplayMode.BEDS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (displayMode == ChartDisplayMode.BEDS) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bed,
                                contentDescription = "Beds",
                                tint = if (displayMode == ChartDisplayMode.BEDS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Beds",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (displayMode == ChartDisplayMode.BEDS) FontWeight.Bold else FontWeight.Medium,
                        color = if (displayMode == ChartDisplayMode.BEDS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stats.totalBeds} Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Multi-segment Proportional Progress Indicator Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Segmented bar
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (displayMode == ChartDisplayMode.ROOMS) {
                            val fullRatio = if (stats.totalRooms > 0) stats.fullRoomsCount.toFloat() / stats.totalRooms else 0f
                            val partialRatio = if (stats.totalRooms > 0) stats.partialRoomsCount.toFloat() / stats.totalRooms else 0f
                            val vacantRatio = if (stats.totalRooms > 0) vacantRoomsCount.toFloat() / stats.totalRooms else 0f

                            if (fullRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(fullRatio)
                                        .height(8.dp)
                                        .background(Red500)
                                )
                            }
                            if (partialRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(partialRatio)
                                        .height(8.dp)
                                        .background(Amber500)
                                )
                            }
                            if (vacantRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(vacantRatio)
                                        .height(8.dp)
                                        .background(Green500)
                                )
                            }
                        } else {
                            val occRatio = if (stats.totalBeds > 0) occupiedBeds.toFloat() / stats.totalBeds else 0f
                            val availRatio = if (stats.totalBeds > 0) availableBeds.toFloat() / stats.totalBeds else 0f

                            if (occRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(occRatio)
                                        .height(8.dp)
                                        .background(Amber500)
                                )
                            }
                            if (availRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(availRatio)
                                        .height(8.dp)
                                        .background(Green500)
                                )
                            }
                        }
                    }
                }

                // Sub-labels underneath the proportional bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (displayMode == ChartDisplayMode.ROOMS) {
                        Text(
                            text = "🔴 ${stats.fullRoomsCount} Full",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "🟡 ${stats.partialRoomsCount} 1-Bed Empty",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "🟢 ${vacantRoomsCount} Empty",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "🟡 ${occupiedBeds} Beds Booked",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "🟢 ${availableBeds} Beds Available",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
