package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QrCodeGenerator {

    const val REGISTRATION_DEEP_LINK = "camptrack://register"
    const val REGISTRATION_WEB_URL = "https://camptrack.awazel.com/register"

    fun generateEmployeeAllotmentUrl(employeeId: String, iqama: String): String {
        return "camptrack://allot?empId=$employeeId&iqama=$iqama"
    }

    /**
     * Generates a crisp QR Code Bitmap from the provided content string.
     */
    fun generateQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Saves QR code bitmap to Downloads / Pictures folder and returns DownloadedFileInfo
     * with accessible content URI for sharing and viewing.
     */
    fun saveQrCodeToDownloads(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "CampTrack_Employee_Registration_QR.png"
    ): Result<DownloadedFileInfo> {
        return try {
            val localDir = File(context.cacheDir, "qr_codes").apply { if (!exists()) mkdirs() }
            val localFile = File(localDir, fileName)
            FileOutputStream(localFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
            } catch (_: Exception) {
                Uri.fromFile(localFile)
            }

            // Save to Public MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val publicUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (publicUri != null) {
                        resolver.openOutputStream(publicUri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("QrCodeGenerator", "Public download MediaStore insert failed: ${e.message}")
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!pubDir.exists()) pubDir.mkdirs()
                    val pubFile = File(pubDir, fileName)
                    FileOutputStream(pubFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("QrCodeGenerator", "Public downloads fallback failed: ${e.message}")
                }
            }

            Result.success(DownloadedFileInfo(fileName, contentUri, localFile, "image/png"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareQrCode(context: Context, info: DownloadedFileInfo) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, info.uri)
                putExtra(Intent.EXTRA_SUBJECT, "CampTrack Awazel - Employee Registration QR Code")
                putExtra(Intent.EXTRA_TEXT, "Scan this QR code or open link to register: ${REGISTRATION_WEB_URL}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Employee Registration QR").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("QrCodeGenerator", "Share QR failed: ${e.message}")
        }
    }

    fun shareQrCode(
        context: Context,
        bitmap: Bitmap,
        title: String = "Awazel Employee Registration QR",
        registrationUrl: String = REGISTRATION_WEB_URL
    ) {
        val saveResult = saveQrCodeToDownloads(context, bitmap, "CampTrack_Awazel_Employee_Registration_QR.png")
        saveResult.fold(
            onSuccess = { info ->
                shareQrCode(context, info)
            },
            onFailure = {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, "Awazel Employee Registration: $registrationUrl")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, title).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e: Exception) {
                    android.util.Log.e("QrCodeGenerator", "Share fallback failed: ${e.message}")
                }
            }
        )
    }
}
