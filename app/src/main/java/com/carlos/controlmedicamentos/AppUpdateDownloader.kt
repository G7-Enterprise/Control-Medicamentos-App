package com.carlos.controlmedicamentos

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object AppUpdateDownloader {
    private const val PREFS_NAME = "app_update_download"
    private const val KEY_DOWNLOAD_ID = "download_id"

    fun enqueue(context: Context, url: String): Long {
        require(url.startsWith("https://")) { "La URL de actualización debe usar HTTPS" }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Control de Medicamentos")
            .setDescription("Descargando actualización")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "control-medicamentos-update.apk"
            )

        val downloadManager = context.getSystemService(DownloadManager::class.java)
        val downloadId = downloadManager.enqueue(request)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .apply()
        return downloadId
    }

    fun isTrackedDownload(context: Context, downloadId: Long): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_DOWNLOAD_ID, -1L) == downloadId
}
