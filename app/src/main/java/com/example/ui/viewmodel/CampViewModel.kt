package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppUpdateInfo
import com.example.data.model.CampRoom
import com.example.data.model.CampStats
import com.example.data.model.CampUser
import com.example.data.model.CampUserRole
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.RoomWithOccupants
import com.example.data.repository.CampRepository
import com.example.util.NetworkConnectivityObserver
import com.example.util.BulkEmployeeImporter
import android.content.Context
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RoomAvailabilityFilter(val label: String) {
    ALL("All Rooms"),
    AVAILABLE("Has Bed Available"),
    PARTIAL("1 Bed Empty"),
    VACANT("Empty (2 Beds)"),
    FULL("Full (2/2)")
}

enum class EmployeeStatusFilter(val label: String) {
    ALL("All Staff"),
    ACTIVE("Active"),
    ON_VACATION("On Vacation"),
    CHECKED_OUT("Checked Out")
}

enum class IqamaExpiryFilter(val label: String) {
    ALL("All Iqamas"),
    EXPIRING_30("Expiring ≤30 Days"),
    EXPIRED("Expired"),
    VALID("Valid (>30 Days)")
}

enum class EmployeeSearchScope(val label: String) {
    ALL("All"),
    NAME("Name"),
    IQAMA("Iqama No."),
    CARD("Card No."),
    ROOM("Room No.")
}

data class CampUiState(
    val currentUser: CampUser? = null,
    val allUsers: List<CampUser> = emptyList(),
    val rooms: List<RoomWithOccupants> = emptyList(),
    val filteredRooms: List<RoomWithOccupants> = emptyList(),
    val stats: CampStats = CampStats(),
    val allEmployees: List<Employee> = emptyList(),
    val filteredEmployees: List<Employee> = emptyList(),
    val searchQuery: String = "",
    val employeeSearchScope: EmployeeSearchScope = EmployeeSearchScope.ALL,
    val roomFilter: RoomAvailabilityFilter = RoomAvailabilityFilter.ALL,
    val selectedBlock: String = "All",
    val availableBlocks: List<String> = listOf("All"),
    val employeeStatusFilter: EmployeeStatusFilter = EmployeeStatusFilter.ACTIVE,
    val iqamaExpiryFilter: IqamaExpiryFilter = IqamaExpiryFilter.ALL,
    val expiringIqamaCount: Int = 0,
    val expiredIqamaCount: Int = 0,
    val validIqamaCount: Int = 0,
    val selectedTab: Int = 0, // 0 = Rooms & Beds, 1 = Employees, 2 = Camp Stats & Reports
    val showAllotmentDialog: Boolean = false,
    val prefillRoomForNewAllotment: String? = null,
    val prefillBedForNewAllotment: Int? = null,
    val prefillEmployeeForNewAllotment: Employee? = null,
    val showRegistrationQrDialog: Boolean = false,
    val showSelfRegistrationDialog: Boolean = false,
    val selectedRoomForDetail: RoomWithOccupants? = null,
    val employeeForCheckout: Employee? = null,
    val employeeForTransfer: Employee? = null,
    val employeeForDetail: Employee? = null,
    val employeeForEdit: Employee? = null,
    val employeeForDelete: Employee? = null,
    val showAddRoomDialog: Boolean = false,
    val showDailyReportDialog: Boolean = false,
    val showUserPanelDialog: Boolean = false,
    val showAppUpdateDialog: Boolean = false,
    val showSyncInfoDialog: Boolean = false,
    val showWebPortalDialog: Boolean = false,
    val isOnline: Boolean = true,
    val lastSyncTime: String = "Just now",
    val appUpdateInfo: AppUpdateInfo? = AppUpdateInfo(
        latestVersionCode = 1L,
        latestVersionName = "1.0",
        updateTitle = "CampTrack Android APK",
        releaseNotes = "Awazel Camp Accommodation & 2-Bed Allotments System. Production APK package with offline room database and real-time cloud sync.",
        downloadUrl = "https://raw.githubusercontent.com/newshuntexpress/CampTrack/main/CampTrack.apk"
    ),
    val hasAvailableUpdate: Boolean = false,
    val isDownloadingApk: Boolean = false,
    val apkDownloadProgress: Int = 0,
    val downloadedApkFile: java.io.File? = null,
    val autoUpdateNoticeDismissed: Boolean = false,
    val isSyncingNow: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    val pendingSelfRegisteredEmployees: List<Employee>
        get() = allEmployees.filter { it.isPendingAllotment }

    val isAdmin: Boolean
        get() = currentUser?.isAdmin == true

    val isSupervisor: Boolean
        get() = currentUser?.isSupervisor == true

    val canManageCamp: Boolean
        get() = isAdmin || isSupervisor

    val isViewOnly: Boolean
        get() = !canManageCamp
}

class CampViewModel(
    private val repository: CampRepository,
    private val connectivityObserver: NetworkConnectivityObserver? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampUiState(isLoading = true))
    val uiState: StateFlow<CampUiState> = _uiState.asStateFlow()

    private var allUsersJob: Job? = null
    private var currentUserJob: Job? = null

    private fun startObservingAllUsers() {
        if (allUsersJob == null || allUsersJob?.isActive == false) {
            allUsersJob = viewModelScope.launch {
                repository.observeAllUsers().collect { users ->
                    _uiState.update { it.copy(allUsers = users) }
                }
            }
        }
    }

    private fun stopObservingAllUsers() {
        allUsersJob?.cancel()
        allUsersJob = null
        _uiState.update { it.copy(allUsers = emptyList()) }
    }

    init {
        // Authenticate user session & seed initial online data if needed
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val user = repository.syncUserSession(firebaseUser)
                    _uiState.update { it.copy(currentUser = user) }
                    if (user.isAdmin) {
                        repository.seedInitialDataIfEmpty()
                        startObservingAllUsers()
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Authentication failed") }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }

            // Observe current user changes in realtime
            currentUserJob = viewModelScope.launch {
                repository.observeCurrentUser(firebaseUser.uid).collect { updatedUser ->
                    if (updatedUser != null) {
                        _uiState.update { it.copy(currentUser = updatedUser) }
                        if (updatedUser.isAdmin) {
                            startObservingAllUsers()
                        } else {
                            stopObservingAllUsers()
                        }
                        if (updatedUser.status.equals("REJECTED", ignoreCase = true) ||
                            updatedUser.status.equals("NOT_APPROVED", ignoreCase = true)) {
                            val email = firebaseUser.email.orEmpty().trim().lowercase()
                            if (email != "newshuntexpress@gmail.com") {
                                try { Firebase.auth.signOut() } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }

        // Combine repository online flows into UI state
        if (firebaseUser != null) {
            viewModelScope.launch {
                try {
                    combine(
                        repository.roomsWithOccupantsFlow,
                        repository.campStatsFlow,
                        repository.observeEmployees()
                    ) { rooms, stats, employees ->
                        Triple(rooms, stats, employees)
                    }.collect { (rooms, stats, employees) ->
                        val blocks = listOf("All") + rooms.map { it.room.block }.distinct().sorted()

                        _uiState.update { current ->
                            val updatedState = current.copy(
                                rooms = rooms,
                                stats = stats,
                                allEmployees = employees,
                                availableBlocks = blocks
                            )
                            applyFilters(updatedState)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.d("CampViewModel", "Camp flows stopped: ${e.message}")
                }
            }
        }

        // Automatic Multi-Device Network Connectivity & Online Sync
        if (connectivityObserver != null) {
            viewModelScope.launch {
                connectivityObserver.observe().collect { online ->
                    val wasOffline = !_uiState.value.isOnline && online
                    val nowFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    _uiState.update {
                        it.copy(
                            isOnline = online,
                            lastSyncTime = if (online) nowFormatted else it.lastSyncTime,
                            bannerMessage = if (wasOffline) "🟢 Back Online: All camp data synchronized across all devices." else it.bannerMessage
                        )
                    }
                    if (online) {
                        repository.enableNetwork()
                        repository.forceSyncNow()
                        checkAppUpdateSilently()
                    }
                }
            }
        }

        // Real-time In-App App Update listener across all installed devices
        viewModelScope.launch {
            repository.observeAppUpdateInfo().collect { updateInfo ->
                if (updateInfo != null) {
                    val currentVersionCode = com.example.BuildConfig.VERSION_CODE.toLong()
                    val isNewer = updateInfo.latestVersionCode > currentVersionCode
                    _uiState.update { current ->
                        current.copy(
                            appUpdateInfo = updateInfo,
                            hasAvailableUpdate = isNewer,
                            showAppUpdateDialog = if (isNewer && updateInfo.isMandatory) true else current.showAppUpdateDialog
                        )
                    }
                }
            }
        }
    }

    fun checkAppUpdateSilently() {
        viewModelScope.launch(Dispatchers.IO) {
            val updateInfo = repository.checkLatestAppUpdate()
            if (updateInfo != null) {
                val currentVersionCode = com.example.BuildConfig.VERSION_CODE.toLong()
                val isNewer = updateInfo.latestVersionCode > currentVersionCode
                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        current.copy(
                            appUpdateInfo = updateInfo,
                            hasAvailableUpdate = isNewer,
                            showAppUpdateDialog = if (isNewer && updateInfo.isMandatory) true else current.showAppUpdateDialog
                        )
                    }
                }
            }
        }
    }

    fun onUserAuthenticated() {
        val firebaseUser = Firebase.auth.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = repository.syncUserSession(firebaseUser)
                _uiState.update { it.copy(currentUser = user, isLoading = false) }
                if (user.isAdmin) {
                    repository.seedInitialDataIfEmpty()
                    startObservingAllUsers()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Authentication failed", isLoading = false) }
                return@launch
            }
        }

        currentUserJob?.cancel()
        currentUserJob = viewModelScope.launch {
            repository.observeCurrentUser(firebaseUser.uid).collect { updatedUser ->
                if (updatedUser != null) {
                    _uiState.update { it.copy(currentUser = updatedUser) }
                    if (updatedUser.isAdmin) {
                        startObservingAllUsers()
                    } else {
                        stopObservingAllUsers()
                    }
                } else {
                    val email = firebaseUser.email.orEmpty().trim().lowercase()
                    if (email != "newshuntexpress@gmail.com") {
                        try { Firebase.auth.signOut() } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            applyFilters(current.copy(searchQuery = query))
        }
    }

    fun setRoomFilter(filter: RoomAvailabilityFilter) {
        _uiState.update { current ->
            applyFilters(current.copy(roomFilter = filter))
        }
    }

    fun setSelectedBlock(block: String) {
        _uiState.update { current ->
            applyFilters(current.copy(selectedBlock = block))
        }
    }

    fun setEmployeeStatusFilter(filter: EmployeeStatusFilter) {
        _uiState.update { current ->
            applyFilters(current.copy(employeeStatusFilter = filter))
        }
    }

    fun setIqamaExpiryFilter(filter: IqamaExpiryFilter) {
        _uiState.update { current ->
            applyFilters(current.copy(iqamaExpiryFilter = filter))
        }
    }

    fun setEmployeeSearchScope(scope: EmployeeSearchScope) {
        _uiState.update { current ->
            applyFilters(current.copy(employeeSearchScope = scope))
        }
    }

    private fun applyFilters(state: CampUiState): CampUiState {
        val q = state.searchQuery.trim().lowercase(Locale.ROOT)
        val todayStr = getCurrentDateString()

        // Filter Rooms
        val filteredRooms = state.rooms.filter { r ->
            val blockMatches = (state.selectedBlock == "All") || (r.room.block == state.selectedBlock)
            val availabilityMatches = when (state.roomFilter) {
                RoomAvailabilityFilter.ALL -> true
                RoomAvailabilityFilter.AVAILABLE -> r.availableBeds > 0
                RoomAvailabilityFilter.PARTIAL -> r.isPartiallyOccupied
                RoomAvailabilityFilter.VACANT -> r.isVacant
                RoomAvailabilityFilter.FULL -> r.isFull
            }

            val queryMatches = if (q.isEmpty()) {
                true
            } else {
                r.room.roomNumber.lowercase(Locale.ROOT).contains(q) ||
                r.room.block.lowercase(Locale.ROOT).contains(q) ||
                (r.bed1Occupant?.name?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                (r.bed1Occupant?.iqamaNumber?.contains(q) == true) ||
                (r.bed1Occupant?.cardNumber?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                (r.bed2Occupant?.name?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                (r.bed2Occupant?.iqamaNumber?.contains(q) == true) ||
                (r.bed2Occupant?.cardNumber?.lowercase(Locale.ROOT)?.contains(q) == true)
            }

            blockMatches && availabilityMatches && queryMatches
        }

        // Count Iqama statuses among active employees
        var expiringCount = 0
        var expiredCount = 0
        var validCount = 0

        state.allEmployees.forEach { emp ->
            val info = emp.calculateIqamaExpiryInfo(todayStr)
            when (info.status) {
                com.example.data.model.IqamaExpiryStatus.EXPIRING_SOON -> expiringCount++
                com.example.data.model.IqamaExpiryStatus.EXPIRED -> expiredCount++
                com.example.data.model.IqamaExpiryStatus.VALID -> validCount++
                com.example.data.model.IqamaExpiryStatus.UNKNOWN -> {}
            }
        }

        // Filter Employees by status, iqama expiry, search query & search scope
        val filteredEmployees = state.allEmployees.filter { emp ->
            val statusMatches = when (state.employeeStatusFilter) {
                EmployeeStatusFilter.ALL -> true
                EmployeeStatusFilter.ACTIVE -> emp.status == EmployeeStatus.ACTIVE
                EmployeeStatusFilter.ON_VACATION -> emp.status == EmployeeStatus.ON_VACATION
                EmployeeStatusFilter.CHECKED_OUT -> emp.status == EmployeeStatus.CHECKED_OUT
            }

            val iqamaMatches = when (state.iqamaExpiryFilter) {
                IqamaExpiryFilter.ALL -> true
                IqamaExpiryFilter.EXPIRING_30 -> {
                    val info = emp.calculateIqamaExpiryInfo(todayStr)
                    info.status == com.example.data.model.IqamaExpiryStatus.EXPIRING_SOON
                }
                IqamaExpiryFilter.EXPIRED -> {
                    val info = emp.calculateIqamaExpiryInfo(todayStr)
                    info.status == com.example.data.model.IqamaExpiryStatus.EXPIRED
                }
                IqamaExpiryFilter.VALID -> {
                    val info = emp.calculateIqamaExpiryInfo(todayStr)
                    info.status == com.example.data.model.IqamaExpiryStatus.VALID
                }
            }

            val queryMatches = if (q.isEmpty()) {
                true
            } else {
                when (state.employeeSearchScope) {
                    EmployeeSearchScope.ALL -> {
                        emp.name.lowercase(Locale.ROOT).contains(q) ||
                        emp.iqamaNumber.lowercase(Locale.ROOT).contains(q) ||
                        emp.cardNumber.lowercase(Locale.ROOT).contains(q) ||
                        (emp.roomNumber?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                        emp.trade.lowercase(Locale.ROOT).contains(q) ||
                        emp.nationality.lowercase(Locale.ROOT).contains(q)
                    }
                    EmployeeSearchScope.NAME -> emp.name.lowercase(Locale.ROOT).contains(q)
                    EmployeeSearchScope.IQAMA -> emp.iqamaNumber.lowercase(Locale.ROOT).contains(q)
                    EmployeeSearchScope.CARD -> emp.cardNumber.lowercase(Locale.ROOT).contains(q)
                    EmployeeSearchScope.ROOM -> emp.roomNumber?.lowercase(Locale.ROOT)?.contains(q) == true
                }
            }

            statusMatches && iqamaMatches && queryMatches
        }.sortedWith(
            compareByDescending<Employee> { it.status == EmployeeStatus.ACTIVE }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )

        val updatedSelectedRoom = state.selectedRoomForDetail?.let { currentRoom ->
            state.rooms.firstOrNull { it.room.roomNumber == currentRoom.room.roomNumber }
        }

        return state.copy(
            filteredRooms = filteredRooms,
            filteredEmployees = filteredEmployees,
            selectedRoomForDetail = updatedSelectedRoom,
            expiringIqamaCount = expiringCount,
            expiredIqamaCount = expiredCount,
            validIqamaCount = validCount
        )
    }

    // --- User Panel Operations (Admin Only) ---
    fun openUserPanel() {
        if (!uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Access Denied: Full Authorization (Admin) required.") }
            return
        }
        startObservingAllUsers()
        _uiState.update { it.copy(showUserPanelDialog = true) }
    }

    fun closeUserPanel() {
        _uiState.update { it.copy(showUserPanelDialog = false) }
    }

    fun createUser(email: String, displayName: String, role: CampUserRole) {
        if (!uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Access Denied: Full Authorization (Admin) required to create users.") }
            return
        }
        viewModelScope.launch {
            val result = repository.createUser(email, displayName, role)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(bannerMessage = "User $email authorized with ${role.displayName}.")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to create user.")
                }
            }
        }
    }

    fun updateUserRole(userId: String, newRole: CampUserRole) {
        if (!uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Access Denied: Full Authorization (Admin) required to update roles.") }
            return
        }
        viewModelScope.launch {
            val result = repository.updateUserRole(userId, newRole)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(bannerMessage = "User authorization updated to ${newRole.displayName}.")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to update role.")
                }
            }
        }
    }

    fun deleteUser(userId: String) {
        if (!uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Access Denied: Full Authorization (Admin) required to delete users.") }
            return
        }
        viewModelScope.launch {
            val result = repository.deleteUser(userId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(bannerMessage = "User authorization removed successfully.")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete user.")
                }
            }
        }
    }

    // --- Dialog Controls ---
    fun openAllotmentDialog(
        roomNumber: String? = null,
        bedNumber: Int? = null,
        prefillEmployee: Employee? = null
    ) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to allocate staff.") }
            return
        }
        _uiState.update {
            it.copy(
                showAllotmentDialog = true,
                prefillRoomForNewAllotment = roomNumber,
                prefillBedForNewAllotment = bedNumber,
                prefillEmployeeForNewAllotment = prefillEmployee,
                errorMessage = null
            )
        }
    }

    fun closeAllotmentDialog() {
        _uiState.update {
            it.copy(
                showAllotmentDialog = false,
                prefillRoomForNewAllotment = null,
                prefillBedForNewAllotment = null,
                prefillEmployeeForNewAllotment = null,
                errorMessage = null
            )
        }
    }

    fun openRegistrationQrDialog() {
        _uiState.update { it.copy(showRegistrationQrDialog = true) }
    }

    fun closeRegistrationQrDialog() {
        _uiState.update { it.copy(showRegistrationQrDialog = false) }
    }

    fun openSelfRegistrationDialog() {
        _uiState.update { it.copy(showSelfRegistrationDialog = true) }
    }

    fun closeSelfRegistrationDialog() {
        _uiState.update { it.copy(showSelfRegistrationDialog = false) }
    }

    fun openRoomDetail(roomWithOccupants: RoomWithOccupants) {
        _uiState.update { it.copy(selectedRoomForDetail = roomWithOccupants) }
    }

    fun closeRoomDetail() {
        _uiState.update { it.copy(selectedRoomForDetail = null) }
    }

    fun openCheckoutDialog(employee: Employee) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to check out staff.") }
            return
        }
        _uiState.update { it.copy(employeeForCheckout = employee) }
    }

    fun closeCheckoutDialog() {
        _uiState.update { it.copy(employeeForCheckout = null) }
    }

    fun openTransferDialog(employee: Employee) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to transfer staff.") }
            return
        }
        _uiState.update { it.copy(employeeForTransfer = employee) }
    }

    fun closeTransferDialog() {
        _uiState.update { it.copy(employeeForTransfer = null) }
    }

    fun openEmployeeDetail(employee: Employee) {
        _uiState.update { it.copy(employeeForDetail = employee) }
    }

    fun closeEmployeeDetail() {
        _uiState.update { it.copy(employeeForDetail = null) }
    }

    fun openEditEmployeeDialog(employee: Employee) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to edit staff details.") }
            return
        }
        _uiState.update { it.copy(employeeForEdit = employee) }
    }

    fun closeEditEmployeeDialog() {
        _uiState.update { it.copy(employeeForEdit = null) }
    }

    fun openDeleteEmployeeDialog(employee: Employee) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to delete staff records.") }
            return
        }
        _uiState.update { it.copy(employeeForDelete = employee) }
    }

    fun closeDeleteEmployeeDialog() {
        _uiState.update { it.copy(employeeForDelete = null) }
    }

    fun openAddRoomDialog() {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to add rooms.") }
            return
        }
        _uiState.update { it.copy(showAddRoomDialog = true) }
    }

    fun closeAddRoomDialog() {
        _uiState.update { it.copy(showAddRoomDialog = false) }
    }

    fun openDailyReportDialog() {
        _uiState.update { it.copy(showDailyReportDialog = true) }
    }

    fun closeDailyReportDialog() {
        _uiState.update { it.copy(showDailyReportDialog = false) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null, errorMessage = null) }
    }

    // --- Action Implementations ---
    fun allotEmployee(
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
        iqamaExpiryDate: String = ""
    ) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to allocate staff.") }
            return
        }
        viewModelScope.launch {
            val result = repository.allotEmployee(
                name = name,
                iqamaNumber = iqamaNumber,
                cardNumber = cardNumber,
                roomNumber = roomNumber,
                bedNumber = bedNumber,
                dateIn = dateIn,
                trade = trade,
                company = company,
                department = department,
                phone = phone,
                nationality = nationality,
                notes = notes,
                iqamaExpiryDate = iqamaExpiryDate
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        showAllotmentDialog = false,
                        prefillRoomForNewAllotment = null,
                        prefillBedForNewAllotment = null,
                        bannerMessage = "Employee $name successfully allotted to Room $roomNumber (Bed $bedNumber)",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to allot employee.")
                }
            }
        }
    }

    fun submitEmployeeSelfRegistration(
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
        notes: String = "",
        onComplete: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.registerEmployeeSelfEntry(
                name = name,
                iqamaNumber = iqamaNumber,
                cardNumber = cardNumber,
                trade = trade,
                company = company,
                department = department,
                phone = phone,
                nationality = nationality,
                dateIn = dateIn,
                roomNumber = roomNumber,
                notes = notes
            )
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        bannerMessage = "Your details have been successfully saved!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Submission failed. Please check your information."
                    )
                }
            }
            onComplete(result)
        }
    }

    fun confirmDeleteEmployee(employee: Employee) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to delete staff records.") }
            return
        }
        val empId = employee.firestoreId.ifEmpty { employee.id.toString() }
        viewModelScope.launch {
            val result = repository.deleteEmployee(empId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        employeeForDelete = null,
                        employeeForCheckout = null,
                        employeeForDetail = null,
                        bannerMessage = "Employee ${employee.name} deleted. Bed status updated immediately."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        employeeForDelete = null,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete employee."
                    )
                }
            }
        }
    }

    fun confirmUpdateEmployee(
        employee: Employee,
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
    ) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to edit staff details.") }
            return
        }
        val empId = employee.firestoreId.ifEmpty { employee.id.toString() }
        viewModelScope.launch {
            val result = repository.updateEmployeeDetails(
                employeeId = empId,
                name = name,
                iqamaNumber = iqamaNumber,
                cardNumber = cardNumber,
                dateIn = dateIn,
                trade = trade,
                company = company,
                department = department,
                phone = phone,
                nationality = nationality,
                notes = notes,
                iqamaExpiryDate = iqamaExpiryDate
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        employeeForEdit = null,
                        employeeForDetail = null,
                        bannerMessage = "Employee $name details updated successfully."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to update employee."
                    )
                }
            }
        }
    }

    fun changeEmployeeStatus(employee: Employee, newStatus: EmployeeStatus) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Administrator or Camp Supervisor authorization required to update employee status.") }
            return
        }
        val empId = employee.firestoreId.ifEmpty { employee.id.toString() }
        val statusLabel = when (newStatus) {
            EmployeeStatus.ACTIVE -> "Active"
            EmployeeStatus.ON_VACATION -> "On Vacation"
            EmployeeStatus.CHECKED_OUT -> "Checked Out"
        }
        viewModelScope.launch {
            val result = repository.updateEmployeeStatus(empId, newStatus)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        employeeForDetail = null,
                        bannerMessage = "Employee ${employee.name} status updated to '$statusLabel' successfully."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to update employee status.")
                }
            }
        }
    }

    fun confirmCheckout(employee: Employee, dateOut: String) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to check out staff.") }
            return
        }
        val empId = employee.firestoreId.ifEmpty { employee.id.toString() }
        viewModelScope.launch {
            val result = repository.checkOutEmployee(empId, dateOut)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        employeeForCheckout = null,
                        bannerMessage = "Employee ${employee.name} checked out. Bed freed up immediately."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        employeeForCheckout = null,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to checkout employee."
                    )
                }
            }
        }
    }

    fun confirmTransfer(employee: Employee, newRoomNumber: String, newBedNumber: Int) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to transfer staff.") }
            return
        }
        val empId = employee.firestoreId.ifEmpty { employee.id.toString() }
        viewModelScope.launch {
            val result = repository.transferEmployee(empId, newRoomNumber, newBedNumber)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        employeeForTransfer = null,
                        bannerMessage = "Employee ${employee.name} relocated to Room $newRoomNumber (Bed $newBedNumber)."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to transfer employee."
                    )
                }
            }
        }
    }

    fun addNewRoom(roomNumber: String, block: String) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to add rooms.") }
            return
        }
        viewModelScope.launch {
            val result = repository.addRoom(
                CampRoom(
                    roomNumber = roomNumber.trim(),
                    block = block.trim().ifEmpty { "Block A" },
                    capacity = 2
                )
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        showAddRoomDialog = false,
                        bannerMessage = "Room $roomNumber (2-bed capacity) added to $block."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to add room."
                    )
                }
            }
        }
    }

    fun deleteRoom(roomNumber: String) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Full Authorization or Camp Supervisor access required to delete rooms.") }
            return
        }
        viewModelScope.launch {
            val result = repository.deleteRoom(roomNumber)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        selectedRoomForDetail = null,
                        bannerMessage = "Room $roomNumber removed."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete room."
                    )
                }
            }
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingNow = true) }
            try {
                if (_uiState.value.isOnline) {
                    repository.enableNetwork()
                }
                repository.forceSyncNow()
                val nowFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                _uiState.update {
                    it.copy(
                        isSyncingNow = false,
                        lastSyncTime = nowFormatted,
                        bannerMessage = if (it.isOnline) "All camp rooms and staff records are fully synced across all devices." else "App is offline: changes are saved locally and will auto-sync when connected."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncingNow = false,
                        errorMessage = "Sync error: ${e.message}"
                    )
                }
            }
        }
    }

    fun openWebPortalDialog() {
        _uiState.update { it.copy(showWebPortalDialog = true) }
    }

    fun closeWebPortalDialog() {
        _uiState.update { it.copy(showWebPortalDialog = false) }
    }

    fun openAppUpdateDialog() {
        _uiState.update { current ->
            current.copy(
                showAppUpdateDialog = true,
                appUpdateInfo = current.appUpdateInfo ?: AppUpdateInfo(
                    latestVersionCode = 1L,
                    latestVersionName = "1.0",
                    updateTitle = "CampTrack Android APK",
                    releaseNotes = "Awazel Camp Accommodation & 2-Bed Allotments System. Production APK package with offline room database and real-time cloud sync.",
                    downloadUrl = "https://raw.githubusercontent.com/newshuntexpress/CampTrack/main/CampTrack.apk"
                )
            )
        }
    }

    fun closeAppUpdateDialog() {
        _uiState.update { it.copy(showAppUpdateDialog = false, autoUpdateNoticeDismissed = true) }
    }

    fun dismissUpdateBanner() {
        _uiState.update { it.copy(autoUpdateNoticeDismissed = true) }
    }

    fun startDownloadAndInstallApk(context: android.content.Context) {
        val updateInfo = uiState.value.appUpdateInfo ?: AppUpdateInfo(
            latestVersionCode = 1L,
            latestVersionName = "1.0",
            updateTitle = "CampTrack Android APK",
            releaseNotes = "Awazel Camp Accommodation & 2-Bed Allotments System.",
            downloadUrl = "https://raw.githubusercontent.com/newshuntexpress/CampTrack/main/CampTrack.apk"
        )
        if (uiState.value.isDownloadingApk) return

        val directUrl = updateInfo.downloadUrl.trim()
        if (directUrl.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "APK download URL has not been configured by the Administrator yet.")
            }
            return
        }

        // If already downloaded, immediately prompt package installer
        val existingApk = com.example.util.ApkUpdateManager.getApkFile(context, updateInfo.latestVersionName)
        if (existingApk.exists() && existingApk.length() > 0) {
            _uiState.update {
                it.copy(
                    isDownloadingApk = false,
                    downloadedApkFile = existingApk,
                    bannerMessage = "APK v${updateInfo.latestVersionName} ready. Prompting installation..."
                )
            }
            val installResult = com.example.util.ApkUpdateManager.installApk(context, existingApk)
            if (installResult.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = installResult.exceptionOrNull()?.message ?: "Please allow install permission to proceed with update.")
                }
            }
            return
        }

        _uiState.update {
            it.copy(isDownloadingApk = true, apkDownloadProgress = 0, errorMessage = null)
        }

        viewModelScope.launch {
            val result = com.example.util.ApkUpdateManager.downloadApk(
                context = context,
                updateInfo = updateInfo,
                onProgress = { pct ->
                    _uiState.update { it.copy(apkDownloadProgress = pct) }
                }
            )

            result.fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            isDownloadingApk = false,
                            downloadedApkFile = file,
                            bannerMessage = "APK v${updateInfo.latestVersionName} downloaded! Launching package installer..."
                        )
                    }
                    val installResult = com.example.util.ApkUpdateManager.installApk(context, file)
                    if (installResult.isFailure) {
                        _uiState.update {
                            it.copy(errorMessage = installResult.exceptionOrNull()?.message ?: "Downloaded to ${file.name}. Please confirm install permissions to update.")
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDownloadingApk = false,
                            bannerMessage = "Direct download failed, opening browser download link..."
                        )
                    }
                    com.example.util.ApkUpdateManager.openBrowserDownload(context, directUrl)
                }
            )
        }
    }

    fun openSyncInfoDialog() {
        _uiState.update { it.copy(showSyncInfoDialog = true) }
    }

    fun closeSyncInfoDialog() {
        _uiState.update { it.copy(showSyncInfoDialog = false) }
    }

    fun triggerManualSync() = forceSync()

    fun updateUserApprovalStatus(userId: String, status: String, remarks: String = "") {
        if (!uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Administrator access required to approve or reject users.") }
            return
        }
        viewModelScope.launch {
            val approverName = uiState.value.currentUser?.displayName ?: "Administrator"
            val result = repository.updateUserApprovalStatus(userId, status, approverName, remarks)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(bannerMessage = "User status updated to $status successfully.")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to update user status.")
                }
            }
        }
    }

    fun publishAppUpdate(
        versionCode: Long,
        versionName: String,
        notes: String,
        downloadUrl: String,
        isMandatory: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.publishAppUpdate(
                versionCode = versionCode,
                versionName = versionName,
                notes = notes,
                downloadUrl = downloadUrl,
                isMandatory = isMandatory
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(bannerMessage = "App update notice broadcasted to all installed devices!")
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to broadcast update.")
                }
            }
        }
    }

    fun bulkImportEmployees(context: Context, uri: Uri, onComplete: (Int, List<String>) -> Unit) {
        if (!uiState.value.canManageCamp) {
            _uiState.update { it.copy(errorMessage = "Administrator or Camp Supervisor authorization required to import bulk staff.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val importResult = BulkEmployeeImporter.parseCsvUri(context, uri)
            if (importResult.importedEmployees.isNotEmpty()) {
                val res = repository.bulkImportEmployees(importResult.importedEmployees)
                val dbRes = res.getOrNull()
                val totalSuccess = dbRes?.importedCount ?: 0
                val combinedErrors = mutableListOf<String>()
                combinedErrors.addAll(importResult.errors)
                if (dbRes != null) {
                    combinedErrors.addAll(dbRes.errors)
                }
                if (res.isFailure) {
                    combinedErrors.add(res.exceptionOrNull()?.message ?: "Database error during import")
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            bannerMessage = "Successfully imported $totalSuccess staff records" + if (combinedErrors.isNotEmpty()) " (${combinedErrors.size} skipped/issues)" else "."
                        )
                    }
                    onComplete(totalSuccess, combinedErrors)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Import failed: ${importResult.errors.firstOrNull() ?: "No valid records found in file."}"
                        )
                    }
                    onComplete(0, importResult.errors)
                }
            }
        }
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}

class CampViewModelFactory(
    private val repository: CampRepository,
    private val connectivityObserver: NetworkConnectivityObserver? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CampViewModel::class.java)) {
            return CampViewModel(repository, connectivityObserver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
