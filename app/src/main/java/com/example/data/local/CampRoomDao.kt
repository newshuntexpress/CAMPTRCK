package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CampRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface CampRoomDao {
    @Query("SELECT * FROM camp_rooms ORDER BY block ASC, roomNumber ASC")
    fun getAllRoomsFlow(): Flow<List<CampRoom>>

    @Query("SELECT * FROM camp_rooms ORDER BY block ASC, roomNumber ASC")
    suspend fun getAllRooms(): List<CampRoom>

    @Query("SELECT * FROM camp_rooms WHERE roomNumber = :roomNumber LIMIT 1")
    suspend fun getRoomByNumber(roomNumber: String): CampRoom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: CampRoom)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<CampRoom>)

    @Query("DELETE FROM camp_rooms WHERE roomNumber = :roomNumber")
    suspend fun deleteRoom(roomNumber: String)

    @Query("SELECT COUNT(*) FROM camp_rooms")
    suspend fun getRoomCount(): Int
}
