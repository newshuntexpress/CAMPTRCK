package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "camp_rooms")
data class CampRoom(
    @PrimaryKey
    val roomNumber: String = "", // e.g., "A-1", "A-2", "B-1", "B-2"
    val block: String = "Block A", // e.g., "Block A", "Block B", "Block C", "Block D"
    val capacity: Int = 2, // Always 2 bed capacity per user requirements
    val note: String = ""
)

data class RoomWithOccupants(
    val room: CampRoom,
    val bed1Occupant: Employee? = null,
    val bed2Occupant: Employee? = null
) {
    val occupiedBeds: Int
        get() = (if (bed1Occupant != null) 1 else 0) + (if (bed2Occupant != null) 1 else 0)

    val availableBeds: Int
        get() = (2 - occupiedBeds).coerceAtLeast(0)

    val isFull: Boolean
        get() = occupiedBeds >= 2

    val isPartiallyOccupied: Boolean
        get() = occupiedBeds == 1

    val isVacant: Boolean
        get() = occupiedBeds == 0

    val occupancyStatusText: String
        get() = when {
            isFull -> "Full (2/2)"
            isPartiallyOccupied -> "1 Bed Available"
            else -> "Vacant (2 Beds)"
        }
}

data class CampStats(
    val totalRooms: Int = 0,
    val totalBeds: Int = 0,
    val occupiedBeds: Int = 0,
    val availableBeds: Int = 0,
    val fullRoomsCount: Int = 0,
    val partialRoomsCount: Int = 0,
    val vacantRoomsCount: Int = 0,
    val activeEmployeesCount: Int = 0,
    val onVacationEmployeesCount: Int = 0,
    val checkedOutCount: Int = 0,
    val totalEmployeesCount: Int = 0
) {
    val occupancyPercentage: Int
        get() = if (totalBeds > 0) ((occupiedBeds.toDouble() / totalBeds) * 100).toInt() else 0
}
