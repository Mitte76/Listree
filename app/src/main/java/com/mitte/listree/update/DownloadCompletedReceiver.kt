package com.mitte.listree.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri

class DownloadCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            return
        }

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) {
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (uriIndex != -1) {
                    val downloadedFileUri = cursor.getString(uriIndex).toUri()
                    installApk(context, downloadedFileUri)
                }
            }
        }
        cursor.close()
    }

    private fun installApk(context: Context, downloadedFileUri: Uri) {
        try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(downloadedFileUri)?.use { inputStream ->
                val file = File(context.cacheDir, "update.apk")
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                val apkUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
            }
        } catch (_: Exception) {
            // Installation failed
        }
    }
}
