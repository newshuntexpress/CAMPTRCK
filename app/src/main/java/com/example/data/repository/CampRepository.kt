package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccessDeniedException
import com.example.data.model.AppUpdateInfo
import com.example.data.model.CampRoom
import com.example.data.model.CampStats
import com.example.data.model.CampUser
import com.example.data.model.CampUserRole
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.RoomWithOccupants
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BulkImportDbResult(
    val importedCount: Int,
    val failureCount: Int,
    val errors: List<String>
)

class CampRepository(
    private val db: FirebaseFirestore
) {
    // Primary runtime constructor using named database ID from firebase_applet_config.xml
    constructor(context: Context) : this(
        getFirestoreWithCache(context)
    )

    companion object {
        private fun getFirestoreWithCache(context: Context): FirebaseFirestore {
            val db = FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                db.firestoreSettings = settings
            } catch (_: Exception) {
                // If firestoreSettings was already configured in process, retain existing
            }
            return db
        }
    }

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before performing data operations.")
    }

    // --- Rooms Realtime Observation ---
    fun observeRooms(): Flow<List<CampRoom>> = flow {
        val path = "rooms"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(CampRoom::class.java)?.copy(roomNumber = doc.id)
                        } catch (e: Exception) {
                            CampRoom(
                                roomNumber = doc.id,
                                block = doc.getString("block") ?: "Block A",
                                capacity = doc.getLong("capacity")?.toInt() ?: 2,
                                note = doc.getString("note").orEmpty()
                            )
                        }
                    }.sortedWith(
                        compareBy<CampRoom> { it.block }
                            .thenBy { room ->
                                val numStr = room.roomNumber.substringAfter("-").trim()
                                numStr.toIntOrNull() ?: 0
                            }
                    )
                }
                .catch { error ->
                    if (error is CancellationException) throw error
                    if (auth.currentUser != null && error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- Employees Realtime Observation ---
    fun observeEmployees(): Flow<List<Employee>> = flow {
        val path = "employees"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Employee::class.java)?.copy(firestoreId = doc.id)
                        } catch (e: Exception) {
                            parseEmployeeFallback(doc)
                        }
                    }.sortedBy { it.name }
                }
                .catch { error ->
                    if (error is CancellationException) throw error
                    if (auth.currentUser != null && error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    private fun parseEmployeeFallback(doc: com.google.firebase.firestore.DocumentSnapshot): Employee? {
        return try {
            val statusStr = doc.getString("status")
            val status = try {
                if (statusStr != null) {
                    val normalized = statusStr.trim().uppercase(Locale.ROOT).replace(" ", "_")
                    EmployeeStatus.valueOf(normalized)
                } else EmployeeStatus.ACTIVE
            } catch (_: Exception) {
                EmployeeStatus.ACTIVE
            }
            val timestamp = try {
                doc.getTimestamp("updatedAt")
            } catch (_: Exception) {
                null
            }
            Employee(
                firestoreId = doc.id,
                name = doc.getString("name").orEmpty(),
                iqamaNumber = doc.getString("iqamaNumber").orEmpty(),
                cardNumber = doc.getString("cardNumber").orEmpty(),
                roomNumber = doc.getString("roomNumber").orEmpty(),
                bedNumber = doc.getLong("bedNumber")?.toInt() ?: 1,
                dateIn = doc.getString("dateIn").orEmpty(),
                dateOut = doc.getString("dateOut"),
                iqamaExpiryDate = doc.getString("iqamaExpiryDate").orEmpty(),
                trade = doc.getString("trade") ?: "General Staff",
                company = doc.getString("companyName") ?: doc.getString("company") ?: "AWAZEL CO.",
                department = doc.getString("department").orEmpty(),
                phone = doc.getString("phone").orEmpty(),
                nationality = doc.getString("nationality").orEmpty(),
                notes = doc.getString("notes").orEmpty(),
                isSelfRegistered = doc.getBoolean("isSelfRegistered") ?: false,
                isLocked = doc.getBoolean("isLocked") ?: false,
                status = status,
                updatedAt = timestamp
            )
        } catch (_: Exception) {
            null
        }
    }

    // --- Rooms with Occupants Flow ---
    val roomsWithOccupantsFlow: Flow<List<RoomWithOccupants>> = combine(
        observeRooms(),
        observeEmployees()
    ) { rooms, employees ->
        rooms.map { room ->
            val occupantsInRoom = employees.filter { it.roomNumber == room.roomNumber && it.status == EmployeeStatus.ACTIVE }
            val bed1Emp = occupantsInRoom.find { it.bedNumber == 1 }
            val bed2Emp = occupantsInRoom.find { it.bedNumber == 2 }
            RoomWithOccupants(
                room = room,
                bed1Occupant = bed1Emp,
                bed2Occupant = bed2Emp
            )
        }
    }

    // --- Camp Statistics Flow ---
    val campStatsFlow: Flow<CampStats> = combine(
        roomsWithOccupantsFlow,
        observeEmployees()
    ) { roomsWithOccupants, allEmployees ->
        val totalRooms = roomsWithOccupants.size
        val totalBeds = totalRooms * 2
        val occupiedBeds = roomsWithOccupants.sumOf { it.occupiedBeds }
        val availableBeds = totalBeds - occupiedBeds
        val occupancyPercent = if (totalBeds > 0) ((occupiedBeds.toDouble() / totalBeds) * 100).toInt() else 0

        val fullRoomsCount = roomsWithOccupants.count { it.isFull }
        val partialRoomsCount = roomsWithOccupants.count { it.isPartiallyOccupied }
        val vacantRoomsCount = roomsWithOccupants.count { it.isVacant }

        val activeEmployees = allEmployees.filter { it.status == EmployeeStatus.ACTIVE }
        val onVacationEmployees = allEmployees.filter { it.status == EmployeeStatus.ON_VACATION }
        val checkedOutEmployees = allEmployees.filter { it.status == EmployeeStatus.CHECKED_OUT }

        CampStats(
            totalRooms = totalRooms,
            totalBeds = totalBeds,
            occupiedBeds = occupiedBeds,
            availableBeds = availableBeds,
            fullRoomsCount = fullRoomsCount,
            partialRoomsCount = partialRoomsCount,
            vacantRoomsCount = vacantRoomsCount,
            activeEmployeesCount = activeEmployees.size,
            onVacationEmployeesCount = onVacationEmployees.size,
            checkedOutCount = checkedOutEmployees.size,
            totalEmployeesCount = allEmployees.size
        )
    }

    // --- User Session Verification & Pre-Login Gate ---
    suspend fun verifyAndSyncUserSession(firebaseUser: FirebaseUser): CampUser = withContext(Dispatchers.IO) {
        val uid = firebaseUser.uid
        val email = firebaseUser.email.orEmpty().trim().lowercase()
        val displayName = firebaseUser.displayName.takeIf { !it.isNullOrBlank() } ?: email.substringBefore("@")

        val docRef = db.collection("users").document(uid)
        val adminMarkerRef = db.collection("admins").document(uid)
        val supervisorMarkerRef = db.collection("supervisors").document(uid)

        val isDesignatedAdmin = email == "newshuntexpress@gmail.com"

        val preExistingDoc = try {
            val snap = docRef.get().await()
            if (snap.exists()) snap else {
                val querySnap = db.collection("users").whereEqualTo("email", email).limit(1).get().await()
                if (!querySnap.isEmpty) querySnap.documents.first() else null
            }
        } catch (_: Exception) {
            null
        }

        val adminByEmail = if (preExistingDoc == null && !isDesignatedAdmin) {
            try {
                db.collection("admins").whereEqualTo("email", email).limit(1).get().await()
            } catch (_: Exception) { null }
        } else null

        val supervisorByEmail = if (preExistingDoc == null && !isDesignatedAdmin) {
            try {
                db.collection("supervisors").whereEqualTo("email", email).limit(1).get().await()
            } catch (_: Exception) { null }
        } else null

        val hasAdminMarker = adminByEmail != null && !adminByEmail.isEmpty
        val hasSupervisorMarker = supervisorByEmail != null && !supervisorByEmail.isEmpty

        if (!isDesignatedAdmin) {
            if (preExistingDoc == null && !hasAdminMarker && !hasSupervisorMarker) {
                try { Firebase.auth.signOut() } catch (_: Exception) {}
                throw AccessDeniedException("Pre-Login Authorization Gate: Access Denied. Your email address ($email) has not been registered in advance by the Administrator.")
            }

            val currentStatus = preExistingDoc?.getString("status") ?: if (hasAdminMarker || hasSupervisorMarker) "APPROVED" else "PENDING"
            if (!currentStatus.equals("APPROVED", ignoreCase = true)) {
                val rejectionRemarks = preExistingDoc?.getString("rejectionRemarks").orEmpty()
                try { Firebase.auth.signOut() } catch (_: Exception) {}
                val msg = when (currentStatus.uppercase()) {
                    "NOT_APPROVED", "REJECTED" -> {
                        if (rejectionRemarks.isNotBlank()) "Pre-Login Authorization Gate: Access Denied. Remarks: $rejectionRemarks"
                        else "Pre-Login Authorization Gate: Access Denied. Not Approved by Administrator."
                    }
                    else -> "Pre-Login Authorization Gate: Access Denied. Registration pending Administrator approval."
                }
                throw AccessDeniedException(msg)
            }
        }

        val userStatus = "APPROVED"
        val assignedRole: CampUserRole = if (isDesignatedAdmin) {
            CampUserRole.ADMIN
        } else if (hasAdminMarker || preExistingDoc?.getString("role") == CampUserRole.ADMIN.name) {
            CampUserRole.ADMIN
        } else if (hasSupervisorMarker || preExistingDoc?.getString("role") == CampUserRole.SUPERVISOR.name) {
            CampUserRole.SUPERVISOR
        } else {
            CampUserRole.VIEW_ONLY
        }

        try {
            val userMap = mutableMapOf<String, Any>(
                "userId" to uid,
                "email" to email,
                "displayName" to displayName,
                "role" to assignedRole.name,
                "status" to userStatus,
                "lastLoginAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val approvedBy = preExistingDoc?.getString("approvedBy")
            if (!approvedBy.isNullOrEmpty()) userMap["approvedBy"] = approvedBy
            val approvedAt = preExistingDoc?.getTimestamp("approvedAt")
            if (approvedAt != null) userMap["approvedAt"] = approvedAt
            val rejectionRemarks = preExistingDoc?.getString("rejectionRemarks")
            if (!rejectionRemarks.isNullOrEmpty()) userMap["rejectionRemarks"] = rejectionRemarks

            if (preExistingDoc == null || !preExistingDoc.exists()) {
                userMap["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(userMap, SetOptions.merge()).await()

            if (preExistingDoc != null && preExistingDoc.id != uid) {
                try { db.collection("users").document(preExistingDoc.id).delete().await() } catch (_: Exception) {}
            }

            return@withContext CampUser(
                id = uid,
                userId = uid,
                email = email,
                displayName = displayName,
                role = assignedRole.name,
                status = userStatus,
                approvedBy = userMap["approvedBy"] as? String ?: "",
                approvedAt = userMap["approvedAt"] as? com.google.firebase.Timestamp,
                rejectionRemarks = userMap["rejectionRemarks"] as? String ?: ""
            )
        } catch (e: AccessDeniedException) {
            Firebase.auth.signOut()
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext CampUser(
                id = uid,
                userId = uid,
                email = email,
                displayName = displayName,
                role = assignedRole.name,
                status = userStatus
            )
        }
    }

    suspend fun syncUserSession(firebaseUser: FirebaseUser): CampUser {
        return verifyAndSyncUserSession(firebaseUser)
    }

    fun observeCurrentUser(uid: String): Flow<CampUser?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { doc ->
                    if (!doc.exists()) null
                    else {
                        CampUser(
                            id = doc.id,
                            userId = doc.getString("userId") ?: doc.id,
                            email = doc.getString("email").orEmpty(),
                            displayName = doc.getString("displayName").orEmpty(),
                            role = doc.getString("role") ?: CampUserRole.VIEW_ONLY.name,
                            status = doc.getString("status") ?: "APPROVED",
                            approvedBy = doc.getString("approvedBy").orEmpty(),
                            approvedAt = doc.getTimestamp("approvedAt"),
                            rejectionRemarks = doc.getString("rejectionRemarks").orEmpty()
                        )
                    }
                }
                .catch { error ->
                    if (error is CancellationException) throw error
                    throw error
                }
        )
    }

    fun observeAllUsers(): Flow<List<CampUser>> = flow {
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            CampUser(
                                id = doc.id,
                                userId = doc.getString("userId") ?: doc.id,
                                email = doc.getString("email").orEmpty(),
                                displayName = doc.getString("displayName").orEmpty(),
                                role = doc.getString("role") ?: CampUserRole.VIEW_ONLY.name,
                                status = doc.getString("status") ?: "APPROVED",
                                approvedBy = doc.getString("approvedBy").orEmpty(),
                                approvedAt = doc.getTimestamp("approvedAt"),
                                rejectionRemarks = doc.getString("rejectionRemarks").orEmpty()
                            )
                        } catch (_: Exception) { null }
                    }.sortedBy { it.displayName }
                }
                .catch { error ->
                    if (error is CancellationException) throw error
                    throw error
                }
        )
    }

    suspend fun seedInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            try {
                val roomsSnap = db.collection("rooms").limit(1).get().await()
                if (roomsSnap.isEmpty) {
                    val batch = db.batch()
                    for (blockLetter in listOf("A", "B", "C", "D")) {
                        for (roomNum in 1..5) {
                            val rNo = "$blockLetter-$roomNum"
                            val ref = db.collection("rooms").document(rNo)
                            batch.set(ref, mapOf(
                                "roomNumber" to rNo,
                                "block" to "Block $blockLetter",
                                "capacity" to 2,
                                "note" to "Standard 2-bed room",
                                "updatedAt" to FieldValue.serverTimestamp()
                            ))
                        }
                    }
                    batch.commit().await()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun enableNetwork() {
        try { db.enableNetwork().await() } catch (_: Exception) {}
    }

    suspend fun forceSyncNow() {
        try {
            db.collection("rooms").get().await()
            db.collection("employees").get().await()
        } catch (_: Exception) {}
    }

    fun observeAppUpdateInfo(): Flow<AppUpdateInfo?> = flow {
        emitAll(
            db.collection("app_metadata").document("latest")
                .snapshots()
                .map { doc ->
                    val defaultUrl = "https://github.com/newshuntexpress/CampTrack/releases/download/v1.0/CampTrack_v1.0.apk"
                    if (doc.exists()) {
                        try {
                            val versionCode = doc.getLong("versionCode")
                                ?: doc.getLong("latestVersionCode")
                                ?: 1L
                            val versionName = doc.getString("latestVersionName")
                                ?: doc.getString("versionName")
                                ?: "1.0"
                            val title = doc.getString("updateTitle") ?: "Awazel Camp System Update"
                            val notes = doc.getString("releaseNotes")
                                ?: doc.getString("notes")
                                ?: "Performance improvements and updates."
                            val rawUrl = doc.getString("downloadUrl").orEmpty()
                            val url = if (rawUrl.isBlank()) defaultUrl else rawUrl
                            val isMandatory = doc.getBoolean("isMandatory") ?: false
                            val timestamp = doc.getTimestamp("updatedAt")
                            val minRequired = doc.getLong("minRequiredVersionCode") ?: 1L

                            AppUpdateInfo(
                                latestVersionCode = versionCode,
                                latestVersionName = versionName,
                                minRequiredVersionCode = minRequired,
                                updateTitle = title,
                                releaseNotes = notes,
                                downloadUrl = url,
                                isMandatory = isMandatory,
                                updatedAt = timestamp
                            )
                        } catch (_: Exception) {
                            AppUpdateInfo(downloadUrl = defaultUrl)
                        }
                    } else {
                        AppUpdateInfo(downloadUrl = defaultUrl)
                    }
                }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(AppUpdateInfo(downloadUrl = "https://github.com/newshuntexpress/CampTrack/releases/download/v1.0/CampTrack_v1.0.apk"))
                }
        )
    }

    suspend fun checkLatestAppUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val defaultUrl = "https://github.com/newshuntexpress/CampTrack/releases/download/v1.0/CampTrack_v1.0.apk"
        try {
            val doc = db.collection("app_metadata").document("latest").get().await()
            if (doc.exists()) {
                val versionCode = doc.getLong("versionCode") ?: doc.getLong("latestVersionCode") ?: 1L
                val versionName = doc.getString("latestVersionName") ?: doc.getString("versionName") ?: "1.0"
                val title = doc.getString("updateTitle") ?: "Awazel Camp System Update"
                val notes = doc.getString("releaseNotes") ?: doc.getString("notes") ?: ""
                val rawUrl = doc.getString("downloadUrl").orEmpty()
                val url = if (rawUrl.isBlank()) defaultUrl else rawUrl
                val isMandatory = doc.getBoolean("isMandatory") ?: false
                val timestamp = doc.getTimestamp("updatedAt")
                val minRequired = doc.getLong("minRequiredVersionCode") ?: 1L

                AppUpdateInfo(
                    latestVersionCode = versionCode,
                    latestVersionName = versionName,
                    minRequiredVersionCode = minRequired,
                    updateTitle = title,
                    releaseNotes = notes,
                    downloadUrl = url,
                    isMandatory = isMandatory,
                    updatedAt = timestamp
                )
            } else {
                AppUpdateInfo(downloadUrl = defaultUrl)
            }
        } catch (_: Exception) {
            AppUpdateInfo(downloadUrl = defaultUrl)
        }
    }

    // --- Room Operations ---
    suspend fun addRoom(room: CampRoom): Result<Unit> {
        val cleanRoomNumber = room.roomNumber.trim()
        val docRef = db.collection("rooms").document(cleanRoomNumber)

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("Room $cleanRoomNumber already exists."))
            }
            val payload = mapOf(
                "roomNumber" to cleanRoomNumber,
                "block" to room.block.trim().ifEmpty { "Block A" },
                "capacity" to 2,
                "note" to room.note.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(roomNumber: String): Result<Unit> {
        val cleanRoomNumber = roomNumber.trim()
        val docRef = db.collection("rooms").document(cleanRoomNumber)

        return try {
            val occupantsSnapshot = db.collection("employees")
                .whereEqualTo("roomNumber", cleanRoomNumber)
                .whereEqualTo("status", EmployeeStatus.ACTIVE.name)
                .get()
                .await()

            if (!occupantsSnapshot.isEmpty) {
                return Result.failure(IllegalStateException("Cannot delete Room $cleanRoomNumber because it still has active occupants."))
            }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Employee Operations ---
    suspend fun allotEmployee(
        name: String,
        iqamaNumber: String,
        cardNumber: String,
        roomNumber: String,
        bedNumber: Int,
        dateIn: String,
        trade: String,
        company: String,
        department: String = "",
        phone: String,
        nationality: String,
        notes: String,
        iqamaExpiryDate: String = "",
        status: EmployeeStatus = EmployeeStatus.ACTIVE
    ): Result<String> {
        val cleanRoomNumber = roomNumber.trim()
        val cleanIqama = iqamaNumber.trim()

        return try {
            if (cleanRoomNumber.isNotEmpty() && status == EmployeeStatus.ACTIVE) {
                val existingInRoom = db.collection("employees")
                    .whereEqualTo("roomNumber", cleanRoomNumber)
                    .whereEqualTo("status", EmployeeStatus.ACTIVE.name)
                    .get()
                    .await()

                val bedTaken = existingInRoom.documents.any { it.getLong("bedNumber")?.toInt() == bedNumber }
                if (bedTaken) {
                    return Result.failure(IllegalStateException("Bed $bedNumber in Room $cleanRoomNumber is already booked."))
                }
            }

            if (cleanIqama.isNotEmpty() && status == EmployeeStatus.ACTIVE) {
                val existingWithIqama = db.collection("employees")
                    .whereEqualTo("iqamaNumber", cleanIqama)
                    .whereEqualTo("status", EmployeeStatus.ACTIVE.name)
                    .get()
                    .await()

                if (!existingWithIqama.isEmpty) {
                    val existingDoc = existingWithIqama.documents.first()
                    val currentRoom = existingDoc.getString("roomNumber").orEmpty()
                    if (currentRoom.isNotEmpty()) {
                        return Result.failure(IllegalStateException("An active employee with Iqama $cleanIqama is already registered in Room $currentRoom."))
                    } else {
                        // Employee previously completed self-registration!
                        // Link and update the existing record with assigned room and bed
                        existingDoc.reference.update(
                            mapOf(
                                "name" to name.trim(),
                                "cardNumber" to cardNumber.trim().ifEmpty { existingDoc.getString("cardNumber").orEmpty() },
                                "roomNumber" to cleanRoomNumber,
                                "bedNumber" to bedNumber,
                                "dateIn" to dateIn.trim().ifEmpty { existingDoc.getString("dateIn").orEmpty() },
                                "trade" to trade.trim().ifEmpty { existingDoc.getString("trade") ?: "General Staff" },
                                "company" to company.trim().ifEmpty { existingDoc.getString("company") ?: "AWAZEL CO." },
                                "department" to department.trim().ifEmpty { existingDoc.getString("department").orEmpty() },
                                "phone" to phone.trim().ifEmpty { existingDoc.getString("phone").orEmpty() },
                                "nationality" to nationality.trim().ifEmpty { existingDoc.getString("nationality").orEmpty() },
                                "notes" to notes.trim().ifEmpty { existingDoc.getString("notes").orEmpty() },
                                "isLocked" to true,
                                "status" to status.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        return Result.success(existingDoc.id)
                    }
                }
            }

            val docRef = db.collection("employees").document()
            val payload = mapOf(
                "name" to name.trim(),
                "iqamaNumber" to cleanIqama,
                "cardNumber" to cardNumber.trim(),
                "roomNumber" to cleanRoomNumber,
                "bedNumber" to bedNumber,
                "dateIn" to dateIn.trim(),
                "dateOut" to null,
                "iqamaExpiryDate" to iqamaExpiryDate.trim(),
                "trade" to trade.trim().ifEmpty { "General Staff" },
                "company" to company.trim().ifEmpty { "AWAZEL CO." },
                "department" to department.trim(),
                "phone" to phone.trim(),
                "nationality" to nationality.trim(),
                "notes" to notes.trim(),
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "employees")
            Result.failure(e)
        }
    }

    // --- Employee Self-Registration ---
    suspend fun registerEmployeeSelfEntry(
        name: String,
        iqamaNumber: String,
        cardNumber: String,
        trade: String,
        company: String,
        department: String,
        phone: String,
        nationality: String,
        dateIn: String,
        roomNumber: String = "",
        notes: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanIqama = iqamaNumber.trim()
        val cleanPhone = phone.trim()
        val cleanDept = department.trim()
        val cleanRoom = roomNumber.trim()

        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (cleanIqama.isBlank() || cleanIqama.length != 10 || !cleanIqama.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("Iqama Number must be exactly 10 digits."))
        }
        if (cleanPhone.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Mobile Number is required."))
        }
        if (cleanDept.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Department is required."))
        }

        try {
            val existing = db.collection("employees")
                .whereEqualTo("iqamaNumber", cleanIqama)
                .whereEqualTo("status", EmployeeStatus.ACTIVE.name)
                .get()
                .await()

            if (!existing.isEmpty) {
                return@withContext Result.failure(
                    IllegalStateException("An employee with Iqama $cleanIqama has already completed registration. Your details are locked. Please contact your Camp Supervisor or Administrator.")
                )
            }

            val docRef = db.collection("employees").document()
            val cleanCard = cardNumber.trim().ifEmpty {
                "C" + System.currentTimeMillis().toString().takeLast(4)
            }
            val cleanDateIn = dateIn.trim().ifEmpty {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }

            val payload = mapOf(
                "name" to cleanName,
                "iqamaNumber" to cleanIqama,
                "cardNumber" to cleanCard,
                "roomNumber" to cleanRoom,
                "bedNumber" to null,
                "dateIn" to cleanDateIn,
                "dateOut" to null,
                "iqamaExpiryDate" to "",
                "trade" to trade.trim().ifEmpty { "General Staff" },
                "company" to company.trim().ifEmpty { "AWAZEL CO." },
                "department" to cleanDept,
                "phone" to cleanPhone,
                "nationality" to nationality.trim(),
                "notes" to notes.trim(),
                "isSelfRegistered" to true,
                "isLocked" to true,
                "status" to EmployeeStatus.ACTIVE.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "employees")
            Result.failure(e)
        }
    }

    suspend fun bulkImportEmployees(employees: List<Employee>): Result<BulkImportDbResult> = withContext(Dispatchers.IO) {
        try {
            var importedCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            for (emp in employees) {
                val result = allotEmployee(
                    name = emp.name,
                    iqamaNumber = emp.iqamaNumber,
                    cardNumber = emp.cardNumber,
                    roomNumber = emp.roomNumber ?: "",
                    bedNumber = emp.bedNumber ?: 1,
                    dateIn = emp.dateIn,
                    trade = emp.trade,
                    company = emp.company,
                    department = emp.department,
                    phone = emp.phone,
                    nationality = emp.nationality,
                    notes = emp.notes,
                    iqamaExpiryDate = emp.iqamaExpiryDate,
                    status = emp.status
                )
                if (result.isSuccess) {
                    importedCount++
                } else {
                    failureCount++
                    val msg = result.exceptionOrNull()?.message ?: "Failed to allot"
                    errors.add("${emp.name} (Iqama ${emp.iqamaNumber}): $msg")
                }
            }
            Result.success(BulkImportDbResult(importedCount, failureCount, errors))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmployeeStatus(employeeId: String, newStatus: EmployeeStatus): Result<Unit> {
        val docRef = db.collection("employees").document(employeeId)
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == EmployeeStatus.CHECKED_OUT) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                updates["dateOut"] = dateFormat.format(Date())
                updates["roomNumber"] = ""
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateEmployeeDetails(
        employeeId: String,
        name: String,
        iqamaNumber: String,
        cardNumber: String,
        dateIn: String,
        trade: String,
        company: String,
        department: String,
        phone: String,
        nationality: String,
        notes: String,
        iqamaExpiryDate: String
    ): Result<Unit> {
        val docRef = db.collection("employees").document(employeeId)
        return try {
            val updates = mapOf(
                "name" to name.trim(),
                "iqamaNumber" to iqamaNumber.trim(),
                "cardNumber" to cardNumber.trim(),
                "dateIn" to dateIn.trim(),
                "trade" to trade.trim(),
                "company" to company.trim(),
                "department" to department.trim(),
                "phone" to phone.trim(),
                "nationality" to nationality.trim(),
                "notes" to notes.trim(),
                "iqamaExpiryDate" to iqamaExpiryDate.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(employeeId: String): Result<Unit> {
        val docRef = db.collection("employees").document(employeeId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun checkOutEmployee(employeeId: String, dateOut: String): Result<Unit> {
        val docRef = db.collection("employees").document(employeeId)
        return try {
            val updates = mapOf(
                "status" to EmployeeStatus.CHECKED_OUT.name,
                "dateOut" to dateOut.trim(),
                "roomNumber" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun transferEmployee(employeeId: String, newRoomNumber: String, newBedNumber: Int): Result<Unit> {
        val cleanRoom = newRoomNumber.trim()
        return try {
            val existingInRoom = db.collection("employees")
                .whereEqualTo("roomNumber", cleanRoom)
                .whereEqualTo("status", EmployeeStatus.ACTIVE.name)
                .get()
                .await()

            val bedTaken = existingInRoom.documents.any { it.id != employeeId && it.getLong("bedNumber")?.toInt() == newBedNumber }
            if (bedTaken) {
                return Result.failure(IllegalStateException("Bed $newBedNumber in Room $cleanRoom is already booked."))
            }

            val docRef = db.collection("employees").document(employeeId)
            val updates = mapOf(
                "roomNumber" to cleanRoom,
                "bedNumber" to newBedNumber,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "employees/$employeeId")
            Result.failure(e)
        }
    }

    suspend fun createUser(email: String, name: String, role: CampUserRole): Result<Unit> {
        return try {
            val userId = UUID.randomUUID().toString()
            val userMap = mapOf(
                "userId" to userId,
                "email" to email.trim().lowercase(),
                "displayName" to name.trim(),
                "role" to role.name,
                "status" to "APPROVED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).set(userMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, newRole: CampUserRole): Result<Unit> {
        return try {
            db.collection("users").document(userId).update(
                mapOf("role" to newRole.name, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserApprovalStatus(userId: String, status: String, approverName: String, remarks: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.trim(),
                "approvedBy" to approverName.trim(),
                "approvedAt" to FieldValue.serverTimestamp(),
                "rejectionRemarks" to remarks.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishAppUpdate(versionCode: Long, versionName: String, notes: String, downloadUrl: String, isMandatory: Boolean): Result<Unit> {
        return try {
            val updateMap = mapOf(
                "versionCode" to versionCode,
                "latestVersionName" to versionName.trim(),
                "releaseNotes" to notes.trim(),
                "downloadUrl" to downloadUrl.trim(),
                "isMandatory" to isMandatory,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_metadata").document("latest").set(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
