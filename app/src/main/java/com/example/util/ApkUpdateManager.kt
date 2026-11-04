package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

import android.provider.Settings

object ApkUpdateManager {

    private const val TAG = "ApkUpdateManager"

    fun getApkFile(context: Context, versionName: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        return File(dir, "CampTrack_v$versionName.apk")
    }

    fun isApkDownloaded(context: Context, versionName: String): Boolean {
        val file = getApkFile(context, versionName)
        return file.exists() && file.length() > 0
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open install permission settings: ${e.message}", e)
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val urlString = updateInfo.downloadUrl.trim()
        if (urlString.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Download URL is empty. Please set a valid APK URL in the Admin Panel."))
        }

        val destinationFile = getApkFile(context, updateInfo.latestVersionName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("Server returned HTTP $responseCode: ${connection.responseMessage}"))
            }

            val fileLength = connection.contentLength
            inputStream = connection.inputStream
            outputStream = FileOutputStream(destinationFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            var lastReportedProgress = 0

            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    if (progress != lastReportedProgress) {
                        lastReportedProgress = progress
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }
                outputStream.write(data, 0, count)
            }
            outputStream.flush()

            withContext(Dispatchers.Main) {
                onProgress(100)
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download APK update: ${e.message}", e)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            Result.failure(e)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("Downloaded APK file not found at ${apkFile.absolutePath}"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                openInstallPermissionSettings(context)
                return Result.failure(SecurityException("Please allow 'Install unknown apps' permission for CampTrack to proceed with update."))
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prompt APK installation: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun openBrowserDownload(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser for APK download: ${e.message}", e)
        }
    }
}
