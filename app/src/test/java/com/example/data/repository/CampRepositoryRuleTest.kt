package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CampRoom
import com.example.data.model.CampUserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addRoom_asAdmin_createsRoomSuccessfully() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = CampRepository(firestore)
        repository.syncUserSession(auth.currentUser!!)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addRoom(CampRoom(roomNumber = "D-1", block = "Block D", capacity = 2, note = "Test"))
        }
        assertTrue("Add room failed: ${result.exceptionOrNull()}", result.isSuccess)

        val rooms = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRooms().first { list: List<CampRoom> -> list.any { it.roomNumber == "D-1" } }
        }
        assertTrue(rooms.map { it.roomNumber }.contains("D-1"))
    }

    @Test
    fun addRoom_asViewOnlyUser_failsWithPermissionDenied() = runBlocking {
        signInTestUser(VIEWER_EMAIL)
        val repository = CampRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addRoom(CampRoom(roomNumber = "D-2", block = "Block D", capacity = 2))
        }
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        val firestoreException = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeRooms_asViewOnlyUser_succeeds() = runBlocking {
        // First add room as admin
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = CampRepository(firestore)
        adminRepo.syncUserSession(auth.currentUser!!)
        val addRes = adminRepo.addRoom(CampRoom(roomNumber = "A-1", block = "Block A", capacity = 2))
        assertTrue("Add room failed: ${addRes.exceptionOrNull()}", addRes.isSuccess)

        // Then view as viewer
        signInTestUser(VIEWER_EMAIL)
        val viewerRepo = CampRepository(firestore)

        val rooms = withTimeout(FLOW_TIMEOUT_MS) {
            viewerRepo.observeRooms().first { list: List<CampRoom> -> list.any { it.roomNumber == "A-1" } }
        }
        assertTrue(rooms.map { it.roomNumber }.contains("A-1"))
    }

    @Test
    fun observeRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CampRepository(firestore)

        var caught = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeRooms().first()
            }
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            if (firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                caught = true
            }
        }
        assertTrue("Expected PERMISSION_DENIED for unauthenticated user", caught)
    }

    @Test
    fun userManagement_asAdmin_canCreateAndManageUsers() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val repository = CampRepository(firestore)

        // Initialize admin profile and marker
        val adminUser = repository.syncUserSession(auth.currentUser!!)
        assertEquals(CampUserRole.ADMIN.name, adminUser.role)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createUser("staff@awazel.com", "Camp Staff", CampUserRole.VIEW_ONLY)
        }
        assertTrue("Create user failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val users = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAllUsers().first { list -> list.any { it.email == "staff@awazel.com" } }
        }
        assertTrue(users.map { it.email }.contains("staff@awazel.com"))
    }

    private companion object {
        const val ADMIN_EMAIL = "newshuntexpress@gmail.com"
        const val VIEWER_EMAIL = "viewer@awazel.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
