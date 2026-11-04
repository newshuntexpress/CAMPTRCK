package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CampRoom
import com.example.data.model.CampStats
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.RoomWithOccupants
import com.example.ui.viewmodel.CampUiState
import com.example.util.ReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CampTrack", appName)
  }

  @Test
  fun `test 2 bed room capacity and availability tracking`() {
    val room = CampRoom(roomNumber = "A-1", block = "Block A", capacity = 2)
    val emp1 = Employee(
        name = "Ahmed",
        iqamaNumber = "2489102341",
        cardNumber = "CRD-1021",
        roomNumber = "A-1",
        bedNumber = 1,
        dateIn = "2026-10-01",
        status = EmployeeStatus.ACTIVE
    )

    // With 1 occupant (1 bed free)
    val partial = RoomWithOccupants(room = room, bed1Occupant = emp1, bed2Occupant = null)
    assertEquals(1, partial.occupiedBeds)
    assertEquals(1, partial.availableBeds)
    assertTrue(partial.isPartiallyOccupied)

    // With 2 occupants (0 beds free, Full)
    val emp2 = emp1.copy(name = "Tariq", bedNumber = 2, cardNumber = "CRD-1022", iqamaNumber = "2498213452")
    val full = RoomWithOccupants(room = room, bed1Occupant = emp1, bed2Occupant = emp2)
    assertEquals(2, full.occupiedBeds)
    assertEquals(0, full.availableBeds)
    assertTrue(full.isFull)

    // Completely vacant
    val vacant = RoomWithOccupants(room = room, bed1Occupant = null, bed2Occupant = null)
    assertEquals(0, vacant.occupiedBeds)
    assertEquals(2, vacant.availableBeds)
    assertTrue(vacant.isVacant)
  }

  @Test
  fun `test employee search filtering by name iqama card and room`() {
    val employees = listOf(
        Employee(name = "Ahmed Al-Mansoor", iqamaNumber = "2489102341", cardNumber = "CRD-1021", roomNumber = "A-1", bedNumber = 1, dateIn = "2026-09-01"),
        Employee(name = "Mohammed Tariq", iqamaNumber = "2498213452", cardNumber = "CRD-1022", roomNumber = "A-1", bedNumber = 2, dateIn = "2026-09-05"),
        Employee(name = "Rajesh Kumar", iqamaNumber = "2318491029", cardNumber = "CRD-1045", roomNumber = "A-2", bedNumber = 1, dateIn = "2026-09-12"),
        Employee(name = "Bilal Farooq", iqamaNumber = "2420918233", cardNumber = "CRD-1088", roomNumber = "A-3", bedNumber = 1, dateIn = "2026-09-20")
    )

    // 1. Search by Name
    val nameMatches = employees.filter { it.name.lowercase(Locale.ROOT).contains("tariq") }
    assertEquals(1, nameMatches.size)
    assertEquals("Mohammed Tariq", nameMatches.first().name)

    // 2. Search by Iqama number
    val iqamaMatches = employees.filter { it.iqamaNumber.contains("231849") }
    assertEquals(1, iqamaMatches.size)
    assertEquals("Rajesh Kumar", iqamaMatches.first().name)

    // 3. Search by Card No.
    val cardMatches = employees.filter { it.cardNumber.lowercase(Locale.ROOT).contains("crd-1088") }
    assertEquals(1, cardMatches.size)
    assertEquals("Bilal Farooq", cardMatches.first().name)

    // 4. Search by assigned Room No.
    val roomMatches = employees.filter { it.roomNumber == "A-1" }
    assertEquals(2, roomMatches.size)
  }

  @Test
  fun `test daily occupancy report generation`() {
    val room1 = CampRoom(roomNumber = "A-1", block = "Block A", capacity = 2)
    val room2 = CampRoom(roomNumber = "A-2", block = "Block A", capacity = 2)
    val emp = Employee(
        name = "Ahmed Al-Mansoor",
        iqamaNumber = "2489102341",
        cardNumber = "CRD-1021",
        roomNumber = "A-1",
        bedNumber = 1,
        dateIn = "2026-10-01",
        status = EmployeeStatus.ACTIVE
    )

    val r1 = RoomWithOccupants(room = room1, bed1Occupant = emp, bed2Occupant = null)
    val r2 = RoomWithOccupants(room = room2, bed1Occupant = null, bed2Occupant = null)

    val stats = CampStats(
        totalRooms = 2,
        totalBeds = 4,
        occupiedBeds = 1,
        availableBeds = 3,
        fullRoomsCount = 0,
        partialRoomsCount = 1,
        vacantRoomsCount = 1,
        activeEmployeesCount = 1
    )

    val state = CampUiState(
        rooms = listOf(r1, r2),
        stats = stats,
        allEmployees = listOf(emp)
    )

    val plainText = ReportGenerator.generatePlainTextReport(state)
    assertTrue(plainText.contains("AWAZEL CO."))
    assertTrue(plainText.contains("Ahmed Al-Mansoor"))
    assertTrue(plainText.contains("2489102341"))
    assertTrue(plainText.contains("Room A-1"))
    assertTrue(plainText.contains("Room A-2"))

    val html = ReportGenerator.generatePrintableHtml(state)
    assertTrue(html.contains("AWAZEL CO."))
    assertTrue(html.contains("Ahmed Al-Mansoor"))
    assertTrue(html.contains("Room A-1"))
    assertTrue(html.contains("Room A-2"))
  }

  @Test
  fun `test iqama expiry within 30 days detection and calculation`() {
    val baseEmp = Employee(
        name = "Test Worker",
        iqamaNumber = "2489102341",
        cardNumber = "CRD-1021",
        roomNumber = "A-1",
        bedNumber = 1,
        dateIn = "2026-09-01"
    )

    // Current test date: 2026-10-24
    val today = "2026-10-24"

    // 1. Expiring in 10 days (≤ 30 days) -> EXPIRING_SOON
    val expiringSoon = baseEmp.copy(iqamaExpiryDate = "2026-11-03")
    val info1 = expiringSoon.calculateIqamaExpiryInfo(today)
    assertEquals(com.example.data.model.IqamaExpiryStatus.EXPIRING_SOON, info1.status)
    assertEquals(10L, info1.daysRemaining)
    assertTrue(info1.isExpiringWithin30Days)
    assertTrue(info1.displayText.contains("10 days"))

    // 2. Expiring in 30 days exactly -> EXPIRING_SOON
    val expiring30d = baseEmp.copy(iqamaExpiryDate = "2026-11-23")
    val info2 = expiring30d.calculateIqamaExpiryInfo(today)
    assertEquals(com.example.data.model.IqamaExpiryStatus.EXPIRING_SOON, info2.status)
    assertEquals(30L, info2.daysRemaining)
    assertTrue(info2.isExpiringWithin30Days)

    // 3. Expiring in 45 days (> 30 days) -> VALID
    val validEmp = baseEmp.copy(iqamaExpiryDate = "2026-12-08")
    val info3 = validEmp.calculateIqamaExpiryInfo(today)
    assertEquals(com.example.data.model.IqamaExpiryStatus.VALID, info3.status)
    assertEquals(45L, info3.daysRemaining)
    org.junit.Assert.assertFalse(info3.isExpiringWithin30Days)

    // 4. Expired 5 days ago (< 0 days) -> EXPIRED
    val expiredEmp = baseEmp.copy(iqamaExpiryDate = "2026-10-19")
    val info4 = expiredEmp.calculateIqamaExpiryInfo(today)
    assertEquals(com.example.data.model.IqamaExpiryStatus.EXPIRED, info4.status)
    assertEquals(-5L, info4.daysRemaining)
    assertTrue(info4.displayText.contains("Expired 5 days ago"))
  }

  @Test
  fun `test employee edit updates details correctly`() {
    val original = Employee(
        id = 10L,
        name = "Rashid Khan",
        iqamaNumber = "2498112233",
        cardNumber = "CRD-9001",
        roomNumber = "A-2",
        bedNumber = 1,
        dateIn = "2026-08-01",
        trade = "Electrician",
        company = "Saudi Oger",
        phone = "+966 50 111 2222",
        nationality = "Pakistan",
        notes = "Old note",
        iqamaExpiryDate = "2026-11-15"
    )

    val updated = original.copy(
        name = "Rashid Mahmood Khan",
        iqamaNumber = "2498112299",
        cardNumber = "CRD-9002",
        trade = "Senior Electrician",
        company = "Aramco Camp Ops",
        phone = "+966 50 333 4444",
        nationality = "Pakistan",
        notes = "Promoted to Senior Tech",
        iqamaExpiryDate = "2027-05-20"
    )

    assertEquals("Rashid Mahmood Khan", updated.name)
    assertEquals("2498112299", updated.iqamaNumber)
    assertEquals("CRD-9002", updated.cardNumber)
    assertEquals("Senior Electrician", updated.trade)
    assertEquals("Aramco Camp Ops", updated.company)
    assertEquals("2027-05-20", updated.iqamaExpiryDate)
  }

  @Test
  fun `test employee deletion frees bed and updates room availability`() {
    val room = CampRoom(roomNumber = "B-1", block = "Block B", capacity = 2)
    val emp1 = Employee(id = 1, name = "Worker A", iqamaNumber = "111", cardNumber = "C1", roomNumber = "B-1", bedNumber = 1, dateIn = "2026-01-01")
    val emp2 = Employee(id = 2, name = "Worker B", iqamaNumber = "222", cardNumber = "C2", roomNumber = "B-1", bedNumber = 2, dateIn = "2026-01-01")

    // Initially both beds are occupied
    val initialRoom = RoomWithOccupants(room = room, bed1Occupant = emp1, bed2Occupant = emp2)
    assertEquals(2, initialRoom.occupiedBeds)
    assertEquals(0, initialRoom.availableBeds)
    assertTrue(initialRoom.isFull)

    // After deleting emp2, bed 2 becomes vacant
    val afterDeletingEmp2 = RoomWithOccupants(room = room, bed1Occupant = emp1, bed2Occupant = null)
    assertEquals(1, afterDeletingEmp2.occupiedBeds)
    assertEquals(1, afterDeletingEmp2.availableBeds)
    assertTrue(afterDeletingEmp2.isPartiallyOccupied)

    // After deleting emp1 as well, entire room becomes vacant
    val afterDeletingEmp1 = RoomWithOccupants(room = room, bed1Occupant = null, bed2Occupant = null)
    assertEquals(0, afterDeletingEmp1.occupiedBeds)
    assertEquals(2, afterDeletingEmp1.availableBeds)
    assertTrue(afterDeletingEmp1.isVacant)
  }

  @Test
  fun `test employee defaults and branding to AWAZEL CO`() {
    val emp = Employee(
        name = "Sami Al-Harbi",
        iqamaNumber = "2498110099",
        cardNumber = "CRD-2030",
        roomNumber = "A-1",
        bedNumber = 1,
        dateIn = "2026-10-01"
    )
    assertEquals("AWAZEL CO.", emp.company)
  }

  @Test
  fun `test pre-login authorization gate user approval and role enforcement`() {
    val approvedSupervisor = com.example.data.model.CampUser(
        userId = "uid-101",
        email = "supervisor@awazel.com",
        displayName = "Supervisor Ahmed",
        role = com.example.data.model.CampUserRole.SUPERVISOR.name,
        status = "APPROVED"
    )
    assertTrue("Approved user should have isApproved = true", approvedSupervisor.isApproved)
    assertTrue("Supervisor role should be detected", approvedSupervisor.isSupervisor)

    val pendingUser = com.example.data.model.CampUser(
        userId = "uid-102",
        email = "unapproved@awazel.com",
        displayName = "Pending Staff",
        role = com.example.data.model.CampUserRole.VIEW_ONLY.name,
        status = "PENDING"
    )
    org.junit.Assert.assertFalse("Pending user should NOT be approved", pendingUser.isApproved)

    val rejectedUser = com.example.data.model.CampUser(
        userId = "uid-103",
        email = "rejected@awazel.com",
        displayName = "Rejected Staff",
        role = com.example.data.model.CampUserRole.VIEW_ONLY.name,
        status = "REJECTED",
        rejectionRemarks = "Unauthorized account registration"
    )
    org.junit.Assert.assertFalse("Rejected user should NOT be approved", rejectedUser.isApproved)
    assertEquals("Unauthorized account registration", rejectedUser.rejectionRemarks)

    val designatedAdmin = com.example.data.model.CampUser(
        userId = "uid-admin",
        email = "newshuntexpress@gmail.com",
        displayName = "Root Admin",
        role = com.example.data.model.CampUserRole.ADMIN.name,
        status = "APPROVED"
    )
    assertTrue("Designated admin should be approved", designatedAdmin.isApproved)
    assertTrue("Designated admin should have isAdmin = true", designatedAdmin.isAdmin)
  }

  @Test
  fun `test employee manual status management and live vacation counts`() {
    val emp1 = Employee(
        id = 1,
        name = "Kareem Al-Otaibi",
        iqamaNumber = "2498110021",
        cardNumber = "CRD-5001",
        roomNumber = "C-1",
        bedNumber = 1,
        dateIn = "2026-08-01",
        status = EmployeeStatus.ACTIVE
    )
    val emp2 = Employee(
        id = 2,
        name = "Zubair Khan",
        iqamaNumber = "2498110022",
        cardNumber = "CRD-5002",
        roomNumber = "C-1",
        bedNumber = 2,
        dateIn = "2026-08-05",
        status = EmployeeStatus.ACTIVE
    )
    val emp3 = Employee(
        id = 3,
        name = "Suresh Patel",
        iqamaNumber = "2498110023",
        cardNumber = "CRD-5003",
        roomNumber = "C-2",
        bedNumber = 1,
        dateIn = "2026-08-10",
        status = EmployeeStatus.CHECKED_OUT,
        dateOut = "2026-10-15"
    )

    // Initially: 2 Active, 0 On Vacation, 1 Checked Out -> Total 3
    val allEmployees = listOf(emp1, emp2, emp3)
    val activeCount1 = allEmployees.count { it.status == EmployeeStatus.ACTIVE }
    val onVacationCount1 = allEmployees.count { it.status == EmployeeStatus.ON_VACATION }
    val totalCount1 = allEmployees.size

    assertEquals(2, activeCount1)
    assertEquals(0, onVacationCount1)
    assertEquals(3, totalCount1)

    // Admin manually changes emp2 status to ON_VACATION
    val emp2Vacation = emp2.copy(status = EmployeeStatus.ON_VACATION)
    val updatedEmployees = listOf(emp1, emp2Vacation, emp3)

    val activeCount2 = updatedEmployees.count { it.status == EmployeeStatus.ACTIVE }
    val onVacationCount2 = updatedEmployees.count { it.status == EmployeeStatus.ON_VACATION }
    val totalCount2 = updatedEmployees.size

    assertEquals(1, activeCount2)
    assertEquals(1, onVacationCount2)
    assertEquals(3, totalCount2)

    // Verify bed reservation: room C-1 still retains emp2Vacation's bed!
    val room = CampRoom(roomNumber = "C-1", block = "Block C", capacity = 2)
    val roomWithOcc = RoomWithOccupants(room = room, bed1Occupant = emp1, bed2Occupant = emp2Vacation)
    assertEquals("Room should remain fully occupied as vacationing staff reserve bed", 2, roomWithOcc.occupiedBeds)
    assertEquals(0, roomWithOcc.availableBeds)
    assertTrue(roomWithOcc.isFull)

    // Admin manually reactivates emp2 to ACTIVE
    val emp2Reactivated = emp2Vacation.copy(status = EmployeeStatus.ACTIVE)
    val reactivatedList = listOf(emp1, emp2Reactivated, emp3)
    assertEquals(2, reactivatedList.count { it.status == EmployeeStatus.ACTIVE })
    assertEquals(0, reactivatedList.count { it.status == EmployeeStatus.ON_VACATION })

    // Test CampStats data model calculations
    val stats = CampStats(
        totalRooms = 10,
        totalBeds = 20,
        occupiedBeds = 2,
        availableBeds = 18,
        activeEmployeesCount = 1,
        onVacationEmployeesCount = 1,
        totalEmployeesCount = 3
    )
    assertEquals(1, stats.activeEmployeesCount)
    assertEquals(1, stats.onVacationEmployeesCount)
    assertEquals(3, stats.totalEmployeesCount)

    // Test report generation contains ON VACATION tag
    val uiState = CampUiState(
        rooms = listOf(roomWithOcc),
        allEmployees = updatedEmployees,
        filteredEmployees = updatedEmployees,
        stats = stats
    )
    val htmlReport = ReportGenerator.generatePrintableHtml(uiState)
    assertTrue("HTML report should indicate staff on vacation", htmlReport.contains("ON VACATION"))
    assertTrue("HTML report should show vacation count in KPI", htmlReport.contains("${stats.onVacationEmployeesCount}"))

    val textReport = ReportGenerator.generatePlainTextReport(uiState)
    assertTrue("Text report should include Staff On Vacation count", textReport.contains("Staff On Vacation:   1 Employees"))
    assertTrue("Text report should mark bed occupant with [ON VACATION]", textReport.contains("[ON VACATION]"))
  }

  @Test
  fun `test footer branding and occupancy stats consistency`() {
    val stats = CampStats(
        totalRooms = 10,
        totalBeds = 20,
        occupiedBeds = 14,
        availableBeds = 6,
        activeEmployeesCount = 12,
        onVacationEmployeesCount = 2,
        totalEmployeesCount = 14
    )
    val uiState = CampUiState(
        isOnline = true,
        stats = stats
    )
    // Validate live footer status data mapping
    val footerBedText = "Beds: ${uiState.stats.occupiedBeds}/${uiState.stats.totalBeds} • Staff: ${uiState.stats.activeEmployeesCount}"
    assertEquals("Beds: 14/20 • Staff: 12", footerBedText)
    assertTrue("Status should indicate online", uiState.isOnline)
  }

  @Test
  fun `test template generation for PDF and CSV formats`() {
    val pdfBytes = com.example.util.BulkEmployeeImporter.generatePdfBytes()
    assertTrue("PDF bytes should not be empty", pdfBytes.isNotEmpty())
    val headerStr = String(pdfBytes.take(10).toByteArray())
    assertTrue("PDF file should start with %PDF", headerStr.startsWith("%PDF"))

    val csvBytes = com.example.util.BulkEmployeeImporter.generateCsvBytes()
    assertTrue("CSV bytes should not be empty", csvBytes.isNotEmpty())
    val csvStr = String(csvBytes)
    assertTrue("CSV should contain header Name,IqamaNumber", csvStr.contains("Name,IqamaNumber"))
    assertTrue("CSV should contain sample employee Ahmed Al-Mutairi", csvStr.contains("Ahmed Al-Mutairi"))
  }

  @Test
  fun `test template saving to downloads creates accessible file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resultPdf = com.example.util.BulkEmployeeImporter.saveTemplateToDownloads(context, com.example.util.TemplateFormat.PDF)
    assertTrue("PDF save should succeed", resultPdf.isSuccess)
    val pdfInfo = resultPdf.getOrNull()
    assertTrue("PDF info should not be null", pdfInfo != null)
    assertTrue("PDF file should exist locally", pdfInfo!!.localFile.exists())
    assertTrue("PDF file should have content", pdfInfo.localFile.length() > 0)

    val resultCsv = com.example.util.BulkEmployeeImporter.saveTemplateToDownloads(context, com.example.util.TemplateFormat.CSV)
    assertTrue("CSV save should succeed", resultCsv.isSuccess)
    val csvInfo = resultCsv.getOrNull()
    assertTrue("CSV info should not be null", csvInfo != null)
    assertTrue("CSV file should exist locally", csvInfo!!.localFile.exists())
    assertTrue("CSV file should contain Name header", csvInfo.localFile.readText().contains("Name,IqamaNumber"))
  }

  @Test
  fun `test bulk CSV parser parses valid employees and normalizes fields`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = java.io.File(context.cacheDir, "test_employees.csv")
    val content = """Name,IqamaNumber,CardNumber,RoomNumber,BedNumber,DateIn,Trade,Company,Phone,Nationality,IqamaExpiryDate,Notes
Salem Al-Dawsari,2412345678,C201,101,1,2026/03/15,Electrician,AWAZEL CO.,+966551122334,Saudi,2027-10-01,VIP Staff
Fahad Al-Muwallad,2498765432,C202,101,2,2026-03-15,Plumber,AWAZEL CO.,+966559988776,Saudi,2027-12-01,Team Lead
"""
    tempFile.writeText(content)
    val uri = android.net.Uri.fromFile(tempFile)

    val importResult = com.example.util.BulkEmployeeImporter.parseCsvUri(context, uri)
    assertEquals(2, importResult.successCount)
    assertEquals(0, importResult.errorCount)
    assertEquals(2, importResult.importedEmployees.size)

    val emp1 = importResult.importedEmployees[0]
    assertEquals("Salem Al-Dawsari", emp1.name)
    assertEquals("2412345678", emp1.iqamaNumber)
    assertEquals("C201", emp1.cardNumber)
    assertEquals("101", emp1.roomNumber)
    assertEquals(1, emp1.bedNumber)
    assertEquals("2026-03-15", emp1.dateIn) // normalized from 2026/03/15
    assertEquals(com.example.data.model.EmployeeStatus.ACTIVE, emp1.status)
  }

  @Test
  fun `test bulk CSV parser handles semicolon delimiters and catches missing required fields`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = java.io.File(context.cacheDir, "test_semicolon.csv")
    val content = """Name;IqamaNumber;CardNumber;RoomNumber;BedNumber
Hassan Ali;2455667788;C301;102;1
;2499999999;C302;102;2
Omar Khalid;;C303;103;1
"""
    tempFile.writeText(content)
    val uri = android.net.Uri.fromFile(tempFile)

    val importResult = com.example.util.BulkEmployeeImporter.parseCsvUri(context, uri)
    assertEquals(1, importResult.successCount)
    assertEquals(2, importResult.errorCount)
    assertEquals("Hassan Ali", importResult.importedEmployees[0].name)
    assertTrue("Should have error for missing name", importResult.errors.any { it.contains("Row 3") })
    assertTrue("Should have error for missing iqama", importResult.errors.any { it.contains("Row 4") })
  }

  @Test
  fun `test bulk CSV parser alerts when user uploads a PDF file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = java.io.File(context.cacheDir, "test_file.pdf")
    tempFile.writeText("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF")
    val uri = android.net.Uri.fromFile(tempFile)

    val importResult = com.example.util.BulkEmployeeImporter.parseCsvUri(context, uri)
    assertEquals(0, importResult.successCount)
    assertTrue("Should notify that file is a PDF", importResult.errors.first().contains("PDF document"))
  }

  @Test
  fun `test app update info version comparison and mandatory update flag`() {
    val currentVersionCode = com.example.BuildConfig.VERSION_CODE.toLong()
    val updateInfo = com.example.data.model.AppUpdateInfo(
        latestVersionCode = currentVersionCode + 1,
        latestVersionName = "1.1",
        releaseNotes = "Automatic multi-device updates enabled across all devices.",
        downloadUrl = "https://example.com/app-v1.1.apk",
        isMandatory = true
    )

    val isNewer = updateInfo.latestVersionCode > currentVersionCode
    assertTrue("Should detect newer version available", isNewer)
    assertTrue("Should mark update as mandatory", updateInfo.isMandatory)
    assertEquals("1.1", updateInfo.latestVersionName)
    assertTrue("Download URL should not be empty", updateInfo.downloadUrl.isNotEmpty())
  }

  @Test
  fun `test ApkUpdateManager target file and cached state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val apkFile = com.example.util.ApkUpdateManager.getApkFile(context, "1.5")
    assertTrue("APK file name should include version", apkFile.name.contains("1.5"))

    // Test cached state before and after creating file
    if (apkFile.exists()) apkFile.delete()
    assertFalse("Should report APK not downloaded yet", com.example.util.ApkUpdateManager.isApkDownloaded(context, "1.5"))

    apkFile.writeText("sample apk binary content")
    assertTrue("Should report APK is downloaded", com.example.util.ApkUpdateManager.isApkDownloaded(context, "1.5"))
    apkFile.delete()
  }

  @Test
  fun `test CampUiState reflects online and offline connectivity transitions`() {
    val defaultState = CampUiState()
    assertTrue("Default UI state should be online", defaultState.isOnline)

    val offlineState = defaultState.copy(isOnline = false)
    assertFalse("Should accurately reflect offline connectivity state", offlineState.isOnline)

    val backOnlineState = offlineState.copy(isOnline = true, lastSyncTime = "10:30 AM")
    assertTrue("Should accurately reflect back online state", backOnlineState.isOnline)
    assertEquals("10:30 AM", backOnlineState.lastSyncTime)
  }
}


