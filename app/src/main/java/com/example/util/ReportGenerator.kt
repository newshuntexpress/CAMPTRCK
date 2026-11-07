package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.model.CampStats
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import com.example.data.model.RoomWithOccupants
import com.example.ui.viewmodel.CampUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {

    fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun generatePrintableHtml(uiState: CampUiState): String {
        val stats = uiState.stats
        val today = getTodayDate()
        val timestamp = getCurrentTimestamp()

        val occupiedRooms = uiState.rooms.filter { it.occupiedBeds > 0 }
        val vacantRooms = uiState.rooms.filter { it.isVacant }

        val occupiedRowsHtml = buildString {
            if (occupiedRooms.isEmpty()) {
                append("<tr><td colspan='6' style='text-align:center; padding:10px; color:#64748b;'>No booked rooms found.</td></tr>")
            } else {
                occupiedRooms.forEach { r ->
                    val b1 = r.bed1Occupant
                    val b2 = r.bed2Occupant

                    val b1StatusBadge = when (b1?.status) {
                        EmployeeStatus.ON_VACATION -> " <span style='background:#fef3c7; color:#d97706; padding:1px 5px; border-radius:3px; font-weight:bold; font-size:10px;'>[ON VACATION]</span>"
                        EmployeeStatus.ACTIVE -> " <span style='background:#dcfce7; color:#15803d; padding:1px 5px; border-radius:3px; font-weight:bold; font-size:10px;'>[ACTIVE]</span>"
                        else -> ""
                    }
                    val bed1Text = if (b1 != null) {
                        "<b>${b1.name}</b>$b1StatusBadge<br><small style='color:#475569;'>Iqama: ${b1.iqamaNumber} | Card: ${b1.cardNumber}<br>Trade: ${b1.trade} | In: ${b1.dateIn}</small>"
                    } else {
                        "<span style='color:#10b981; font-weight:bold;'>[EMPTY]</span>"
                    }

                    val b2StatusBadge = when (b2?.status) {
                        EmployeeStatus.ON_VACATION -> " <span style='background:#fef3c7; color:#d97706; padding:1px 5px; border-radius:3px; font-weight:bold; font-size:10px;'>[ON VACATION]</span>"
                        EmployeeStatus.ACTIVE -> " <span style='background:#dcfce7; color:#15803d; padding:1px 5px; border-radius:3px; font-weight:bold; font-size:10px;'>[ACTIVE]</span>"
                        else -> ""
                    }
                    val bed2Text = if (b2 != null) {
                        "<b>${b2.name}</b>$b2StatusBadge<br><small style='color:#475569;'>Iqama: ${b2.iqamaNumber} | Card: ${b2.cardNumber}<br>Trade: ${b2.trade} | In: ${b2.dateIn}</small>"
                    } else {
                        "<span style='color:#10b981; font-weight:bold;'>[EMPTY]</span>"
                    }

                    val statusBadge = if (r.isFull) {
                        "<span style='background:#fee2e2; color:#dc2626; padding:3px 8px; border-radius:4px; font-weight:bold; font-size:11px;'>FULL (2/2)</span>"
                    } else {
                        "<span style='background:#fef3c7; color:#d97706; padding:3px 8px; border-radius:4px; font-weight:bold; font-size:11px;'>1 BED FREE</span>"
                    }

                    append("""
                        <tr>
                            <td style='font-weight:bold; font-size:14px; text-align:center;'>Room ${r.room.roomNumber}</td>
                            <td>${r.room.block}</td>
                            <td style='text-align:center;'>$statusBadge</td>
                            <td>$bed1Text</td>
                            <td>$bed2Text</td>
                        </tr>
                    """.trimIndent())
                }
            }
        }

        val vacantRowsHtml = buildString {
            if (vacantRooms.isEmpty()) {
                append("<tr><td colspan='4' style='text-align:center; padding:10px; color:#64748b;'>All rooms currently have at least one occupant.</td></tr>")
            } else {
                vacantRooms.forEach { r ->
                    append("""
                        <tr>
                            <td style='font-weight:bold; font-size:14px; text-align:center; color:#059669;'>Room ${r.room.roomNumber}</td>
                            <td>${r.room.block}</td>
                            <td style='text-align:center;'><span style='background:#d1fae5; color:#059669; padding:3px 8px; border-radius:4px; font-weight:bold; font-size:11px;'>2 BEDS EMPTY</span></td>
                            <td style='color:#059669; font-weight:500;'>Bed 1 & Bed 2 available for allotment</td>
                        </tr>
                    """.trimIndent())
                }
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Daily Camp Occupancy Report - $today</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; margin: 20px; color: #1e293b; background: #fff; }
                    .header { border-bottom: 2px solid #0f3d5e; padding-bottom: 12px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: flex-end; }
                    .title { font-size: 22px; font-weight: bold; color: #0f3d5e; margin: 0; }
                    .subtitle { font-size: 13px; color: #64748b; margin-top: 4px; }
                    .meta { text-align: right; font-size: 12px; color: #475569; }
                    
                    .kpi-container { display: flex; gap: 10px; margin-bottom: 24px; }
                    .kpi-box { flex: 1; border: 1px solid #cbd5e1; border-radius: 8px; padding: 12px; background: #f8fafc; text-align: center; }
                    .kpi-val { font-size: 20px; font-weight: bold; color: #0f3d5e; }
                    .kpi-lbl { font-size: 11px; text-transform: uppercase; color: #64748b; margin-top: 2px; }
                    
                    h2 { font-size: 16px; color: #0f3d5e; margin-top: 24px; margin-bottom: 10px; border-left: 4px solid #0f3d5e; padding-left: 8px; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 12px; }
                    th { background: #f1f5f9; color: #334155; font-weight: 600; text-align: left; padding: 8px 10px; border: 1px solid #cbd5e1; }
                    td { padding: 8px 10px; border: 1px solid #e2e8f0; vertical-align: top; }
                    tr:nth-child(even) { background: #fafafa; }
                    
                    .footer { margin-top: 40px; border-top: 1px dashed #cbd5e1; padding-top: 16px; display: flex; justify-content: space-between; font-size: 12px; color: #64748b; }
                    .sign-box { border-top: 1px solid #000; width: 180px; text-align: center; padding-top: 6px; margin-top: 40px; }
                    
                    @media print {
                        body { margin: 10mm; }
                        .no-print { display: none; }
                        table { page-break-inside: auto; }
                        tr { page-break-inside: avoid; page-break-after: auto; }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <h1 class="title">AWAZEL CO.</h1>
                        <div class="subtitle">Camp Accommodation Daily Report • Allotments & Occupancy Tracking</div>
                    </div>
                    <div class="meta">
                        <b>Date:</b> $today<br>
                        <b>Generated:</b> $timestamp
                    </div>
                </div>

                <div class="kpi-container" style="flex-wrap: wrap;">
                    <div class="kpi-box">
                        <div class="kpi-val">${stats.totalRooms}</div>
                        <div class="kpi-lbl">Total Rooms</div>
                    </div>
                    <div class="kpi-box">
                        <div class="kpi-val">${stats.totalBeds}</div>
                        <div class="kpi-lbl">Bed Capacity</div>
                    </div>
                    <div class="kpi-box">
                        <div class="kpi-val" style="color:#d97706;">${stats.occupiedBeds}</div>
                        <div class="kpi-lbl">Booked Beds</div>
                    </div>
                    <div class="kpi-box">
                        <div class="kpi-val" style="color:#059669;">${stats.availableBeds}</div>
                        <div class="kpi-lbl">Available Beds</div>
                    </div>
                    <div class="kpi-box">
                        <div class="kpi-val" style="color:#0f3d5e;">${stats.occupancyPercentage}%</div>
                        <div class="kpi-lbl">Occupancy Rate</div>
                    </div>
                    <div class="kpi-box">
                        <div class="kpi-val" style="color:#15803d;">${stats.activeEmployeesCount}</div>
                        <div class="kpi-lbl">Active Staff</div>
                    </div>
                    <div class="kpi-box">
                        <div class="kpi-val" style="color:#d97706;">${stats.onVacationEmployeesCount}</div>
                        <div class="kpi-lbl">On Vacation</div>
                    </div>
                    <div class="kpi-box">
                        <div class="kpi-val" style="color:#0f3d5e;">${stats.totalEmployeesCount}</div>
                        <div class="kpi-lbl">Total Staff</div>
                    </div>
                </div>

                <h2>1. Booked Rooms & Resident Details (${occupiedRooms.size} Rooms)</h2>
                <table>
                    <thead>
                        <tr>
                            <th style="width: 14%; text-align:center;">Room No.</th>
                            <th style="width: 14%;">Camp Block</th>
                            <th style="width: 14%; text-align:center;">Status</th>
                            <th style="width: 29%;">Bed 1 (Bed A) Resident</th>
                            <th style="width: 29%;">Bed 2 (Bed B) Resident</th>
                        </tr>
                    </thead>
                    <tbody>
                        $occupiedRowsHtml
                    </tbody>
                </table>

                <h2>2. Empty Rooms & Available Capacity (${vacantRooms.size} Rooms)</h2>
                <table>
                    <thead>
                        <tr>
                            <th style="width: 15%; text-align:center;">Room No.</th>
                            <th style="width: 20%;">Camp Block</th>
                            <th style="width: 25%; text-align:center;">Status</th>
                            <th style="width: 40%;">Capacity Note</th>
                        </tr>
                    </thead>
                    <tbody>
                        $vacantRowsHtml
                    </tbody>
                </table>

                <div class="footer">
                    <div>AWAZEL CO. • CampTrack Accommodation Management • Confidential</div>
                    <div>
                        <div class="sign-box">Camp Supervisor Signature</div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun generatePlainTextReport(uiState: CampUiState): String {
        val stats = uiState.stats
        val today = getTodayDate()
        val timestamp = getCurrentTimestamp()
        val occupiedRooms = uiState.rooms.filter { it.occupiedBeds > 0 }
        val vacantRooms = uiState.rooms.filter { it.isVacant }

        val sb = StringBuilder()
        sb.appendLine("=========================================================")
        sb.appendLine("    AWAZEL CO. - CAMP ACCOMMODATION DAILY REPORT         ")
        sb.appendLine("=========================================================")
        sb.appendLine("Date: $today | Generated at: $timestamp")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("EXECUTIVE SUMMARY:")
        sb.appendLine("• Total Camp Rooms:    ${stats.totalRooms} (2 Beds Each)")
        sb.appendLine("• Total Bed Capacity:  ${stats.totalBeds} Beds")
        sb.appendLine("• Booked Beds:         ${stats.occupiedBeds} (${stats.occupancyPercentage}%)")
        sb.appendLine("• Available Free Beds: ${stats.availableBeds}")
        sb.appendLine("• Fully Booked Rooms:  ${stats.fullRoomsCount}")
        sb.appendLine("• 1-Bed Free Rooms:    ${stats.partialRoomsCount}")
        sb.appendLine("• Completely Empty:    ${stats.vacantRoomsCount} Rooms")
        sb.appendLine("• Active In-Camp Staff:${stats.activeEmployeesCount} Residents")
        sb.appendLine("• Staff On Vacation:   ${stats.onVacationEmployeesCount} Employees (Beds Reserved)")
        sb.appendLine("• Total Staff Roster:  ${stats.totalEmployeesCount} Employees")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("1. BOOKED ROOMS & RESIDENT ROSTER (${occupiedRooms.size} Rooms):")
        sb.appendLine("---------------------------------------------------------")

        occupiedRooms.forEach { r ->
            sb.appendLine("Room ${r.room.roomNumber} [${r.room.block}] - ${r.occupancyStatusText}:")
            if (r.bed1Occupant != null) {
                val b = r.bed1Occupant
                val statusTag = if (b.status == EmployeeStatus.ON_VACATION) " [ON VACATION]" else ""
                sb.appendLine("  -> Bed 1: ${b.name}$statusTag | Iqama: ${b.iqamaNumber} | Card: ${b.cardNumber} | Trade: ${b.trade} | In: ${b.dateIn}")
            } else {
                sb.appendLine("  -> Bed 1: [EMPTY]")
            }
            if (r.bed2Occupant != null) {
                val b = r.bed2Occupant
                val statusTag = if (b.status == EmployeeStatus.ON_VACATION) " [ON VACATION]" else ""
                sb.appendLine("  -> Bed 2: ${b.name}$statusTag | Iqama: ${b.iqamaNumber} | Card: ${b.cardNumber} | Trade: ${b.trade} | In: ${b.dateIn}")
            } else {
                sb.appendLine("  -> Bed 2: [EMPTY]")
            }
            sb.appendLine()
        }

        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("2. EMPTY ROOMS AVAILABLE FOR ALLOTMENT (${vacantRooms.size} Rooms):")
        sb.appendLine("---------------------------------------------------------")
        vacantRooms.forEach { r ->
            sb.appendLine("• Room ${r.room.roomNumber} (${r.room.block}) - 2 Beds Available")
        }
        sb.appendLine()
        sb.appendLine("=========================================================")
        sb.appendLine("Camp Supervisor Signature: ______________________________")
        return sb.toString()
    }

    fun printReport(context: Context, htmlContent: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Printing service not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter("Camp_Daily_Occupancy_Report")
                val jobName = "Camp_Occupancy_Report_${System.currentTimeMillis()}"
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun shareReport(context: Context, reportText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Daily Camp Occupancy Report - ${getTodayDate()}")
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Daily Occupancy Report")
        context.startActivity(shareIntent)
    }

    fun copyReport(context: Context, reportText: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Daily Camp Occupancy Report", reportText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
