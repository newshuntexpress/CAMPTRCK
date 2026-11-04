package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CampUserRole(val displayName: String, val description: String) {
    ADMIN("Full Authorization (Admin)", "Can create, edit, delete rooms, employees, and manage user authorizations"),
    SUPERVISOR("Camp Supervisor", "Full creation, edit, change, and delete in all camp sections (not permitted user/admin deletion rights)"),
    VIEW_ONLY("View Only", "Can view room occupancy, residents, expiry statuses, and reports without modification access")
}

@IgnoreExtraProperties
data class CampUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = CampUserRole.VIEW_ONLY.name,
    val status: String = "APPROVED",
    val approvedBy: String = "",
    val approvedAt: Timestamp? = null,
    val rejectionRemarks: String = "",
    val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isAdmin: Boolean
        get() = role == CampUserRole.ADMIN.name

    val isSupervisor: Boolean
        get() = role == CampUserRole.SUPERVISOR.name

    val isApproved: Boolean
        get() = status.uppercase() == "APPROVED"

    val roleEnum: CampUserRole
        get() = try {
            CampUserRole.valueOf(role)
        } catch (e: Exception) {
            CampUserRole.VIEW_ONLY
        }
}

class AccessDeniedException(message: String) : SecurityException(message)
