package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EmployeeStatus {
    ACTIVE,
    ON_VACATION,
    CHECKED_OUT
}

enum class IqamaExpiryStatus {
    EXPIRED,        // Less than 0 days (past expiry date)
    EXPIRING_SOON,  // Expiring within the next 30 days (0..30 days)
    VALID,          // More than 30 days remaining
    UNKNOWN         // No expiry date recorded or unparseable
}

data class IqamaExpiryInfo(
    val status: IqamaExpiryStatus,
    val daysRemaining: Long?,
    val formattedExpiryDate: String,
    val displayText: String,
    val isExpiringWithin30Days: Boolean
)

@IgnoreExtraProperties
@Entity(
    tableName = "employees",
    indices = [
        Index(value = ["iqamaNumber"]),
        Index(value = ["cardNumber"]),
        Index(value = ["roomNumber"]),
        Index(value = ["status"])
    ]
)
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @DocumentId
    val firestoreId: String = "",
    val name: String = "",
    val iqamaNumber: String = "",
    val cardNumber: String = "",
    val roomNumber: String? = null, // null when checked out or unallotted
    val bedNumber: Int? = null,     // 1 for Bed 1, 2 for Bed 2; null when checked out
    val dateIn: String = "",      // YYYY-MM-DD
    val dateOut: String? = null, // YYYY-MM-DD when checked out or expected
    val iqamaExpiryDate: String = "", // YYYY-MM-DD
    val trade: String = "General Staff", // e.g., Electrician, Driver, Technician
    val company: String = "AWAZEL CO.",
    val department: String = "",
    val phone: String = "",
    val nationality: String = "",
    val status: EmployeeStatus = EmployeeStatus.ACTIVE,
    val notes: String = "",
    val isSelfRegistered: Boolean = false,
    val isLocked: Boolean = false,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    val isPendingAllotment: Boolean
        get() = (roomNumber.isNullOrEmpty() || roomNumber == "Unallotted") && status == EmployeeStatus.ACTIVE
    val updatedAtMillis: Long
        get() = updatedAt?.toDate()?.time ?: System.currentTimeMillis()

    val uniqueKey: String
        get() = when {
            firestoreId.isNotEmpty() -> firestoreId
            id != 0L -> "local_$id"
            else -> "emp_${name.trim()}_${iqamaNumber.trim()}_${cardNumber.trim()}_${hashCode()}"
        }
    /**
     * Computes the Iqama expiry status relative to a given date (defaults to current system date).
     * Accurately determines if the Iqama is expiring within the next 30 days.
     */
    fun calculateIqamaExpiryInfo(
        currentDateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): IqamaExpiryInfo {
        if (iqamaExpiryDate.isBlank()) {
            return IqamaExpiryInfo(
                status = IqamaExpiryStatus.UNKNOWN,
                daysRemaining = null,
                formattedExpiryDate = "Not Set",
                displayText = "Expiry Not Set",
                isExpiringWithin30Days = false
            )
        }

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
                isLenient = false
            }
            val expiry = sdf.parse(iqamaExpiryDate.trim())
            val today = sdf.parse(currentDateStr.trim())

            if (expiry == null || today == null) {
                IqamaExpiryInfo(
                    status = IqamaExpiryStatus.UNKNOWN,
                    daysRemaining = null,
                    formattedExpiryDate = iqamaExpiryDate,
                    displayText = iqamaExpiryDate,
                    isExpiringWithin30Days = false
                )
            } else {
                val diffMs = expiry.time - today.time
                // Round to whole days
                val days = diffMs / (1000L * 60 * 60 * 24)
                when {
                    days < 0 -> {
                        val daysAgo = -days
                        IqamaExpiryInfo(
                            status = IqamaExpiryStatus.EXPIRED,
                            daysRemaining = days,
                            formattedExpiryDate = iqamaExpiryDate,
                            displayText = if (daysAgo == 1L) "Expired 1 day ago" else "Expired $daysAgo days ago",
                            isExpiringWithin30Days = true
                        )
                    }
                    days == 0L -> {
                        IqamaExpiryInfo(
                            status = IqamaExpiryStatus.EXPIRING_SOON,
                            daysRemaining = 0,
                            formattedExpiryDate = iqamaExpiryDate,
                            displayText = "Expires Today!",
                            isExpiringWithin30Days = true
                        )
                    }
                    days <= 30 -> {
                        IqamaExpiryInfo(
                            status = IqamaExpiryStatus.EXPIRING_SOON,
                            daysRemaining = days,
                            formattedExpiryDate = iqamaExpiryDate,
                            displayText = if (days == 1L) "Expires tomorrow (1 day)" else "Expires in $days days",
                            isExpiringWithin30Days = true
                        )
                    }
                    else -> {
                        IqamaExpiryInfo(
                            status = IqamaExpiryStatus.VALID,
                            daysRemaining = days,
                            formattedExpiryDate = iqamaExpiryDate,
                            displayText = "Valid ($days days left)",
                            isExpiringWithin30Days = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            IqamaExpiryInfo(
                status = IqamaExpiryStatus.UNKNOWN,
                daysRemaining = null,
                formattedExpiryDate = iqamaExpiryDate,
                displayText = iqamaExpiryDate,
                isExpiringWithin30Days = false
            )
        }
    }
}
