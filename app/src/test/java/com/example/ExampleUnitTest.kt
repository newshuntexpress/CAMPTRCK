package com.example

import com.example.ui.components.compareRoomSequences
import com.example.ui.viewmodel.CampUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testOnlineOfflineStateTransitions() {
    val defaultState = CampUiState()
    assertTrue("Default state should be online", defaultState.isOnline)

    val offlineState = defaultState.copy(isOnline = false)
    assertFalse("State should accurately reflect offline mode", offlineState.isOnline)
  }

  @Test
  fun testEmployeePendingAllotment() {
    val pendingEmployee = com.example.data.model.Employee(
      id = 101,
      name = "Ahmed Al-Harbi",
      iqamaNumber = "2489102341",
      department = "Maintenance",
      isSelfRegistered = true,
      isLocked = true,
      roomNumber = "",
      bedNumber = null
    )
    assertTrue("Self-registered employee without room should be pending allotment", pendingEmployee.isPendingAllotment)
    assertTrue("Self-registered employee should be locked after submission", pendingEmployee.isLocked)

    val allottedEmployee = pendingEmployee.copy(roomNumber = "A-101", bedNumber = 1)
    assertFalse("Allotted employee should not be pending allotment", allottedEmployee.isPendingAllotment)
  }

  @Test
  fun testQrCodeUrls() {
    val url = com.example.util.QrCodeGenerator.generateEmployeeAllotmentUrl("emp_123", "2489102341")
    assertTrue("Allotment URL should contain empId", url.contains("empId=emp_123"))
    assertTrue("Allotment URL should contain iqama", url.contains("iqama=2489102341"))
    assertEquals("camptrack://register", com.example.util.QrCodeGenerator.REGISTRATION_DEEP_LINK)
    assertEquals("https://awazelcamp.online/", com.example.util.QrCodeGenerator.REGISTRATION_WEB_URL)
  }

  @Test
  fun testRoomNumberSequence() {
    val unsorted = listOf("A-10", "B-2", "A-2", "B-10", "A-1", "B-1", "A-20", "C-5", "C-1")
    val sorted = unsorted.sortedWith(::compareRoomSequences)
    val expected = listOf("A-1", "A-2", "A-10", "A-20", "B-1", "B-2", "B-10", "C-1", "C-5")
    assertEquals(expected, sorted)
  }
}
