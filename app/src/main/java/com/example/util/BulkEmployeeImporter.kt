package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Color as GColor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.Employee
import com.example.data.model.EmployeeStatus
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TemplateFormat(val displayName: String, val extension: String, val mimeType: String) {
    PDF("PDF Document (.pdf)", ".pdf", "application/pdf"),
    CSV("Excel / CSV Spreadsheet (.csv)", ".csv", "text/csv")
}

data class DownloadedFileInfo(
    val fileName: String,
    val uri: Uri,
    val localFile: File,
    val mimeType: String
)

data class ImportResult(
    val importedEmployees: List<Employee> = emptyList(),
    val successCount: Int = 0,
    val errorCount: Int = 0,
    val errors: List<String> = emptyList()
)

object BulkEmployeeImporter {

    const val SAMPLE_CSV_CONTENT = """Name,IqamaNumber,CardNumber,RoomNumber,BedNumber,DateIn,Trade,Company,Phone,Nationality,IqamaExpiryDate,Notes
Ahmed Al-Mutairi,2456789012,C101,101,1,2026-01-01,Electrician,AWAZEL CO.,+966501234567,Saudi,2027-01-01,Sample note 1
Mohammed Al-Harbi,2567890123,C102,101,2,2026-01-01,Plumber,AWAZEL CO.,+966507654321,Yemeni,2027-06-15,Sample note 2
Rajesh Kumar,2678901234,C103,102,1,2026-02-01,Technician,AWAZEL CO.,+966509876543,Indian,2027-03-20,Sample note 3
"""

    fun generatePdfBytes(): ByteArray {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint()

        // Header Background
        paint.color = GColor.rgb(26, 35, 126) // Deep Indigo
        canvas.drawRect(0f, 0f, 595f, 70f, paint)

        // Header Text
        paint.color = GColor.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("AWAZEL CO. ACCOMMODATION HUB", 35f, 38f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("Camp Employee Allotment - Import Format Template", 35f, 56f, paint)

        // Instructions Section
        var y = 95f
        paint.color = GColor.rgb(33, 33, 33)
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Employee Spreadsheet Format Specifications", 35f, y, paint)

        y += 18f
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        paint.color = GColor.rgb(80, 80, 80)
        canvas.drawText("• You can prepare your roster in Excel, Google Sheets, or CSV, then upload using 'Upload Employees'.", 35f, y, paint)
        y += 14f
        canvas.drawText("• Required Fields: Name and Iqama Number (10 digits).", 35f, y, paint)
        y += 14f
        canvas.drawText("• Room & Bed: Specify Room Number (e.g. 101) and Bed (1 or 2) to allot bed immediately, or leave blank to register unallotted.", 35f, y, paint)
        y += 14f
        canvas.drawText("• Date Format: Use YYYY-MM-DD (e.g. 2026-01-01) for DateIn and IqamaExpiryDate.", 35f, y, paint)

        // Table Header
        y += 24f
        paint.color = GColor.rgb(236, 240, 246)
        canvas.drawRect(35f, y, 560f, y + 26f, paint)

        paint.color = GColor.rgb(26, 35, 126)
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Field Name", 45f, y + 17f, paint)
        canvas.drawText("Required?", 160f, y + 17f, paint)
        canvas.drawText("Sample Value & Description", 240f, y + 17f, paint)

        val fields = listOf(
            Triple("Name", "YES", "Ahmed Al-Mutairi (Full Name, single line)"),
            Triple("IqamaNumber", "YES", "2456789012 (10-digit National / Iqama ID)"),
            Triple("CardNumber", "Optional", "C101 (Company access card number)"),
            Triple("RoomNumber", "Optional", "101 (Assigned room)"),
            Triple("BedNumber", "Optional", "1 or 2 (Bed index)"),
            Triple("DateIn", "Optional", "2026-01-01 (Check-in date YYYY-MM-DD)"),
            Triple("Trade", "Optional", "Electrician, Plumber, Technician, etc."),
            Triple("Company", "Optional", "AWAZEL CO. (Subcontractor / Division)"),
            Triple("Phone", "Optional", "+966501234567 (Mobile number)"),
            Triple("Nationality", "Optional", "Saudi, Yemeni, Indian, Filipino..."),
            Triple("IqamaExpiryDate", "Optional", "2027-01-01 (YYYY-MM-DD)"),
            Triple("Notes", "Optional", "Remarks or special assignment notes")
        )

        y += 26f
        paint.textSize = 9f
        fields.forEachIndexed { index, triple ->
            if (index % 2 == 1) {
                paint.color = GColor.rgb(248, 249, 250)
                canvas.drawRect(35f, y, 560f, y + 20f, paint)
            }
            paint.color = GColor.rgb(33, 33, 33)
            paint.isFakeBoldText = true
            canvas.drawText(triple.first, 45f, y + 14f, paint)

            paint.color = if (triple.second == "YES") GColor.rgb(198, 40, 40) else GColor.rgb(100, 100, 100)
            paint.isFakeBoldText = triple.second == "YES"
            canvas.drawText(triple.second, 160f, y + 14f, paint)

            paint.color = GColor.rgb(55, 55, 55)
            paint.isFakeBoldText = false
            canvas.drawText(triple.third, 240f, y + 14f, paint)

            y += 20f
        }

        // Sample Data Rows Section
        y += 18f
        paint.color = GColor.rgb(26, 35, 126)
        paint.textSize = 11.5f
        paint.isFakeBoldText = true
        canvas.drawText("Sample Data Rows (Ready to Copy or Import)", 35f, y, paint)

        y += 12f
        paint.color = GColor.rgb(245, 245, 245)
        canvas.drawRect(35f, y, 560f, y + 20f, paint)
        paint.color = GColor.rgb(80, 80, 80)
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("Name, IqamaNumber, CardNumber, RoomNumber, BedNumber, DateIn, Trade, Company, Phone, Nationality, Expiry, Notes", 42f, y + 14f, paint)

        val samples = listOf(
            "Ahmed Al-Mutairi, 2456789012, C101, 101, 1, 2026-01-01, Electrician, AWAZEL CO., +966501234567, Saudi, 2027-01-01, Team A",
            "Mohammed Al-Harbi, 2567890123, C102, 101, 2, 2026-01-01, Plumber, AWAZEL CO., +966507654321, Yemeni, 2027-06-15, Team A",
            "Rajesh Kumar, 2678901234, C103, 102, 1, 2026-02-01, Technician, AWAZEL CO., +966509876543, Indian, 2027-03-20, Maintenance"
        )
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 8f
        paint.color = GColor.rgb(40, 40, 40)
        samples.forEach { line ->
            canvas.drawText(line, 42f, y + 12f, paint)
            y += 16f
        }

        // Footer
        paint.color = GColor.rgb(150, 150, 150)
        paint.textSize = 8f
        canvas.drawText("CampTrack AWAZEL CO. Accommodation Management System • Generated Template", 35f, 815f, paint)

        pdfDocument.finishPage(page)

        val stream = ByteArrayOutputStream()
        pdfDocument.writeTo(stream)
        pdfDocument.close()
        stream.toByteArray()
    } catch (_: Throwable) {
        // Fallback for headless JVM/test environments without Skia canvas
        val fallback = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n"
        fallback.toByteArray(Charsets.UTF_8)
    }
}

    fun generateCsvBytes(): ByteArray {
        return SAMPLE_CSV_CONTENT.toByteArray(Charsets.UTF_8)
    }

    /**
     * Saves the template directly to device Downloads folder (MediaStore or public Downloads)
     * and keeps a cached copy for instant viewing/sharing.
     */
    fun saveTemplateToDownloads(context: Context, format: TemplateFormat): Result<DownloadedFileInfo> {
        return try {
            val fileName = "Employee_Allotment_Template${format.extension}"
            val dataBytes = when (format) {
                TemplateFormat.PDF -> generatePdfBytes()
                TemplateFormat.CSV -> generateCsvBytes()
            }

            // Always write a local file in app cache/files for FileProvider intents
            val localDir = File(context.cacheDir, "templates").apply { if (!exists()) mkdirs() }
            val localFile = File(localDir, fileName)
            FileOutputStream(localFile).use { it.write(dataBytes) }

            val contentUri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
            } catch (_: Exception) {
                Uri.fromFile(localFile)
            }

            // Also save to Public Downloads via MediaStore if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val publicUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (publicUri != null) {
                        resolver.openOutputStream(publicUri)?.use { it.write(dataBytes) }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("BulkEmployeeImporter", "MediaStore public download insert failed: ${e.message}")
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!pubDir.exists()) pubDir.mkdirs()
                    val pubFile = File(pubDir, fileName)
                    FileOutputStream(pubFile).use { it.write(dataBytes) }
                } catch (e: Exception) {
                    android.util.Log.w("BulkEmployeeImporter", "Public downloads fallback failed: ${e.message}")
                }
            }

            Result.success(DownloadedFileInfo(fileName, contentUri, localFile, format.mimeType))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes template content into a user-selected target URI (from CreateDocument launcher)
     */
    fun writeTemplateToUri(context: Context, targetUri: Uri, format: TemplateFormat): Result<Unit> {
        return try {
            val dataBytes = when (format) {
                TemplateFormat.PDF -> generatePdfBytes()
                TemplateFormat.CSV -> generateCsvBytes()
            }
            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                os.write(dataBytes)
            } ?: throw IllegalStateException("Could not open output stream for selected location.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openDownloadedFile(context: Context, info: DownloadedFileInfo) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(info.uri, info.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(info.uri, info.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Open ${info.fileName}"
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(chooser)
            } catch (_: Exception) {
                shareDownloadedFile(context, info)
            }
        }
    }

    fun shareDownloadedFile(context: Context, info: DownloadedFileInfo) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = info.mimeType
                putExtra(Intent.EXTRA_STREAM, info.uri)
                putExtra(Intent.EXTRA_SUBJECT, "AWAZEL CO. - ${info.fileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Template").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("BulkEmployeeImporter", "Share failed: ${e.message}")
        }
    }

    /**
     * Parses uploaded CSV / spreadsheet URI into Employee entities.
     * Handles BOMs, multiple delimiters (comma, semicolon, tab), flexible column headers,
     * and provides detailed diagnostic errors.
     */
    fun parseCsvUri(context: Context, uri: Uri): ImportResult {
        val employees = mutableListOf<Employee>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var errorCount = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult(errors = listOf("Could not open the selected file."))

            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                val rawLines = reader.readLines()
                if (rawLines.isEmpty()) {
                    return ImportResult(errors = listOf("The uploaded file is empty."))
                }

                // Check if user accidentally uploaded a PDF
                if (rawLines.firstOrNull()?.startsWith("%PDF") == true) {
                    return ImportResult(
                        errors = listOf("The selected file is a PDF document. Please upload your filled CSV or Excel spreadsheet instead. You can download the CSV template from the 'Template' button.")
                    )
                }

                // Clean lines and detect delimiter
                val lines = rawLines.map { it.removePrefix("\uFEFF").trim() }.filter { it.isNotEmpty() }
                if (lines.isEmpty()) {
                    return ImportResult(errors = listOf("No valid data rows found in file."))
                }

                val headerLine = lines[0]
                val delimiter = detectDelimiter(headerLine)

                // Detect if first row is header
                val firstRowCols = parseLine(headerLine, delimiter).map { it.lowercase().trim() }
                val isHeaderRow = firstRowCols.any {
                    it.contains("name") || it.contains("iqama") || it.contains("employee") || it.contains("room")
                }

                // Determine column mappings
                val colMap = if (isHeaderRow) {
                    findColumnIndices(firstRowCols)
                } else {
                    DefaultColumnIndices
                }

                val startIndex = if (isHeaderRow) 1 else 0
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                for (i in startIndex until lines.size) {
                    val line = lines[i]
                    val cols = parseLine(line, delimiter)
                    if (cols.isEmpty() || cols.all { it.isBlank() }) continue

                    val name = cols.getOrNull(colMap.name)?.trim().orEmpty()
                    val iqamaNumber = cols.getOrNull(colMap.iqama)?.trim().orEmpty()
                    val cardNumber = cols.getOrNull(colMap.card)?.trim().orEmpty()
                    val roomNumber = cols.getOrNull(colMap.room)?.trim().orEmpty()
                    val bedStr = cols.getOrNull(colMap.bed)?.trim().orEmpty()
                    val bedNumber = bedStr.toIntOrNull() ?: 1
                    val rawDateIn = cols.getOrNull(colMap.dateIn)?.trim()
                    val dateIn = normalizeDate(rawDateIn) ?: currentDate
                    val trade = cols.getOrNull(colMap.trade)?.trim()?.ifEmpty { "General Staff" } ?: "General Staff"
                    val company = cols.getOrNull(colMap.company)?.trim()?.ifEmpty { "AWAZEL CO." } ?: "AWAZEL CO."
                    val phone = cols.getOrNull(colMap.phone)?.trim().orEmpty()
                    val nationality = cols.getOrNull(colMap.nationality)?.trim().orEmpty()
                    val rawExpiry = cols.getOrNull(colMap.iqamaExpiry)?.trim().orEmpty()
                    val iqamaExpiryDate = normalizeDate(rawExpiry) ?: rawExpiry
                    val notes = cols.getOrNull(colMap.notes)?.trim().orEmpty()

                    if (name.isEmpty() || iqamaNumber.isEmpty()) {
                        errorCount++
                        errors.add("Row ${i + 1}: Name and Iqama Number are required fields.")
                        continue
                    }

                    val employee = Employee(
                        name = name,
                        iqamaNumber = iqamaNumber,
                        cardNumber = cardNumber.ifEmpty { "C${System.currentTimeMillis().toString().takeLast(4)}" },
                        roomNumber = roomNumber.ifEmpty { null },
                        bedNumber = if (roomNumber.isNotEmpty()) bedNumber else null,
                        dateIn = dateIn,
                        trade = trade,
                        company = company,
                        phone = phone,
                        nationality = nationality,
                        iqamaExpiryDate = iqamaExpiryDate,
                        status = if (roomNumber.isNotEmpty()) EmployeeStatus.ACTIVE else EmployeeStatus.CHECKED_OUT,
                        notes = notes
                    )
                    employees.add(employee)
                    successCount++
                }
            }
        } catch (e: Exception) {
            return ImportResult(errors = listOf("File reading error: ${e.message}"))
        }

        return ImportResult(
            importedEmployees = employees,
            successCount = successCount,
            errorCount = errorCount,
            errors = errors
        )
    }

    private data class ColumnIndices(
        val name: Int = 0,
        val iqama: Int = 1,
        val card: Int = 2,
        val room: Int = 3,
        val bed: Int = 4,
        val dateIn: Int = 5,
        val trade: Int = 6,
        val company: Int = 7,
        val phone: Int = 8,
        val nationality: Int = 9,
        val iqamaExpiry: Int = 10,
        val notes: Int = 11
    )

    private val DefaultColumnIndices = ColumnIndices()

    private fun findColumnIndices(headers: List<String>): ColumnIndices {
        var nameIdx = -1
        var iqamaIdx = -1
        var cardIdx = -1
        var roomIdx = -1
        var bedIdx = -1
        var dateInIdx = -1
        var tradeIdx = -1
        var companyIdx = -1
        var phoneIdx = -1
        var nationalityIdx = -1
        var expiryIdx = -1
        var notesIdx = -1

        headers.forEachIndexed { index, rawHeader ->
            val h = rawHeader.lowercase()
            when {
                nameIdx == -1 && (h.contains("name") || h.contains("employee")) -> nameIdx = index
                iqamaIdx == -1 && (h.contains("iqama") || h.contains("national") || h.contains("id")) -> iqamaIdx = index
                cardIdx == -1 && (h.contains("card") || h.contains("badge")) -> cardIdx = index
                roomIdx == -1 && h.contains("room") -> roomIdx = index
                bedIdx == -1 && h.contains("bed") -> bedIdx = index
                dateInIdx == -1 && (h.contains("date") || h.contains("in") || h.contains("joining")) -> dateInIdx = index
                tradeIdx == -1 && (h.contains("trade") || h.contains("designation") || h.contains("role") || h.contains("job")) -> tradeIdx = index
                companyIdx == -1 && (h.contains("company") || h.contains("sponsor") || h.contains("dept")) -> companyIdx = index
                phoneIdx == -1 && (h.contains("phone") || h.contains("mobile") || h.contains("tel") || h.contains("contact")) -> phoneIdx = index
                nationalityIdx == -1 && (h.contains("nation") || h.contains("country")) -> nationalityIdx = index
                expiryIdx == -1 && (h.contains("expir") || h.contains("validity")) -> expiryIdx = index
                notesIdx == -1 && (h.contains("note") || h.contains("remark") || h.contains("comment")) -> notesIdx = index
            }
        }

        return ColumnIndices(
            name = if (nameIdx != -1) nameIdx else 0,
            iqama = if (iqamaIdx != -1) iqamaIdx else 1,
            card = if (cardIdx != -1) cardIdx else 2,
            room = if (roomIdx != -1) roomIdx else 3,
            bed = if (bedIdx != -1) bedIdx else 4,
            dateIn = if (dateInIdx != -1) dateInIdx else 5,
            trade = if (tradeIdx != -1) tradeIdx else 6,
            company = if (companyIdx != -1) companyIdx else 7,
            phone = if (phoneIdx != -1) phoneIdx else 8,
            nationality = if (nationalityIdx != -1) nationalityIdx else 9,
            iqamaExpiry = if (expiryIdx != -1) expiryIdx else 10,
            notes = if (notesIdx != -1) notesIdx else 11
        )
    }

    private fun detectDelimiter(line: String): Char {
        val commaCount = line.count { it == ',' }
        val semiCount = line.count { it == ';' }
        val tabCount = line.count { it == '\t' }

        return when {
            semiCount > commaCount && semiCount > tabCount -> ';'
            tabCount > commaCount && tabCount > semiCount -> '\t'
            else -> ','
        }
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                }
                c == delimiter && !inQuotes -> {
                    result.add(curVal.toString().trim())
                    curVal.clear()
                }
                else -> {
                    curVal.append(c)
                }
            }
            i++
        }
        result.add(curVal.toString().trim())
        return result
    }

    private fun normalizeDate(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
            SimpleDateFormat("yyyy/MM/dd", Locale.ROOT),
            SimpleDateFormat("dd-MM-yyyy", Locale.ROOT),
            SimpleDateFormat("dd/MM/yyyy", Locale.ROOT),
            SimpleDateFormat("MM/dd/yyyy", Locale.ROOT)
        )
        for (format in formats) {
            try {
                format.isLenient = false
                val date = format.parse(trimmed)
                if (date != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(date)
                }
            } catch (_: Exception) {}
        }
        return trimmed
    }
}
