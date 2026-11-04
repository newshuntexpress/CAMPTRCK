package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.CampRoom
import com.example.data.model.Employee
import com.google.firebase.Timestamp
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Timestamp?): Long? = value?.toDate()?.time

    @TypeConverter
    fun toTimestamp(value: Long?): Timestamp? = value?.let { Timestamp(Date(it)) }
}

@Database(
    entities = [CampRoom::class, Employee::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CampDatabase : RoomDatabase() {
    abstract fun campRoomDao(): CampRoomDao
    abstract fun employeeDao(): EmployeeDao

    companion object {
        @Volatile
        private var INSTANCE: CampDatabase? = null

        fun getDatabase(context: Context): CampDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampDatabase::class.java,
                    "camp_management_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
