package com.mitte.listree.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

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
            if (statusIndex == -1) {
                cursor.close()
                return
            }
            val status = cursor.getInt(statusIndex)

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val localFilenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)
                if (localFilenameIndex == -1) {
                    cursor.close()
                    return
                }
                val localFilename = cursor.getString(localFilenameIndex)

                if (localFilename != null) {
                    val apkFile = File(localFilename)
                    val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            context,
                            context.packageName + ".provider",
                            apkFile
                        )
                    } else {
                        Uri.fromFile(apkFile)
                    }

                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(installIntent)
                }
            }
        }
        cursor.close()
    }
}
